package integration.env

import env.{ConnectionPoolIntegration, DatabaseSessionIntegration, HasDBSession}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{Assertion, BeforeAndAfterAll}
import scalikejdbc.{
  ConnectionPool,
  DBSession,
  scalikejdbcSQLInterpolationImplicitDef
}
import zio.{Runtime, ZIO}

class DatabaseSessionIntegrationSpec
    extends AnyFunSpec
    with Matchers
    with BeforeAndAfterAll {

  override protected def beforeAll(): Unit = {
    ConnectionPool.singleton(
      url = sys.env.getOrElse("DB_URL", ""),
      user = sys.env.getOrElse("DB_USER", ""),
      password = sys.env.getOrElse("DB_PASSWORD", "")
    )
  }

  describe("Getting a read-only database connection") {
    it("should get a read-only database connection") {
      val program: ZIO[HasDBSession, Nothing, Assertion] = {
        ZIO.access[HasDBSession](implicit hasSession => {
          implicit val session: DBSession = hasSession.get

          session.isReadOnly shouldBe true

          val Some(result) = sql"SELECT 42"
            .map(_.int(1))
            .single()
            .apply()

          result shouldBe 42
        })
      }

      val layer =
        ConnectionPoolIntegration.live >>> DatabaseSessionIntegration.readOnly

      val runnable = program
        .provideLayer(layer)

      Runtime.default.unsafeRun(runnable)
    }
  }
}
