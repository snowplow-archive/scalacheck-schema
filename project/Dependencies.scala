/*
 * Copyright (c) 2018-2019 Snowplow Analytics Ltd. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
import sbt._

object Dependencies {

  object V {
    // Scala
    val igluClient       = "0.6.0"
    val circe            = "0.12.3"
    val schemaDdl        = "0.10.0-M8"
    val scalaCheck       = "1.14.0"
    val scalaCheckCats   = "0.1.0"
    // Scala (test only)
    val validator        = "1.6"
    val specs2           = "4.8.1"
  }

  // Scala
  val igluClient       = "com.snowplowanalytics"      %% "iglu-scala-client"         % V.igluClient
  val circeLiteral     = "io.circe"                   %% "circe-literal"             % V.circe
  val schemaDdl        = "com.snowplowanalytics"      %% "schema-ddl"                % V.schemaDdl
  val scalaCheck       = "org.scalacheck"             %% "scalacheck"                % V.scalaCheck
  val scalaCheckCats   = "io.chrisdavenport"          %% "cats-scalacheck"           % V.scalaCheckCats
  // Scala (test only)
  val validator        = "commons-validator"          %  "commons-validator"         % V.validator      % "test"
  val specs2           = "org.specs2"                 %% "specs2-core"               % V.specs2         % "test"
  val specs2ScalaCheck = "org.specs2"                 %% "specs2-scalacheck"         % V.specs2         % "test"
}
