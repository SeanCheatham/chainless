package chainless.runner.persistent

import cats.data.OptionT
import cats.effect.{IO, Resource}
import cats.implicits.*
import chainless.utils.*
import com.comcast.ip4s.{Host, Port}
import io.circe.Json
import io.circe.syntax.*
import org.http4s.*
import org.http4s.circe.*
import org.http4s.dsl.io.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.{Logger, LoggerFactory}

/** Provides an API server to an instance of a function. Offers the following routes:
  *   - GET /next
  *   - POST /success
  *   - POST /error
  * @param nextTask
  *   retrieves the next task for the particular function. If the job is complete, none is returned.
  * @param requestCompleted
  *   a function to invoke when a task is completed (either successfully or with error)
  */
class RunnerHttpServer(
    nextTask: IO[Option[Task]],
    requestCompleted: TaskResult => IO[Unit]
)(using
    LoggerFactory[IO]
):
  type F[A] = IO[A]

  private given Logger[F] = Slf4jLogger.getLoggerFromName("RunnerHttpServer")

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
          .withHttpApp(Router("/" -> routes).orNotFound)
          .build
      )
      .void

  val routes: HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case GET -> Root / "next" =>
        OptionT(nextTask)
          .semiflatTap(_ => Logger[F].info("Dispatching task to function"))
          .flatTapNone(Logger[F].info("Tasks completed for job"))
          .map(_.asJson)
          .map(_.noSpaces)
          .fold(Response())(Response().withEntity)
          .logError
      case request @ POST -> Root / "success" =>
        request
          .as[Json]
          .flatTap(_ => Logger[F].info("Task success"))
          .flatTap(state => requestCompleted(state.asRight))
          .as(Response())
          .logError
      case request @ POST -> Root / "error" =>
        request
          .as[String]
          .flatTap(error => Logger[F].warn(s"Task error=$error"))
          .flatTap(message => requestCompleted(message.asLeft))
          .as(Response())
          .logError
    }
