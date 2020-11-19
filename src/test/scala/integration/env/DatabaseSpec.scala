package integration.env

import env.{ConnectionPoolIntegration, Database}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import scalikejdbc.{
  ConnectionPool,
  DB,
  DBSession,
  scalikejdbcSQLInterpolationImplicitDef
}
import zio.{Runtime, UIO, ZIO}

class DatabaseSpec
    extends AnyFunSpec
    with Matchers
    with BeforeAndAfterAll
    with BeforeAndAfterEach {

  override protected def beforeAll(): Unit = {
    ConnectionPool.singleton(
      url = sys.env.getOrElse("DB_URL", ""),
      user = sys.env.getOrElse("DB_USER", ""),
      password = sys.env.getOrElse("DB_PASSWORD", "")
    )
  }

  override protected def beforeEach() {
    DB autoCommit { implicit session =>
      session.execute("DROP TABLE IF EXISTS foo")
    }
  }

  private val layer = ConnectionPoolIntegration.live >>> Database.live

  private def createTable(implicit db: DBSession): Unit = {
    db.execute("CREATE TABLE foo (id SERIAL, PRIMARY KEY (id))")
  }

  private def runSimpleQuery(implicit db: DBSession): Int = {
    sql"SELECT 59".map(_.int(1)).single().apply().get
  }

  describe("Getting a database transaction") {
    it("should get a database transaction") {
      val program: ZIO[Database, Throwable, Unit] = {
        ZIO.accessM[Database](db => {
          for {
            tx <- db.get.tx()
            _  <-
              ZIO
                .effect(tx.isReadOnly shouldBe false)
                .ensuring(UIO(tx.close()))
          } yield ()
        })
      }

      val runnable = program.provideLayer(layer)

      Runtime.default.unsafeRun(runnable)
    }

    it("should access the database") {
      val program: ZIO[Database, Throwable, Unit] = {
        ZIO.accessM[Database](db => {
          for {
            tx     <- db.get.tx()
            result <- ZIO.effect(runSimpleQuery(tx))
            _      <-
              ZIO
                .effect(result shouldBe 59)
                .ensuring(UIO(tx.close()))
          } yield ()
        })
      }

      val runnable = program.provideLayer(layer)

      Runtime.default.unsafeRun(runnable)
    }

    it("should commit changes to the database") {
      val program: ZIO[Database, Throwable, Unit] = {
        ZIO.accessM[Database](db => {
          for {
            tx <- db.get.tx()
            _  <-
              ZIO
                .effect(createTable(tx))
                .ensuring(UIO.fromFunction(_ => {
                  tx.connection.commit()
                  tx.close()
                }))
          } yield ()
        })
      }

      val runnable = program.provideLayer(layer)

      Runtime.default.unsafeRun(runnable)

      DB autoCommit { implicit session =>
        val Some(fooTableWasCreated) =
          sql"""
          SELECT EXISTS (
            SELECT FROM information_schema.tables
            WHERE table_name = 'foo'
          )
        """
            .map(_.boolean(1))
            .single()
            .apply()

        fooTableWasCreated shouldBe true
      }
    }

    it("should roll back transaction") {
      val program: ZIO[Database, Throwable, Unit] = {
        ZIO.accessM[Database](db => {
          for {
            tx <- db.get.tx()
            _  <-
              ZIO
                .effect(createTable(tx))
                .ensuring(UIO.fromFunction(_ => {
                  tx.connection.rollback()
                  tx.close()
                }))
          } yield ()
        })
      }

      val runnable = program.provideLayer(layer)

      Runtime.default.unsafeRun(runnable)

      DB autoCommit { implicit session =>
        val Some(fooTableWasCreated) =
          sql"""
          SELECT EXISTS (
            SELECT FROM information_schema.tables
            WHERE table_name = 'foo'
          )
        """
            .map(_.boolean(1))
            .single()
            .apply()

        fooTableWasCreated shouldBe false
      }
    }
  }
}
