resolvers ++= Seq(
  "Sonatype OSS Snapshot Repository" at "https://oss.sonatype.org/content/repositories/snapshots/",
  "Sonatype OSS Release Repository" at "https://oss.sonatype.org/content/repositories/releases/",
  Resolver.bintrayRepo("beyondthelines", "maven")
)
libraryDependencies ++= Seq(
  "com.thesamet.scalapb" %% "compilerplugin" % "0.10.2",
  "com.google.protobuf"  % "protobuf-java"   % "3.7.0"
)

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.3.2")

addSbtPlugin("com.lightbend.sbt" % "sbt-javaagent" % "0.1.4")

addSbtPlugin("com.thesamet" % "sbt-protoc" % "0.99.30")

addSbtPlugin("com.lightbend.akka.grpc" % "sbt-akka-grpc" % "0.8.1")
