package controllers

import javax.inject.Inject

import play.api.mvc._
import play.api.libs.streams._
import akka.actor._
import akka.stream.Materializer
import play.api.libs.json.Json
import play.api.mvc.BodyParsers.parse.{json => BodyJson}

object MyWebSocketActor {
  def props(out: ActorRef, parent: ActorRef) = Props(new MyWebSocketActor(out, parent))
}

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

class GameMasterActor extends Actor {

  var sockets = Set.empty[ActorRef]

  var currentState = PresentationState(false, false, false, false)

  def sendToAll(message: OutEvent) = {
    sockets.foreach { ref =>
      ref ! message
    }
  }

  def receive = {
    case RegisterSocket(ref) =>
      ref ! OutEvent.updateStateEvent(currentState)
      sockets = sockets + ref
    case UnregisterSocket(ref) =>
      sockets = sockets - ref
    case GlobalSend(message: String) =>
      sendToAll(OutEvent(`type` = "test", test = Some(message)))
    case state: PresentationState =>
      currentState = state
      sendToAll(OutEvent.updateStateEvent(currentState))
  }
}

class MyWebSocketActor(out: ActorRef, parent: ActorRef) extends Actor {

  parent ! RegisterSocket(out)

  def receive = {
    case InEvent(s) =>
      out ! OutEvent("I received your message: " + s)
  }

  override def postStop() = {
     parent ! UnregisterSocket(out)
  }
}

class WebsocketsController @Inject() (implicit system: ActorSystem, materializer: Materializer) extends Controller {

  val gameMaster = system.actorOf(
    Props(classOf[GameMasterActor]),
    "game-master"
  )

  import play.api.mvc.WebSocket.MessageFlowTransformer

  implicit val messageFlowTransformer = MessageFlowTransformer.jsonMessageFlowTransformer[InEvent, OutEvent]

  def socket2 = WebSocket.accept[String, String] { request =>
    ActorFlow.actorRef(out => MyWebSocketActor.props(out, gameMaster))
  }

  def socket = WebSocket.accept[InEvent, OutEvent] { request =>
    ActorFlow.actorRef(out => MyWebSocketActor.props(out, gameMaster))
  }

  def sendGlobalMessage = Action(BodyJson[GlobalSend]) { implicit request =>
    gameMaster ! request.body
    Accepted("")
  }

  def updateState = Action(BodyJson[PresentationState]) { implicit request =>
    gameMaster ! request.body
    Accepted("")
  }
}
