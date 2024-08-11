package chainless.runner.temporary

import cats.NonEmptyParallel
import cats.effect.implicits.*
import cats.effect.{Async, Resource, Sync}
import cats.implicits.*
import chainless.models.{*, given}
import io.circe.syntax.*
import org.graalvm.polyglot.Context

/** A running instance of a temporary function. Applies each block to the current function.
  */
trait Runner[F[_]]:
  def applyBlock(stateWithChains: FunctionState, blockWithChain: BlockWithChain): F[FunctionState]

object LocalGraalRunner:
  import GraalSupport.*
  def make[F[_]: Async: NonEmptyParallel](code: String, language: String): Resource[F, Runner[F]] =
    GraalSupport
      .makeContext[F]
      .evalMap((ec, context) => ec.evalSync(context.eval(language, code)).map((ec, context, _)))
      .map((ec, context, compiled) =>
        new Runner[F]:
          given Context = context

          override def applyBlock(
              stateWithChains: FunctionState,
              blockWithChain: BlockWithChain
          ): F[FunctionState] =
            Async[F].cede *>
              (Sync[F].delay(stateWithChains.asJson), Sync[F].delay(blockWithChain.asJson)).parTupled
                .guarantee(Async[F].cede)
                .flatMap((stateWithChainsJson, blockWithChainJson) =>
                  ec.evalSync {
                    if (language == "js") {
                      val result = compiled.execute(stateWithChainsJson.asValue, blockWithChainJson.asValue)
                      val json = result.asJson
                      json
                    } else {
                      val result = context.getPolyglotBindings
                        .getMember("apply_block")
                        .execute(stateWithChainsJson.asValue, blockWithChainJson.asValue)
                      val json = result.asJson
                      json
                    }
                  }
                )
                .guarantee(Async[F].cede)
                .map(result =>
                  FunctionState(
                    stateWithChains.chainStates.updated(blockWithChain.meta.chain.name, blockWithChain.meta.blockId),
                    result
                  )
                )
                .guarantee(Async[F].cede)
      )
