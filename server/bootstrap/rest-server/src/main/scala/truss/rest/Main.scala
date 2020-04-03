package truss.rest

import grpcGateway.server.GrpcGatewayServerBuilder

object Main {
  val channel =
    io.grpc.ManagedChannelBuilder
      .forAddress("localhost", 8980)
      .build()
  val gateway = GrpcGatewayServerBuilder.forPort(8991).build()
  gateway.start()
}
