package chainless.db

import cats.data.OptionT
import cats.effect.{Async, Resource}
import cats.effect.implicits.*
import cats.implicits.*
import chainless.models.*
import fs2.{Chunk, Stream}
import fs2.io.file.{Files, Path}
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.nio.charset.StandardCharsets

/** Stores byte data in a directory structure. Data is stored in "revisions", meaning if a key already exists, a new
  * revision is added (and marked "latest") instead of deleting the old entry.
  * @param baseDir
  *   a base directory in which "objects" of data are saved
  * @param name
  *   the name of this store (for logging)
  */
class ObjectStore[F[_]: Async: Files](baseDir: Path, name: String):

  private given Logger[F] = Slf4jLogger.getLoggerFromName(s"ObjectStore-$name")

  def save(id: String)(data: Stream[F, Byte]): F[Unit] =
    Logger[F].info(s"Saving id=$id") >>
      (baseDir / id)
        .pure[F]
        .flatTap(Files[F].createDirectories(_))
        .flatMap(objectDir =>
          OptionT(contains(objectDir))
            .fold(0)(_ + 1)
            .map(nextRevision => objectDir / nextRevision.toString)
            .flatMap(file => Files[F].createFile(file) *> data.through(Files[F].writeAll(file)).compile.drain)
        ) >>
      Logger[F].info(s"Finished saving id=$id")

  def get(id: String): Stream[F, Byte] =
    Stream
      .eval(
        OptionT
          .pure[F](baseDir / id)
          .flatMap(objectDir =>
            OptionT(contains(objectDir))
              .map(revision => objectDir / revision.toString)
          )
          .getOrRaise(new NoSuchElementException("No data"))
      )
      .flatMap(Files[F].readAll)

  def exists(id: String): F[Boolean] =
    OptionT
      .pure[F](baseDir / id)
      .flatMap(objectDir => OptionT(contains(objectDir)))
      .isDefined

  def delete(id: String): F[Boolean] =
    exists(id)
      .flatTap(Async[F].whenA(_)(Files[F].deleteRecursively(baseDir / id)))

  private def contains(objectDir: Path): F[Option[Int]] =
    Files[F]
      .list(objectDir)
      .evalFilter(Files[F].isRegularFile(_))
      .map(_.fileName.toString.toIntOption)
      .collect { case Some(i) => i }
      .compile
      .fold(-1)(_.max(_))
      .map(_.some.filter(_ >= 0))

object ObjectStore:
  def make[F[_]: Async: Files](baseDir: Path, name: String): Resource[F, ObjectStore[F]] =
    Files[F].createDirectories(baseDir).toResource.as(new ObjectStore[F](baseDir, name))

class BlocksStore[F[_]: Async](objectStore: ObjectStore[F]):
  def saveBlock(block: BlockWithChain): F[Unit] =
    objectStore.save(s"${block.meta.chain.name}/${block.meta.blockId}")(
      Stream(block.block)
        .map(_.noSpaces)
        .through(fs2.text.utf8.encode)
    )

  def getBlock(meta: BlockMeta): F[BlockWithChain] =
    objectStore
      .get(s"${meta.chain.name}/${meta.blockId}")
      .compile
      .to(Chunk)
      .map(chunk => new String(chunk.toArray[Byte], StandardCharsets.UTF_8))
      .map(io.circe.parser.parse)
      .rethrow
      .map(BlockWithChain(meta, _))

class FunctionsStore[F[_]: Async](objectStore: ObjectStore[F]):
  def get(id: String)(revision: Int): Stream[F, Byte] =
    objectStore.get(s"$id/$revision")

  def delete(id: String): F[Boolean] =
    objectStore.delete(id)

  def save(id: String, revision: Int)(data: Stream[F, Byte]): F[Unit] =
    objectStore.save(s"$id/$revision")(data)
