package chainless.db

import cats.data.NonEmptyChain
import cats.implicits.*
import chainless.models.*
import fs2.io.file.Files
import munit.CatsEffectSuite

class SqlBlocksDbTest extends CatsEffectSuite {
  test("Insert and retrieve blocks") {
    val blocks @ List(b1, b2, b3, e1, e2, e3) = List(
      BlockMeta(Chain.Bitcoin, "b1", "b0", 1, 1),
      BlockMeta(Chain.Bitcoin, "b2", "b1", 2, 2),
      BlockMeta(Chain.Bitcoin, "b3", "b2", 3, 3),
      BlockMeta(Chain.Ethereum, "e1", "e0", 1, 1),
      BlockMeta(Chain.Ethereum, "e2", "e1", 2, 2),
      BlockMeta(Chain.Ethereum, "e3", "e2", 3, 3)
    )
    Files.forIO.tempFile
      .map(_.toString)
      .flatMap(Sqlite.connection)
      .flatMap(SqlBlocksDb.make)
      .use(db =>
        for
          _ <- blocks.traverse(db.insert)
          _ <- db.getBlock("b1").assertEquals(b1)
          _ <- db.getBlock("b2").assertEquals(b2)
          _ <- db.getBlock("b3").assertEquals(b3)
          _ <- db.getBlock("e1").assertEquals(e1)
          _ <- db.getBlock("e2").assertEquals(e2)
          _ <- db.getBlock("e3").assertEquals(e3)
          _ <- db.chainTip(Chain.Bitcoin).getOrRaise(new IllegalArgumentException()).assertEquals("b3")
          _ <- db.chainTip(Chain.Ethereum).getOrRaise(new IllegalArgumentException()).assertEquals("e3")
          _ <- db.blocksAfterTimestamp(NonEmptyChain.one(Chain.Bitcoin))(2).compile.toList.assertEquals(List(b3))
          _ <- db.blocksAfterTimestamp(NonEmptyChain.one(Chain.Ethereum))(1).compile.toList.assertEquals(List(e2, e3))
          _ <- db.blocksAfterBlocks(NonEmptyChain.one("b1")).compile.toList.assertEquals(List(b2, b3))
          _ <- db.blocksAfterBlocks(NonEmptyChain.one("e2")).compile.toList.assertEquals(List(e3))
        yield ()
      )

  }
}
