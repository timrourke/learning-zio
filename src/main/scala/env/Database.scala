package env

import scalikejdbc.{DB, DBSession}
import zio.{ZIO, ZLayer}

object Database {

  trait Service {
    def tx(): ZIO[Database, Throwable, DBSession]
  }

  val live: ZLayer[HasDBConnection, Throwable, Database] =
    ZLayer.fromFunction(c => {
      () => {
        val conn = c.get
        val db   = DB(conn)
        db.begin()
        ZIO(db.withinTxSession())
      }
    })

  def tx(): ZIO[Database, Throwable, DBSession] =
    ZIO.accessM(f => f.get.tx())

}
