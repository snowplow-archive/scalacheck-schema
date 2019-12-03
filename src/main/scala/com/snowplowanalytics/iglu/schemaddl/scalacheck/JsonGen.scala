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

import io.circe.Json
import org.scalacheck.{Arbitrary, Gen}

/** Schema-less JSON generators */
object JsonGen {

  /** Restricted set of keys for Object-generator in order to repeat same key in multiple instances */
  val jsonObjectKeys = List(
    "one",
    "type",
    "property",
    "geo",
    "position",
    "currency",
    "кирилица",
    "foo",
    "with space",
    "camelCase",
    "PascalCase",
    "snake_case",
    "random",
    "key",
    "0",
    "o",
    "data",
    "schema",
    "name",
    "vendor",
    "privateIp",
    "version",
    "region"
  )

  def json: Gen[Json] =
    for {
      depth <- Gen.chooseNum(0, 2)
      value <- json(depth)
    } yield value

  def json(maxDepth: Int): Gen[Json] =
    for {
      varyDepth <- Gen.chooseNum(0, maxDepth)
      jsonGen <- if (varyDepth == 0) primitive
      else
        Gen.frequency(
          (10, primitive),
          (2, jsonObject(maxDepth - 1)),
          (1, array(maxDepth - 1))
        )
    } yield jsonGen

  def bool: Gen[Json] =
    Gen.oneOf(Json.True, Json.False)

  def jsonNull: Gen[Json] =
    Gen.const(Json.Null)

  def string: Gen[Json] =
    Gen.alphaNumStr.map(Json.fromString)

  def int: Gen[Json] =
    Arbitrary.arbBigInt.arbitrary.map(Json.fromBigInt)

  /** Any JSON value except object and array */
  def primitive: Gen[Json] =
    Gen.oneOf(string, int, bool, jsonNull)

  /** List of JSON keys pairs (values are primitive) */
  def fields: Gen[List[(String, Json)]] =
    Gen.listOf(Gen.oneOf(jsonObjectKeys).flatMap(k => primitive.map { (k, _) }))

  /** List of JSON keys pairs with specified depth for non-primitive types */
  def fields(depth: Int): Gen[List[(String, Json)]] =
    Gen.listOf(
      Gen.oneOf(jsonObjectKeys).flatMap(k => json(depth).map { (k, _) })
    )

  def jsonObject: Gen[Json] =
    fields.map(Json.fromFields)

  def jsonObject(depth: Int): Gen[Json] =
    fields(depth).map(Json.fromFields)

  def array: Gen[Json] =
    Gen.listOf(Gen.oneOf(primitive, jsonObject)).map(Json.arr)

  def array(depth: Int): Gen[Json] =
    Gen.listOf(json(depth)).map(Json.arr)
}
