package chainless

import cats.Eq
import cats.data.NonEmptyChain
import cats.implicits.*
import io.circe.generic.semiauto.*
import io.circe.{Codec, Json}

import java.util.UUID
import scala.concurrent.duration.*

package object models:
  case class InitRequest(code: String, config: Json)

  case class RetroactRequest(code: String, timestampMs: Long, chains: NonEmptyChain[Chain], language: String = "js")

  case class StreamRequest(
      code: String,
      language: String = "js",
      chainStates: Option[Map[String, String]],
      state: Option[Json],
      chains: NonEmptyChain[Chain]
  )

  case class FunctionState(chainStates: Map[String, String], state: Json)

  case class BlockMeta(chain: Chain, blockId: String, parentBlockId: String, height: Long, timestampMs: Long)

  case class BlockWithChain(
      meta: BlockMeta,
      block: Json
  )

  case class FunctionInfo(
      name: String,
      language: Language,
      chains: List[Chain],
      chainStates: Map[String, String], // Chain -> BlockId
      state: Json,
      error: Option[String],
      initialized: Boolean,
      revision: Option[Int]
  )

  case class Job(jobId: String, functionId: String, rpc: String, data: Json)

  object Job:
    def newJobId(): String = UUID.randomUUID().toString

  /** Data about the invocation of a function
    * @param jobId
    *   A unique identifier of the invocation/run
    * @param functionId
    *   If the function is "Permanent", its id is provided. If the function is "Temporary", None is provided.
    * @param startTimestampMs
    *   The timestamp at which the invocation was launched
    * @param endTimestampMs
    *   The timestamp at which the invocation was completed
    * @param activeDurationMs
    *   In cases where the function may be idle for parts of the invocation, this is the sum of milliseconds for which
    *   the function was actively working
    */
  case class FunctionInvocation(
      jobId: String,
      functionId: Option[String],
      startTimestampMs: Long,
      endTimestampMs: Long,
      activeDurationMs: Long
  ):
    def duration: FiniteDuration = (endTimestampMs - startTimestampMs).milli

  object FunctionInvocation:
    def temporary(
        jobId: String,
        startTimestampMs: Long,
        endTimestampMs: Long,
        activeDurationMs: Long
    ): FunctionInvocation =
      FunctionInvocation(jobId, none, startTimestampMs, endTimestampMs, activeDurationMs)
    def permanent(
        jobId: String,
        functionId: String,
        startTimestampMs: Long,
        endTimestampMs: Long,
        activeDurationMs: Long
    ): FunctionInvocation =
      FunctionInvocation(
        jobId,
        functionId.some,
        startTimestampMs,
        endTimestampMs,
        activeDurationMs
      )
  case class CreateFunctionRequest(name: String, language: String, chains: List[String])
  case class CreateFunctionResponse(id: String)

  given Codec[FunctionInvocation] = deriveCodec

  given Codec[InitRequest] = deriveCodec

  given Codec[RetroactRequest] = deriveCodec

  given Codec[StreamRequest] = deriveCodec

  given Codec[FunctionState] = deriveCodec

  given Codec[BlockMeta] = deriveCodec

  given Codec[BlockWithChain] = deriveCodec

  given Codec[Job] = deriveCodec

  given Codec[FunctionInfo] = deriveCodec

  given Codec[CreateFunctionRequest] = deriveCodec

  given Codec[CreateFunctionResponse] = deriveCodec

  given Eq[Job] = Eq.fromUniversalEquals
