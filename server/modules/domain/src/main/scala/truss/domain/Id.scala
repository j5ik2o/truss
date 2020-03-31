package truss.domain

import truss.infrastructure.ulid.ULID

abstract class Id(val modelName: String, val value: ULID)
