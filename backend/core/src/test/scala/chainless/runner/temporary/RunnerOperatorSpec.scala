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

class RunnerOperatorSpec extends CatsEffectSuite {
  private val b1 = BlockWithChain(BlockMeta(Chain.Bitcoin, "b1", "b0", 1, 50), Json.obj("a" -> true.asJson))
  private val b2 = BlockWithChain(BlockMeta(Chain.Bitcoin, "b2", "b1", 2, 51), Json.obj("a" -> true.asJson))
  private val e1 = BlockWithChain(BlockMeta(Chain.Ethereum, "e1", "e0", 1, 70), Json.obj("b" -> true.asJson))

  private val expectedS1 = Json.obj("bitcoin" -> 1.asJson)
  private val expectedS2 = Json.obj("bitcoin" -> 2.asJson)
  private val expectedS3 = Json.obj("bitcoin" -> 2.asJson, "ethereum" -> 1.asJson)

  test("Run js") {
    val blocksDb = new BlocksDb[IO] {
      override def insert(meta: BlockMeta): IO[Unit] = ???

      override def blocksAfterTimestamp(chains: NonEmptyChain[Chain])(timestampMs: Long): Stream[IO, BlockMeta] =
        Stream(b1.meta, b2.meta)

      override def blocksAfterBlocks(blockIds: NonEmptyChain[String]): Stream[IO, BlockMeta] = ???

      override def getBlock(blockId: String): IO[BlockMeta] = ???

      override def chainTip(chain: Chain): OptionT[IO, String] = ???
    }
    val blocksStore = new BlocksStore[IO] {
      override def saveBlock(block: BlockWithChain): IO[Unit] = ???

      override def getBlock(meta: BlockMeta): IO[BlockWithChain] = IO.delay(List(b1, b2, e1).find(_.meta == meta).get)
    }

    val invocationsDb = new FunctionInvocationsDb[IO] {

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
      _ <- LocalGraalRunner
        .make[IO](code, "python")
        .use(runner =>
          for {
            s0 <- IO.pure(FunctionState(Map.empty, Json.obj()))
            s1 <- runner.applyBlock(s0, b1)
            _ <- IO(s1.state).assertEquals(expectedS1)
            s2 <- runner.applyBlock(s1, b2)
            _ <- IO(s2.state).assertEquals(expectedS2)
            s3 <- runner.applyBlock(s2, e1)
            _ <- IO(s3.state).assertEquals(expectedS3)
          } yield ()
        )
    } yield ()

  }

}
