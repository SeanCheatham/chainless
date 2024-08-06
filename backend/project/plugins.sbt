Seq(
  "org.scalameta" % "sbt-scalafmt" % "2.5.2",
  "ch.epfl.scala" % "sbt-scalafix" % "0.12.1",
  "com.github.sbt" % "sbt-native-packager" % "1.10.0",
  "com.eed3si9n" % "sbt-buildinfo" % "0.11.0",
  "com.github.sbt" % "sbt-dynver" % "5.0.1"
).map(addSbtPlugin)
