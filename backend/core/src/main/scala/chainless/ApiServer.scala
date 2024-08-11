package chainless

import cats.data.OptionT
import cats.effect.{IO, Resource}
import cats.implicits.*
import chainless.db.*
import chainless.models.{*, given}
import chainless.runner.persistent.JobDispatcher
import chainless.runner.temporary.RunnerOperator
import chainless.utils.*
import com.comcast.ip4s.{Host, Port}
import fs2.Stream
import fs2.io.file.Files
import io.circe.Json
import io.circe.syntax.*
import org.http4s.*
import org.http4s.QueryParamDecoder.*
import org.http4s.circe.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.io.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.multipart.Multipart
import org.http4s.server.Router
import org.http4s.server.middleware.CORS
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.{Logger, LoggerFactory}

import java.time.Instant
import scala.concurrent.duration.*

/** The user-facing API server. Offers the following routes:
  *   - GET /
  *   - GET /functions
  *   - GET /functions/{id}
  *   - DELETE /functions/{id}
  *   - POST /functions
  *   - POST /function-store/{id}
  *   - POST /function-init/{id}
  *   - POST /retroact
  *   - POST /live
  *
  * @param functions
  *   functions database
  * @param functionStoreClient
  *   function object storage
  * @param invocationsDB
  *   function invocations database
  * @param operator
  *   manager for temporary functions
  * @param jobDispatcher
  *   dispatcher for persistent functions
  */
class ApiServer(
    functions: FunctionsDb[IO],
    functionStoreClient: FunctionsStore[IO],
    invocationsDB: FunctionInvocationsDb[IO],
    operator: RunnerOperator[IO],
    jobDispatcher: JobDispatcher[IO]
)(using LoggerFactory[IO]):
  import ApiServer.*
  type F[A] = IO[A]

  private given Logger[F] = Slf4jLogger.getLoggerFromName("ApiServer")

  def serve(bindHost: String, bindPort: Int): Resource[IO, Unit] =
    Resource
      .eval(
        IO
          .delay(
            (Host.fromString(bindHost), Port.fromInt(bindPort)).tupled
              .toRight(new IllegalArgumentException("Invalid bindHost/bindPort"))
          )
          .rethrow
      )
      .flatMap((host, port) =>
        EmberServerBuilder
          .default[IO]
          .withHost(host)
          .withPort(port)
          .withHttpApp(
            CORS.policy.withAllowOriginAll.withAllowMethodsAll
              .withAllowHeadersAll(
                Router("/api" -> apiRoutes, "/" -> webRoutes)
              )
              .orNotFound
          )
          .build
      )
      .void

  private val webRoutes: HttpRoutes[IO] = {
    val classloader = this.getClass.getClassLoader
    HttpRoutes.of[IO] {
      case req @ GET -> Root =>
        StaticFile
          .fromResource[IO]("/web/index.html", req.some, classloader = classloader.some)
          .getOrElseF(NotFound("Well this is awkward..."))
      case req @ GET -> path =>
        StaticFile
          .fromResource[IO](s"/web/$path", req.some, classloader = classloader.some)
          .getOrElseF(NotFound("Well this is super awkward..."))
    }
  }

  private val apiRoutes: HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case GET -> Root =>
        Ok(Json.obj("chain" -> "less".asJson)).logError
      case request @ GET -> Root / "functions" =>
        Ok(
          functions.list
            .intersperse("\n")
            .through(fs2.text.utf8.encode)
            .logError
        )
      case request @ GET -> Root / "functions" / id =>
        functions
          .get(id)
          .fold(
            Response().withStatus(Status.NotFound)
          )(function => Response().withEntity(function.asJson))
          .logError
      case request @ GET -> Root / "function-invocations" / functionId :? AfterTimestampQueryParamMatcher(
            after
          ) +& BeforeTimestampQueryParamMatcher(before) =>
        IO(
          Response()
            .withEntity(
              invocationsDB
                .byFunction(functionId, after, before)
                .map(_.asJson)
                .map(_.noSpaces)
                .intersperse("\n")
                .through(fs2.text.utf8.encode)
                .logError
            )
        )

      case request @ DELETE -> Root / "functions" / id =>
        functions
          .delete(id)
          .flatTap(deleted => IO.whenA(deleted)(functionStoreClient.delete(id).void))
          .ifM(
            Response().pure[F],
            Response().withStatus(Status.NotFound).pure[F]
          )
          .logError

      case request @ POST -> Root / "functions" =>
        request
          .as[Json]
          .map(_.hcursor)
          .flatMap(cursor =>
            IO.fromEither(
              (
                cursor.get[String]("name"),
                cursor.get[Language]("language"),
                cursor.get[List[Chain]]("chains")
              ).mapN((name, language, chains) =>
                Logger[F].info(s"Creating new function name=$name language=$language chains=$chains") >>
                  functions
                    .create(name, language, chains)
                    .flatTap(id => Logger[F].info(s"Finished creating function id=$id"))
              )
            )
          )
          .flatten
          .map(id => Response().withEntity(Json.obj("id" -> id.asJson)))
          .logError
      case request @ POST -> Root / "function-store" / id =>
        def handleUpload(revision: Int)(stream: fs2.Stream[F, Byte]): F[Response[F]] =
          Files.forIO.tempFile
            .use(file =>
              stream.through(Files.forIO.writeAll(file)).compile.drain >>
                functionStoreClient.save(id, revision)(
                  Files.forIO.readAll(file)
                )
            ) >> functions.updateRevision(id, revision).as(Response())

        functions
          .get(id)
          .foldF(Response().withStatus(Status.NotFound).pure[F])(info =>
            Logger[F].info(s"Uploading function code for functionId=$id") >>
              (
                if (
                  request.headers
                    .get[headers.`Content-Type`]
                    .exists(_.mediaType == MediaType.application.`octet-stream`)
                )
                  handleUpload(info.revision.fold(0)(_ + 1))(request.body)
                else
                  request.decode[Multipart[F]](multipart =>
                    handleUpload(info.revision.fold(0)(_ + 1))(multipart.parts.head.body)
                  )
              ) <* Logger[F].info(s"Finished uploading function code for functionId=$id")
          )
          .logError
      case request @ POST -> Root / "function-init" / id =>
        functions
          .get(id)
          .foldF(Response().withStatus(Status.NotFound).pure[F])(info =>
            if (info.revision.isEmpty)
              Response().withStatus(Status.NotAcceptable).pure[F]
            else
              request
                .as[Json]
                .map(_.hcursor)
                .flatMap(cursor =>
                  IO.fromEither(
                    (
                      cursor.get[Option[Json]]("config"),
                      cursor.get[Option[Long]]("retroactTimestampMs")
                    ).mapN((config, retroact) =>
                      Json.obj("config" -> config.asJson, "retroactTimestampMs" -> retroact.asJson)
                    )
                  ).flatTap(_ => Logger[F].info(s"Submitting init job for functionId=$id"))
                    .flatMap(jobDispatcher.initFunction(id, _))
                )
                .as(Response())
          )
          .logError
      case request @ POST -> Root / "retroact" =>
        request
          .as[RetroactRequest]
          .map(request =>
            Response(
              body = operator
                .retroact(request.code, request.language)(request.timestampMs, request.chains)
                .map(_.asJson)
                .map(_.noSpaces)
                .mergeHaltL(keepAliveTick(2.seconds))
                .intersperse("\n")
                .through(fs2.text.utf8.encode)
                .onError { case e =>
                  Stream.exec(Logger[F].warn(e)("Runner failure"))
                }
            )
          )
          .logError
      case request @ POST -> Root / "live" =>
        request
          .as[StreamRequest]
          .map(request =>
            Response(
              body = operator
                .live(
                  request.code,
                  request.language,
                  FunctionState(request.chainStates.getOrElse(Map.empty), request.state.getOrElse(Json.Null))
                )(request.chains)
                .map(_.asJson)
                .map(_.noSpaces)
                .mergeHaltL(keepAliveTick(2.seconds))
                .intersperse("\n")
                .through(fs2.text.utf8.encode)
                .onError { case e =>
                  Stream.exec(Logger[F].warn(e)("Runner failure"))
                }
            )
          )
    }

  private def keepAliveTick(period: FiniteDuration): Stream[F, String] =
    Stream.fixedRate[F](period).as("")

object ApiServer:
  implicit val instantParamDecoder: QueryParamDecoder[Instant] = QueryParamDecoder[Long].map(Instant.ofEpochMilli)
  object AfterTimestampQueryParamMatcher extends QueryParamDecoderMatcher[Instant]("afterTimestampMs")
  object BeforeTimestampQueryParamMatcher extends QueryParamDecoderMatcher[Instant]("beforeTimestampMs")
