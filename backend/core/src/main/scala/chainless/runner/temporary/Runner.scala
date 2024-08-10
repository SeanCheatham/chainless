package chainless.runner.temporary

import cats.effect.implicits.*
import cats.effect.{Async, Resource, Sync}
import cats.implicits.*
import cats.NonEmptyParallel
import chainless.models.{*, given}
import io.circe.syntax.*
import io.circe.{Json, JsonNumber, JsonObject}
import org.graalvm.polyglot.proxy.{ProxyArray, ProxyObject}
import org.graalvm.polyglot.{Context, Value}

import java.util.concurrent.Executors
import scala.annotation.tailrec
import scala.concurrent.ExecutionContext
import scala.jdk.CollectionConverters.*

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

object GraalSupport:

  extension (ec: ExecutionContext)
    def eval[F[_]: Async, A](fa: F[A]): F[A] =
      Async[F].evalOn(fa, ec)

    def evalSync[F[_]: Async, A](a: => A): F[A] =
      eval(Sync[F].delay(a))

  def makeContext[F[_]: Async]: Resource[F, (ExecutionContext, Context)] =
    Resource
      .make(Async[F].delay(ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor())))(ec =>
        Async[F].delay(ec.shutdownNow)
      )
      .flatMap(executor =>
        Resource
          .make(executor.eval(Sync[F].delay(Context.newBuilder("js", "python").allowAllAccess(true).build())))(
            context => executor.eval(Sync[F].blocking(context.close()))
          )
          .tupleLeft(executor)
      )

  def verifyCompatibility[F[_]: Async]: Resource[F, Unit] =
    for {
      (ec, context) <- makeContext[F]
      given Context = context
      j <- ec.evalSync {
        val compiled = context.eval("js", compatibilityTestCode)
        val res = compiled.execute(Json.obj("bar" -> 42.asJson).asValue)
        val j = res.asJson
        j
      }.toResource
      bar <- Sync[F].fromEither(j.as[Int]).toResource
      _ <- Sync[F]
        .raiseUnless(bar == 42)(new IllegalStateException("Unexpected JS Result"))
        .toResource
    } yield ()

  private val compatibilityTestCode =
    """
      |(function(foo) {
      |  return foo.bar;
      |})
      |""".stripMargin

  private def jsonFolder(using context: Context) =
    new Json.Folder[Value] {
      def onNull: Value = context.asValue(null)

      def onBoolean(value: Boolean): Value = context.asValue(value)

      def onNumber(value: JsonNumber): Value = context.asValue(value.toDouble)

      def onString(value: String): Value = context.asValue(value)

      def onArray(value: Vector[Json]): Value = {
        val arr: Array[AnyRef] = new Array(value.size)
        value.zipWithIndex.foreach { case (j, idx) =>
          arr.update(idx, j.asValue)
        }
        val x = context.asValue(ProxyArray.fromArray(arr*))
        x
      }

      def onObject(value: JsonObject): Value = {
        val map = new java.util.HashMap[String, AnyRef](value.size)
        value.toMap.foreach { case (key, value) =>
          map.put(key, value.asValue)
        }
        context.asValue(ProxyObject.fromMap(map))
      }
    }

  extension (value: Value)
    def asJson(using context: Context): Json =
      if (value.isNull) Json.Null
      else if (value.isBoolean) value.asBoolean().asJson
      else if (value.isNumber) {
        if (value.fitsInInt()) value.asInt().asJson
        else if (value.fitsInLong()) value.asLong().asJson
        else if (value.fitsInFloat()) value.asFloat().asJson
        else value.asDouble().asJson
      } else if (value.isString) value.asString().asJson
      else if (value.hasArrayElements) Json.arr(value.asScalaIterator.map(_.asJson).toSeq*)
      else if (value.hasHashEntries)
        Json.obj(
          value.asScalaMapIterator.map { case (k, v) =>
            k.asString() -> v.asJson
          }.toSeq*
        )
      else if (value.hasMembers)
        Json.obj(
          value.asScalaMapIterator.map { case (k, v) =>
            k.asString() -> v.asJson
          }.toSeq*
        )
      else throw new MatchError(value)

    @tailrec
    def asScalaIterator(using context: Context): Iterator[Value] =
      if (value.isIterator) {
        Iterator.unfold(value)(i => Option.when(i.hasIteratorNextElement)(i.getIteratorNextElement -> i))
      } else value.getIterator.asScalaIterator

    @tailrec
    def asScalaMapIterator(using context: Context): Iterator[(Value, Value)] =
      if (value.isIterator) {
        Iterator.unfold(value)(i =>
          Option.when(i.hasIteratorNextElement) {
            val arr = i.getIteratorNextElement

            (arr.getArrayElement(0) -> arr.getArrayElement(1)) -> i
          }
        )
      } else if (value.hasMembers)
        value.getMemberKeys.asScala.iterator.map(key => context.asValue(key) -> value.getMember(key))
      else value.getHashEntriesIterator.asScalaMapIterator

  extension (json: Json) def asValue(using context: Context): Value = json.foldWith(jsonFolder)
