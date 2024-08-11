package chainless.db

import cats.effect.Async
import cats.implicits.*
import fs2.Stream
import fs2.io.file.{Files, Path}

trait FunctionsStore[F[_]]:
  def get(id: String)(revision: Int): Stream[F, Byte]
  def delete(id: String): F[Unit]
  def save(id: String, revision: Int)(data: Stream[F, Byte]): F[Unit]

class DirFunctionsStore[F[_]: Async](baseDir: Path) extends FunctionsStore[F]:
  def get(id: String)(revision: Int): Stream[F, Byte] =
    Files[F].readAll(baseDir / id / revision.toString)

  def delete(id: String): F[Unit] =
    Files[F].deleteRecursively(baseDir / id)

  def save(id: String, revision: Int)(data: Stream[F, Byte]): F[Unit] =
    Files[F].createDirectories(baseDir / id) >> data
      .through(Files[F].writeAll(baseDir / id / revision.toString))
      .compile
      .drain
