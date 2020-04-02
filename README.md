
## setup

```sh
$ brew install grpc
```

## run in local machine

```sh
# terminal 3
$ TRUSS_API_SERVER_GRPC_PORT=18080 TRUSS_API_SERVER_GRPC_WEB_PORT=28080 TRUSS_MANAGEMENT_PORT=8558 sbt api-server/run
```

```sh
# terminal 4
$ TRUSS_API_SERVER_GRPC_PORT=18081 TRUSS_API_SERVER_GRPC_WEB_PORT=28081 TRUSS_MANAGEMENT_PORT=8559 sbt api-server/run
```

```sh
# terminal 5
$ TRUSS_API_SERVER_GRPC_PORT=18082 TRUSS_API_SERVER_GRPC_WEB_PORT=28082 TRUSS_MANAGEMENT_PORT=8560 sbt api-server/run
```

