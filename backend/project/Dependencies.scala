import sbt.*

object Dependencies {
  val logging: Seq[ModuleID] = Seq(
    "ch.qos.logback" % "logback-classic" % Versions.logback,
    "ch.qos.logback" % "logback-core" % Versions.logback,
    "org.slf4j" % "slf4j-api" % "2.0.12",
    "org.typelevel" %% "log4cats-slf4j" % "2.6.0"
  )

  val cats = Seq(
    "org.typelevel" %% "cats-core" % Versions.cats,
    "org.typelevel" %% "cats-effect" % Versions.catsEffect,
    "org.typelevel" %% "cats-collections-core" % "0.9.8",
    "com.github.cb372" %% "cats-retry" % "3.1.3"
  )

  val scalaCache = Seq(
    "com.github.cb372" %% "scalacache-caffeine" % "1.0.0-M6"
  )

  val guava = Seq(
    "com.google.guava" % "guava" % "33.0.0-jre"
  )

  val fs2 = Seq(
    "co.fs2" %% "fs2-core" % Versions.fs2,
    "co.fs2" %% "fs2-io" % Versions.fs2
  )

  val scodec = Seq(
    "org.scodec" %% "scodec-core" % "2.2.2"
  )

  val caseApp = Seq(
    "com.github.alexarchambault" %% "case-app" % "2.1.0-M26"
  )

  val mUnitTest = Seq(
    "org.scalameta" %% "munit" % "1.0.0",
    "org.scalameta" %% "munit-scalacheck" % "1.0.0",
    "org.typelevel" %% "munit-cats-effect" % "2.0.0",
    "org.typelevel" %% "scalacheck-effect-munit" % "2.0-9366e44"
  ).map(_ % Test)

  val graalVM = Seq(
    "org.graalvm.polyglot" % "polyglot" % "24.0.2",
    "org.graalvm.polyglot" % "js-community" % "24.0.2"
  )

  val http4s = Seq(
    "org.http4s" %% "http4s-ember-client" % Versions.http4s,
    "org.http4s" %% "http4s-ember-server" % Versions.http4s,
    "org.http4s" %% "http4s-dsl" % Versions.http4s,
    "org.http4s" %% "http4s-circe" % Versions.http4s
  )

  val circe = Seq(
    "io.circe" %% "circe-core" % Versions.circe,
    "io.circe" %% "circe-generic" % Versions.circe,
    "io.circe" %% "circe-parser" % Versions.circe
  )

  val docker = Seq(
    "com.github.docker-java" % "docker-java-core" % Versions.dockerJava,
    "com.github.docker-java" % "docker-java-transport-httpclient5" % Versions.dockerJava
  )

  val sqlite = Seq(
    "org.xerial" % "sqlite-jdbc" % "3.46.0.0"
  )

  val apparatus = Seq(
    "co.topl" %% "protobuf-fs2" % "2.0.0-beta3",
    "com.thesamet.scalapb" %% "scalapb-json4s" % "0.12.1",
    "io.grpc" % "grpc-netty-shaded" % "1.66.0"
  )
}

object Versions {
  val cats = "2.12.0"
  val catsEffect = "3.5.2"
  val fs2 = "3.10.2"
  val logback = "1.5.6"
  val http4s = "0.23.26"
  val circe = "0.14.9"
  val mongo4cats = "0.6.17"
  val fs2Rabbit = "5.1.0"
  val dockerJava = "3.4.0"
  val pekko = "1.0.2"
  val pekkoManagement = "1.0.0"
  val slick = "3.3.3"
}
