val akkaVersion           = "2.6.4"
val alpakkaKafkaVersion   = "2.0.2+4-30f1536b"
val akkaManagementVersion = "1.0.5"
val AkkaHttpVersion       = "10.1.11"
val kafkaVersion          = "2.4.0"
val logbackVersion        = "1.2.3"
val circeVersion          = "0.12.3"

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
    resolvers ++= Seq(
        "Sonatype OSS Snapshot Repository" at "https://oss.sonatype.org/content/repositories/snapshots/",
        "Sonatype OSS Release Repository" at "https://oss.sonatype.org/content/repositories/releases/",
        "Akka Snapshots" at "https://repo.akka.io/snapshots",
        Resolver.bintrayRepo("akka", "snapshots"),
        "Seasar Repository" at "https://maven.seasar.org/maven2/",
        "DynamoDB Local Repository" at "https://s3-ap-northeast-1.amazonaws.com/dynamodb-local-tokyo/release",
        Resolver.bintrayRepo("beyondthelines", "maven")
      ),
    libraryDependencies ++= Seq(
        "org.scala-lang"   % "scala-reflect"         % scalaVersion.value,
        "com.iheart"       %% "ficus"                % "1.4.7",
        "org.slf4j"        % "slf4j-api"             % "1.7.30",
        "de.huxhorn.sulky" % "de.huxhorn.sulky.ulid" % "8.2.0",
        "org.scalatest"    %% "scalatest"            % "3.1.1" % Test
      ),
    scalafmtOnCompile := true,
    parallelExecution in Test := false
  )
import scalapb.compiler.Version.{ grpcJavaVersion, protobufVersion, scalapbVersion }
val baseDir = "server"
// ---

val `grpc-protocol` = (project in file("grpc-protocol"))
  .settings(baseSettings)
  .settings(name := "truss-grpc-protocol")
  .settings(
    libraryDependencies ++= Seq(
        ("io.grpc"             % "protoc-gen-grpc-java"  % "1.19.0") asProtocPlugin (),
        "io.grpc"              % "grpc-netty"            % grpcJavaVersion,
        "com.thesamet.scalapb" %% "scalapb-runtime"      % scalapbVersion % "compile,protobuf",
        "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapbVersion,
        "io.grpc"              % "grpc-all"              % grpcJavaVersion
      ),
    PB.targets in Compile := Seq(
        PB.gens.java(protobufVersion) -> ((sourceManaged in Compile).value / "protobuf-java"),
        scalapb.gen()                 -> ((sourceManaged in Compile).value / "protobuf-scala")
      ),
    PB.protoSources in Compile += (baseDirectory in LocalRootProject).value / "protobuf"
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
    .enablePlugins(AkkaGrpcPlugin)
    .settings(baseSettings)
    .settings(
      name := "truss-interface-adaptor",
      libraryDependencies ++= Seq(
          "io.circe"                      %% "circe-core"                         % circeVersion,
          "io.circe"                      %% "circe-generic"                      % circeVersion,
          "io.circe"                      %% "circe-parser"                       % circeVersion,
          "com.typesafe.akka"             %% "akka-stream-kafka"                  % alpakkaKafkaVersion,
          "com.typesafe.akka"             %% "akka-stream-kafka-cluster-sharding" % alpakkaKafkaVersion,
          "com.typesafe.akka"             %% "akka-discovery"                     % akkaVersion,
          "com.typesafe.akka"             %% "akka-actor-typed"                   % akkaVersion,
          "com.typesafe.akka"             %% "akka-cluster-typed"                 % akkaVersion,
          "com.typesafe.akka"             %% "akka-cluster-sharding-typed"        % akkaVersion,
          "com.typesafe.akka"             %% "akka-persistence-typed"             % akkaVersion,
          "com.typesafe.akka"             %% "akka-serialization-jackson"         % akkaVersion,
          "com.lightbend.akka.management" %% "akka-management"                    % akkaManagementVersion,
          "com.lightbend.akka.management" %% "akka-management-cluster-http"       % akkaManagementVersion,
          "com.github.j5ik2o"             %% "akka-persistence-dynamodb"          % "1.0.21",
          "com.github.j5ik2o"             %% "akka-persistence-kafka"             % "1.0.6",
          "com.github.j5ik2o"             %% "akka-persistence-s3"                % "1.0.4",
          "com.github.j5ik2o"             %% "reactive-aws-dynamodb-test"         % "1.2.1" % Test,
          "com.thesamet.scalapb"          %% "scalapb-runtime"                    % scalapbVersion,
          "com.thesamet.scalapb"          %% "scalapb-runtime"                    % scalapbVersion % "protobuf",
          "ch.qos.logback"                % "logback-classic"                     % logbackVersion % Test,
          "com.typesafe.akka"             %% "akka-testkit"                       % akkaVersion % Test,
          "com.typesafe.akka"             %% "akka-actor-testkit-typed"           % akkaVersion % Test,
          "com.typesafe.akka"             %% "akka-stream-testkit"                % akkaVersion % Test,
          "io.github.embeddedkafka"       %% "embedded-kafka"                     % kafkaVersion % Test,
          "org.slf4j"                     % "jul-to-slf4j"                        % "1.7.30" % Test
        ),
      PB.protoSources in Compile += (baseDirectory in LocalRootProject).value / "protobuf"
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
    domain,
    `grpc-protocol`
  )
