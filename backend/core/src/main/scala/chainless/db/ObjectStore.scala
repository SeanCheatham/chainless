package chainless.db

import cats.effect.Async
import cats.effect.implicits.*
import cats.implicits.*
import chainless.models.*
import fs2.io.file.{Files, Path}
import fs2.{Chunk, Stream}

import java.nio.charset.StandardCharsets

class BlocksStore[F[_]: Async](baseDir: Path):
  def saveBlock(block: BlockWithChain): F[Unit] =
    Files[F].createDirectories(baseDir / block.meta.chain.name) >>
      Stream(block.block)
        .map(_.noSpaces)
        .through(fs2.text.utf8.encode)
        .through(Files[F].writeAll(baseDir / block.meta.chain.name / block.meta.blockId))
        .compile
        .drain

  def getBlock(meta: BlockMeta): F[BlockWithChain] =
    Files[F]
      .readAll(baseDir / meta.chain.name / meta.blockId)
      .compile
      .to(Chunk)
      .map(chunk => new String(chunk.toArray[Byte], StandardCharsets.UTF_8))
      .map(io.circe.parser.parse)
      .rethrow
      .map(BlockWithChain(meta, _))

class FunctionsStore[F[_]: Async](baseDir: Path):
  def get(id: String)(revision: Int): Stream[F, Byte] =
    Files[F].readAll(baseDir / id / revision.toString)

  def delete(id: String): F[Unit] =
    Files[F].deleteRecursively(baseDir / id)

  def save(id: String, revision: Int)(data: Stream[F, Byte]): F[Unit] =
    Files[F].createDirectories(baseDir / id) >> data
      .through(Files[F].writeAll(baseDir / id / revision.toString))
      .compile
      .drain
