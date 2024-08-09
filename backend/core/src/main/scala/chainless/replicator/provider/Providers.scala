package chainless.replicator.provider

import cats.MonadThrow
import cats.data.NonEmptyChain
import cats.effect.implicits.*
import cats.effect.{Async, Resource}
import cats.implicits.*
import org.http4s.client.Client
import org.typelevel.log4cats.LoggerFactory

object Providers {

  def make[F[_]: Async: LoggerFactory: Client](
      bitcoinRpcAddress: Option[String],
      ethereumRpcAddress: Option[String],
      apparatusRpcAddress: Option[String]
  ): Resource[F, NonEmptyChain[BlocksProvider[F]]] =
    for {
      bitcoinProvider <- bitcoinRpcAddress.traverse(BitcoinProvider.make[F])
      ethereumProvider <- ethereumRpcAddress.traverse(EthereumProvider.make[F])
      apparatusProvider <- apparatusRpcAddress.traverse(ApparatusProvider.make[F])
      providers <- MonadThrow[F]
        .fromOption(
          NonEmptyChain.fromSeq(bitcoinProvider.toList ++ ethereumProvider ++ apparatusProvider),
          new IllegalArgumentException("At least one provider must be specified")
        )
        .toResource
    } yield providers

}
