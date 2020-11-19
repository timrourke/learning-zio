package env

import scalikejdbc.{DB, DBSession}
import zio.{UIO, ZIO, ZLayer}

object DatabaseSessionIntegration {

  def createReadOnlySession(): ZIO[HasDBConnection, Throwable, DBSession] =
    ZIO.access(env => {
      val conn = env.get
      val db   = DB(conn)
      db.readOnlySession()
    })

  val readOnly: ZLayer[HasDBConnection, Throwable, HasDBSession] =
    ZLayer.fromAcquireRelease(createReadOnlySession())(sess => {
      UIO(sess.close())
    })

}
