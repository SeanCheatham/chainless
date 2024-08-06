package chainless.runner.persistent

import cats.effect.*
import chainless.db.*
import chainless.models.*
import fs2.{io as _, *}
import io.circe.Json

/** Dispatches jobs to the job processor. It listens for new blocks and dispatches jobs for each function that is active
  * for the new block's chain. Also provides a function which dispatches an "init" job for a new function.
  * @param newBlocks
  *   a stream of newly minted blocks
  * @param functionsDb
  *   the database of functions
  * @param jobProcessor
  *   the job processor which handles the produces jobs
  */
class JobDispatcher[F[_]: Async](
    newBlocks: Stream[F, BlockMeta],
    functionsDb: FunctionsDb[F],
    jobProcessor: JobProcessor[F]
):
  def background: Stream[F, Unit] =
    newBlocks
      .flatMap(b =>
        functionsDb
          .initializedFunctionIdsForChain(b.chain)
          .map(functionId => Job(Job.newJobId(), functionId, "apply", Json.Null))
          .parEvalMapUnorderedUnbounded(jobProcessor.process)
      )

  def initFunction(functionId: String, data: Json): F[Unit] =
    jobProcessor.process(Job(Job.newJobId(), functionId, "init", data))
