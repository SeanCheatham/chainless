package chainless.db

import cats.effect.implicits.*
import cats.implicits.*
import cats.data.OptionT
import cats.effect.{Async, Resource}
import chainless.models.*
import fs2.Stream

import java.sql.Connection
import java.util.UUID
import io.circe.*
import io.circe.syntax.*

/** Offers functionality for creating, updating, and retrieving functions
  */
trait FunctionsDb[F[_]]:
  def create(name: String, language: Language, chains: List[Chain]): F[String]

  def setState(id: String)(stateWithChains: FunctionState): F[Unit]

  def setError(id: String)(error: String): F[Unit]

  def get(id: String): OptionT[F, FunctionInfo]

  def list: Stream[F, String]

  def initializedFunctionIdsForChain(chain: Chain): Stream[F, String]

  def delete(id: String): F[Boolean]

  def updateRevision(id: String, newRevision: Int): F[Unit]

class SqlFunctionsDb[F[_]: Async](connection: Connection) extends FunctionsDb[F]:
  import SqlFunctionsDb.*

  override def create(name: String, language: Language, chains: List[Chain]): F[String] =
    Async[F].blocking {
      val statement = connection.prepareStatement(InsertFunctionSql)
      val id = UUID.randomUUID().toString
      statement.setString(1, id)
      statement.setString(2, name)
      statement.setString(3, language.toString)
      statement.setString(4, chains.map(_.toString.asJson).asJson.noSpaces)
      statement.setString(5, "{}")
      statement.execute()
      id
    }

  override def setState(id: String)(stateWithChains: FunctionState): F[Unit] =
    Async[F].blocking {
      val statement = connection.prepareStatement(UpdateStateSql)
      statement.setString(1, stateWithChains.state.noSpaces)
      statement.setString(2, stateWithChains.chainStates.asJson.noSpaces)
      statement.setString(3, id)
      statement.execute()
    }

  override def setError(id: String)(error: String): F[Unit] =
    Async[F].blocking {
      val statement = connection.prepareStatement(UpdateErrorSql)
      statement.setString(1, error)
      statement.setString(2, id)
      statement.execute()
    }

  override def get(id: String): OptionT[F, FunctionInfo] =
    OptionT(
      Stream
        .eval(Async[F].blocking {
          val statement = connection.prepareStatement(SelectFunctionSql)
          statement.setString(1, id)
          statement
        })
        .flatMap(statement =>
          Stream
            .eval(
              Async[F].blocking(
                Iterator.unfold(statement.executeQuery())(resultSet =>
                  Option.when(resultSet.next())(
                    (
                      FunctionInfo(
                        resultSet.getString("name"),
                        Language.parse(resultSet.getString("language")).get,
                        io.circe.parser
                          .parse(
                            resultSet
                              .getString("chains")
                          )
                          .flatMap(_.as[List[String]])
                          .toOption
                          .get
                          .map(Chain.parse(_).get),
                        io.circe.parser
                          .parse(resultSet.getString("chain_states"))
                          .toOption
                          .get
                          .as[Map[String, String]]
                          .toOption
                          .get,
                        Option(resultSet.getString("state")).fold(Json.Null)(io.circe.parser.parse(_).toOption.get),
                        Option(resultSet.getString("error")),
                        resultSet.getBoolean("initialized"),
                        Option(resultSet.getInt("revision")).filterNot(_ => resultSet.wasNull())
                      ),
                      resultSet
                    )
                  )
                )
              )
            )
            .flatMap(
              Stream.fromBlockingIterator[F](_, 1)
            )
        )
        .head
        .compile
        .last
    )

  override def list: Stream[F, String] = Stream
    .eval(Async[F].blocking {
      connection.prepareStatement(ListFunctionsSql)
    })
    .flatMap(statement =>
      Stream
        .eval(
          Async[F].blocking(
            Iterator.unfold(statement.executeQuery())(resultSet =>
              Option.when(resultSet.next())(
                (
                  resultSet.getString("id"),
                  resultSet
                )
              )
            )
          )
        )
        .flatMap(
          Stream.fromBlockingIterator[F](_, 1)
        )
    )

  override def initializedFunctionIdsForChain(chain: Chain): Stream[F, String] = Stream
    .eval(Async[F].blocking {
      val statement = connection.prepareStatement(InitializedFunctionIdsForChainSql)
      statement.setString(1, s"%$chain%")
      statement
    })
    .flatMap(statement =>
      Stream
        .eval(
          Async[F].blocking(
            Iterator.unfold(statement.executeQuery())(resultSet =>
              Option.when(resultSet.next())(
                (
                  resultSet.getString("id"),
                  resultSet
                )
              )
            )
          )
        )
        .flatMap(
          Stream.fromBlockingIterator[F](_, 1)
        )
    )

  override def delete(id: String): F[Boolean] =
    Async[F].blocking {
      val statement = connection.prepareStatement(DeleteFunctionSql)
      statement.setString(1, id)
      statement.execute()
      statement.getUpdateCount > 0
    }

  override def updateRevision(id: String, newRevision: Int): F[Unit] =
    Async[F].blocking {
      val statement = connection.prepareStatement(UpdateRevisionSql)
      statement.setInt(1, newRevision)
      statement.setString(2, id)
      statement.execute()
    }

object SqlFunctionsDb:

  def make[F[_]: Async](connection: Connection): Resource[F, FunctionsDb[F]] =
    init(connection).toResource >>
      Async[F].delay(new SqlFunctionsDb[F](connection)).toResource

  def init[F[_]: Async](connection: Connection): F[Unit] =
    Async[F].blocking {
      val statement = connection.createStatement()
      statement.execute(
        // TODO: chains array?
        """
          |CREATE TABLE IF NOT EXISTS functions (
          |  id VARCHAR(128) PRIMARY KEY,
          |  name VARCHAR(256) NOT NULL,
          |  language VARCHAR(32) NOT NULL,
          |  chains VARCHAR(256) NOT NULL,
          |  chain_states TEXT NOT NULL,
          |  state TEXT,
          |  error TEXT,
          |  revision INTEGER,
          |  initialized BOOLEAN NOT NULL DEFAULT FALSE
          |)
          |""".stripMargin
      )

      statement.execute("CREATE INDEX IF NOT EXISTS functions_initialized ON functions (initialized)")
      statement.execute("CREATE INDEX IF NOT EXISTS functions_chains ON functions (chains)")
    }

  val InsertFunctionSql =
    """INSERT INTO functions (id, name, language, chains, chain_states, revision, initialized)
        |VALUES (?, ?, ?, ?, ?, NULL, false)""".stripMargin

  val UpdateStateSql =
    """UPDATE functions
        |SET state = ?, chain_states = ?, initialized = true
        |WHERE id = ?""".stripMargin

  val UpdateErrorSql =
    """UPDATE functions
        |SET error = ?, initialized = true
        |WHERE id = ?""".stripMargin

  val SelectFunctionSql =
    """SELECT *
        |FROM functions
        |WHERE id = ?
        |LIMIT 1""".stripMargin

  val ListFunctionsSql =
    """SELECT id
        |FROM functions""".stripMargin

  val InitializedFunctionIdsForChainSql =
    """SELECT id
        |FROM functions
        |WHERE initialized = 1 AND chains LIKE ? AND error IS NULL""".stripMargin

  val DeleteFunctionSql =
    """DELETE FROM functions
        |WHERE id = ?""".stripMargin

  val UpdateRevisionSql =
    """UPDATE functions
        |SET revision = ?
        |WHERE id = ?""".stripMargin
