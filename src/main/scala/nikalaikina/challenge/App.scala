package nikalaikina.challenge

import cats.effect._
import cats.syntax.functor._
import fs2.Stream
import nikalaikina.challenge.config.Config

object App extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {
    Config.load() flatMap { config =>
      Database.transactor[IO](config.database) use { db =>
        val stream = for {
          initialized <- Stream.eval(Database.initialize(db))
          token <- Stream.eval(IO(System.getenv("CHALLENGE_BOT_TOKEN")))
          exitCode <- new TodoListBotProcess[IO](token, db).run
        } yield exitCode
        stream.compile.drain.as(ExitCode.Success)
      }
    }
  }

}
