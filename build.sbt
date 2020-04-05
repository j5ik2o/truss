import Dependencies._
import com.amazonaws.regions.{ Region, Regions }
import scalapb.compiler.Version.{ grpcJavaVersion, protobufVersion, scalapbVersion }

val scala213Version = "2.13.1"
val scala212Version = "2.12.10"

val baseSettings =
  Seq(
    organization := "truss",
    name := "truss",
    version := "1.0.0-SNAPSHOT",
    scalaVersion := scala213Version,
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
    scalacOptions ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, n)) if n >= 13 => "-Ymacro-annotations" :: Nil
        case _                       => "-Ypartial-unification" :: Nil
      }
    },
    resolvers ++= Seq(
        "Sonatype OSS Snapshot Repository" at "https://oss.sonatype.org/content/repositories/snapshots/",
        "Sonatype OSS Release Repository" at "https://oss.sonatype.org/content/repositories/releases/",
        "Akka Snapshots" at "https://repo.akka.io/snapshots",
        Resolver.bintrayRepo("akka", "snapshots"),
        "Seasar Repository" at "https://maven.seasar.org/maven2/",
        "DynamoDB Local Repository" at "https://s3-ap-northeast-1.amazonaws.com/dynamodb-local-tokyo/release",
        Resolver.bintrayRepo("beyondthelines", "maven"),
        Resolver.bintrayRepo("segence", "maven-oss-releases"),
        // Resolver.bintrayRepo("everpeace", "maven"),
        Resolver.bintrayRepo("tanukkii007", "maven"),
        Resolver.bintrayRepo("kamon-io", "snapshots")
      ),
    libraryDependencies ++= Seq(
        scalaLang.scalaReflect(scalaVersion.value),
        iheart.ficus,
        slf4j.api,
        sulky.ulid,
        monix.monix,
        timepit.refined,
        airframe.airframe,
        scalatest.scalatest   % Test,
        scalacheck.scalacheck % Test
      ),
    libraryDependencies ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, n)) if n >= 13 => Nil
        case _ =>
          compilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full) :: Nil
      }
    },
    dependencyOverrides ++= Seq(
        "com.typesafe.akka" %% "akka-http"            % "10.1.11",
        "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.11"
      ),
    scalafmtOnCompile := true,
    parallelExecution in Test := false
  )

lazy val dockerCommonSettings = Seq(
  dockerBaseImage := "adoptopenjdk/openjdk8:x86_64-alpine-jdk8u191-b12",
  maintainer in Docker := "Junichi Kato <j5ik2o@gmail.com>",
  dockerUpdateLatest := true,
  bashScriptExtraDefines ++= Seq(
      "addJava -Xms${JVM_HEAP_MIN:-1024m}",
      "addJava -Xmx${JVM_HEAP_MAX:-1024m}",
      "addJava -XX:MaxMetaspaceSize=${JVM_META_MAX:-512M}",
      "addJava ${JVM_GC_OPTIONS:--XX:+UseG1GC}",
      "addJava -Dconfig.resource=${CONFIG_RESOURCE:-application.conf}",
      "addJava -Dakka.remote.startup-timeout=60s"
    )
)

val ecrSettings = Seq(
  region in Ecr := Region.getRegion(Regions.AP_NORTHEAST_1),
  repositoryName in Ecr := "j5ik2o/truss-api-server",
  repositoryTags in Ecr ++= Seq(version.value),
  localDockerImage in Ecr := "j5ik2o/" + (packageName in Docker).value + ":" + (version in Docker).value,
  push in Ecr := ((push in Ecr) dependsOn (publishLocal in Docker, login in Ecr)).value
)

val baseDir = "server"

// ---
lazy val `healthchecks-core` = project
  .in(file("healthchecks/core"))
  .settings(baseSettings)

lazy val `healthchecks-k8s-probes` = project
  .in(file("healthchecks/k8s-probes"))
  .settings(baseSettings)
  .dependsOn(`healthchecks-core` % "test->test;compile->compile")

val infrastructure =
  (project in file(s"$baseDir/modules/infrastructure"))
    .settings(baseSettings)
    .settings(
      name := "truss-infrastructure",
      libraryDependencies ++= Seq(
          logback.classic % Test
        )
    )

val domain = (project in file(s"$baseDir/modules/domain"))
  .settings(baseSettings)
  .settings(name := "truss-domain")
  .dependsOn(infrastructure)

// --- contracts

val `contract-interface-adaptor-command` =
  (project in file(s"$baseDir/contracts/interface-adaptor-command"))
    .enablePlugins(AkkaGrpcPlugin)
    .settings(baseSettings)
    .settings(
      name := "truss-contract-interface-command",
      libraryDependencies ++= Seq(
          akka.actorTyped,
          akka.slf4j,
          akka.stream
        ),
      PB.protoSources in Compile += (baseDirectory in LocalRootProject).value / "protobuf" / "command"
    )
    .dependsOn(domain)

val `contract-interface-adaptor-query` =
  (project in file(s"$baseDir/contracts/interface-adaptor-query"))
  //    .enablePlugins(AkkaGrpcPlugin)
    .settings(baseSettings)
    .settings(
      name := "truss-contract-interface-query",
      libraryDependencies ++= Seq(
          akka.slf4j,
          akka.stream
        ),
      PB.protoSources in Compile += (baseDirectory in LocalRootProject).value / "protobuf" / "query"
    )

val `contract-use-case` =
  (project in file(s"$baseDir/contracts/use-case"))
    .settings(baseSettings)
    .settings(
      name := "truss-contract-use-case",
      libraryDependencies ++= Seq(
          akka.actorTyped,
          akka.stream
        )
    )
    .dependsOn(domain)

// --- modules

val `use-case` =
  (project in file(s"$baseDir/modules/use-case"))
    .settings(baseSettings)
    .settings(
      name := "truss-use-case",
      libraryDependencies ++= Seq(
          akka.actorTyped,
          logback.classic    % Test,
          akka.testKitTyped  % Test,
          akka.streamTestKit % Test
        )
    )
    .dependsOn(`contract-use-case`, `contract-interface-adaptor-command`, infrastructure, domain)

val `interface-adaptor-common` = (project in file(s"$baseDir/modules/interface-adaptor-common"))
  .settings(baseSettings)
  .settings(
    name := "truss-interface-adaptor-common",
    libraryDependencies ++= Seq(
        circe.core,
        circe.generic,
        circe.parser,
        akka.http,
        heikoseeberger.akkaHttpCirce
      )
  )
  .dependsOn(`healthchecks-k8s-probes`)

val `interface-adaptor-query` =
  (project in file(s"$baseDir/modules/interface-adaptor-query"))
    .settings(baseSettings)
    .settings(
      name := "truss-interface-adaptor-query",
      libraryDependencies ++= Seq(
          ghostdogpr.caliban,
          ghostdogpr.calibanAkkaHttp
        )
    )
    .dependsOn(`contract-interface-adaptor-query`, `interface-adaptor-common`, infrastructure)

val `interface-adaptor-command` =
  (project in file(s"$baseDir/modules/interface-adaptor-command"))
    .settings(baseSettings)
    .settings(
      name := "truss-interface-adaptor-command",
      libraryDependencies ++= Seq(
          akka.streamKafka,
          akka.streamKafkaClusterSharding,
          akka.discovery,
          akka.actorTyped,
          akka.clusterTyped,
          akka.clusterShardingTyped,
          akka.persistenceTyped,
          akka.serializationJackson,
          megard.akkaHttpCors,
          j5ik2o.akkaPersistenceDynamodb,
          j5ik2o.akkaPersistenceKafka,
          j5ik2o.akkaPersistenceS3,
          akkaManagement.akkaManagement,
          akkaManagement.clusterHttp,
          akkaManagement.clusterBootstrap,
          akkaManagement.k8sApi,
          aspectj.aspectjweaver,
          j5ik2o.reactiveAwsDynamodbTest % Test,
          logback.classic                % Test,
          akka.testKit                   % Test,
          akka.testKitTyped              % Test,
          akka.streamTestKit             % Test,
          akka.multiNodeTestKit          % Test,
          embeddedkafka.embeddedKafka    % Test,
          whisk.dockerTestkitScalaTest   % Test,
          whisk.dockerTestkitImplSpotify % Test,
          slf4j.julToSlf4j               % Test
        )
    )
    .dependsOn(`contract-interface-adaptor-command`, `interface-adaptor-common`, infrastructure, `use-case`)

// ---- bootstrap

val `rest-server` = (project in file(s"$baseDir/bootstrap/rest-server"))
  .enablePlugins(AshScriptPlugin, JavaAgent, EcrPlugin)
  .settings(baseSettings)
  .settings(dockerCommonSettings)
  .settings(ecrSettings)
  .settings(
    libraryDependencies ++= Seq(
        "com.google.protobuf" % "protobuf-java" % protobufVersion
      ),
    PB.targets in Compile := Seq(
        // compile your proto files into scala source files
        scalapb.gen() -> (sourceManaged in Compile).value
      ),
    PB.protoSources in Compile += (baseDirectory in LocalRootProject).value / "protobuf" / "query"
  )

val `api-server` = (project in file(s"$baseDir/bootstrap/api-server"))
  .enablePlugins(AshScriptPlugin, JavaAgent, EcrPlugin)
  .settings(baseSettings)
  .settings(dockerCommonSettings)
  .settings(ecrSettings)
  .settings(
    name := "truss-api-server",
    mainClass in (Compile, run) := Some("truss.api.Main"),
    mainClass in reStart := Some("truss.api.Main"),
    dockerEntrypoint := Seq("/opt/docker/bin/truss-api-server"),
    dockerUsername := Some("j5ik2o"),
    fork in run := true,
    javaAgents += "org.aspectj"            % "aspectjweaver"    % "1.8.13",
    javaAgents += "org.mortbay.jetty.alpn" % "jetty-alpn-agent" % "2.0.9" % "runtime;test",
    javaOptions in Universal += "-Dorg.aspectj.tracing.factory=default",
    javaOptions in run ++= Seq(
        s"-Dcom.sun.management.jmxremote.port=${sys.env.getOrElse("JMX_PORT", "8999")}",
        "-Dcom.sun.management.jmxremote.authenticate=false",
        "-Dcom.sun.management.jmxremote.ssl=false",
        "-Dcom.sun.management.jmxremote.local.only=false",
        "-Dcom.sun.management.jmxremote"
      ),
    javaOptions in Universal ++= Seq(
        "-Dcom.sun.management.jmxremote",
        "-Dcom.sun.management.jmxremote.local.only=true",
        "-Dcom.sun.management.jmxremote.authenticate=false"
      ),
    libraryDependencies ++= Seq(
        scopt.scopt,
        logback.logstashLogbackEncoder,
        slf4j.julToSlf4j,
        logback.classic,
        jaino.jaino
      )
  )
  .dependsOn(`interface-adaptor-command`, `interface-adaptor-query`, infrastructure)

val root = (project in file("."))
  .settings(baseSettings)
  .settings(name := "truss-root")
  .aggregate(
    //`api-server`,
    //`rest-server`,
    infrastructure,
    `interface-adaptor-command`,
    `interface-adaptor-query`,
    `use-case`,
    domain
  )
