package controllers

import java.util.UUID
import javax.inject._

import play.api.mvc.BodyParsers.parse.{json => BodyJson}
import play.api._
import play.api.libs.json.Json
import play.api.mvc._
import scala.concurrent.Future
import akka.agent.Agent

import scala.concurrent.ExecutionContext.Implicits.global

object Database {

  val scoresAgent = Agent(Map.empty[UUID, Int])

  def incrementScore(playerId: UUID): Future[Int] = {
    scoresAgent.alter { scores =>
      val score = scores.getOrElse(playerId, 0) + 1
      scores.updated(playerId, score)
    } map { _.getOrElse(playerId, 0) }
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
