package chainless.db

import cats.data.OptionT
import cats.effect.Async
import cats.implicits.*
import fs2.Stream
import fs2.io.file.{Files, Path}
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

/** Stores byte data in a directory structure. Data is stored in "revisions", meaning if a key already exists, a new
  * revision is added (and marked "latest") instead of deleting the old entry.
  * @param baseDir
  *   a base directory in which "buckets" of data are saved
  */
class ObjectStore[F[_]: Async: Files](baseDir: Path):

  private given Logger[F] = Slf4jLogger.getLoggerFromName("ObjectStore")

  def save(bucket: String)(id: String)(data: Stream[F, Byte]): F[Unit] =
    Logger[F].info(s"Saving id=$id") >>
      (baseDir / bucket / id)
        .pure[F]
        .flatTap(Files[F].createDirectories(_))
        .flatMap(objectDir =>
          OptionT(contains(objectDir))
            .fold(0)(_ + 1)
            .map(nextRevision => objectDir / nextRevision.toString)
            .flatMap(file => Files[F].createFile(file) *> data.through(Files[F].writeAll(file)).compile.drain)
        ) >>
      Logger[F].info(s"Finished saving id=$id")

  def get(bucket: String)(id: String): Stream[F, Byte] =
    Stream
      .eval(
        OptionT
          .pure[F](baseDir / bucket / id)
          .flatMap(objectDir =>
            OptionT(contains(objectDir))
              .map(revision => objectDir / revision.toString)
          )
          .getOrRaise(new NoSuchElementException("No data"))
      )
      .flatMap(Files[F].readAll)

  def exists(bucket: String)(id: String): F[Boolean] =
    OptionT
      .pure[F](baseDir / bucket / id)
      .flatMap(objectDir => OptionT(contains(objectDir)))
      .isDefined

  def delete(bucket: String)(id: String): F[Boolean] =
    exists(bucket)(id)
      .flatTap(Async[F].whenA(_)(Files[F].deleteRecursively(baseDir / bucket / id)))

  private def contains(objectDir: Path): F[Option[Int]] =
    Files[F]
      .list(objectDir)
      .evalFilter(Files[F].isRegularFile(_))
      .map(_.fileName.toString.toIntOption)
      .collect { case Some(i) => i }
      .compile
      .fold(-1)(_.max(_))
      .map(_.some.filter(_ >= 0))
