ThisBuild / version := "1.11.0"
ThisBuild / scalaVersion := "3.9.0"

ThisBuild / dockerBaseImage := "eclipse-temurin:17-jre"

ThisBuild / scalacOptions += "-Wunused:imports"

lazy val root = (project in file(".")).aggregate(tracker, altfinder)

lazy val common = (project in file("common")).settings(libraryDependencies ++= commonDependencies)

lazy val tracker = (project in file("tracker")).enablePlugins(JavaAppPackaging, DockerPlugin).settings(
  name := "online-tracker",
  Compile / mainClass := Some("com.kiktibia.onlinetracker.tracker.Main"),
  Compile / doc / sources := Seq.empty,
  libraryDependencies ++= trackerDependencies,
  scalacOptions ++= Seq("-Xmax-inlines", "64"), // https://github.com/circe/circe/issues/1760
  dockerExposedPorts += 443
).dependsOn(common)

lazy val altfinder = project.in(file("altfinder")).enablePlugins(JavaAppPackaging, DockerPlugin).settings(
  name := "alt-finder",
  Compile / mainClass := Some("com.kiktibia.onlinetracker.altfinder.BotApp"),
  Compile / doc / sources := Seq.empty,
  libraryDependencies ++= altfinderDependencies,
  dockerExposedPorts += 443
).dependsOn(common)

lazy val http4sVersion = "0.23.38"
lazy val circeVersion = "0.14.17"
lazy val cirisVersion = "3.15.2"
lazy val catsVersion = "2.13.0"
lazy val catsEffectVersion = "3.7.1"
lazy val log4catsVersion = "2.8.0"
lazy val logbackVersion = "1.6.5"
lazy val skunkVersion = "1.0.0"
lazy val commonsTextVersion = "1.15.0"
lazy val javaSizeofVersion = "0.0.5"
lazy val xchartVersion = "3.8.8"
lazy val jdaVersion = "6.7.0"
lazy val jsoupVersion = "1.23.2"

lazy val commonDependencies = Seq(
  "org.typelevel" %% "cats-core" % catsVersion,
  "org.typelevel" %% "cats-effect" % catsEffectVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-literal" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "is.cir" %% "ciris" % cirisVersion,
  "org.typelevel" %% "log4cats-slf4j" % log4catsVersion,
  "ch.qos.logback" % "logback-classic" % logbackVersion,
  "org.tpolecat" %% "skunk-core" % skunkVersion,
  "org.apache.commons" % "commons-text" % commonsTextVersion,
  "org.http4s" %% "http4s-circe" % http4sVersion,
  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.http4s" %% "http4s-ember-client" % http4sVersion
)

lazy val trackerDependencies = Seq("org.http4s" %% "http4s-ember-server" % http4sVersion)

lazy val altfinderDependencies = Seq(
  "com.carrotsearch" % "java-sizeof" % javaSizeofVersion,
  "org.knowm.xchart" % "xchart" % xchartVersion,
  "net.dv8tion" % "JDA" % jdaVersion,
  "org.http4s" %% "http4s-circe" % http4sVersion,
  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.http4s" %% "http4s-ember-client" % http4sVersion,
  "org.jsoup" % "jsoup" % jsoupVersion
)
