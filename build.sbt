lazy val root = project.in(file("."))
  .settings(
    name := "scalacheck-schema",
    organization := "com.snowplowanalytics",
    description := "ScalaCheck generators for various Iglu-compatible Schema formats",
    scalaVersion := "2.12.13",
    licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0"))
  )
  .settings(BuildSettings.publishSettings)
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.igluClient,
      Dependencies.circeLiteral,
      Dependencies.schemaDdl,
      Dependencies.scalaCheck,
      Dependencies.scalaCheckCats,
      Dependencies.logger,
      Dependencies.validator,
      Dependencies.specs2,
      Dependencies.specs2ScalaCheck
    )
  )
  .settings(BuildSettings.helpersSettings)
