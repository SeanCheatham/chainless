package chainless.runner.persistent

import cats.data.{NonEmptyChain, OptionT, Chain as CChain}
import cats.effect.implicits.*
import cats.effect.std.{Mutex, Queue}
import cats.effect.*
import cats.implicits.*
import cats.{MonadThrow, NonEmptyParallel}
import chainless.db.*
import chainless.models.*
import fs2.Chunk
import io.circe.Json
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.nio.charset.StandardCharsets
import scala.concurrent.duration.*

/** Handles a "job" which is a request to run a function. The job is processed by a sub-container which handles each
  * sub-task of the job. Once complete, the container is cleaned up.
  */
trait JobProcessor[F[_]]:
  def process(job: Job): F[Unit]

case class JobProcessorImpl[F[_]: Async: NonEmptyParallel](
    dockerDriver: DockerDriver[F],
    functionsDb: FunctionsDb[F],
    functionInvocationsDb: FunctionInvocationsDb[F],
    blockApiClient: BlocksDb[F],
    blockStoreClient: ObjectStore[F],
    jobMutex: Mutex[F],
    stateRef: Ref[F, Option[TaskProcessor[F]]],
    wasCanceledRef: Ref[F, Boolean]
) extends JobProcessor[F] {

  import JobProcessor.*

  private given Logger[F] = Slf4jLogger.getLoggerFromName("JobProcessor")

  def process(job: Job): F[Unit] = Async[F].uncancelable(_ =>
    (
      for {
        _ <- Logger[F].info(s"Running job functionId=${job.functionId} rpc=${job.rpc}")
        _ <- Logger[F].info(s"Fetching functionId=${job.functionId}")
        functionInfo <- functionsDb
          .get(job.functionId)
          .getOrRaise(new NoSuchElementException("Function does not exist"))
        (liteTasks, nextJob) <- createLiteTasks(job, functionInfo)
        _ <-
          if (liteTasks.isEmpty)
            Logger[F].info("No tasks to complete.  Skipping job.")
          else
            jobMutex.lock
              .surround(processImpl(job, functionInfo, liteTasks))
              .void
      } yield ()
    )
      .timeout(JobTimeout)
      .recoverWith { case e =>
        Logger[F].warn(e)(s"Error during processing functionId=${job.functionId}")
      }
  )

  def processImpl(job: Job, functionInfo: FunctionInfo, liteTasks: List[LiteTask]): F[Unit] =
    (
      for {
        completionDeferred <- Deferred[F, (FiniteDuration, Either[String, FunctionState], Option[Throwable])]
        functionRevision <- OptionT
          .fromOption(functionInfo.revision)
          .getOrRaise(new IllegalArgumentException("Function code not uploaded"))
        functionRevisionId = job.functionId + functionRevision.some.filter(_ > 0).fold("")(r => s"-$r")
        _ <- Logger[F].info(
          s"Launching container for functionId=${job.functionId} functionRevisionId=$functionRevisionId invocationId=${job.jobId}"
        )
        _ <- dockerDriver
          .createContainer(functionInfo.language, functionRevisionId)
          .use(containerId =>
            processWithInactiveContainer(containerId)(job, functionInfo, liteTasks, job.jobId, completionDeferred)
          )
      } yield ()
    )
      .guarantee(stateRef.set(none))

  def processWithInactiveContainer(containerId: String)(
      job: Job,
      functionInfo: FunctionInfo,
      liteTasks: List[LiteTask],
      jobId: String,
      completionDeferred: Deferred[F, (FiniteDuration, Either[String, FunctionState], Option[Throwable])]
  ): F[Unit] =
    for {
      initialTimeoutFiber <- Async[F]
        .delayBy(
          completionDeferred.complete((LaunchTimeout, "Container launch timed out".asLeft, none)),
          LaunchTimeout
        )
        .start
      startTime <- Async[F].realTime
      initialTask = TasksPending(
        job.functionId,
        FunctionState(functionInfo.chainStates, functionInfo.state),
        this,
        CChain.fromSeq(liteTasks),
        initialTimeoutFiber.cancel,
        (
            computeDuration,
            result,
            internalErrorOpt
        ) => completionDeferred.complete((computeDuration, result, internalErrorOpt)).void,
        Duration.Zero,
        startTime
      )
      _ <- stateRef.set(initialTask.some)
      _ <- dockerDriver.startContainer(containerId)
      (computeDuration, result, internalErrorOpt) <- completionDeferred.get
      endTime <- Async[F].realTime
      record =
        FunctionInvocation.permanent(
          jobId,
          job.functionId,
          startTime.toMillis,
          endTime.toMillis,
          computeDuration.toMillis
        )
      _ <- result.fold(
        error => functionsDb.setError(job.functionId)(error),
        state => functionsDb.setState(job.functionId)(state)
      )
      _ <- functionInvocationsDb.record(record)
      _ <- internalErrorOpt.traverse(Async[F].raiseError)
    } yield ()

  def nextTask: F[Option[Task]] =
    OptionT(stateRef.get)
      .getOrRaise(new IllegalStateException("Inactive"))
      .flatMap(processor =>
        processor.nextTask
          .flatMap((nextProcessor, nextTask) => stateRef.set(nextProcessor) >> nextTask.pure[F])
      )

  def completeTask(result: TaskResult): F[Unit] =
    OptionT(stateRef.get)
      .getOrRaise(new IllegalStateException("Inactive"))
      .flatMap(_.completeTask(result).flatMap(stateRef.set))
      .void

  /** @return
    *   (Tasks List, Requeue Dispatcher)
    */
  private def createLiteTasks(job: Job, function: FunctionInfo): F[(List[LiteTask], Option[Job])] =
    job.rpc match {
      case "init" =>
        Async[F]
          .fromEither(job.data.hcursor.getOrElse[Json]("config")(Json.Null))
          .map(InitLiteTask.apply)
          .flatMap(initTask =>
            OptionT(Async[F].fromEither(job.data.hcursor.get[Option[Long]]("retroactTimestampMs")))
              .foldF(
                function.chains
                  .traverse(blockApiClient.chainTip(_).semiflatMap(blockApiClient.getBlock).value)
                  .map(_.flatten)
                  .map((_, none))
              )(timestampMs =>
                function.chains
                  .traverse(chain =>
                    blockApiClient.blocksAfterTimestamp(NonEmptyChain(chain))(timestampMs).head.compile.last
                  )
                  .map(_.flatten)
                  .tupleRight(Job(Job.newJobId(), job.functionId, "apply", Json.Null).some)
              )
              .map((blocks, nextJob) => (initTask +: blocks.map(ApplyLiteTask.apply), nextJob))
          )
      case "apply" =>
        NonEmptyChain.fromSeq(function.chainStates.values.toSeq) match {
          case Some(blockIds) =>
            blockApiClient
              .blocksAfterBlocks(blockIds)
              .map(ApplyLiteTask.apply)
              .take(TaskBatchSize + 1)
              .compile
              .toList
              .map(applyTasks =>
                if (applyTasks.length > TaskBatchSize)
                  (applyTasks.take(TaskBatchSize), Job(Job.newJobId(), job.functionId, "apply", Json.Null).some)
                else (applyTasks, none)
              )
          case _ =>
            function.chains
              .traverse(blockApiClient.chainTip(_).semiflatMap(blockApiClient.getBlock).map(ApplyLiteTask.apply).value)
              .map(_.flatten)
              .map((_, Job(Job.newJobId(), job.functionId, "apply", Json.Null).some))
        }
    }

  def fetchBlock(meta: BlockMeta): F[BlockWithChain] =
    blockStoreClient
      .get(meta.chain.name)(meta.blockId)
      .compile
      .to(Chunk)
      .map(chunk => new String(chunk.toArray[Byte], StandardCharsets.UTF_8))
      .map(io.circe.parser.parse)
      .rethrow
      .map(BlockWithChain(meta, _))
      .timeout(5.seconds)

}

object JobProcessor:

  def make[F[_]: Async](
      dockerDriver: DockerDriver[F],
      functionsDb: FunctionsDb[F],
      functionInvocationsDb: FunctionInvocationsDb[F],
      blockApiClient: BlocksDb[F],
      blockStoreClient: ObjectStore[F],
      canceledRef: Ref[F, Boolean]
  ): Resource[F, JobProcessorImpl[F]] =
    (Mutex[F].toResource, Ref.of(none[TaskProcessor[F]]).toResource)
      .mapN(
        JobProcessorImpl(
          dockerDriver,
          functionsDb,
          functionInvocationsDb,
          blockApiClient,
          blockStoreClient,
          _,
          _,
          canceledRef
        )
      )

  val JobTimeout: FiniteDuration = 10.minutes
  val LaunchTimeout: FiniteDuration = 60.seconds
  val TaskTimeout: FiniteDuration = 30.seconds
  val TaskBatchSize: Int = 100

class MultiJobProcessor[F[_]: MonadCancelThrow](subProcessors: Queue[F, JobProcessor[F]]) extends JobProcessor[F]:
  override def process(job: Job): F[Unit] =
    subProcessors.take.flatMap(p => p.process(job).guarantee(subProcessors.offer(p)))

object MultiJobProcessor:
  def make[F[_]: Async](subProcessors: NonEmptyChain[JobProcessor[F]]): Resource[F, JobProcessor[F]] =
    Queue
      .bounded[F, JobProcessor[F]](subProcessors.size.toInt)
      .flatTap(queue => subProcessors.traverse(queue.offer))
      .toResource
      .map(new MultiJobProcessor[F](_))

object ErrorJobProcessor:
  def make[F[_]: MonadThrow]: JobProcessor[F] = new JobProcessor[F]:
    def process(job: Job): F[Unit] = MonadThrow[F].raiseError(new IllegalStateException("Job processing disabled"))

sealed abstract class LiteTask
case class InitLiteTask(config: Json) extends LiteTask
case class ApplyLiteTask(meta: BlockMeta) extends LiteTask

sealed abstract class TaskProcessor[F[_]]:
  def functionId: String

  def currentState: FunctionState

  def nextTask: F[(Option[TaskProcessor[F]], Option[Task])]

  def completeTask(result: TaskResult): F[Option[TaskProcessor[F]]]

  def cancelTimeout: F[Unit]

  def onFinished: (FiniteDuration, Either[String, FunctionState], Option[Throwable]) => F[Unit]

  def computeDuration: FiniteDuration

  def lastComputeStart: FiniteDuration

  def startTimeout(duration: FiniteDuration)(using Async[F])(using Logger[F]): F[F[Unit]] =
    Async[F]
      .delayBy(
        Async[F].defer(
          Logger[F]
            .warn(s"Timeout=$duration exceeded") >> onFinished(computeDuration + duration, currentState.asRight, none)
        ),
        duration
      )
      .start
      .map(fiber => Async[F].defer(fiber.cancel))

  protected def invalidState[T](message: String)(using Async[F])(using Logger[F]): F[T] =
    cancelTimeout *> Async[F].realTime
      .map(_ - lastComputeStart)
      .map(_ + computeDuration)
      .flatMap(onFinished(_, currentState.asRight, none)) *> Async[F]
      .delay(new IllegalStateException(message))
      .flatTap(Logger[F].warn(_)("TaskProcessor error"))
      .flatMap(Async[F].raiseError[T])

case class TasksPending[F[_]: Async: Logger](
    functionId: String,
    currentState: FunctionState,
    jobProcessor: JobProcessorImpl[F],
    tasks: CChain[LiteTask],
    cancelTimeout: F[Unit],
    onFinished: (FiniteDuration, Either[String, FunctionState], Option[Throwable]) => F[Unit],
    computeDuration: FiniteDuration,
    lastComputeStart: FiniteDuration
) extends TaskProcessor[F]:

  def nextTask: F[(Option[TaskProcessor[F]], Option[Task])] =
    Async[F].realTime
      .map(_ - lastComputeStart)
      .map(computeDuration + _)
      .flatMap(computeDuration =>
        cancelTimeout >>
          Async[F]
            .delay(tasks.uncons)
            .flatMap {
              case Some((t, remaining)) =>
                jobProcessor.wasCanceledRef.get
                  .ifM(
                    onFinished(computeDuration, currentState.asRight, none) >> (none, none).pure[F],
                    (t match {
                      case InitLiteTask(config) =>
                        InitTask(config).pure[F]
                      case ApplyLiteTask(block) =>
                        jobProcessor
                          .fetchBlock(block)
                          .onError(e => onFinished(computeDuration, currentState.asRight, e.some))
                          .map(ApplyBlockTask(currentState, _))
                    })
                      .flatTap(logTask)
                      .flatMap(task =>
                        Async[F].realTime.flatMap(computeStart =>
                          startTimeout(JobProcessor.TaskTimeout).map { cancelTimeout =>
                            val nextProcessor =
                              AwaitingTaskCompletion(
                                functionId,
                                currentState,
                                jobProcessor,
                                task,
                                remaining,
                                cancelTimeout,
                                onFinished,
                                computeDuration,
                                computeStart
                              )
                            nextProcessor.some -> task.some
                          }
                        )
                      )
                  )
              case _ =>
                onFinished(computeDuration, currentState.asRight, none) >> (none, none).pure[F]
            }
      )

  def completeTask(result: TaskResult): F[Option[TaskProcessor[F]]] =
    invalidState("Task has not been requested")

  private def logTask(task: Task) = task match {
    case _: InitTask =>
      Logger[F].info("Sending `init` task to sub-container")
    case ApplyBlockTask(stateWithChains, blockWithChain) =>
      Logger[F].info(
        "Sending `apply` task to sub-container." +
          s" chain=${blockWithChain.meta.chain}" +
          s" blockId=${blockWithChain.meta.blockId}" +
          s" previousBlockId=${stateWithChains.chainStates.getOrElse(blockWithChain.meta.chain.name, "")}"
      )
  }

case class AwaitingTaskCompletion[F[_]: Async: Logger](
    functionId: String,
    currentState: FunctionState,
    jobProcessor: JobProcessorImpl[F],
    activeTask: Task,
    remainingTasks: CChain[LiteTask],
    cancelTimeout: F[Unit],
    onFinished: (FiniteDuration, Either[String, FunctionState], Option[Throwable]) => F[Unit],
    computeDuration: FiniteDuration,
    lastComputeStart: FiniteDuration
) extends TaskProcessor[F]:
  def nextTask: F[(Option[TaskProcessor[F]], Option[Task])] =
    invalidState("New task requested before previous task finished")

  def completeTask(result: TaskResult): F[Option[TaskProcessor[F]]] =
    Async[F].realTime
      .map(_ - lastComputeStart)
      .map(computeDuration + _)
      .flatMap(computeDuration =>
        cancelTimeout >> (
          result match {
            case Left(reason) =>
              Logger[F].warn(s"Task failed with reason=$reason") >>
                onFinished(computeDuration, reason.asLeft, none) >>
                none[TaskProcessor[F]].pure[F]
            case Right(newFunctionState) =>
              Async[F]
                .delay {
                  val newChainStates = activeTask match {
                    case ApplyBlockTask(stateWithChains, blockWithChain) =>
                      stateWithChains.chainStates
                        .updated(blockWithChain.meta.chain.name, blockWithChain.meta.blockId)
                    case _ =>
                      currentState.chainStates
                  }
                  FunctionState(newChainStates, newFunctionState)
                }
                .flatMap(functionState =>
                  Async[F].realTime.flatMap(computeStart =>
                    startTimeout(5.seconds).map(cancelTimeout =>
                      TasksPending(
                        functionId,
                        functionState,
                        jobProcessor,
                        remainingTasks,
                        cancelTimeout,
                        onFinished,
                        computeDuration,
                        computeStart
                      ).some
                    )
                  )
                )
          }
        )
      )
