package chainless.runner.persistent

import cats.data.OptionT
import cats.effect.implicits.*
import cats.effect.std.Dispatcher
import cats.effect.{Async, Resource}
import cats.implicits.*
import chainless.db.ObjectStore
import chainless.models.Language
import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.command.{AsyncDockerCmd, SyncDockerCmd}
import com.github.dockerjava.api.model.{Bind, HostConfig, PruneType}
import com.github.dockerjava.core.{DefaultDockerClientConfig, DockerClientImpl}
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient
import fs2.io.file.{Files, Path}
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import retry.{RetryPolicies, retryingOnAllErrors}

import java.io.Closeable
import scala.concurrent.duration.*

/** Interacts with the Docker daemon to create and manage containers for running functions.
  * @param callbackBasePath
  *   the HTTP URL base path which is provided to user functions via the `CALLBACK_BASE_URL` environment variable
  * @param dockerClient
  *   client to interact with docker
  * @param objectStore
  *   object store containing the function code
  * @param tmpDir
  *   a directory in which a function can be extracted and mounted into a container
  */
class DockerDriver[F[_]: Async: Files](
    callbackBasePath: String,
    dockerClient: DockerClient,
    objectStore: ObjectStore[F],
    tmpDir: Path
) {
  given Logger[F] = Slf4jLogger.getLoggerFromName("DockerDriver")

  def createContainer(
      language: Language,
      functionRevisionId: String
  ): Resource[F, String] =
    prune.toResource >>
      currentCachedCodeFunctionId
        .filter(_ == functionRevisionId)
        .void
        .semiflatTap(_ => Logger[F].info(s"Code cache hit for functionRevisionId=$functionRevisionId"))
        .getOrElseF(clearLocalCodeCache >> fetchAndSave(language, functionRevisionId))
        .as(tmpDir / functionRevisionId)
        .toResource
        .flatMap(codeDir =>
          Async[F]
            .delay(DockerImage.forLanguage(language))
            .map(i =>
              i.split(':') match {
                case Array(repository, tag) => (repository, tag)
                case Array(repository)      => (repository, "latest")
                case _                      => throw MatchError(i)
              }
            )
            .toResource
            .flatMap((imageRepository, imageTag) =>
              Logger[F].info(s"Pulling $imageRepository:$imageTag").toResource >>
                useDockerAsync(dockerClient.pullImageCmd(imageRepository).withTag(imageTag))
                  .timeout(2.minutes)
                  .toResource >>
                Logger[F].info("Creating container").toResource >>
                Resource
                  .make(
                    useDocker(
                      dockerClient
                        .createContainerCmd(s"$imageRepository:$imageTag")
                        .withEnv(s"CALLBACK_BASE_URL=$callbackBasePath" +: DockerDriver.envForLanguage(language)*)
                        .withHostConfig(
                          HostConfig
                            .newHostConfig()
                            .withBinds(Bind.parse(s"$codeDir:/code:ro"))
                            .withNetworkMode("chainless")
                        )
                        .withCmd(DockerDriver.commandForLanguage(language)*)
                        .withWorkingDir("/code")
                    )
                      .map(_.getId)
                      .timeout(30.seconds)
                  )(removeContainer)
            )
        )

  def startContainer(containerId: String): F[Unit] =
    Logger[F].info(s"Starting container=$containerId") >>
      useDocker(dockerClient.startContainerCmd(containerId)) >>
      Logger[F].info(s"Started container=$containerId")

  private def currentCachedCodeFunctionId: OptionT[F, String] =
    OptionT
      .liftF(Files[F].exists(tmpDir))
      .filter(identity)
      .semiflatMap(_ => Files[F].list(tmpDir).evalFilter(Files[F].isDirectory).compile.toList)
      .subflatMap(_.headOption)
      .map(_.fileName.toString)

  private def clearLocalCodeCache: F[Unit] =
    Files[F]
      .exists(tmpDir)
      .ifM(
        Logger[F].info("Clearing function cache") *> Files[F]
          .list(tmpDir)
          .evalTap(Files[F].deleteRecursively(_))
          .compile
          .drain,
        Files[F].createDirectories(tmpDir)
      )

  private def fetchAndSave(language: Language, functionId: String): F[Unit] =
    for {
      _ <- Logger[F].info(s"Fetching function code for functionId=$functionId")
      _ <- Files[F].createDirectories(tmpDir / functionId)
      data = objectStore.get("functions")(functionId)
      _ <- (language match {
        case Language.JS =>
          data
            .through(Unzip[F]())
            .evalTap((name, isDirectory, data) =>
              if (isDirectory) data.compile.drain
              else
                (tmpDir / functionId / name)
                  .pure[F]
                  .flatTap(file => Logger[F].debug(s"Extracting $file"))
                  .flatTap(_.parent.traverse(Files[F].createDirectories))
                  .flatTap(file => data.through(Files[F].writeAll(file)).compile.drain)
                  .void
            )
            .compile
            .drain
        case Language.JVM =>
          (tmpDir / functionId / "function.jar")
            .pure[F]
            .flatTap(file => Logger[F].debug(s"Saving JAR $file"))
            .flatTap(file => data.through(Files[F].writeAll(file)).compile.drain)
            .void
      })
    } yield ()

  private def removeContainer(containerId: String): F[Unit] =
    Logger[F].info(s"Terminating container=$containerId") *>
      (useDocker(dockerClient.stopContainerCmd(containerId)).attempt.void >>
        useDocker(dockerClient.removeContainerCmd(containerId).withForce(true).withRemoveVolumes(true)).void)
        .timeout(45.seconds)

  private def useDocker[R](command: => SyncDockerCmd[R]): F[R] =
    Resource
      .fromAutoCloseable(Async[F].delay(command))
      .use(req => Async[F].blocking(req.exec))

  private def useDockerAsync[Req <: AsyncDockerCmd[Req, Res], Res](command: => AsyncDockerCmd[Req, Res]): F[Res] =
    Resource
      .fromAutoCloseable(Async[F].delay(command))
      .use(command =>
        Dispatcher
          .sequential[F]
          .use(dispatcher =>
            Async[F]
              .deferred[Either[Throwable, Res]]
              .flatMap(deferred =>
                Async[F].delay(command.exec(new ResultCallback[Res] {
                  private var latest: Option[Res] = None
                  private var error: Option[Throwable] = None
                  override def onStart(closeable: Closeable): Unit = ()
                  override def onNext(`object`: Res): Unit = latest = `object`.some
                  override def onError(throwable: Throwable): Unit = error = throwable.some
                  override def onComplete(): Unit =
                    error match {
                      case Some(e) => dispatcher.unsafeRunAndForget(deferred.complete(e.asLeft))
                      case _ =>
                        latest match {
                          case Some(res) => dispatcher.unsafeRunAndForget(deferred.complete(res.asRight))
                          case _ =>
                            dispatcher.unsafeRunAndForget(deferred.complete(new NoSuchElementException().asLeft))
                        }
                    }
                  override def close(): Unit = ()
                })) >> deferred.get.rethrow
              )
          )
      )

  def awaitDockerReady: F[Unit] =
    retryingOnAllErrors[Unit](
      policy = RetryPolicies.limitRetries(10).join(RetryPolicies.fibonacciBackoff(300.milli)),
      onError = (_: Throwable, _) => ().pure[F]
    )(useDocker(dockerClient.pingCmd()).void)

  def prune: F[Unit] =
    Logger[F].info("Pruning unused docker data") *>
      useDocker(dockerClient.pruneCmd(PruneType.BUILD)) >>
      useDocker(dockerClient.pruneCmd(PruneType.CONTAINERS)) >>
      useDocker(dockerClient.pruneCmd(PruneType.NETWORKS)) >>
      useDocker(dockerClient.pruneCmd(PruneType.VOLUMES)).void

}

object DockerDriver:
  def make[F[_]: Async](
      callbackBasePath: String,
      functionStoreClient: ObjectStore[F],
      tmpDir: Path
  ): Resource[F, DockerDriver[F]] =
    Resource
      .pure(DefaultDockerClientConfig.createDefaultConfigBuilder().build)
      .flatMap(config =>
        Resource
          .fromAutoCloseable(
            Async[F].delay(
              new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost)
                .sslConfig(config.getSSLConfig)
                .build
            )
          )
          .flatMap(httpClient =>
            Resource.fromAutoCloseable(Async[F].delay(DockerClientImpl.getInstance(config, httpClient)))
          )
      )
      .map(new DockerDriver(callbackBasePath, _, functionStoreClient, tmpDir))
      .evalTap(_.awaitDockerReady)

  def commandForLanguage(language: Language): List[String] =
    language match {
      case Language.JS  => List("node", "index.js")
      case Language.JVM => List("java", "-jar", "function.jar")
    }

  def envForLanguage(language: Language): List[String] =
    language match {
      case Language.JVM =>
        List(
          "_JAVA_OPTIONS=-XX:MaxRAMPercentage=70.0 -XX:ActiveProcessorCount=2"
        )
      case _ => Nil
    }

object DockerImage:
  val js: String = Option(System.getenv("CHAINLESS_RUNNER_JS_IMAGE")).getOrElse("node:slim")
  val jvm: String = Option(System.getenv("CHAINLESS_RUNNER_JVM_IMAGE")).getOrElse("eclipse-temurin:17-jre")

  def forLanguage(language: Language): String =
    language match {
      case Language.JS  => js
      case Language.JVM => jvm
    }
