import com.amazonaws.regions.{ Region, Regions }
import scalapb.compiler.Version.{ grpcJavaVersion, protobufVersion, scalapbVersion }

val akkaVersion           = "2.6.4"
val alpakkaKafkaVersion   = "2.0.2+4-30f1536b"
val akkaManagementVersion = "1.0.6"
val AkkaHttpVersion       = "10.1.11"
val kafkaVersion          = "2.4.0"
val logbackVersion        = "1.2.3"
val circeVersion          = "0.12.3"

val scala213Version = "2.13.1"
val scala212Version = "2.12.10"

val baseSettings =
  Seq(
    organization := "truss",
    name := "truss",
    version := "1.0.0-SNAPSHOT",
    scalaVersion := scala212Version,
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
        Resolver.bintrayRepo("beyondthelines", "maven"),
        Resolver.bintrayRepo("segence", "maven-oss-releases"),
        Resolver.bintrayRepo("everpeace", "maven"),
        Resolver.bintrayRepo("tanukkii007", "maven"),
        Resolver.bintrayRepo("kamon-io", "snapshots")
      ),
    libraryDependencies ++= Seq(
        "org.scala-lang"     % "scala-reflect"         % scalaVersion.value,
        "com.iheart"         %% "ficus"                % "1.4.7",
        "org.slf4j"          % "slf4j-api"             % "1.7.30",
        "de.huxhorn.sulky"   % "de.huxhorn.sulky.ulid" % "8.2.0",
        "io.monix"           %% "monix"                % "3.1.0",
        "eu.timepit"         %% "refined"              % "0.9.13",
        "org.wvlet.airframe" %% "airframe"             % "20.3.3",
        "org.scalatest"      %% "scalatest"            % "3.1.1" % Test,
        "org.scalacheck"     %% "scalacheck"           % "1.14.3" % Test
      ),
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

// --- contracts

val `contract-interface-adaptor-command` =
  (project in file(s"$baseDir/contracts/interface-adaptor-command"))
  // .enablePlugins(AkkaGrpcPlugin)
    .settings(baseSettings)
    .settings(
      name := "truss-contract-interface-command",
      libraryDependencies ++= Seq(
          "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
          "com.typesafe.akka" %% "akka-slf4j"       % akkaVersion,
          "com.typesafe.akka" %% "akka-stream"      % akkaVersion,
          "io.grpc"           % "grpc-all"          % grpcJavaVersion
        ),
      PB.protoSources in Compile += (baseDirectory in LocalRootProject).value / "protobuf" / "command",
      PB.targets in Compile := Seq(
          scalapb.gen() -> (sourceManaged in Compile).value
        )
    )
    .dependsOn(domain)

val `contract-interface-adaptor-query` =
  (project in file(s"$baseDir/contracts/interface-adaptor-query"))
    .enablePlugins(AkkaGrpcPlugin)
    .settings(baseSettings)
    .settings(
      name := "truss-contract-interface-query",
      libraryDependencies ++= Seq(
          "com.typesafe.akka" %% "akka-slf4j"  % akkaVersion,
          "com.typesafe.akka" %% "akka-stream" % akkaVersion
        ),
      PB.protoSources in Compile += (baseDirectory in LocalRootProject).value / "protobuf" / "query"
    )

val `contract-use-case` =
  (project in file(s"$baseDir/contracts/use-case"))
    .settings(baseSettings)
    .settings(
      name := "truss-contract-use-case",
      libraryDependencies ++= Seq(
          "com.typesafe.akka" %% "akka-stream"      % akkaVersion,
          "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion
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
          "com.typesafe.akka" %% "akka-actor-typed"    % akkaVersion,
          "ch.qos.logback"    % "logback-classic"      % logbackVersion % Test,
          "com.typesafe.akka" %% "akka-testkit"        % akkaVersion % Test,
          "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test
        )
    )
    .dependsOn(`contract-use-case`, `contract-interface-adaptor-command`, infrastructure, domain)

val `interface-adaptor-common` = (project in file(s"$baseDir/modules/interface-adaptor-common"))
  .settings(baseSettings)
  .settings(
    name := "truss-interface-adaptor-common",
    libraryDependencies ++= Seq(
        "io.circe"             %% "circe-core"      % circeVersion,
        "io.circe"             %% "circe-generic"   % circeVersion,
        "io.circe"             %% "circe-parser"    % circeVersion,
        "com.typesafe.akka"    %% "akka-http"       % "10.1.11",
        "de.heikoseeberger"    %% "akka-http-circe" % "1.31.0",
        "com.thesamet.scalapb" %% "scalapb-runtime" % scalapbVersion,
        "com.thesamet.scalapb" %% "scalapb-runtime" % scalapbVersion % "protobuf"
      )
  )

val `interface-adaptor-query` =
  (project in file(s"$baseDir/modules/interface-adaptor-query"))
    .settings(baseSettings)
    .settings(
      name := "truss-interface-adaptor-query",
      libraryDependencies ++= Seq(
          "com.github.ghostdogpr" %% "caliban"           % "0.7.3",
          "com.github.ghostdogpr" %% "caliban-akka-http" % "0.7.3"
        )
    )
    .dependsOn(`contract-interface-adaptor-query`, `interface-adaptor-common`, infrastructure)

val `interface-adaptor-command` =
  (project in file(s"$baseDir/modules/interface-adaptor-command"))
    .settings(baseSettings)
    .settings(
      name := "truss-interface-adaptor-command",
      libraryDependencies ++= Seq(
          "com.typesafe.akka"             %% "akka-stream-kafka"                  % alpakkaKafkaVersion,
          "com.typesafe.akka"             %% "akka-stream-kafka-cluster-sharding" % alpakkaKafkaVersion,
          "com.typesafe.akka"             %% "akka-discovery"                     % akkaVersion,
          "com.typesafe.akka"             %% "akka-actor-typed"                   % akkaVersion,
          "com.typesafe.akka"             %% "akka-cluster-typed"                 % akkaVersion,
          "com.typesafe.akka"             %% "akka-cluster-sharding-typed"        % akkaVersion,
          "com.typesafe.akka"             %% "akka-persistence-typed"             % akkaVersion,
          "com.typesafe.akka"             %% "akka-serialization-jackson"         % akkaVersion,
          "ch.megard"                     %% "akka-http-cors"                     % "0.4.2",
          "com.github.j5ik2o"             %% "akka-persistence-dynamodb"          % "1.0.21",
          "com.github.j5ik2o"             %% "akka-persistence-kafka"             % "1.0.6",
          "com.github.j5ik2o"             %% "akka-persistence-s3"                % "1.0.4",
          "com.lightbend.akka.management" %% "akka-management"                    % akkaManagementVersion,
          "com.lightbend.akka.management" %% "akka-management-cluster-http"       % akkaManagementVersion,
          "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap"  % akkaManagementVersion,
          "com.lightbend.akka.discovery"  %% "akka-discovery-kubernetes-api"      % akkaManagementVersion,
          "com.github.j5ik2o"             %% "reactive-aws-dynamodb-test"         % "1.2.1" % Test,
          "com.typesafe.akka"             %% "akka-multi-node-testkit"            % akkaVersion % Test,
          "ch.qos.logback"                % "logback-classic"                     % logbackVersion % Test,
          "com.typesafe.akka"             %% "akka-testkit"                       % akkaVersion % Test,
          "com.typesafe.akka"             %% "akka-actor-testkit-typed"           % akkaVersion % Test,
          "com.typesafe.akka"             %% "akka-stream-testkit"                % akkaVersion % Test,
          "io.github.embeddedkafka"       %% "embedded-kafka"                     % kafkaVersion % Test,
          "com.whisk"                     %% "docker-testkit-scalatest"           % "0.9.9" % Test,
          "com.whisk"                     %% "docker-testkit-impl-spotify"        % "0.9.9" % Test,
          "org.slf4j"                     % "jul-to-slf4j"                        % "1.7.30" % Test,
          "org.aspectj"                   % "aspectjweaver"                       % "1.8.13"
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
        "com.google.protobuf" % "protobuf-java"         % protobufVersion,
        "com.github.j5ik2o"   %% "grpc-gateway-runtime" % "1.0.0" % "compile,protobuf"
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
//        "com.github.j5ik2o"    %% "healthchecks-core"       % "feature~scala-2.13-support-SNAPSHOT",
//        "com.github.j5ik2o"    %% "healthchecks-k8s-probes" % "feature~scala-2.13-support-SNAPSHOT",
        "com.github.scopt"     %% "scopt"                   % "4.0.0-RC2",
        "net.logstash.logback" % "logstash-logback-encoder" % "4.11" excludeAll (
          ExclusionRule(organization = "com.fasterxml.jackson.core", name = "jackson-core"),
          ExclusionRule(organization = "com.fasterxml.jackson.core", name = "jackson-databind")
        ),
        "org.slf4j"           % "jul-to-slf4j"    % "1.7.26",
        "ch.qos.logback"      % "logback-classic" % "1.2.3",
        "org.codehaus.janino" % "janino"          % "3.0.6"
      )
  )
  .dependsOn(`interface-adaptor-command`, `interface-adaptor-query`, infrastructure)

val root = (project in file("."))
  .settings(baseSettings)
  .settings(name := "truss-root")
  .aggregate(
    `api-server`,
    `rest-server`,
    infrastructure,
    `interface-adaptor-command`,
    `interface-adaptor-query`,
    `use-case`,
    domain
  )
