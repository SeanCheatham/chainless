package chainless.db

import cats.effect.Async
import cats.implicits.*
import chainless.models.*
import fs2.io.file.{Files, Path}
import fs2.{Chunk, Stream}

import java.nio.charset.StandardCharsets

trait BlocksStore[F[_]]:
  def saveBlock(block: BlockWithChain): F[Unit]
  def getBlock(meta: BlockMeta): F[BlockWithChain]

class DirBlocksStore[F[_]: Async](baseDir: Path) extends BlocksStore[F]:
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
