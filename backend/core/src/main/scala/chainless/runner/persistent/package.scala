package chainless.runner

import chainless.models.{*, given}
import io.circe.*
import io.circe.syntax.*

package object persistent:
  sealed abstract class Task
  case class ApplyBlockTask(stateWithChains: FunctionState, blockWithChain: BlockWithChain) extends Task
  case class InitTask(config: Json) extends Task

  given Encoder[Task] = {
    case ApplyBlockTask(stateWithChains, blockWithChain) =>
      Json.obj(
        "taskType" -> "apply".asJson,
        "stateWithChains" -> stateWithChains.asJson,
        "blockWithChain" -> blockWithChain.asJson
      )
    case InitTask(config) =>
      Json.obj(
        "taskType" -> "init".asJson,
        "config" -> config
      )
  }

  type TaskResult = Either[String, Json]
