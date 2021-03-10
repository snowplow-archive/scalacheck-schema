/*
 * Copyright (c) 2018-2021 Snowplow Analytics Ltd. All rights reserved.
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

import com.snowplowanalytics.iglu.schemaddl.jsonschema.Schema
import com.snowplowanalytics.iglu.schemaddl.jsonschema.circe.implicits._
import io.circe.Json
import io.circe.literal._
import org.scalacheck.Arbitrary
import org.specs2.{ScalaCheck, Specification}

class JsonGenSpec extends Specification with ScalaCheck {
  def is = s2"""
  int builds valid integer with minimum $e1
  int builds valid integer with minimum and maximum $e2
  jsonObject builds valid with required keys $e3
  json builds valid JSON from enum $e4
  json builds valid JSON for oneOf even without properties $e5
  json builds values with specified depth e6
  test depth function $e7
  """

  def e1 = {
    val json = json"""{"type": "integer", "minimum": 10 }"""
    val input =
      Schema.parse(json).getOrElse(throw new RuntimeException("Invalid schema"))

    implicit val arb: Arbitrary[Json] = Arbitrary(JsonGenSchema.int(input))

    val check: Json => Boolean = _.asNumber.flatMap(_.toLong) match {
      case Some(i) => i >= 10
      case _       => false
    }

    prop(check)
  }

  def e2 = {
    val json = json"""{"type": "integer", "minimum": 10, "maximum": 100 }"""
    val input =
      Schema.parse(json).getOrElse(throw new RuntimeException("Invalid schema"))

    implicit val arb: Arbitrary[Json] = Arbitrary(JsonGenSchema.int(input))

    val check: Json => Boolean = _.asNumber.flatMap(_.toInt) match {
      case Some(i) => i >= 10 && i <= 100
      case _       => false
    }

    prop(check)
  }

  def e3 = {
    val json = json"""{
      "type": "object",
      "properties": {
        "foo": {
          "type": "integer",
          "maximum": 5
        },
        "bar": {
          "type": "integer",
          "maximum": 5
        }
      },
      "required": ["foo"],
      "additionalProperties": true
    }"""

    val input =
      Schema.parse(json).getOrElse(throw new RuntimeException("Invalid schema"))

    implicit val arb: Arbitrary[Json] = Arbitrary(
      JsonGenSchema.jsonObject(input)
    )

    val check: Json => Boolean = _.asObject.map(_.keys) match {
      case Some(keys) if keys.toList.contains("foo") => true
      case _                                         => false
    }

    prop(check)
  }

  def e4 = {
    val json = json"""{ "enum": ["one", 2, [], false] }"""
    val input =
      Schema.parse(json).getOrElse(throw new RuntimeException("Invalid schema"))

    implicit val arb: Arbitrary[Json] = Arbitrary(JsonGenSchema.json(input))

    val check: Json => Boolean = _.fold(
      false,
      !_,
      _.toInt.map(_ == 2).getOrElse(false),
      _ == "one",
      _.isEmpty,
      _ => false
    )

    prop(check)
  }

  def e5 = {
    val json = json"""{ "oneOf": [
      {
        "type": "object",
        "required": [ "pojo" ]
      },
      {
        "type": "object",
        "required": [ "json" ]
      }
    ]}"""
    val input =
      Schema.parse(json).getOrElse(throw new RuntimeException("Invalid schema"))

    val check: Json => Boolean = _.asObject.map(_.keys.toList) match {
      case Some(List("pojo")) | Some(List("json")) => true
      case _ => false
    }

    implicit val arb: Arbitrary[Json] = Arbitrary(JsonGenSchema.json(input))

    prop(check)
  }

  def e6 = {
    val gen = JsonGen.json(6)

    implicit val arb: Arbitrary[Json] = Arbitrary(gen)

    val check: Json => Boolean = {
      case j =>
        val d = JsonGenSpec.depth(0)(j)
        println(s"wo! ${d}")
        true
    }

    prop(check)
  }

  def e7 = {
    val d  = JsonGenSpec.depth(0)(_)
    val d0 = json"1"
    val d1 = json"""{"foo": "1"}"""
    val d2 = json"""{"foo": {"bar": 1}}"""
    val d3 = json"""{"foo": {"bar": {"bar": 1}}}"""

    (d(d0) must beEqualTo(0))
      .and(d(d1) must beEqualTo(1))
      .and(d(d2) must beEqualTo(2))
      .and(d(d3) must beEqualTo(3))
  }
}

object JsonGenSpec {
  def safeMax(default: Int)(list: List[Int]): Int =
    try {
      list.max
    } catch { case _: UnsupportedOperationException => default }

  def depth(i: Int)(json: Json): Int = {
    val go = depth(i + 1)(_)
    json
      .asObject
      .map(obj => safeMax(i) { obj.values.toList.map(go) })
      .orElse(json.asArray.map(js => safeMax(i) { js.toList.map(go) }))
      .getOrElse(i)
  }
}
