package chainless

import caseapp.*
import cats.data.{Chain, NonEmptyChain}
import cats.effect.kernel.Ref
import cats.effect.{IO, Resource, ResourceApp}
import cats.implicits.*
import chainless.db.{FunctionInvocationsDb, *}
import chainless.models.*
import chainless.replicator.Replicator
import chainless.replicator.provider.{BitcoinProvider, EthereumProvider}
import chainless.runner.persistent.*
import chainless.runner.temporary.*
import chainless.utils.Healthcheck
import fs2.concurrent.Topic
import fs2.io.file.{Files, Path}
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder
import org.typelevel.log4cats.slf4j.{Slf4jFactory, Slf4jLogger}
import org.typelevel.log4cats.{Logger, LoggerFactory}

import scala.concurrent.duration.*

object ChainlessMain extends ResourceApp.Forever {

  private given LoggerFactory[IO] = Slf4jFactory.create[IO]
  given Logger[F] = Slf4jLogger.getLoggerFromName("Chainless")

  type F[A] = IO[A]

  override def run(args: List[String]): Resource[F, Unit] =
    for {
      _ <- GraalSupport.verifyCompatibility[F]
      given Client[F] <- EmberClientBuilder.default[F].withTimeout(5.seconds).build
      given Files[F] = Files.forIO
      (args, _) <- IO
        .fromEither(CaseApp.parse[RunnerManagerArgs](args).leftMap(_.message).leftMap(new IllegalArgumentException(_)))
        .toResource
      _ <- Files[F].createDirectories(Path(args.dataDir)).toResource
      healthcheck <- Healthcheck.make
      _ <- healthcheck.setLiveliness(Healthcheck.Healthy()).toResource
      _ <- healthcheck.serve(args.healthcheckBindHost, args.healthcheckBindPort)
      newBlocksTopic <- Resource.make(Topic[F, BlockMeta])(_.close.void)
      sqliteConnection <- Sqlite.connection[F]((Path(args.dataDir) / "chainless.db").toString)
      functionsDb <- SqlFunctionsDb.make[F](sqliteConnection)
      functionInvocationsDb <- SqlFunctionInvocationsDb.make[F](sqliteConnection)
      blocksDb <- SqlBlocksDb.make[F](sqliteConnection)
      objectStore = new ObjectStore[F](Path(args.dataDir) / "objects")
      runnerOperator = new RunnerOperator[F](
        blocksDb,
        objectStore,
        functionInvocationsDb,
        newBlocksTopic.subscribeUnbounded
      )
      jobProcessor <- makeJobProcessor(args, objectStore, functionsDb, functionInvocationsDb, blocksDb)
      bitcoinProvider <- args.bitcoinRpcAddress.traverse(BitcoinProvider.make[F])
      ethereumProvider <- args.ethereumRpcAddress.traverse(EthereumProvider.make[F])
      providers = (bitcoinProvider.toList ++ ethereumProvider).map(provider => provider.chain -> provider).toMap
      _ <- IO.raiseWhen(providers.isEmpty)(new IllegalArgumentException("No chains configured")).toResource
      replicator = new Replicator[F](blocksDb, providers, objectStore, newBlocksTopic.publish1(_).void)
      dispatcher = new JobDispatcher[F](newBlocksTopic.subscribeUnbounded, functionsDb, jobProcessor)
      _ <- healthcheck.setReadiness(Healthcheck.Healthy()).toResource
      apiServer = new ApiServer(functionsDb, objectStore, functionInvocationsDb, runnerOperator, dispatcher)
      _ <- (
        Logger[F].info("Running").toResource,
        replicator.replicate.toResource,
        dispatcher.background.compile.drain.background,
        apiServer.serve(args.bindHost, args.bindPort)
      ).parTupled
    } yield ()

  private def makeJobProcessor(
      args: RunnerManagerArgs,
      objectStore: ObjectStore[F],
      functionsDb: FunctionsDb[F],
      functionInvocationsDb: FunctionInvocationsDb[F],
      blocksDb: BlocksDb[F]
  ) =
    Ref
      .of[F, Boolean](false)
      .toResource
      .flatMap(canceledRef =>
        NonEmptyChain
          .fromChainUnsafe(Chain.fromSeq(List.tabulate(args.runnerCount)(i => i + args.runnerApiBindPortStart)))
          .traverse(port =>
            Files[F].tempDirectory
              .flatMap(localCodeCache =>
                DockerDriver.make[F](
                  s"http://localhost:$port",
                  objectStore,
                  localCodeCache
                )
              )
              .flatMap(dockerDriver =>
                JobProcessor.make[F](
                  dockerDriver,
                  functionsDb,
                  functionInvocationsDb,
                  blocksDb,
                  objectStore,
                  canceledRef
                )
              )
              .flatTap(jobProcessor =>
                new RunnerHttpServer(jobProcessor.nextTask, jobProcessor.completeTask).serve("localhost", port)
              )
          )
          .flatMap(MultiJobProcessor.make[F])
          .flatTap(_ => Resource.onFinalize(canceledRef.set(true)))
      )
}

@AppName("Chainless")
case class RunnerManagerArgs(
    dataDir: String = "/app/data",
    bitcoinRpcAddress: Option[String] = None,
    ethereumRpcAddress: Option[String] = None,
    bindHost: String = "0.0.0.0",
    bindPort: Int = 42069,
    runnerApiBindPortStart: Int = 8093,
    runnerCount: Int = 1,
    healthcheckBindHost: String = "0.0.0.0",
    healthcheckBindPort: Int = 9999
)
