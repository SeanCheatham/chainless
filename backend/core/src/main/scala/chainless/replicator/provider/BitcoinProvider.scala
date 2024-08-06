package chainless.replicator.provider

import cats.NonEmptyParallel
import cats.effect.implicits.*
import cats.effect.{Async, Resource, Sync}
import cats.implicits.*
import chainless.models.*
import com.github.benmanes.caffeine.cache.Caffeine
import fs2.io.net.Network
import fs2.{Pull, Stream}
import io.circe.*
import io.circe.generic.semiauto.deriveEncoder
import io.circe.syntax.*
import org.http4s.circe.*
import org.http4s.client.Client
import org.http4s.{Method, Request, Uri}
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.{Logger, LoggerFactory}
import retry.*
import scalacache.caffeine.CaffeineCache
import scalacache.{Cache, Entry}

import java.time.Instant
import scala.concurrent.duration.*

class BitcoinProvider[F[_]: Async: NonEmptyParallel: Network: LoggerFactory](
    basePath: String,
    blockCache: Cache[F, String, BlockMeta],
    blockByHeightCache: Cache[F, Long, String],
    blockTimestampCache: Cache[F, String, Instant],
    blockHeightCache: Cache[F, String, Long]
)(using client: Client[F])
    extends BlocksProvider[F]:
  import BitcoinProvider.*

  private given logger: Logger[F] =
    Slf4jLogger.getLoggerFromName("BitcoinProvider")

  def chain: Chain = Chain.Bitcoin

  def tips: Stream[F, String] = {
    def go(s: Stream[F, String], previous: Option[String]): Pull[F, String, Unit] =
      s.pull.uncons1.flatMap {
        case Some((h, tail)) =>
          if (previous.contains(h)) Pull.sleep(15.seconds) >> go(tail, previous)
          else Pull.output1(h) >> go(tail, h.some)
        case None =>
          Pull.done
      }

    Stream.repeatEval(tip).through(go(_, None).stream)
  }

  def blockById(id: String): F[BlockWithChain] =
    request("getblock", List(id.asJson, 2.asJson))
      .flatMap(json => extractMeta(id)(json).map(BlockWithChain(_, json)))

  def blockMetaById(id: String): F[BlockMeta] =
    blockCache.cachingF(id)(ttl = None)(blockById(id).map(_.meta))

  def blockIdAfter(instant: Instant): F[String] =
    tip.flatMap(id =>
      timestampAtBlockId(id)
        .ensureOr(i2 =>
          new IllegalArgumentException(s"Requested time=${instant.toEpochMilli} is after head=${i2.toEpochMilli}")
        )(_.isAfter(instant)) >>
        heightAtBlockId(id)
          .tupleLeft(id)
          .flatMap(_.tailRecM((previous, previousHeight) =>
            if (previousHeight == 1) Right(previous).pure[F]
            blockIdAtHeight(previousHeight - 1).flatMap(id =>
              timestampAtBlockId(id).map(blockInstant =>
                if (blockInstant.isBefore(instant)) Right(previous)
                else Left((id, previousHeight - 1))
              )
            )
          ))
    )

  def extractMeta(id: String)(json: Json): F[BlockMeta] =
    (extractParentId(json), extractHeight(json), extractTimestamp(json).map(_.toEpochMilli))
      .parMapN(BlockMeta(chain, id, _, _, _))

  def extractParentId(block: Json): F[String] =
    Async[F].delay(block.hcursor.downField("previousblockhash").as[String]).rethrow

  def extractHeight(block: Json): F[Long] =
    Async[F].delay(block.hcursor.downField("height").as[Long]).rethrow

  def extractTimestamp(block: Json): F[Instant] =
    Async[F]
      .delay(block.hcursor.downField("time").as[Long])
      .rethrow
      .map(Instant.ofEpochSecond)

  def tip: F[String] =
    request("getbestblockhash", Nil)
      .map(_.as[String])
      .rethrow

  def blockIdAtHeight(height: Long): F[String] =
    blockByHeightCache.cachingF(height)(ttl = None)(
      request("getblockhash", List(height.asJson))
        .map(_.as[String])
        .rethrow
    )

  def timestampAtBlockId(id: String): F[Instant] =
    blockTimestampCache.cachingF(id)(ttl = None)(blockMetaById(id).map(_.timestampMs).map(Instant.ofEpochMilli))

  def heightAtBlockId(id: String): F[Long] =
    blockHeightCache.cachingF(id)(ttl = None)(blockMetaById(id).map(_.height))

  def request(method: String, params: List[Json]): F[Json] =
    retryingOnAllErrors[Json](
      policy = RetryPolicies.limitRetries(10).join(RetryPolicies.fibonacciBackoff(500.milli)),
      onError = logError
    )(
      client
        .run(
          Request(uri = Uri.unsafeFromString(basePath))
            .withMethod(Method.POST)
            .withEntity(RpcRequest("1.0", "chainless", method, params).asJson)
        )
        .use(response =>
          response
            .as[String]
            .flatMap(body =>
              if (response.status.isSuccess)
                Async[F].cede *>
                  Async[F]
                    .delay(io.circe.parser.parse(body))
                    .guarantee(Async[F].cede)
                    .rethrow
                    .map(_.asObject.flatMap(_.apply("result")).toRight(new IllegalArgumentException("Not JSON")))
                    .rethrow
                    .guarantee(Async[F].cede)
              else
                Async[F]
                  .raiseError[Json](
                    new IllegalArgumentException(s"Invalid HTTP code${response.status.code} body=$body")
                  )
            )
        )
    )

  def logError(error: Throwable, details: RetryDetails): F[Unit] =
    logger.warn(error)("Request Error")

object BitcoinProvider:
  case class RpcRequest(jsonrpc: String, id: String, method: String, params: List[Json])
  given Encoder[RpcRequest] = deriveEncoder

  def make[F[_]: Async: NonEmptyParallel: Network: LoggerFactory: Client](
      basePath: String
  ): Resource[F, BitcoinProvider[F]] =
    (
      makeCache[F, String, BlockMeta](64),
      makeCache[F, Long, String](128),
      makeCache[F, String, Instant](128),
      makeCache[F, String, Long](128)
    ).mapN(new BitcoinProvider(basePath, _, _, _, _))

  private def makeCache[F[_]: Sync, K, V](size: Int) =
    Resource.make(Sync[F].delay(Caffeine.newBuilder.maximumSize(size).build[K, Entry[V]]()).map(CaffeineCache(_)))(
      cache => cache.close
    )
