package chainless

import cats.MonadThrow
import cats.implicits.*
import fs2.{RaiseThrowable, Stream}
import org.typelevel.log4cats.Logger

package object utils:

  extension [F[_], A](io: F[A])
    def logError(using MonadThrow[F])(using Logger[F]): F[A] =
      io.onError(e => Logger[F].warn(e)("Request error"))

  extension [F[_], A](stream: Stream[F, A])
    def logError(using RaiseThrowable[F])(using Logger[F]): Stream[F, A] =
      stream.handleErrorWith(e => Stream.exec(Logger[F].warn(e)("Stream error")) ++ Stream.raiseError(e))
