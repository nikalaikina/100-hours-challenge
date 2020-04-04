package nikalaikina.challenge

import cats.effect.{ConcurrentEffect, Effect}
import cats.syntax.flatMap._
import cats.syntax.functor._
import doobie.util.transactor.Transactor
import fs2.Stream
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.circe.Decoder
import io.circe.generic.auto._
import io.circe.generic.semiauto._
import nikalaikina.api.Http4SBotAPI
import nikalaikina.{BotResponse, Update}
import org.http4s._
import org.http4s.circe._
import org.http4s.client.blaze.BlazeClientBuilder

import scala.concurrent.ExecutionContext.global

class TodoListBotProcess[F[_]: Effect](token: String, db: Transactor[F]) {

  implicit val decoder: Decoder[BotResponse[List[Update]]] = deriveDecoder[BotResponse[List[Update]]]
  implicit val eDecoder: EntityDecoder[F, BotResponse[List[Update]]] = jsonOf

  def run(implicit F: ConcurrentEffect[F]): Stream[F, Unit] = {
    val client = Stream.resource(BlazeClientBuilder[F](global).resource).flatMap { client =>
      val x = for {
        logger <- Slf4jLogger.create[F]
        people = new PeopleStorage(db)
        likes = new LikeStorage(db)
        botAPI <- F.delay(new Http4SBotAPI(token, client, logger))
        todoListBot <- F.delay(new BotLogic(botAPI, people, likes, logger))
      } yield todoListBot.launch
      Stream.force(x)
    }
    client
  }
}
