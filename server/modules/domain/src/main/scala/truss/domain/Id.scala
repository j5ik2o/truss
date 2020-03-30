package truss.domain

import truss.infrastructure.ulid.ULID
import scala.reflect.runtime.universe

case class Id[M](clazz: Class[M], value: ULID) {
  private val className     = clazz.getName
  private val runtimeMirror = universe.runtimeMirror(clazz.getClassLoader)
  private val classSymbol   = runtimeMirror.staticClass(className)
  val model: String         = classSymbol.fullName
}
