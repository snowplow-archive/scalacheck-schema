lazy val root = project.in(file("."))
  .settings(
    name := "scalacheck-schemaddl",
    version := "0.1.0-rc1",
    organization := "com.snowplowanalytics",
    scalaVersion := "2.11.12"
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

