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

import cats.implicits._

import com.snowplowanalytics.iglu.schemaddl.jsonschema.Schema
import com.snowplowanalytics.iglu.schemaddl.jsonschema.json4s.Json4sToSchema._

import org.json4s.jackson.JsonMethods.fromJsonNode

import com.snowplowanalytics.iglu.client.{Bootstrap, Resolver}
import com.snowplowanalytics.iglu.client.repositories._
import com.snowplowanalytics.iglu.client.validation.ValidatableJValue.validateAgainstSchema

import com.snowplowanalytics.iglu.core.SchemaKey

import org.json4s.JsonAST.JValue

object IgluSchemas {
  val IgluCentral = HttpRepositoryRef(
    RepositoryRefConfig("Iglu Central", 1, List("com.snowplowanalytics")),
    "http://iglucentral.com", None
  )

  val repositoryRefs: List[RepositoryRef] = List(Bootstrap.Repo, IgluCentral)

  val IgluResolver = Resolver(100, repositoryRefs: _*)

  def parseSchema(json: JValue): Either[String, Schema] =
    Schema
      .parse(json)
      .fold("Fetched JSON cannot be parsed into a Schema".asLeft[Schema])(_.asRight[String])

  /** Validate instance against schema */
  def validate(instance: JValue, schema: JValue): Either[String, JValue] =
    validateAgainstSchema(instance, schema)(IgluResolver)   // Resolver is not used
      .leftMap(_.list.mkString(", "))
      .toEither

  def lookup(resolver: Option[Resolver])(schemaKey: SchemaKey): Either[String, JValue] =
    resolver.fold(IgluResolver)(r => r)
      .lookupSchema(schemaKey.toSchemaUri)
      .leftMap(_.list.mkString(", "))
      .map(fromJsonNode)
      .toEither
}
