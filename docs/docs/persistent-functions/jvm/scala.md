---
title: Scala
description: Information on writing Chainless functions using Scala.
---

# Scala Functions

Persistent Functions can be written in Scala + SBT.


## What you'll need
- [SBT](https://www.scala-sbt.org/download/)

## SBT
Your code files are structured like a normal SBT project, including a `build.sbt`.  You can use either a single-module or multi-module project.  For simplicity, we describe a single-module project here.

### `build.sbt`
```sbt
scalaVersion := "3.3.1"
name := "chainless-function-example"
publish / skip := true
version := "0.1.0"
assembly / assemblyJarName := "function.jar"
libraryDependencies ++= Seq(
    "org.typelevel" %% "cats-core" % "2.10.0",
    "org.typelevel" %% "cats-effect" % "3.5.2",
    "org.http4s" %% "http4s-ember-client" % "1.0.0-M40",
    "org.http4s" %% "http4s-circe" % "1.0.0-M40",
    "io.circe" %% "circe-core" % "0.14.6",
    "io.circe" %% "circe-generic" % "0.14.6",
    "io.circe" %% "circe-parser" % "0.14.6"
)
```
There are several items listed under `libraryDependencies`.  These are not specifically required, but at minimum, you'll need an HTTP client in order to call the event API.  In this example, we use `cats` as a general framework, `http4s` to make API calls, and `circe` to handle JSON.

## SBT Plugins
In addition to the normal build.sbt file, you will need to include an SBT plugin which can package up your project as a self-contained JAR.

### `project/plugins.sbt`
```sbt
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "2.1.5")
```

## Function Code
Next, write your function's `main` method.

### `src/main/scala/MyFunction.scala`
```scala
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
    EmberClientBuilder.default[IO].build.use(client =>
      handleAllTasks(client)
        .onError(throwable =>
          submitError(client)(
            throwable.getMessage + "\n" + throwable.getStackTrace.mkString("\n"))
        )
    )

  def handleAllTasks(client: Client[IO]): IO[Unit] =
    nextTask(client).semiflatMap(handleTask(client)).fold(().some)(_ => none).untilDefinedM

  def nextTask(client: Client[IO]): OptionT[IO, Task] =
    OptionT.liftF(
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
      case InitTask(config) => init(config)
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
    cursor.get[String]("taskType")
      .flatMap {
        case "init" => cursor.as[InitTask]
        case "apply" => cursor.as[ApplyTask]
      }

}

case class FunctionState(chainStates: Map[String, String], state: Json)

case class BlockWithMeta(meta: BlockMeta, block: Json)

case class BlockMeta(chain: String, blockId: String, height: Long)

sealed abstract class Task

case class InitTask(config: Json) extends Task

case class ApplyTask(blockWithChain: BlockWithMeta, stateWithChains: FunctionState) extends Task
```
There are several model/class definitions and their corresponding encoders.  In the future, we hope to provide helper libraries to handle all of this for you.  Alternatively, you can just use work with raw JSON at the risk of losing type safety.

## Assemble and Upload

Once your function is written, you can package the whole thing up into a self-contained JAR by running from the command line:
```sh
sbt assembly
```

Assuming a single-module SBT project, a JAR file will be produced at `target/scala-3.3.1/function.jar`, but its path will also be printed to the console.  This JAR file can be uploaded to Chainless.
