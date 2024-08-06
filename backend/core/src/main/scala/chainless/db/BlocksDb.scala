package chainless.db

import cats.data.{NonEmptyChain, OptionT}
import cats.effect.implicits.*
import cats.effect.{Async, Resource}
import cats.implicits.*
import chainless.models.*
import chainless.utils.SortMergeStream
import fs2.Stream

import java.sql.{Connection, PreparedStatement}

/** Offers functionality for getting and setting block metadata
  */
trait BlocksDb[F[_]]:

  def insert(meta: BlockMeta): F[Unit]

  def blocksAfterTimestamp(chains: NonEmptyChain[Chain])(timestampMs: Long): Stream[F, BlockMeta]

  def blocksAfterBlocks(blockIds: NonEmptyChain[String]): Stream[F, BlockMeta]

  def getBlock(blockId: String): F[BlockMeta]

  def chainTip(chain: Chain): OptionT[F, String]

class SqlBlocksDb[F[_]: Async](connection: Connection) extends BlocksDb[F]:
  import SqlBlocksDb.{given, *}
  override def insert(meta: BlockMeta): F[Unit] =
    Async[F].blocking {
      val statement = connection.prepareStatement(InsertBlockSql)
      statement.setString(1, meta.chain.toString)
      statement.setString(2, meta.blockId)
      statement.setString(3, meta.parentBlockId)
      statement.setLong(4, meta.height)
      statement.setLong(5, meta.timestampMs)
      statement.execute()
    }

  override def blocksAfterTimestamp(chains: NonEmptyChain[Chain])(timestampMs: Long): Stream[F, BlockMeta] =
    SortMergeStream(
      chains.toList.map(chain =>
        Stream
          .eval(Async[F].blocking {
            chains.toList.map(_.toString)
            val statement = connection.prepareStatement(SelectBlocksAfterTimestampSql)
            statement.setString(1, chain.toString)
            statement.setLong(2, timestampMs)
            statement
          })
          .flatMap(blockResultStream)
      )
    )

  override def blocksAfterBlocks(blockIds: NonEmptyChain[String]): Stream[F, BlockMeta] =
    Stream
      .iterable(blockIds.toIterable)
      .flatMap(id =>
        Stream
          .eval(
            Async[F].blocking {
              val statement = connection.prepareStatement(SelectBlocksAfterBlockStep1Sql)
              statement.setString(1, id)
              Iterator.unfold(statement.executeQuery())(resultSet =>
                Option.when(resultSet.next())(
                  (
                    (resultSet.getString("chain"), resultSet.getLong("height")),
                    resultSet
                  )
                )
              )
            }
          )
          .flatMap(Stream.fromBlockingIterator[F](_, 1).head)
          .flatMap((chain, height) =>
            Stream
              .eval(
                Async[F].blocking {
                  val statement = connection.prepareStatement(SelectBlocksAfterBlockStep2Sql)
                  statement.setString(1, chain)
                  statement.setLong(2, height)
                  statement
                }
              )
              .flatMap(blockResultStream)
          )
      )

  override def getBlock(blockId: String): F[BlockMeta] =
    Stream
      .eval(Async[F].blocking {
        val statement = connection.prepareStatement(SelectBlockById)
        statement.setString(1, blockId)
        statement
      })
      .flatMap(blockResultStream)
      .head
      .compile
      .lastOrError

  override def chainTip(chain: Chain): OptionT[F, String] =
    OptionT(
      Stream
        .eval(Async[F].blocking {
          val statement = connection.prepareStatement(SelectChainTip)
          statement.setString(1, chain.toString)
          statement
        })
        .flatMap(idResultStream)
        .head
        .compile
        .last
    )

  private def blockResultStream(statement: PreparedStatement) =
    Stream
      .eval(
        Async[F].blocking(
          Iterator.unfold(statement.executeQuery())(resultSet =>
            Option.when(resultSet.next())(
              (
                BlockMeta(
                  Chain.parse(resultSet.getString("chain")).get,
                  resultSet.getString("block_id"),
                  resultSet.getString("parent_block_id"),
                  resultSet.getLong("height"),
                  resultSet.getLong("timestamp_ms")
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

  private def idResultStream(statement: PreparedStatement) =
    Stream
      .eval(
        Async[F].blocking(
          Iterator.unfold(statement.executeQuery())(resultSet =>
            Option.when(resultSet.next())(
              (
                resultSet.getString("block_id"),
                resultSet
              )
            )
          )
        )
      )
      .flatMap(
        Stream.fromBlockingIterator[F](_, 1)
      )

object SqlBlocksDb:

  def make[F[_]: Async](connection: Connection): Resource[F, BlocksDb[F]] =
    init(connection).toResource >>
      Async[F].delay(new SqlBlocksDb[F](connection)).toResource

  def init[F[_]: Async](connection: Connection): F[Unit] =
    Async[F].blocking {
      val statement = connection.createStatement()
      statement.execute(
        """
          |CREATE TABLE IF NOT EXISTS blocks (
          |  chain VARCHAR(128) NOT NULL,
          |  block_id VARCHAR(256) PRIMARY KEY,
          |  parent_block_id VARCHAR(256) NOT NULL,
          |  height BIGINT NOT NULL,
          |  timestamp_ms BIGINT NOT NULL
          |)
          |""".stripMargin
      )

      statement.execute("CREATE INDEX IF NOT EXISTS blocks_chain ON blocks (chain)")
      statement.execute("CREATE INDEX IF NOT EXISTS blocks_chain_height ON blocks (chain, height)")
      statement.execute("CREATE INDEX IF NOT EXISTS blocks_chain_timestamp ON blocks (chain, timestamp_ms)")
    }

  val InsertBlockSql =
    "INSERT INTO blocks (chain, block_id, parent_block_id, height, timestamp_ms) VALUES (?, ?, ?, ?, ?)"

  val SelectBlocksAfterTimestampSql =
    "SELECT * FROM blocks WHERE chain = ? AND timestamp_ms > ? ORDER BY timestamp_ms ASC"

  val SelectBlocksAfterBlockStep1Sql =
    "SELECT chain, height FROM blocks WHERE block_id = ? LIMIT 1"

  val SelectBlocksAfterBlockStep2Sql =
    "SELECT * FROM blocks WHERE chain = ? AND height > ? ORDER BY height ASC"

  val SelectBlockById =
    "SELECT * FROM blocks WHERE block_id = ? LIMIT 1"

  val SelectChainTip =
    "SELECT block_id FROM blocks WHERE chain = ? ORDER BY height DESC LIMIT 1"

  given Ordering[BlockMeta] = Ordering.by(_.timestampMs)
