package nikalaikina.challenge

import cats.effect.IO
import com.typesafe.config.ConfigFactory
import pureconfig.error.ConfigReaderException

package object config {

  case class DatabaseConfig(driver: String, url: String, user: String, password: String)

  case class Config(database: DatabaseConfig)

  object Config {
    import pureconfig._
    import pureconfig.generic.auto._

    def load(): IO[Config] = {
      IO {
        loadConfig[Config](ConfigFactory.load())
      }.flatMap {
        case Left(e) => IO.raiseError[Config](new ConfigReaderException[Config](e))
        case Right(config) => IO.pure(config)
      }
    }
  }
}
