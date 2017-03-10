package controllers

import java.util.UUID
import javax.inject.Inject

import play.api.mvc._
import play.api.libs.streams._
import akka.actor._
import akka.stream.Materializer
import play.api.libs.json.Json
import play.api.mvc.BodyParsers.parse.{json => BodyJson}

import models._

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
    case (id: UUID, HitFakeNews) =>
      Database.updateScore(id, 10)
    case (id: UUID, HitGoodNews) =>
      Database.updateScore(id, -10)
    case (id: UUID, Fired) =>
      Database.updateScore(id, -1)
    case (id: UUID, FakeNewsHitPublic) =>
      Database.updateScore(id, -100)
    case (id: UUID, GoodNewsHitPublic) =>
      Database.updateScore(id, 10)
    case (id: UUID, PlayerName(name)) =>
      Database.setName(id, name.replace("<", "").replace(">", ""))
    case "tick" =>
      sendToAll(OutEvent.leaderBoardEvent(Database.scores))
  }
}

object PlayerActor {
  def props(out: ActorRef, parent: ActorRef) = Props(new PlayerActor(out, parent))
}

class PlayerActor(out: ActorRef, parent: ActorRef) extends Actor {

  val id = UUID.randomUUID()

  parent ! RegisterSocket(out)

  def receive = {
    case HitFakeNews =>
      parent ! (id -> HitFakeNews)
    case HitGoodNews =>
      parent ! (id -> HitGoodNews)
    case Fired =>
      parent ! (id -> Fired)
    case GoodNewsHitPublic =>
      parent ! (id -> GoodNewsHitPublic)
    case FakeNewsHitPublic =>
      parent ! (id -> FakeNewsHitPublic)
    case name: PlayerName =>
      parent ! (id -> name)
  }

  override def postStop() = {
     parent ! UnregisterSocket(out)
  }
}

class WebsocketsController @Inject() (implicit system: ActorSystem, materializer: Materializer) extends Controller {

  import play.api.mvc.WebSocket.MessageFlowTransformer

  implicit val messageFlowTransformer = MessageFlowTransformer.jsonMessageFlowTransformer[InEvent, OutEvent]

  implicit val ec = system.dispatcher

  val gameMaster = system.actorOf(
    Props(classOf[GameMasterActor]),
    "game-master"
  )

  import scala.concurrent.duration._
  system.scheduler.schedule(1.seconds, 1.seconds, gameMaster, "tick")
  def socket = WebSocket.accept[InEvent, OutEvent] { request =>
    ActorFlow.actorRef(out => PlayerActor.props(out, gameMaster))
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
