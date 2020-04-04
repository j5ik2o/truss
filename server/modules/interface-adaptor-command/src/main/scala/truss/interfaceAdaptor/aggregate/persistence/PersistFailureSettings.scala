package truss.interfaceAdaptor.aggregate.persistence

import scala.concurrent.duration.FiniteDuration

case class PersistFailureSettings(
    minBackoff: FiniteDuration,
    maxBackoff: FiniteDuration,
    randomFactor: Double
)
