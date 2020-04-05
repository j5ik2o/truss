import sbt._
import sbt.Keys._

object Version {
  val circe         = "0.13.0"
  val akka          = "2.6.4"
  val akkaHttp      = "10.1.11"
  val akkaHttpCirce = "1.31.0"
  val cats          = "2.1.1"
  val scalaTest     = "3.1.1"
  val paradise      = "2.1.1"

  val akkaStreamKafka = "2.0.2+4-30f1536b"
  val akkaManagement  = "1.0.6"
  val kafka           = "2.4.0"
  val logback         = "1.2.3"

  val slf4j = "1.7.30"

  val cariban       = "0.7.3"
  val airframe      = "20.3.3"
  val ficus         = "1.4.7"
  val ulid          = "8.2.0"
  val monix         = "3.1.0"
  val refined       = "0.9.13"
  val scopt         = "4.0.0-RC2"
  val jaino         = "3.0.6"
  val dockerTestkit = "0.9.9"
  val aspectj       = "1.8.13"
}

object Dependencies {

  object scalaLang {
    def scalaReflect(version: String): ModuleID = "org.scala-lang" % "scala-reflect" % version
  }

  object akka {
    val slf4j         = "com.typesafe.akka" %% "akka-slf4j"               % Version.akka
    val actor         = "com.typesafe.akka" %% "akka-actor"               % Version.akka
    val testKit       = "com.typesafe.akka" %% "akka-testkit"             % Version.akka
    val actorTyped    = "com.typesafe.akka" %% "akka-actor-typed"         % Version.akka
    val testKitTyped  = "com.typesafe.akka" %% "akka-actor-testkit-typed" % Version.akka
    val stream        = "com.typesafe.akka" %% "akka-stream"              % Version.akka
    val streamTestKit = "com.typesafe.akka" %% "akka-stream-testkit"      % Version.akka

    val discovery            = "com.typesafe.akka" %% "akka-discovery"              % Version.akka
    val clusterTyped         = "com.typesafe.akka" %% "akka-cluster-typed"          % Version.akka
    val clusterShardingTyped = "com.typesafe.akka" %% "akka-cluster-sharding-typed" % Version.akka
    val persistenceTyped     = "com.typesafe.akka" %% "akka-persistence-typed"      % Version.akka
    val serializationJackson = "com.typesafe.akka" %% "akka-serialization-jackson"  % Version.akka

    val http        = "com.typesafe.akka" %% "akka-http"         % Version.akkaHttp
    val httpTestKit = "com.typesafe.akka" %% "akka-http-testkit" % Version.akkaHttp

    val streamKafka = "com.typesafe.akka" %% "akka-stream-kafka" % Version.akkaStreamKafka
    val streamKafkaClusterSharding =
      "com.typesafe.akka" %% "akka-stream-kafka-cluster-sharding" % Version.akkaStreamKafka

    val multiNodeTestKit = "com.typesafe.akka" %% "akka-multi-node-testkit" % Version.akka
  }

  object akkaManagement {
    val akkaManagement = "com.lightbend.akka.management" %% "akka-management"              % Version.akkaManagement
    val clusterHttp    = "com.lightbend.akka.management" %% "akka-management-cluster-http" % Version.akkaManagement
    val clusterBootstrap =
      "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % Version.akkaManagement
    val k8sApi = "com.lightbend.akka.discovery" %% "akka-discovery-kubernetes-api" % Version.akkaManagement
  }

  object j5ik2o {
    val reactiveAwsDynamodbTest = "com.github.j5ik2o" %% "reactive-aws-dynamodb-test" % "1.2.1"
    val akkaPersistenceDynamodb = "com.github.j5ik2o" %% "akka-persistence-dynamodb"  % "1.0.21"
    val akkaPersistenceKafka    = "com.github.j5ik2o" %% "akka-persistence-kafka"     % "1.0.6"
    val akkaPersistenceS3       = "com.github.j5ik2o" %% "akka-persistence-s3"        % "1.0.4"
  }

  object embeddedkafka {
    val embeddedKafka = "io.github.embeddedkafka" %% "embedded-kafka" % Version.kafka
  }

  object heikoseeberger {
    val akkaHttpCirce = "de.heikoseeberger" %% "akka-http-circe" % Version.akkaHttpCirce
  }

  object megard {
    val akkaHttpCors = "ch.megard" %% "akka-http-cors" % "0.4.2"
  }

  object cats {
    val core   = "org.typelevel" %% "cats-core"   % Version.cats
    val macros = "org.typelevel" %% "cats-macros" % Version.cats
    val kernel = "org.typelevel" %% "cats-kernel" % Version.cats
  }

  object monix {
    val monix = "io.monix" %% "monix" % Version.monix
  }

  object circe {
    val core          = "io.circe" %% "circe-core"           % Version.circe
    val generic       = "io.circe" %% "circe-generic"        % Version.circe
    val genericExtras = "io.circe" %% "circe-generic-extras" % Version.circe
    val parser        = "io.circe" %% "circe-parser"         % Version.circe
  }

  object airframe {
    val airframe = "org.wvlet.airframe" %% "airframe" % Version.airframe
  }

  object slf4j {
    val api        = "org.slf4j" % "slf4j-api"    % Version.slf4j
    val julToSlf4j = "org.slf4j" % "jul-to-slf4j" % Version.slf4j
  }

  object logback {
    val classic = "ch.qos.logback" % "logback-classic" % Version.logback
    val logstashLogbackEncoder = "net.logstash.logback" % "logstash-logback-encoder" % "4.11" excludeAll (
        ExclusionRule(organization = "com.fasterxml.jackson.core", name = "jackson-core"),
        ExclusionRule(organization = "com.fasterxml.jackson.core", name = "jackson-databind")
    )
  }

  object scalatest {
    val scalatest = "org.scalatest" %% "scalatest" % Version.scalaTest
  }

  object scalacheck {
    val scalacheck = "org.scalacheck" %% "scalacheck" % "1.14.3"
  }

  object ghostdogpr {
    val caliban         = "com.github.ghostdogpr" %% "caliban"           % Version.cariban
    val calibanAkkaHttp = "com.github.ghostdogpr" %% "caliban-akka-http" % Version.cariban
  }

  object iheart {
    val ficus = "com.iheart" %% "ficus" % Version.ficus
  }

  object sulky {
    val ulid = "de.huxhorn.sulky" % "de.huxhorn.sulky.ulid" % Version.ulid
  }

  object timepit {
    val refined = "eu.timepit" %% "refined" % Version.refined
  }

  object scopt {
    val scopt = "com.github.scopt" %% "scopt" % Version.scopt
  }

  object jaino {
    val jaino = "org.codehaus.janino" % "janino" % Version.jaino
  }

  object whisk {
    val dockerTestkitScalaTest   = "com.whisk" %% "docker-testkit-scalatest"    % Version.dockerTestkit
    val dockerTestkitImplSpotify = "com.whisk" %% "docker-testkit-impl-spotify" % Version.dockerTestkit
  }

  object aspectj {
    val aspectjweaver = "org.aspectj" % "aspectjweaver" % Version.aspectj
  }

}
