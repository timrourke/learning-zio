package env

import java.sql.Connection

import scalikejdbc.ConnectionPool
import zio.{UIO, ZIO, ZLayer}

object ConnectionPoolIntegration {

  def borrowConnectionFromPool(): ZIO[Any, Throwable, Connection] = {
    for {
      // TODO: Extract database configuration to uppermost layer
      _    <- ZIO.effectTotal(
        ConnectionPool.singleton(
          url = sys.env.getOrElse("DB_URL", ""),
          user = sys.env.getOrElse("DB_USER", ""),
          password = sys.env.getOrElse("DB_PASSWORD", "")
        )
      )
      conn <- ZIO.effect(ConnectionPool.borrow())
    } yield conn
  }

  val live: ZLayer[Any, Throwable, HasDBConnection] =
    ZLayer.fromAcquireRelease(borrowConnectionFromPool())(conn =>
      UIO(conn.close())
    )

}
