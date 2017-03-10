package controllers

import java.util.UUID
import javax.inject._

import play.api.mvc.BodyParsers.parse.{json => BodyJson}
import play.api._
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.Future
import akka.agent.Agent
import models.PlayerScore

import scala.concurrent.ExecutionContext.Implicits.global

object Database {

  val scoresAgent = Agent(Map.empty[UUID, Int])

  val namesAgent = Agent(Map.empty[UUID, String])

  def incrementScore(playerId: UUID): Future[Int] = {
    scoresAgent.alter { scores =>
      val score = scores.getOrElse(playerId, 0) + 1
      scores.updated(playerId, score)
    } map { _.getOrElse(playerId, 0) }
  }

  def updateScore(playerId: UUID, offset: Int): Future[Int] = {
    scoresAgent.alter { scores =>
      val score = scores.getOrElse(playerId, 0) + offset
      if (score < 0)
        scores.updated(playerId, 0)
      else
        scores.updated(playerId, score)
    } map { _.getOrElse(playerId, 0) }
  }

  def setName(playerId: UUID, name: String) = {
    namesAgent.send { names =>
      names.updated(playerId, name)
    }
  }

  def scores: List[PlayerScore] = {
    val scores = scoresAgent.get
    val names = namesAgent.get
    scores.toList.map { case (id, score) =>
      PlayerScore(names.getOrElse(id, id.toString.take(8)), score)
    } sortBy { -_.score } take 10
  }
}

case class ClientRequest(action: String, playerId: UUID)
object ClientRequest {
  implicit val jf = Json.format[ClientRequest]
}

case class Score(playId: UUID, score: Int)
object Score {
  implicit val jf = Json.format[Score]
}

@Singleton
class HomeController @Inject() extends Controller {

  def submitRequest = Action.async(BodyJson[ClientRequest]) { implicit request =>
    if (request.body.action == "hit-target") {
      Database.incrementScore(request.body.playerId) map { score =>
        Ok(Json.toJson(Score(request.body.playerId, score)))
      }
    } else {
      Future.successful(BadRequest)
    }
  }
}
