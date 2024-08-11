package chainless.db

import cats.effect.IO
import chainless.models.*
import fs2.io.file.Files
import munit.CatsEffectSuite
import io.circe.*
import io.circe.syntax.*

class DirBlocksStoreSpec extends CatsEffectSuite {

  test("Save, read blocks") {
    val b1 = BlockWithChain(BlockMeta(Chain.Bitcoin, "b1", "b0", 1, 50), Json.obj("a" -> true.asJson))
    Files.forIO.tempDirectory.use(baseDir =>
      for {
        store <- IO.delay(new DirBlocksStore[IO](baseDir))
        _ <- store.saveBlock(b1)
        _ <- store.getBlock(b1.meta).assertEquals(b1)
      } yield ()
    )
  }

}
