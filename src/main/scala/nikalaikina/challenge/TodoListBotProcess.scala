package nikalaikina.challenge

import cats.effect.concurrent.Ref
import cats.effect.{ConcurrentEffect, Effect}
import cats.syntax.flatMap._
import cats.syntax.functor._
import fs2.Stream
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.circe.Decoder
import io.circe.generic.auto._
import io.circe.generic.semiauto._
import nikalaikina.api.dto.{BotResponse, BotUpdate}
import nikalaikina.api.{ChatId, Http4SBotAPI}
import org.http4s._
import org.http4s.circe._
import org.http4s.client.blaze.BlazeClientBuilder

import scala.concurrent.ExecutionContext.global
import scala.language.higherKinds


class TodoListBotProcess[F[_]: Effect](token: String) {

  implicit val decoder: Decoder[BotResponse[List[BotUpdate]]] = deriveDecoder[BotResponse[List[BotUpdate]]]
  implicit val eDecoder: EntityDecoder[F, BotResponse[List[BotUpdate]]] = jsonOf

  def run(implicit F: ConcurrentEffect[F]): Stream[F, Unit] = {
    val client = Stream.resource(BlazeClientBuilder[F](global).resource).flatMap { client =>
      val x = for {
        logger <- Slf4jLogger.create[F]
        storage <- Ref.of(Map.empty[ChatId, List[Task]]).map(new InMemoryTaskStorage(_))
        botAPI <- F.delay(new Http4SBotAPI(token, client, logger))
        todoListBot <- F.delay(new BotLogic(botAPI, storage, logger))
      } yield todoListBot.launch
      Stream.force(x)
    }
    client
  }
}
