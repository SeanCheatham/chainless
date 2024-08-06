package chainless.models

import cats.Show
import cats.implicits.*
import io.circe.*
import io.circe.syntax.*

sealed abstract class Language:
  def name: String

  override def toString: String = name

object Language:

  def parse(language: String): Option[Language] =
    language match {
      case "js"  => JS.some
      case "jvm" => JVM.some
      case _     => none
    }

  case object JS extends Language:
    def name: String = "js"
  case object JVM extends Language:
    def name: String = "jvm"

  given Encoder[Language] = _.name.asJson
  given Decoder[Language] = Decoder[String].emap(parse(_).toRight("Unknown language"))
  given Show[Language] = _.name
