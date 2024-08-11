package chainless.runner.persistent

import cats.data.OptionT
import cats.effect.*
import chainless.runner
import fs2.*

import java.util.zip.*

// https://gist.github.com/nmehitabel/a7c976ef8f0a41dfef88e981b9141075
object Unzip {

  val chunkSize = 1024

  def apply[F[_]: Async](chunkSize: Int = chunkSize): Pipe[F, Byte, (String, Boolean, Stream[F, Byte])] = {

    def entry(zis: ZipInputStream): OptionT[F, (String, Boolean, Stream[F, Byte])] =
      OptionT(Sync[F].blocking(Option(zis.getNextEntry))).map { ze =>
        (ze.getName, ze.isDirectory, io.readInputStream[F](Async[F].delay(zis), chunkSize, closeAfterUse = false))
      }

    def unzipEntries(zis: ZipInputStream): Stream[F, (String, Boolean, Stream[F, Byte])] =
      Stream.unfoldEval(zis) { zis0 =>
        entry(zis0).map((_, zis0)).value
      }

    (value: Stream[F, Byte]) =>
      value.through(io.toInputStream).flatMap { is =>
        val zis: F[ZipInputStream] = Sync[F].delay(new ZipInputStream(is))
        val zres: Stream[F, ZipInputStream] = Stream.bracket(zis)(zis => Sync[F].delay(zis.close()))
        zres.flatMap { z =>
          unzipEntries(z)
        }
      }
  }

}
