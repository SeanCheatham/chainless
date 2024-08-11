package chainless.runner.temporary

import cats.data.{NonEmptyChain, OptionT}
import cats.effect.{IO, Resource}
import chainless.db.{BlocksDb, BlocksStore, FunctionInvocationsDb}
import chainless.models.*
import io.circe.syntax.*
import io.circe.*
import munit.CatsEffectSuite
import fs2.*

import java.time.Instant

class RunnerOperatorTest extends CatsEffectSuite {
  private val b1 = BlockWithChain(BlockMeta(Chain.Bitcoin, "b1", "b0", 1, 50), Json.obj("a" -> true.asJson))
  private val b2 = BlockWithChain(BlockMeta(Chain.Bitcoin, "b2", "b1", 2, 51), Json.obj("a" -> true.asJson))
  private val e1 = BlockWithChain(BlockMeta(Chain.Ethereum, "e1", "e0", 1, 70), Json.obj("b" -> true.asJson))

  private val expectedS1 = FunctionState(Map("bitcoin" -> "b1"), Json.obj("bitcoin" -> 1.asJson))
  private val expectedS2 = FunctionState(Map("bitcoin" -> "b2"), Json.obj("bitcoin" -> 2.asJson))
  private val expectedS3 =
    FunctionState(Map("bitcoin" -> "b2", "ethereum" -> "e1"), Json.obj("bitcoin" -> 2.asJson, "ethereum" -> 1.asJson))

  private val blocksDb = new BlocksDb[IO] {
    override def insert(meta: BlockMeta): IO[Unit] = ???

    override def blocksAfterTimestamp(chains: NonEmptyChain[Chain])(timestampMs: Long): Stream[IO, BlockMeta] =
      Stream(b1.meta, b2.meta)

    override def blocksAfterBlocks(blockIds: NonEmptyChain[String]): Stream[IO, BlockMeta] = ???

    override def getBlock(blockId: String): IO[BlockMeta] = ???

    override def chainTip(chain: Chain): OptionT[IO, String] = ???
  }
  private val blocksStore = new BlocksStore[IO] {
    override def saveBlock(block: BlockWithChain): IO[Unit] = ???

    override def getBlock(meta: BlockMeta): IO[BlockWithChain] = IO.delay(List(b1, b2, e1).find(_.meta == meta).get)
  }

  private val invocationsDb = new FunctionInvocationsDb[IO] {

    def record(invocation: FunctionInvocation): IO[Unit] = IO.unit

    def byFunction(
        functionId: String,
        after: Instant = Instant.ofEpochMilli(0),
        before: Instant = Instant.ofEpochMilli(System.currentTimeMillis() * 2)
    ): Stream[IO, FunctionInvocation] = Stream.raiseError(new IllegalStateException())

    def temporary(
        after: Instant = Instant.ofEpochMilli(0),
        before: Instant = Instant.ofEpochMilli(Long.MaxValue)
    ): Stream[IO, FunctionInvocation] = Stream.raiseError(new IllegalStateException())
  }
  test("Run js") {
    for {
      code <- Resource
        .fromAutoCloseable(IO.delay(scala.io.Source.fromResource("functions/f1.js")))
        .use(s => IO.blocking(s.mkString))
      operator = new RunnerOperator[IO](blocksDb, blocksStore, invocationsDb, Stream(e1.meta))
      resultStates <- operator.retroact(code, "js")(0, NonEmptyChain(Chain.Bitcoin, Chain.Ethereum)).compile.toList
      _ <- IO(List(expectedS1, expectedS2)).assertEquals(resultStates)
      resultStatesLive <- operator
        .live(code, "js", resultStates.last)(NonEmptyChain(Chain.Bitcoin, Chain.Ethereum))
        .compile
        .toList
      _ <- IO(List(expectedS3)).assertEquals(resultStatesLive)
    } yield ()
  }

  test("Run python") {
    for {
      code <- Resource
        .fromAutoCloseable(IO.delay(scala.io.Source.fromResource("functions/f1.py")))
        .use(s => IO.blocking(s.mkString))
      operator = new RunnerOperator[IO](blocksDb, blocksStore, invocationsDb, Stream(e1.meta))
      resultStates <- operator.retroact(code, "python")(0, NonEmptyChain(Chain.Bitcoin, Chain.Ethereum)).compile.toList
      _ <- IO(List(expectedS1, expectedS2)).assertEquals(resultStates)
      resultStatesLive <- operator
        .live(code, "python", resultStates.last)(NonEmptyChain(Chain.Bitcoin, Chain.Ethereum))
        .compile
        .toList
      _ <- IO(List(expectedS3)).assertEquals(resultStatesLive)
    } yield ()
  }

}
