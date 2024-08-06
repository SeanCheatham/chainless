package chainless.replicator.provider

import cats.effect.Async
import cats.implicits.*
import chainless.models.*
import fs2.{Pipe, Pull, Stream}
import org.typelevel.log4cats.Logger

import java.time.Instant

trait BlocksProvider[F[_]]:
  def chain: Chain
  def tip: F[String]
  def tips: Stream[F, String]
  def blockById(id: String): F[BlockWithChain]
  def blockIdAtHeight(height: Long): F[String]
  def blockIdAfter(instant: Instant): F[String]

  def tipBlocks(using Async[F])(using Logger[F]): Stream[F, BlockWithChain] =
    tips.evalMap(blockById).through(blocksWithBackfill)

  def tipBlocksWithBackfill(known: BlockMeta)(using Async[F])(using Logger[F]): Stream[F, BlockWithChain] =
    (missingBlocksAfter(known) ++ tipBlocks).through(blocksWithBackfill)

  def blocksWithBackfill(using Async[F])(using Logger[F]): Pipe[F, BlockWithChain, BlockWithChain] = {
    def mainPull(s: Stream[F, BlockWithChain], previous: Option[BlockMeta]): Pull[F, BlockWithChain, Unit] =
      s.pull.uncons1.flatMap {
        case Some((h, tail)) =>
          previous.fold(Pull.output1(h) >> mainPull(tail, h.meta.some))(previous =>
            backfillGap(tail, missingBlocksBetween(previous, h), previous)
          )
        case _ => Pull.done
      }
    def backfillGap(
        mainStream: Stream[F, BlockWithChain],
        gap: Stream[F, BlockWithChain],
        previous: BlockMeta
    ): Pull[F, BlockWithChain, Unit] =
      gap.pull.uncons1.flatMap {
        case Some((h, tail)) => Pull.output1(h) >> backfillGap(mainStream, tail, h.meta)
        case _               => mainPull(mainStream, previous.some)
      }

    mainPull(_, none).stream.through(logForks)
  }

  def logForks(using Async[F])(using Logger[F]): Pipe[F, BlockWithChain, BlockWithChain] = {
    def mainStream(s: Stream[F, BlockWithChain], previous: Option[BlockMeta]): Pull[F, BlockWithChain, Unit] =
      s.pull.uncons1.flatMap {
        case Some((head, tail)) =>
          val happyPath = Pull.output1(head) >> mainStream(tail, head.meta.some)
          previous.fold(happyPath)(previous =>
            if (previous.height == head.meta.height) {
              if (previous.blockId == head.meta.blockId) mainStream(tail, head.meta.some)
              else Pull.eval(Logger[F].info(s"Fork detected at height=${previous.height}")) >> happyPath
            } else if (previous.blockId != head.meta.parentBlockId)
              Pull.eval(Logger[F].info(s"Fork detected at height=${previous.height}")) >> happyPath
            else happyPath
          )
        case _ => Pull.done
      }

    mainStream(_, none).stream
  }

  // Includes head in result
  def missingBlocksBetween(known: BlockMeta, head: BlockWithChain)(using
      Async[F]
  ): Stream[F, BlockWithChain] =
    if (known.blockId == head.meta.blockId)
      Stream.empty.covary[F]
    else if (head.meta.height == known.height) // Fork condition
      Stream.emit(head).covary[F]
    else if (head.meta.height == (known.height + 1))
      Stream.emit(head).covary[F]
    else
      Stream
        .range(known.height + 1, head.meta.height)
        .parEvalMap(10)(blockIdAtHeight)
        .evalMap(blockById)

  def missingBlocksAfter(known: BlockMeta)(using Async[F]): Stream[F, BlockWithChain] =
    Stream
      .eval(tip)
      .flatMap(tipId =>
        if (known.blockId == tipId) Stream.empty
        else
          Stream
            .eval(blockById(tipId))
            .flatMap(
              missingBlocksBetween(known, _)
            )
      )
