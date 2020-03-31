val akkaVersion    = "2.6.4"
val logbackVersion = "1.2.3"

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
      ),
    libraryDependencies ++= Seq(
        "org.scala-lang"   % "scala-reflect"         % scalaVersion.value,
        "com.iheart"       %% "ficus"                % "1.4.7",
        "org.slf4j"        % "slf4j-api"             % "1.7.30",
        "de.huxhorn.sulky" % "de.huxhorn.sulky.ulid" % "8.2.0",
        "org.scalatest"    %% "scalatest"            % "3.1.1" % Test
      ),
    scalafmtOnCompile := true
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
    .settings(
      name := "truss-contract-interface",
      libraryDependencies ++= Seq(
          "com.typesafe.akka" %% "akka-slf4j"  % akkaVersion,
          "com.typesafe.akka" %% "akka-stream" % akkaVersion
        )
    )

val `contract-use-case` =
  (project in file(s"$baseDir/contracts/use-case"))
    .settings(baseSettings)
    .settings(
      name := "truss-contract-use-case",
      libraryDependencies ++= Seq(
          "com.typesafe.akka" %% "akka-stream" % akkaVersion
        )
    )

// --- modules

val infrastructure =
  (project in file(s"$baseDir/modules/infrastructure"))
    .settings(baseSettings)
    .settings(
      name := "truss-infrastructure",
      libraryDependencies ++= Seq(
          "ch.qos.logback" % "logback-classic" % logbackVersion % Test
        )
    )

val domain = (project in file(s"$baseDir/modules/domain"))
  .settings(baseSettings)
  .settings(name := "truss-domain")
  .dependsOn(infrastructure)

val `use-case` =
  (project in file(s"$baseDir/modules/use-case"))
    .settings(baseSettings)
    .settings(
      name := "truss-use-case",
      libraryDependencies ++= Seq(
          "ch.qos.logback"    % "logback-classic"      % logbackVersion % Test,
          "com.typesafe.akka" %% "akka-testkit"        % akkaVersion    % Test,
          "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion    % Test
        )
    )
    .dependsOn(`contract-use-case`, infrastructure, domain)

val `interface-adaptor` =
  (project in file(s"$baseDir/modules/interface-adaptor"))
    .settings(baseSettings)
    .settings(
      name := "truss-interface-adaptor",
      libraryDependencies ++= Seq(
          "com.typesafe.akka" %% "akka-actor-typed"            % akkaVersion,
          "com.typesafe.akka" %% "akka-cluster-typed"          % akkaVersion,
          "com.typesafe.akka" %% "akka-cluster-sharding-typed" % akkaVersion,
          "com.typesafe.akka" %% "akka-persistence-typed"      % akkaVersion,
          "ch.qos.logback"    % "logback-classic"              % logbackVersion % Test,
          "com.typesafe.akka" %% "akka-testkit"                % akkaVersion % Test,
          "com.typesafe.akka" %% "akka-actor-testkit-typed"    % akkaVersion % Test,
          "com.typesafe.akka" %% "akka-stream-testkit"         % akkaVersion % Test
        )
    )
    .dependsOn(`contract-interface-adaptor`, infrastructure, `use-case`)

// ---- bootstrap

val `api-server` = (project in file(s"$baseDir/bootstrap/api-server"))
  .settings(baseSettings)
  .settings(
    name := "truss-api-server",
    libraryDependencies ++= Seq(
        "ch.qos.logback" % "logback-classic" % "1.2.3"
      )
  )
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
