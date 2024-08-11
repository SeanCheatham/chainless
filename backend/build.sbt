import com.typesafe.sbt.packager.docker.{Cmd, ExecCmd}

val scala3 = "3.4.2"

inThisBuild(
  List(
    organization := "chainless",
    scalaVersion := scala3,
    testFrameworks += TestFrameworks.MUnit,
    versionScheme := Some("early-semver"),
    dynverSeparator := "-",
    version := dynverGitDescribeOutput.value.mkVersion(versionFmt, fallbackVersion(dynverCurrentDate.value)),
    dynver := {
      val d = new java.util.Date
      sbtdynver.DynVer.getGitDescribeOutput(d).mkVersion(versionFmt, fallbackVersion(d))
    },
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision,
    coverageEnabled := true
  )
)

lazy val chainless = project
  .in(file("."))
  .settings(
    name := "chainless",
    publish / skip := true,
    publishArtifact := false,
    scalaVersion := scala3
  )
  .aggregate(
    core
  )

lazy val core = project
  .in(file("core"))
  .enablePlugins(DockerPlugin, JavaAppPackaging)
  .settings(
    name := "chainless-core",
    libraryDependencies ++=
      Dependencies.logging ++
        Dependencies.cats ++
        Dependencies.scalaCache ++
        Dependencies.scodec ++
        Dependencies.fs2 ++
        Dependencies.caseApp ++
        Dependencies.mUnitTest ++
        Dependencies.http4s ++
        Dependencies.circe ++
        Dependencies.graalVM ++
        Dependencies.docker ++
        Dependencies.sqlite ++
        Dependencies.apparatus,
    dependencyOverrides ++= Seq(
      "org.typelevel" %% "cats-parse" % "1.0.0"
    )
  )
  .settings(
    scalacOptions ++= Seq(
      "-source:3.4-migration",
      "-rewrite",
      "-Wunused:all"
    )
  )
  .settings(
//    bashScriptExtraDefines += "start-docker.sh",
    dockerBaseImage := "cruizba/ubuntu-dind",
    dockerUpdateLatest := sys.env.get("DOCKER_PUBLISH_LATEST_TAG").fold(false)(_.toBoolean),
    dockerLabels ++= Map(
      "chainless.version" -> version.value
    ),
    dockerExposedPorts := Seq(42069),
    Docker / packageName := "chainless",
    dockerExposedVolumes += "/app",
    dockerRepository := Some("docker.io"),
    dockerAlias := DockerAlias(Some("docker.io"), Some("seancheatham"), "chainless", Some(version.value)),
    dockerAliases ++= (if (sys.env.get("DOCKER_PUBLISH_DEV_TAG").fold(false)(_.toBoolean))
                         Seq(dockerAlias.value.withTag(Some("dev")))
                       else Seq()),
    dockerCommands := Seq(
      Cmd("FROM", "cruizba/ubuntu-dind"),
      Cmd("RUN", "apt update && apt install --no-install-recommends -y bash curl wget zip"),
      Cmd(
        "RUN",
        "mkdir -p /graalvm && wget -q https://download.oracle.com/graalvm/22/latest/graalvm-jdk-22_linux-x64_bin.tar.gz -O - | tar -xzf - --strip-components 1 -C /graalvm"
      ),
      Cmd("ENV", "JAVA_HOME=/graalvm"),
      Cmd("ENV", "PATH=\"$JAVA_HOME/bin:$PATH\""),
      Cmd("WORKDIR", "/opt/docker"),
      Cmd("COPY", "2/opt/docker /opt/docker"),
      Cmd("COPY", "4/opt/docker /opt/docker"),
      ExecCmd("RUN", "chmod", "u+x,g+x", "/opt/docker/bin/chainless-core"),
      Cmd("EXPOSE", "42069"),
      ExecCmd("RUN", "mkdir", "-p", "/app"),
      ExecCmd("VOLUME", "/app"),
      ExecCmd("ENTRYPOINT", "/opt/docker/bin/chainless-core"),
      ExecCmd("CMD")
    )
  )

def versionFmt(out: sbtdynver.GitDescribeOutput): String = {
  val dirtySuffix = out.dirtySuffix.dropPlus.mkString("-", "")
  if (out.isCleanAfterTag) out.ref.dropPrefix + dirtySuffix // no commit info if clean after tag
  else out.ref.dropPrefix + out.commitSuffix.mkString("-", "-", "") + dirtySuffix
}

def fallbackVersion(d: java.util.Date): String = s"HEAD-${sbtdynver.DynVer.timestamp(d)}"

addCommandAlias("checkPR", s"; scalafixAll --check; scalafmtCheckAll; +test")
addCommandAlias("preparePR", s"; scalafixAll; scalafmtAll; +test")
addCommandAlias("checkPRTestQuick", s"; scalafixAll --check; scalafmtCheckAll; testQuick")
