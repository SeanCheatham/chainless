package chainless.replicator.provider

import cats.data.OptionT
import cats.effect.{Async, Resource}
import cats.effect.implicits.*
import cats.implicits.*
import chainless.models.*
import co.topl.brambl.models.TransactionId
import co.topl.consensus.models.*
import co.topl.node.models.*
import co.topl.node.services.*
import com.google.protobuf.ByteString
import io.grpc.Metadata
import scodec.bits.ByteVector
import scalapb.json4s.JsonFormat
import fs2.Stream

import java.time.Instant

class ApparatusProvider[F[_]: Async](client: NodeRpcFs2Grpc[F, Metadata]) extends BlocksProvider[F] {

  override val chain: Chain = Chain.Apparatus

  override def tip: F[String] =
    tipId.map(_.stringified)

  private def tipId =
    OptionT(client.fetchBlockIdAtDepth(FetchBlockIdAtDepthReq(), new Metadata()).map(_.blockId))
      .getOrRaise(new IllegalStateException("Apparatus tip not found"))

  override def tips: Stream[F, String] =
    client
      .synchronizationTraversal(SynchronizationTraversalReq(), new Metadata())
      .map(_.status)
      .collect { case SynchronizationTraversalRes.Status.Applied(id) =>
        id.stringified
      }

  override def blockById(id: String): F[BlockWithChain] =
    Async[F]
      .pure(id)
      .flatMap(id =>
        if (id.startsWith("b_")) blockById(id.substring(2))
        else
          Async[F]
            .delay(ByteVector.fromValidBase58(id))
            .map(v => ByteString.copyFrom(v.toArray))
            .map(BlockId(_))
            .flatMap(blockByBlockId)
      )

  private def blockByBlockId(id: BlockId): F[BlockWithChain] =
    for {
      header <- OptionT(client.fetchBlockHeader(FetchBlockHeaderReq(id), new Metadata()).map(_.header))
        .getOrRaise(new NoSuchElementException(id.stringified))
      body <- OptionT(client.fetchBlockBody(FetchBlockBodyReq(id), new Metadata()).map(_.body))
        .getOrRaise(new NoSuchElementException(id.stringified))
      transactions <- body.transactionIds.traverse(txId =>
        OptionT(client.fetchTransaction(FetchTransactionReq(txId), new Metadata()).map(_.transaction))
          .getOrRaise(new NoSuchElementException(txId.stringified))
      )
      fullBlock = FullBlock(header, FullBlockBody(transactions))
      json <- Async[F].fromEither(io.circe.parser.parse(JsonFormat.toJsonString(fullBlock)))
      blockWithChain = BlockWithChain(
        BlockMeta(chain, id.stringified, header.parentHeaderId.stringified, header.height, header.timestamp),
        json
      )
    } yield blockWithChain

  override def blockIdAtHeight(height: Long): F[String] =
    OptionT(client.fetchBlockIdAtHeight(FetchBlockIdAtHeightReq(height), new Metadata()).map(_.blockId))
      .getOrRaise(new NoSuchElementException(s"height=$height"))
      .map(_.stringified)

  override def blockIdAfter(instant: Instant): F[String] = {
    val instantMs = instant.toEpochMilli
    tipId.flatMap(id =>
      (id, none[BlockHeader]).tailRecM {
        case (id, None) =>
          OptionT(client.fetchBlockHeader(FetchBlockHeaderReq(id), new Metadata()).map(_.header))
            .getOrRaise(new NoSuchElementException(id.stringified))
            .ensure(new IllegalArgumentException(s"No blocks after $instant"))(_.timestamp > instantMs)
            .map(header => (header.parentHeaderId, header.some).asLeft)
        case (id, Some(previous)) =>
          OptionT(client.fetchBlockHeader(FetchBlockHeaderReq(id), new Metadata()).map(_.header))
            .getOrRaise(new NoSuchElementException(id.stringified))
            .map(header =>
              Either.cond(header.timestamp <= instantMs, id.stringified, (header.parentHeaderId, header.some))
            )
      }
    )
  }

  extension (id: BlockId) def stringified: String = s"b_${ByteVector(id.value.toByteArray).toBase58}"
  extension (id: TransactionId) def stringified: String = s"t_${ByteVector(id.value.toByteArray).toBase58}"
}

object ApparatusProvider {
  def make[F[_]: Async](address: String): Resource[F, ApparatusProvider[F]] =
    Async[F]
      .delay(java.net.URI.create(address))
      .toResource
      .flatMap { uri =>
        import fs2.grpc.syntax.all._
        val base = io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder
          .forAddress(uri.getHost, Option(uri.getPort).filter(_ >= 0).getOrElse(9084))

        val withTls =
          if (Option(uri.getScheme).contains("https")) base.useTransportSecurity() else base.usePlaintext()

        withTls.resource[F]
      }
      .flatMap(NodeRpcFs2Grpc.stubResource[F])
      .map(new ApparatusProvider[F](_))
}
