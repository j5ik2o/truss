package truss.interfaceAdaptor.aggregate

class WalletAggregateException(message: String, cause: Option[Throwable] = None)
    extends Exception(message, cause.orNull)
