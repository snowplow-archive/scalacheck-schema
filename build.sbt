lazy val root = project.in(file("."))
  .settings(
    name := "scalacheck-schema",
    version := "0.1.0-rc1",
    organization := "com.snowplowanalytics",
    description := "ScalaCheck generators for various Iglu-compatible Schema formats",
    scalaVersion := "2.11.12",
    crossScalaVersions := Seq("2.11.12", "2.12.6")
  )
  .settings(BuildSettings.buildSettings)
  .settings(BuildSettings.scalifySettings)
  .settings(
    resolvers ++= Seq(
      "Sonatype OSS Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"
    ),
    libraryDependencies ++= Seq(
      Dependencies.igluClient,
      Dependencies.schemaDdl,
      Dependencies.scalaCheck,
      Dependencies.scalaCheckCats,
      Dependencies.validator,
      Dependencies.specs2,
      Dependencies.specs2ScalaCheck
    )
  )
  .settings(BuildSettings.helpersSettings)

