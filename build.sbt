import com.lucidchart.sbtcross.{CrossableProject, DefaultAxis, LibraryVersionAxis}
import sbt.CrossVersion.binaryScalaVersion

inScope(Global)(
  Seq(
    credentials += Credentials(
      "Sonatype Nexus Repository Manager",
      "oss.sonatype.org",
      sys.env.getOrElse("SONATYPE_USERNAME", ""),
      sys.env.getOrElse("SONATYPE_PASSWORD", "")
    ),
    developers ++= List(
      Developer("pauldraper", "Paul Draper", "", url("https://github.com/pauldraper"))
    ),
    homepage := Some(url("https://github.com/lucidsoftware/neo-sbt-scalafmt")),
    licenses += "Apache License 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"),
    organization := "com.lucidchart",
    PgpKeys.pgpPassphrase := Some(Array.emptyCharArray),
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/lucidsoftware/neo-sbt-scalafmt"),
        "scm:git:git@github.com:lucidsoftware/neo-sbt-scalafmt.git"
      )
    ),
    startYear := Some(2017),
    version := sys.props.getOrElse("build.version", "0-SNAPSHOT")
  )
)

inThisBuild(
  Seq(
    scalafmtOnCompile := sys.props.get("scalafmtOnCompile") != Some("false"),
    scalafmtVersion := "1.3.0"
  )
)

lazy val `scalafmt-api` = project

lazy val localScalafmtVersion = settingKey[String]("Scalafmt version")

lazy val `scalafmt-impl` = project
  .dependsOn(`scalafmt-api` % Provided.name)
  .cross(new LibraryVersionAxis("scalafmt", localScalafmtVersion, _.split("\\.").take(2).mkString(".")))
lazy val `scalafmt-impl-0.6` = `scalafmt-impl`("0.6.8").settings(
  scalaVersion := "2.11.8"
)
lazy val `scalafmt-impl-1.0` = `scalafmt-impl`("1.0.0-RC3").settings(
  scalaVersion := "2.12.2"
)
lazy val `scalafmt-impl-1.2` = `scalafmt-impl`("1.2.0").settings(
  scalaVersion := "2.12.2"
)

lazy val sbtVersionAxis = new DefaultAxis {
  protected[this] val name = "sbt"
  protected[this] def major(version: String) = CrossVersion.binarySbtVersion(version)
  override def apply[A <: CrossableProject[A]](delegate: A, version: String) = {
    val newDelegate = super.apply(delegate, version)
    newDelegate.withProject(
      newDelegate.project.settings(
        Keys.name := Keys.name.value.dropRight(major(version).size + 1),
        sbtVersion := version,
        scalaCompilerBridgeSource :=
          xsbti.ArtifactInfo.SbtOrganization % "compiler-interface" % (sbtVersion in ThisBuild).value % "component" sources (),
        sbtBinaryVersion := CrossVersion.binarySbtVersion(sbtVersion.value),
        sbtDependency := {
          val app = appConfiguration.value
          val id = app.provider.id
          val scalaVersion = app.provider.scalaProvider.version
          val binVersion = binaryScalaVersion(scalaVersion)
          val cross = if (id.crossVersioned) CrossVersion.binary else CrossVersion.Disabled
          val base = ModuleID(id.groupID, id.name, sbtVersion.value, crossVersion = cross)
          CrossVersion(scalaVersion, binVersion)(base).copy(crossVersion = CrossVersion.Disabled)
        },
        scalaVersion := (sbtVersion.value match {
          case version if version.startsWith("0.13.") => "2.10.6"
          case version if version.startsWith("1.0.")  => "2.12.2"
        })
      )
    )
  }
}

lazy val `sbt-scalafmt` = project.dependsOn(`scalafmt-api`).cross(sbtVersionAxis)
lazy val `sbt-scalafmt_0.13` = `sbt-scalafmt`("0.13.16")
  .settings(scriptedSettings)
  .settings(
    scriptedDependencies := {
      (publishLocal in `scalafmt-api`).value
      (publishLocal in `scalafmt-impl-1.0`).value
      scriptedDependencies.value
    },
    scriptedLaunchOpts += s"-Dplugin.version=${version.value}"
  )
lazy val `sbt-scalafmt_1.0` = `sbt-scalafmt`("1.0.2")

lazy val `sbt-scalafmt-coursier` = project.cross(sbtVersionAxis).dependsOn(`sbt-scalafmt`)
lazy val `sbt-scalafmt-coursier_0.13` = `sbt-scalafmt-coursier`("0.13.16")
lazy val `sbt-scalafmt-coursier_1.0` = `sbt-scalafmt-coursier`("1.0.2")
