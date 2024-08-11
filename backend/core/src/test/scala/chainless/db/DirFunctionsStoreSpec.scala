package chainless.db

import cats.effect.IO
import fs2.io.file.Files
import munit.CatsEffectSuite

class DirFunctionsStoreSpec extends CatsEffectSuite {

  test("Save, read, delete functions") {
    Files.forIO.tempDirectory.use(baseDir =>
      for {
        store <- IO.delay(new DirFunctionsStore[IO](baseDir))
        _ <- store.save("f1", 0)(fs2.Stream.emits("f1r0".getBytes))
        _ <- store.save("f1", 1)(fs2.Stream.emits("f1r1".getBytes))
        _ <- store.get("f1")(0).compile.to(Array).map(new String(_)).assertEquals("f1r0")
        _ <- store.get("f1")(1).compile.to(Array).map(new String(_)).assertEquals("f1r1")
        _ <- store.delete("f1")
      } yield ()
    )
  }

}
