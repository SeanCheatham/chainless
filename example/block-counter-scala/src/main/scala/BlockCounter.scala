import cats.data.OptionT
import cats.effect.*
import cats.implicits.*
import io.circe.*
import io.circe.generic.semiauto.*
import io.circe.syntax.*
import org.http4s.*
import org.http4s.circe.*
import org.http4s.client.*
import org.http4s.ember.client.EmberClientBuilder
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory

object BlockCounter extends IOApp.Simple {
  private given LoggerFactory[IO] = Slf4jFactory.create[IO]
  private val callbackBase = System.getEnv("CALLBACK_BASE_URL")

  override def run: IO[Unit] =
    EmberClientBuilder
      .default[IO]
      .build
      .use(client =>
        handleAllTasks(client)
          .onError(throwable =>
            submitError(client)(throwable.getMessage + "\n" + throwable.getStackTrace.mkString("\n"))
          )
      )

  def handleAllTasks(client: Client[IO]): IO[Unit] =
    nextTask(client).semiflatMap(handleTask(client)).fold(().some)(_ => none).untilDefinedM

  def nextTask(client: Client[IO]): OptionT[IO, Task] =
    OptionT
      .liftF(
        client.expect[String](
          Request().withUri(Uri.unsafeFromString(s"$callbackBase/next"))
        )
      )
      .filter(_.nonEmpty)
      .semiflatMap(string => IO.fromEither(io.circe.parser.parse(string)))
      .filterNot(_.isNull)
      .semiflatMap(json => IO.fromEither(json.as[Task]))

  def handleTask(client: Client[IO])(task: Task): IO[Unit] =
    (task match {
      case InitTask(config)                        => init(config)
      case ApplyTask(blockWithMeta, functionState) => applyBlock(blockWithMeta, functionState)
    })
      .flatMap(newState => submitResult(client)(newState))

  def submitResult(client: Client[IO])(state: Json): IO[Unit] =
    client.expect[Unit](
      Request()
        .withUri(Uri.unsafeFromString(s"$callbackBase/success"))
        .withMethod(Method.POST)
        .withEntity(state)
    )

  def submitError(client: Client[IO])(error: String): IO[Unit] =
    client.expect[Unit](
      Request()
        .withUri(Uri.unsafeFromString(s"$callbackBase/error"))
        .withMethod(Method.POST)
        .withEntity(error)
    )

  def init(config: Json): IO[Json] =
    IO(Json.obj())

  def applyBlock(blockWithMeta: BlockWithMeta, functionState: FunctionState): IO[Json] =
    for {
      currentState <- IO(functionState.state.as[Map[String, Long]]).rethrow
      previousValue = currentState.getOrElse(blockWithMeta.meta.chain, 0L)
      newState = currentState.asJsonObject.add(blockWithMeta.meta.chain, (previousValue + 1).asJson).toJson
    } yield newState

  given Codec[BlockMeta] = deriveCodec

  given Codec[BlockWithMeta] = deriveCodec

  given Codec[FunctionState] = deriveCodec

  given Decoder[InitTask] = deriveDecoder

  given Decoder[ApplyTask] = deriveDecoder

  given Decoder[Task] = cursor =>
    cursor
      .get[String]("taskType")
      .flatMap {
        case "init"  => cursor.as[InitTask]
        case "apply" => cursor.as[ApplyTask]
      }

}

case class FunctionState(chainStates: Map[String, String], state: Json)

case class BlockWithMeta(meta: BlockMeta, block: Json)

case class BlockMeta(chain: String, blockId: String, height: Long)

sealed abstract class Task

case class InitTask(config: Json) extends Task

case class ApplyTask(blockWithChain: BlockWithMeta, stateWithChains: FunctionState) extends Task
