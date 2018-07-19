/*
 * Copyright (c) 2018 Snowplow Analytics Ltd. All rights reserved.
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
package com.snowplowanalytics.iglu.schemaddl.scalacheck

import cats.syntax.either._
import com.snowplowanalytics.iglu.core.SchemaKey
import org.json4s.JsonAST.{JDouble, JValue}
import org.json4s.jackson.prettyJson
import org.scalacheck.{Arbitrary, Gen}
import org.specs2.{ScalaCheck, Specification}

/** Integration test suite */
class IgluSchemasSpec extends Specification with ScalaCheck { def is = s2"""
  com.snowplowanalytics.snowplow/geolocation_context/jsonschema/1-1-0 ${run("iglu:com.snowplowanalytics.snowplow/geolocation_context/jsonschema/1-1-0")}
  com.snowplowanalytics.snowplow/anon_ip/jsonschema/1-0-0 ${run("iglu:com.snowplowanalytics.snowplow/anon_ip/jsonschema/1-0-0")}
  com.getvero/delivered/jsonschema/1-0-0 ${run("iglu:com.getvero/delivered/jsonschema/1-0-0")}
  com.snowplowanalytics.snowplow.enrichments/pii_enrichment_config/jsonschema/2-0-0 ${skipped("Does not merge oneOf into primary Schema")}
  """

  def run(uri: String) = {
    val (gen, schema) = IgluSchemasSpec.fetch(uri)
    implicit val arb: Arbitrary[JValue] = Arbitrary(gen)
    prop { (json: JValue) =>
      IgluSchemas.validate(json, schema) match {
        case Right(s) => true
        case Left(error) =>
          println(s"Failed for schema:\n${prettyJson(json)}\nReason:\n $error")
          false
      }
    }
  }
}

object IgluSchemasSpec {
  def fetch(uri: String): (Gen[JValue], JValue) = {
    val schemaKey = SchemaKey
      .fromUri(uri)
      .getOrElse(throw new RuntimeException("Invalid Iglu URI"))

    val result = for {
      s <- IgluSchemas.lookup(None)(schemaKey)
      a <- IgluSchemas.parseSchema(s)
    } yield (JsonGenSchema.json(a), s)

    result.fold(e => throw new RuntimeException(e), x => x)
  }
}
