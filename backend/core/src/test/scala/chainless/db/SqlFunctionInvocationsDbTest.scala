package chainless.db

import cats.effect.IO
import chainless.models.*
import fs2.io.file.Files
import munit.CatsEffectSuite

import java.time.Instant

class SqlFunctionInvocationsDbTest extends CatsEffectSuite {
  test("Record and retrieve function invocations") {
    val i1 = FunctionInvocation.temporary("j1", 1, 10, 5)
    val i2 = FunctionInvocation.temporary("j2", 11, 20, 4)
    val i3 = FunctionInvocation.temporary("j3", 50, 80, 10)

    val i4 = FunctionInvocation.permanent("j4", "a", 100, 150, 10)
    val i5 = FunctionInvocation.permanent("j5", "b", 110, 130, 20)
    val i6 = FunctionInvocation.permanent("j6", "a", 170, 190, 30)
    Files.forIO.tempFile
      .map(_.toString)
      .flatMap(Sqlite.connection)
      .flatMap(SqlFunctionInvocationsDb.make)
      .use(db =>
        for
          _ <- db.record(i1)
          _ <- db.record(i2)
          _ <- db.record(i3)
          _ <- db.byFunction("a").compile.toList.assertEquals(Nil)
          _ <- db.temporary().compile.toList.assertEquals(List(i1, i2, i3))
          _ <- db.record(i4)
          _ <- db.record(i5)
          _ <- db.record(i6)
          _ <- db.byFunction("a").compile.toList.assertEquals(List(i4, i6))
          _ <- db.byFunction("a", after = Instant.ofEpochMilli(160)).compile.toList.assertEquals(List(i6))
          _ <- db.byFunction("a", before = Instant.ofEpochMilli(160)).compile.toList.assertEquals(List(i4))
          _ <- db.byFunction("b").compile.toList.assertEquals(List(i5))
        yield ()
      )

  }
}
