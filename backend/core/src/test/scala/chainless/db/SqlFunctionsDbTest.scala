package chainless.db

import cats.effect.IO
import cats.implicits.*
import chainless.models.*
import fs2.io.file.Files
import io.circe.Json
import munit.CatsEffectSuite

class SqlFunctionsDbTest extends CatsEffectSuite {
  test("Insert, retrieve, and update functions") {
    val f1Base =
      FunctionInfo("f1", Language.JVM, List(Chain.Bitcoin, Chain.Ethereum), Map.empty, Json.Null, none, false, none)
    val f1NewState = FunctionState(Map("bitcoin" -> "b1"), Json.obj())
    Files.forIO.tempFile
      .map(_.toString)
      .flatMap(Sqlite.connection)
      .flatMap(SqlFunctionsDb.make)
      .use(db =>
        for
          f1Id <- db.create("f1", Language.JVM, List(Chain.Bitcoin, Chain.Ethereum))
          _ <- db
            .get(f1Id)
            .getOrRaise(new IllegalArgumentException())
            .assertEquals(f1Base)
          _ <- db.setState(f1Id)(f1NewState)
          f1WithState <- db
            .get(f1Id)
            .getOrRaise(new IllegalArgumentException())
          _ <- IO(f1WithState).assertEquals(
            f1Base.copy(initialized = true, chainStates = f1NewState.chainStates, state = f1NewState.state)
          )
          _ <- db.updateRevision(f1Id, 2)
          f1WithRevision <- db
            .get(f1Id)
            .getOrRaise(new IllegalArgumentException())
          _ <- IO(f1WithRevision).assertEquals(f1WithState.copy(revision = 2.some))
          _ <- db.initializedFunctionIdsForChain(Chain.Bitcoin).compile.toList.assertEquals(List(f1Id))
          _ <- db.setError(f1Id)("sad")
          f1WithError <- db
            .get(f1Id)
            .getOrRaise(new IllegalArgumentException())
          _ <- IO(f1WithError).assertEquals(f1WithRevision.copy(error = "sad".some))
          _ <- db.initializedFunctionIdsForChain(Chain.Bitcoin).compile.toList.assertEquals(Nil)
          _ <- db.list.compile.toList.assertEquals(List(f1Id))
          _ <- db.delete(f1Id).assert
          _ <- db.list.compile.toList.assertEquals(Nil)
          _ <- db.get(f1Id).value.assertEquals(none)
        yield ()
      )

  }
}
