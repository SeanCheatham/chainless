package chainless.runner.temporary

import cats.NonEmptyParallel
import cats.data.NonEmptyChain
import cats.effect.{Async, Ref}
import cats.implicits.*
import chainless.db.*
import chainless.models.*
import fs2.{io as _, *}
import io.circe.*
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.concurrent.duration.*
import java.util.UUID

/** Manages new requests to run temporary functions
  * @param blocksDb
  *   blocks database
  * @param blocksStore
  *   blocks object data
  * @param invocationsDb
  *   function invocations database
  * @param newBlocks
  *   a stream of new blocks
  */
class RunnerOperator[F[_]: Async: NonEmptyParallel](
    blocksDb: BlocksDb[F],
    blocksStore: BlocksStore[F],
    invocationsDb: FunctionInvocationsDb[F],
    newBlocks: Stream[F, BlockMeta]
):

  given Logger[F] = Slf4jLogger.getLoggerFromName("Operator")

  def retroact(
      code: String,
      language: String
  )(timestampMs: Long, chains: NonEmptyChain[Chain]): Stream[F, FunctionState] =
    Stream
      .resource(LocalGraalRunner.make[F](code, language))
      .flatMap(runner =>
        blocksDb
          .blocksAfterTimestamp(chains)(timestampMs)
          .evalMap(blocksStore.getBlock)
          .evalScan((Duration.Zero, FunctionState(Map.empty, Json.Null))) {
            case ((_, stateWithChains), blockWithMeta) =>
              Async[F]
                .timed(runner.applyBlock(stateWithChains, blockWithMeta))
          }
          .drop(1)
          .broadcastThrough(recorded)
          .map(_._2)
      )

  def live(code: String, language: String, stateWithChains: FunctionState)(
      chains: NonEmptyChain[Chain]
  ): Stream[F, FunctionState] =
    Stream
      .resource(LocalGraalRunner.make[F](code, language))
      .flatMap(runner =>
        newBlocks
          .filter(meta => chains.contains(meta.chain))
          .evalMap(blocksStore.getBlock)
          .evalScan((Duration.Zero, stateWithChains)) { case ((_, stateWithChains), blockWithMeta) =>
            Async[F].timed(runner.applyBlock(stateWithChains, blockWithMeta))
          }
          .drop(1)
          .broadcastThrough(recorded)
          .map(_._2)
      )

  private def recorded[T]: Pipe[F, (FiniteDuration, T), (FiniteDuration, T)] =
    stream =>
      Stream
        .eval(
          (Async[F].delay(UUID.randomUUID().toString), Async[F].realTime, Ref.of[F, Long](0)).tupled
            .flatTap((invocationId, startTime, _) =>
              Logger[F].info(s"Starting run for invocationId=$invocationId at startTimestampMs=${startTime.toMillis}")
            )
        )
        .flatMap((jobId, startTime, activeDurationRef) =>
          stream
            .evalTap((duration, _) => activeDurationRef.update(_ + duration.toMillis))
            .onFinalize(
              Async[F].realTime.flatMap(endTime =>
                Logger[F]
                  .info(
                    "Finishing job for" +
                      s" jobId=$jobId" +
                      s" at endTimestampMs=${endTime.toMillis}" +
                      s" duration=${endTime - startTime}"
                  ) *>
                  activeDurationRef.get.flatMap(activeDurationMs =>
                    invocationsDb.record(
                      FunctionInvocation.temporary(
                        jobId,
                        startTime.toMillis,
                        endTime.toMillis,
                        activeDurationMs
                      )
                    )
                  )
              )
            )
        )
