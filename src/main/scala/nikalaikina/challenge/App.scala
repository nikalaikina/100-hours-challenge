package nikalaikina.challenge

import cats.effect._
import cats.syntax.functor._
import fs2.Stream

import scala.language.higherKinds

object App extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {
    val stream = for {
      token <- Stream.eval(IO(System.getenv("CHALLENGE_BOT_TOKEN")))
      exitCode <- new TodoListBotProcess[IO](token).run
    } yield exitCode
    stream.compile.drain.as(ExitCode.Success)
  }

}
