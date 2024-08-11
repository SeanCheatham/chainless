package chainless.runner.temporary

import cats.effect.IO
import munit.CatsEffectSuite

class GraalSupportTest extends CatsEffectSuite {
  test("verify compatibility") {
    GraalSupport.verifyCompatibility[IO]
  }

}
