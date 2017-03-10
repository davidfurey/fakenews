package models

import java.util.UUID

import akka.actor.ActorRef
import play.api.libs.json._

case class RegisterSocket(out: ActorRef)
case class UnregisterSocket(out: ActorRef)
case class GlobalSend(message: String)
object GlobalSend {
  implicit val jf = Json.format[GlobalSend]
}

case class PresentationState(
  playerVisible: Boolean,
  canShoot: Boolean,
  generateNews: Boolean,
  hasReaders: Boolean
)

object PresentationState {
  implicit val jf = Json.format[PresentationState]
}

case class OutEvent(`type`: String, presentationState: Option[PresentationState] = None, test: Option[String] = None, leaderBoard: Option[List[PlayerScore]] = None)
object OutEvent {
  implicit val jf = Json.format[OutEvent]

  def updateStateEvent(state: PresentationState) =
    OutEvent("update-state", presentationState = Some(state))

  def leaderBoardEvent(leaderBoard: List[PlayerScore]) =
    OutEvent("update-leader-board", leaderBoard = Some(leaderBoard))
}


case class PlayerScore(name: String, score: Int)
object PlayerScore {
  implicit val jf = Json.format[PlayerScore]
}

sealed trait InEvent

case object HitFakeNews extends InEvent
case object HitGoodNews extends InEvent
case object GoodNewsHitPublic extends InEvent
case object FakeNewsHitPublic extends InEvent
case object Fired extends InEvent
case class PlayerName(name: String) extends InEvent
object PlayerName {
  implicit val jf = Json.format[PlayerName]
}

object InEvent {
  implicit val jr = new Reads[InEvent] {
    override def reads(json: JsValue): JsResult[InEvent] = {
      (json \ "type").validate[String] flatMap {
        case "hit-fake-news" => JsSuccess(HitFakeNews)
        case "hit-good-news" => JsSuccess(HitGoodNews)
        case "fired" => JsSuccess(Fired)
        case "good-news-hit-public" => JsSuccess(GoodNewsHitPublic)
        case "fake-news-hit-public" => JsSuccess(FakeNewsHitPublic)
        case "player-name" => json.validate[PlayerName]
      }
    }
  }
}
