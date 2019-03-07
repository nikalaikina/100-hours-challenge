name := "100-hours-challenge"

version := "0.1"

scalaVersion := "2.12.8"

libraryDependencies ++= Seq(
  compilerPlugin("org.spire-math" %% "kind-projector" % "0.9.9"),
  "org.http4s" %% "http4s-blaze-client" % "0.19.0",
  "org.http4s" %% "http4s-circe" % "0.19.0",
  "io.circe" %% "circe-core" % "0.11.1",
  "io.circe" %% "circe-generic" % "0.11.1",
  "io.chrisdavenport" %% "log4cats-core" % "0.3.0",
  "io.chrisdavenport" %% "log4cats-slf4j" % "0.3.0",
  "org.slf4j" % "slf4j-simple" % "1.7.26",
  "org.specs2" %% "specs2-core" % "4.4.1" % "test"
)
