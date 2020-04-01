package truss.interfaceAdaptor.utils

import akka.actor.typed.ActorRef

import scala.concurrent.duration._

trait TypedActorSpecSupport {

  def killActor(actor: ActorRef[Nothing], max: FiniteDuration = 3 seconds)

  def killActors(actors: ActorRef[Nothing]*): Unit = {
    actors.foreach { actor =>
      killActor(actor)
      Thread.sleep(
        1000
      ) // the actor name is not unique intermittently on travis when creating it again after killActors, this is ducktape.
    }
  }

}
