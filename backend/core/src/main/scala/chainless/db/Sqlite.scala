package chainless.db

import cats.effect.{Async, Resource}

import java.sql.Connection

object Sqlite {

  def connection[F[_]: Async](name: String): Resource[F, Connection] =
    Resource.fromAutoCloseable(
      Async[F].delay {
        Class.forName("org.sqlite.JDBC")
        java.sql.DriverManager.getConnection(s"jdbc:sqlite:$name")
      }
    )

}
