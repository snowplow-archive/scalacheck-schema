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
package com.snowplowanalytics.iglu.schemaddl.scalacheck

import cats.Monad
import cats.effect.Clock
import cats.implicits._
import com.snowplowanalytics.iglu.client.validator.CirceValidator
import com.snowplowanalytics.iglu.client.resolver.{
  InitListCache,
  InitSchemaCache,
  Resolver
}
import com.snowplowanalytics.iglu.client.resolver.registries.RegistryLookup
import com.snowplowanalytics.iglu.core.SchemaKey
import com.snowplowanalytics.iglu.schemaddl.jsonschema.Schema
import com.snowplowanalytics.iglu.schemaddl.jsonschema.circe.implicits._
import io.circe.Json

object IgluSchemas {
  def parseSchema(json: Json): Either[String, Schema] =
    Schema
      .parse(json)
      .fold("Fetched JSON cannot be parsed into a Schema".asLeft[Schema])(
        _.asRight[String]
      )

  /** Validate instance against schema */
  def validate(instance: Json, schema: Json): Either[String, Json] =
    CirceValidator
      .validate(instance, schema)
      .leftMap(_.toClientError.getMessage)
      .as(instance)

  def lookup[F[_]: Monad: InitSchemaCache: InitListCache: RegistryLookup: Clock](
    resolver: Resolver[F],
    schemaKey: SchemaKey
  ): F[Either[String, Json]] =
    resolver
      .lookupSchema(schemaKey)
      .map(_.leftMap(_.getMessage))
}
