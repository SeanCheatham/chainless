package chainless.models

import cats.{Eq, Show}
import cats.implicits.*
import io.circe.*
import io.circe.syntax.*

sealed abstract class Chain:
  def name: String

  override def toString: String = name

object Chain:

  def parse(chain: String): Option[Chain] =
    all.find(_.name == chain)

  case object Bitcoin extends Chain:
    val name: String = "bitcoin"

  case object Ethereum extends Chain:
    val name: String = "ethereum"

  case object Apparatus extends Chain:
    val name: String = "apparatus"

  val all: List[Chain] = List(Bitcoin, Ethereum, Apparatus)

  given Encoder[Chain] = _.toString.asJson
  given Decoder[Chain] = Decoder[String].emap(parse(_).toRight("Unknown chain"))
  given Show[Chain] = _.name
  given Eq[Chain] = Eq.fromUniversalEquals
