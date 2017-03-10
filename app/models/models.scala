package models

import akka.actor.ActorRef
import play.api.libs.json.Json

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

case class OutEvent(`type`: String, presentationState: Option[PresentationState] = None, test: Option[String] = None)
object OutEvent {
  implicit val jf = Json.format[OutEvent]

  def updateStateEvent(state: PresentationState) =
    OutEvent("update-state", presentationState = Some(state))
}

case class InEvent(phase: String)
object InEvent {
  implicit val jf = Json.format[InEvent]
}
