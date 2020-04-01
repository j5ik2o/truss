package truss.interfaceAdaptor.aggregate.persistence

import scala.concurrent.duration.FiniteDuration

case class PersistFailureRestartWithBackoffSettings(
    minBackoff: FiniteDuration,
    maxBackoff: FiniteDuration,
    randomFactor: Double
)
