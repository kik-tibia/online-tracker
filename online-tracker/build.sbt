ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.2.2"

lazy val root = (project in file("."))
  .settings(
    name := "online-tracker"
  )

scalacOptions ++= Seq("-Xmax-inlines", "64") // https://github.com/circe/circe/issues/1760

val http4sVersion = "1.0.0-M38"

libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.http4s" %% "http4s-blaze-server" % http4sVersion,
  "org.http4s" %% "http4s-blaze-client" % http4sVersion
)

libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-circe" % http4sVersion,
  "io.circe" %% "circe-generic" % "0.14.3",
  "io.circe" %% "circe-literal" % "0.14.3"
)

libraryDependencies ++= Seq(
  "org.typelevel" %% "log4cats-slf4j" % "2.5.0"
)

libraryDependencies += "org.tpolecat" %% "skunk-core" % "0.5.1"
libraryDependencies += "org.apache.commons" % "commons-text" % "1.9"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.10"
