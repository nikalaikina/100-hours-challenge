name := "100-hours-challenge"

version := "0.1"

scalaVersion := "2.13.1"

libraryDependencies ++= Seq(
  compilerPlugin("org.spire-math" % "kind-projector_2.13.0-RC1" % "0.9.10"),
  "org.http4s" %% "http4s-blaze-client" % "0.21.0",
  "org.http4s" %% "http4s-circe" % "0.21.0",
  "io.circe" %% "circe-core" % "0.13.0",
  "io.circe" %% "circe-generic" % "0.13.0",
  "io.chrisdavenport" %% "log4cats-core" % "1.0.1",
  "io.chrisdavenport" %% "log4cats-slf4j" % "1.0.1",
  "org.slf4j" % "slf4j-simple" % "1.7.26",
  "org.flywaydb" % "flyway-core" % "5.2.4",
  "org.tpolecat" %% "doobie-core" % "0.8.8",
  "org.tpolecat" %% "doobie-h2" % "0.8.8",
  "org.tpolecat" %% "doobie-hikari" % "0.8.8",
  "com.github.pureconfig" %% "pureconfig" % "0.12.3",
  "org.postgresql" % "postgresql" % "42.2.5",
  "org.specs2" %% "specs2-core" % "4.9.2" % "test"
)
