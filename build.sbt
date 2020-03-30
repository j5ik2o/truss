val akkaVersion = "2.6.4"

val baseSettings =
  Seq(
    organization := "truss",
    name := "truss",
    version := "1.0.0-SNAPSHOT",
    scalaVersion := "2.13.1",
    scalacOptions ++=
      Seq(
        "-feature",
        "-deprecation",
        "-unchecked",
        "-encoding",
        "UTF-8",
        "-language:_",
        "-target:jvm-1.8"
      )
  )

val baseDir = "server"

// ---

val `grpc-protocol` = (project in file("grpc-protocol"))
  .settings(baseSettings)
  .settings(name := "truss-grpc-protocol")
  .settings(
    PB.targets in Compile := Seq(
        scalapb.gen() -> (sourceManaged in Compile).value
      )
  )

// --- contracts

val `contract-interface-adaptor` =
  (project in file(s"$baseDir/contracts/interface-adaptor"))
    .settings(baseSettings)
    .settings(name := "truss-contract-interface")

val `contract-use-case` =
  (project in file(s"$baseDir/contracts/use-case"))
    .settings(baseSettings)
    .settings(name := "truss-contract-use-case")

// --- modules

val infrastructure =
  (project in file(s"$baseDir/modules/infrastructure"))
    .settings(baseSettings)
    .settings(name := "truss-infrastructure")

val `interface-adaptor` =
  (project in file(s"$baseDir/modules/interface-adaptor"))
    .settings(baseSettings)
    .settings(name := "truss-interface-adaptor")
    .dependsOn(`contract-interface-adaptor`, infrastructure)

val `use-case` =
  (project in file(s"$baseDir/modules/use-case"))
    .settings(baseSettings)
    .settings(name := "truss-use-case")
    .dependsOn(`contract-use-case`, infrastructure)

val domain = (project in file(s"$baseDir/modules/domain"))
  .settings(baseSettings)
  .settings(name := "truss-domain")
  .dependsOn(infrastructure)

// ---- bootstrap

val `api-server` = (project in file(s"$baseDir/bootstrap/api-server"))
  .settings(baseSettings)
  .settings(name := "truss-api-server")
  .dependsOn(`interface-adaptor`, infrastructure)

val root = (project in file("."))
  .settings(baseSettings)
  .settings(name := "truss-root")
  .aggregate(
    `api-server`,
    infrastructure,
    `interface-adaptor`,
    `use-case`,
    domain
  )
