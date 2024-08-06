package chainless.db

import cats.effect.implicits.*
import cats.implicits.*
import cats.effect.{Async, Resource}
import chainless.models.*
import fs2.Stream

import java.sql.{Connection, PreparedStatement}
import java.time.Instant

/** Offers functionality for recording and retrieving the records of function invocations
  */
trait FunctionInvocationsDb[F[_]]:

  def record(invocation: FunctionInvocation): F[Unit]

  def byFunction(
      functionId: String,
      after: Instant = Instant.ofEpochMilli(0),
      before: Instant = Instant.ofEpochMilli(System.currentTimeMillis() * 2)
  ): Stream[F, FunctionInvocation]

  def temporary(
      after: Instant = Instant.MIN,
      before: Instant = Instant.MAX
  ): Stream[F, FunctionInvocation]

class SqlFunctionInvocationsDb[F[_]: Async](connection: Connection) extends FunctionInvocationsDb[F]:
  import SqlFunctionInvocationsDb.*

  override def record(invocation: FunctionInvocation): F[Unit] =
    Async[F].blocking {
      val statement = connection.prepareStatement(InsertInvocationSql)
      statement.setString(1, invocation.jobId)
      invocation.functionId.fold(statement.setNull(2, java.sql.Types.VARCHAR))(statement.setString(2, _))
      statement.setLong(3, invocation.startTimestampMs)
      statement.setLong(4, invocation.endTimestampMs)
      statement.setLong(5, invocation.activeDurationMs)
      statement.execute()
    }

  override def byFunction(
      functionId: String,
      after: Instant = Instant.ofEpochMilli(0),
      before: Instant = Instant.ofEpochMilli(System.currentTimeMillis() * 2)
  ): Stream[F, FunctionInvocation] =
    Stream
      .eval(Async[F].blocking {
        val statement = connection.prepareStatement(SelectInvocationsByFunctionSql)
        statement.setString(1, functionId)
        statement.setLong(2, after.toEpochMilli)
        statement.setLong(3, before.toEpochMilli)
        statement
      })
      .flatMap(invocationResultStream)

  override def temporary(
      after: Instant = Instant.MIN,
      before: Instant = Instant.MAX
  ): Stream[F, FunctionInvocation] =
    Stream
      .eval(Async[F].blocking {
        val statement = connection.prepareStatement(SelectInvocationsTemporarySql)
        statement.setLong(1, after.toEpochMilli)
        statement.setLong(2, before.toEpochMilli)
        statement
      })
      .flatMap(invocationResultStream)

  private def invocationResultStream(statement: PreparedStatement) =
    Stream
      .eval(
        Async[F].blocking(
          Iterator.unfold(statement.executeQuery())(resultSet =>
            Option.when(resultSet.next())(
              (
                FunctionInvocation(
                  resultSet.getString("job_id"),
                  Option(resultSet.getString("function_id")),
                  resultSet.getLong("start_timestamp_ms"),
                  resultSet.getLong("end_timestamp_ms"),
                  resultSet.getLong("active_duration_ms")
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

object SqlFunctionInvocationsDb:

  def make[F[_]: Async](connection: Connection): Resource[F, FunctionInvocationsDb[F]] =
    init(connection).toResource >>
      Async[F].delay(new SqlFunctionInvocationsDb[F](connection)).toResource

  def init[F[_]: Async](connection: Connection): F[Unit] =
    Async[F].blocking {
      val statement = connection.createStatement()
      // TODO: function_id foreign key
      statement.execute(
        """
          |CREATE TABLE IF NOT EXISTS function_invocations (
          |  job_id VARCHAR(64) PRIMARY KEY,
          |  function_id VARCHAR(64),
          |  start_timestamp_ms BIGINT NOT NULL,
          |  end_timestamp_ms BIGINT NOT NULL,
          |  active_duration_ms BIGINT NOT NULL
          |)
          |""".stripMargin
      )
      statement.execute(
        "CREATE INDEX IF NOT EXISTS function_invocations_function_id ON function_invocations (function_id)"
      )
      statement.execute(
        "CREATE INDEX IF NOT EXISTS function_invocations_start_timestamp ON function_invocations (start_timestamp_ms)"
      )
      statement.execute(
        "CREATE INDEX IF NOT EXISTS function_invocations_end_timestamp ON function_invocations (end_timestamp_ms)"
      )
    }

  val InsertInvocationSql =
    """INSERT INTO function_invocations (job_id, function_id, start_timestamp_ms, end_timestamp_ms, active_duration_ms)
      |VALUES (?, ?, ?, ?, ?)""".stripMargin

  val SelectInvocationsByFunctionSql =
    """SELECT *
      |FROM function_invocations
      |WHERE function_id = ?
      |AND start_timestamp_ms >= ?
      |AND end_timestamp_ms <= ?""".stripMargin

  val SelectInvocationsTemporarySql =
    """SELECT *
      |FROM function_invocations
      |WHERE function_id IS NULL
      |AND start_timestamp_ms >= ?
      |AND end_timestamp_ms <= ?""".stripMargin
