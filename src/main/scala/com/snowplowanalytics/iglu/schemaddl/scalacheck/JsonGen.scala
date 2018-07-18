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

import org.scalacheck.{Arbitrary, Gen}

import org.json4s.JsonAST._

/** Schema-less JSON generators */
object JsonGen {

  /** Restricted set of keys for Object-generator in order to repeat same key in multiple instances */
  val jsonObjectKeys = List("one", "type", "property", "geo", "position", "currency", "кирилица",
  "foo", "with space", "camelCase", "PascalCase", "snake_case", "random", "key", "0", "o", "data",
  "schema", "name", "vendor", "privateIp", "version", "region")

  def json: Gen[JValue] =
    for { 
      depth <- Gen.chooseNum(0, 2)
      value <- json(depth)
    } yield value


  def json(maxDepth: Int): Gen[JValue] =
    for {
      varyDepth <- Gen.chooseNum(0, maxDepth)
      jsonGen <- if (varyDepth == 0) primitive else Gen.frequency((10, primitive), (2, jsonObject(maxDepth - 1)), (1, array(maxDepth - 1)))
      result <- jsonGen
    } yield result

  def bool: Gen[JBool] =
    Gen.oneOf(JBool(true), JBool(false))

  def jsonNull: Gen[JValue] =
    Gen.const(JNull)

  def string: Gen[JString] =
    Gen.alphaNumStr.map(JString.apply)

  def int: Gen[JInt] =
    Arbitrary.arbBigInt.arbitrary.map(JInt.apply)

  /** Any JSON value except object and array */
  def primitive: Gen[JValue] =
    Gen.oneOf(string, int, bool, jsonNull)

  /** List of JSON keys pairs (values are primitive) */
  def fields: Gen[List[(String, JValue)]] =
    Gen.listOf(Gen.oneOf(jsonObjectKeys).flatMap(k => primitive.map { (k, _) }))

  /** List of JSON keys pairs with specified depth for non-primitive types */
  def fields(depth: Int): Gen[List[(String, JValue)]] =
    Gen.listOf(Gen.oneOf(jsonObjectKeys).flatMap(k => json(depth).map { (k, _) }))

  def jsonObject: Gen[JValue] =
    fields.map(JObject.apply)

  def jsonObject(depth: Int): Gen[JValue] =
    fields(depth).map(JObject.apply)

  def array: Gen[JArray] =
    Gen.listOf(Gen.oneOf(primitive, jsonObject)).map(JArray.apply)

  def array(depth: Int): Gen[JArray] =
    Gen.listOf(json(depth)).map(JArray.apply)
}
