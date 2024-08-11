package chainless.replicator

import cats.Show
import cats.effect.Async
import cats.implicits.*
import chainless.db.*
import chainless.models.*
import chainless.replicator.provider.BlocksProvider
import fs2.{io as _, *}
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

/** Retrieves blocks from _all_ configured blockchains and saves them to the block store.
  * @param blocks
  *   the database to save to
  * @param providers
  *   the configured block providers
  * @param blockStore
  *   the object store to save raw blocks to
  * @param broadcaster
  *   a function which is invoked after a block is fetched and saved
  */
class Replicator[F[_]: Async](
    blocks: BlocksDb[F],
    providers: Map[Chain, BlocksProvider[F]],
    blockStore: BlocksStore[F],
    broadcaster: BlockMeta => F[Unit]
):

  private given logger: Logger[F] =
    Slf4jLogger.getLoggerFromName[F]("Replicator")

  private given Show[BlockWithChain] = blockWithChain =>
    show"chain=${blockWithChain.meta.chain}" +
      show" blockId=${blockWithChain.meta.blockId}" +
      show" height=${blockWithChain.meta.height}" +
      show" timestampMs=${blockWithChain.meta.timestampMs}" +
      show" parentBlockId=${blockWithChain.meta.parentBlockId}"

  /** Runs a never-ending background stream which fetches and saves new blocks from the configured providers
    * @return
    *   never
    */
  def replicate: F[Unit] =
    Stream
      .emits(
        providers.values.toList
      )
      .map(provider =>
        Stream
          .force(
            blocks
              .chainTip(provider.chain)
              .semiflatMap(blocks.getBlock)
              .fold(provider.tipBlocks)(provider.tipBlocksWithBackfill)
          )
          .through(pipe)
      )
      .parJoinUnbounded
      .compile
      .drain

  private def pipe: Pipe[F, BlockWithChain, Unit] =
    _.evalTap(block => logger.info(show"Saving block $block"))
      .evalTap(blockStore.saveBlock)
      .map(_.meta)
      .evalTap(blocks.insert)
      .evalTap(broadcaster)
      .void
