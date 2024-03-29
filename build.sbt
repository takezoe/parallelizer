name := "parallelizer"

organization := "com.github.takezoe"

version := "0.0.7-SNAPSHOT"

scalaVersion := "2.13.7"

crossScalaVersions := Seq("2.12.15", "2.13.7")

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.0.8" % "test"
)

publishMavenStyle := true
Test / publishArtifact := false
pomIncludeRepository := { _ =>
  false
}

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (version.value.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

scalacOptions := Seq("-deprecation", "-feature")

pomExtra := (
  <url>https://github.com/takezoe/parallelizer</url>
  <licenses>
    <license>
      <name>The Apache Software License, Version 2.0</name>
      <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
    </license>
  </licenses>
  <scm>
    <url>https://github.com/takezoe/parallelizer</url>
    <connection>scm:git:https://github.com/takezoe/parallelizer.git</connection>
  </scm>
  <developers>
    <developer>
      <id>takezoe</id>
      <name>Naoki Takezoe</name>
    </developer>
  </developers>
)
