package truss.interfaceAdaptor.rest.controller

import akka.http.scaladsl.server.Route

class WalletCommandControllerImpl extends WalletCommandController {
  override def toRoute: Route = ???

  override private[controller] def createWallet = ???

  override private[controller] def getName = ???

  override private[controller] def getBalance = ???
}
