package truss.api

import akka.actor.ActorSystem
import akka.cluster.{ Cluster, MemberStatus }
import cats.syntax.validated._
import com.github.everpeace.healthchecks.{ asyncHealthCheck, healthy, HealthCheck }

import scala.concurrent.Future

object HealthCheck {

  def akka(implicit system: ActorSystem): HealthCheck = {
    import system.dispatcher
    asyncHealthCheck("akka-cluster") {
      Future {
        val cluster = Cluster(system)
        val status  = cluster.selfMember.status
        val result  = status == MemberStatus.Up || status == MemberStatus.WeaklyUp
        if (result)
          healthy
        else
          "Not Found".invalidNel

      }
    }
  }
}
