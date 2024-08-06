scalaVersion := "3.3.1"
name := "chainless-block-counter-scala"
publish / skip := true
version := "0.1.0"
assembly / assemblyJarName := "function.jar"

libraryDependencies ++= Seq(
    "org.typelevel" %% "cats-core" % "2.10.0",
    "org.typelevel" %% "cats-effect" % "3.5.2",
    "org.http4s" %% "http4s-ember-client" % "1.0.0-M40",
    "org.http4s" %% "http4s-circe" % "1.0.0-M40",
    "io.circe" %% "circe-core" % "0.14.6",
    "io.circe" %% "circe-generic" % "0.14.6",
    "io.circe" %% "circe-parser" % "0.14.6"
)
