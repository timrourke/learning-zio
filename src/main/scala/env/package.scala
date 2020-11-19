import java.sql.Connection

import scalikejdbc.DBSession
import zio.Has

package object env {
  type Database        = Has[Database.Service]
  type HasDBSession    = Has[DBSession]
  type HasDBConnection = Has[Connection]
}
