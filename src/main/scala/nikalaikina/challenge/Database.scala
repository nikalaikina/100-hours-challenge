package nikalaikina.challenge

import cats.effect._
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import nikalaikina.challenge.config.DatabaseConfig
import org.flywaydb.core.Flyway

import scala.language.higherKinds

object Database {
  def transactor[F[_]: Async: ContextShift](config: DatabaseConfig): Resource[F, HikariTransactor[F]] = {
    for {
      ce <- ExecutionContexts.fixedThreadPool[F](32)
      te <- ExecutionContexts.cachedThreadPool[F]
      xa <- HikariTransactor.newHikariTransactor[F](
        config.driver,
        config.url,
        config.user,
        config.password,
        ce, // await connection here
        Blocker.liftExecutionContext(te)  // execute JDBC operations here
      )
    } yield xa
  }

  def initialize[F[_]](transactor: HikariTransactor[F])(implicit F: ConcurrentEffect[F]): F[Unit] = {
    transactor.configure { dataSource =>
      F.delay {
        val flyWay = Flyway.configure().dataSource(dataSource).load()
        flyWay.migrate()
        ()
      }
    }
  }
}
