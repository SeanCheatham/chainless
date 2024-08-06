package chainless.utils

import cats.effect.*
import cats.implicits.*
import com.comcast.ip4s.{Host, Port}
import io.circe.Json
import org.http4s.circe.jsonEncoder
import org.http4s.dsl.io.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router
import org.http4s.{HttpRoutes, Response, Status}
import org.typelevel.log4cats.LoggerFactory

class Healthcheck(livelinessState: Ref[IO, Healthcheck.Status], readinessState: Ref[IO, Healthcheck.Status]) {
  type F[A] = IO[A]
  def serve(bindHost: String, bindPort: Int)(using LoggerFactory[IO]): Resource[F, Unit] =
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

  private val routes =
    HttpRoutes.of[IO] {
      case GET -> Root / "status" / "startup" =>
        Ok()
      case GET -> Root / "status" / "ready" =>
        readinessState.get.flatMap(statusAsEntity)
      case GET -> Root / "status" / "liveliness" =>
        readinessState.get.flatMap(statusAsEntity)
    }
  private def statusAsEntity(status: Healthcheck.Status) = status match {
    case Healthcheck.Healthy(None)       => Response().pure[F]
    case Healthcheck.Healthy(Some(body)) => Response().withEntity(body).pure[F]
    case Healthcheck.Unhealthy(None)     => Response().withStatus(Status.InternalServerError).pure[F]
    case Healthcheck.Unhealthy(Some(body)) =>
      Response().withEntity(body).withStatus(Status.InternalServerError).pure[F]
    case Healthcheck.Unresponsive => IO.never[Response[IO]]
  }

  def setLiveliness(status: Healthcheck.Status): F[Unit] = livelinessState.set(status)
  def setReadiness(status: Healthcheck.Status): F[Unit] = readinessState.set(status)
}

object Healthcheck:
  def make: Resource[IO, Healthcheck] =
    (Ref.of[IO, Status](Unresponsive), Ref.of[IO, Status](Unresponsive))
      .mapN(new Healthcheck(_, _))
      .toResource

  sealed trait Status
  case class Healthy(body: Option[Json] = None) extends Status
  case class Unhealthy(body: Option[Json] = None) extends Status
  case object Unresponsive extends Status
