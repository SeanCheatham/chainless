package chainless

import cats.effect.IO
import munit.CatsEffectSuite

class StubTest extends CatsEffectSuite {

  test("Hello World") {
    IO(true).assertEquals(true)
  }

}
