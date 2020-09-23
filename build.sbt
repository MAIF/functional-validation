import sbt.Keys._
import Dependencies._
import sbtrelease.ReleaseStateTransformations._

organization := "fr.maif"

name := "functional-validation"

scalaVersion := "2.12.12"

lazy val root = (project in file("."))
  .settings(publishCommonsSettings: _*)

val res = Seq(
  Resolver.jcenterRepo,
  Resolver.bintrayRepo("maif-functional-java", "maven")
)

libraryDependencies ++= Seq(
  "io.vavr"                  % "vavr"                 % vavrVersion,
  "org.hibernate.validator"  % "hibernate-validator"  % "6.0.13.Final",
  "javax.validation"         % "validation-api"       % "2.0.1.Final",
  "org.glassfish"            % "javax.el"             % "3.0.1-b09",
  "com.novocode"             % "junit-interface"      % "0.11"  % Test,
  "org.assertj"              % "assertj-core"         % "3.10.0" % Test
)

val javaVersion = "8"

javacOptions ++= Seq("-source", javaVersion, "-target", javaVersion)
javacOptions in (Compile, compile) ++= Seq("-target", javaVersion, "-Xlint:unchecked")
// Skip the javadoc for the moment
sources in(Compile, doc) := Seq.empty

testFrameworks := Seq(TestFrameworks.JUnit)
testOptions += Tests.Argument(TestFrameworks.JUnit, "-v")

(parallelExecution in Test) := false

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  setNextVersion,
  commitNextVersion,
  pushChanges
)

lazy val githubRepo = "maif/functional-validation"

lazy val publishCommonsSettings = Seq(
  homepage := Some(url(s"https://github.com/$githubRepo")),
  startYear := Some(2020),
  bintrayOmitLicense := true,
  crossPaths := false,
  scmInfo := Some(
    ScmInfo(
      url(s"https://github.com/$githubRepo"),
      s"scm:git:https://github.com/$githubRepo.git",
      Some(s"scm:git:git@github.com:$githubRepo.git")
    )
  ),
  licenses := Seq(("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))),
  developers := List(
    Developer("alexandre.delegue", "Alexandre Delègue", "", url(s"https://github.com/larousso")),
    Developer("benjamin.cavy", "Benjamin Cavy", "", url(s"https://github.com/ptitFicus")),
    Developer("gregory.bevan", "Grégory Bévan", "", url(s"https://github.com/GregoryBevan"))
  ),
  releaseCrossBuild := true,
  publishMavenStyle := true,
  publishArtifact in Test := false,
  bintrayVcsUrl := Some(s"scm:git:git@github.com:$githubRepo.git"),
  resolvers ++= res,
  bintrayOrganization := Some("maif-functional-java"),
  bintrayRepository := "maven",
  pomIncludeRepository := { _ => false }
)
