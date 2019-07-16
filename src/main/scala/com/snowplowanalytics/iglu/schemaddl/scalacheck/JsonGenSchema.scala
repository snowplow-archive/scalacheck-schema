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
import com.snowplowanalytics.iglu.schemaddl.jsonschema._
import com.snowplowanalytics.iglu.schemaddl.jsonschema.properties._
import io.circe.Json
import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.cats.implicits._

import Utils.traverseMap
import JsonStringGen._

object JsonGenSchema {

  /** Generate JSON compatible with provided JSON Schema */
  def json(schema: Schema): Gen[Json] =
    schema
      .enum
      .map(fromEnum)
      .orElse(fromOneOf(schema))
      .orElse(fromType(schema))
      .getOrElse(JsonGen.json)

  /** Generate JSON string from schema with supposed integer type */
  def string(schema: Schema): Gen[Json] = {
    val minLength = schema.minLength.map(_.value.toInt).getOrElse(0)
    val maxLength = schema.maxLength.map(_.value.toInt).getOrElse(128)
    val sizedRandom = if (minLength == maxLength)
      sizedStr(maxLength)
    else
      for { card <- Gen.chooseNum(minLength, maxLength); str <- sizedStr(card) } yield str

    schema.format
      .map(forFormat)
      .getOrElse(sizedRandom)
      .map(Json.fromString)
  }

  def sizedStr(size: Int) =
    Gen.listOfN(size, Gen.alphaLowerChar.map(_.toByte)).map(bytes => new String(bytes.toArray))

  /** Generate JSON integer from schema with supposed integer type */
  def int(schema: Schema): Gen[Json] = {
    val minimum = schema.minimum.map(_.getAsDecimal.toLong).getOrElse(Long.MinValue)
    val maximum = schema.maximum.map(_.getAsDecimal.toLong).getOrElse(Long.MaxValue)
    val result = schema.multipleOf match {
      case Some(NumberProperty.MultipleOf.IntegerMultipleOf(value)) =>
        Gen.chooseNum(minimum, maximum).suchThat(_ % value == 0)
      case _ =>
        Gen.chooseNum(minimum, maximum)
    }
    result.map(x => Json.fromLong(x))
  }

  import Gen.Choose._
  val ArbitraryBigDecimal = BigDecimal(100000)
  implicit val bigDecimalChoose: Gen.Choose[BigDecimal] =
    Gen.Choose.xmap[Double, BigDecimal](BigDecimal(_), _.toDouble)

  def number(schema: Schema): Gen[Json] = {
    val minimum = schema.minimum.map(_.getAsDecimal).getOrElse(-ArbitraryBigDecimal)
    val maximum = schema.maximum.map(_.getAsDecimal).getOrElse(ArbitraryBigDecimal)
    val result = schema.multipleOf match {
      case Some(NumberProperty.MultipleOf.NumberMultipleOf(value)) =>
        Gen.chooseNum(minimum, maximum).suchThat(_ % value == 0)
      case _ =>
        Gen.chooseNum(minimum, maximum)
    }
    result.map(Json.fromBigDecimal)

  }

  /** Generate JSON object from schema with supposed array type */
  def array(schema: Schema): Gen[Json] = {
    val items = schema.items.map {
      case ArrayProperty.Items.ListItems(itemsSchema) =>
        Gen.listOf(json(itemsSchema)).map(Json.arr)
      case ArrayProperty.Items.TupleItems(schemas) =>
        schemas.traverse(json).map(Json.arr)
    }
    items.getOrElse(JsonGen.array)
  }

  /** Generate JSON object from schema with supposed object type */
  def jsonObject(schema: Schema): Gen[Json] = {
    val properties = getProperties(schema).getOrElse(Gen.const(List.empty))
    val additionalProperties = getAdditionalProperties(schema).getOrElse(Gen.const(List.empty))

    (additionalProperties, properties).mapN((a, b) => Json.fromFields((a ++ b).toMap.toList))
  }

  def fromType(schema: Schema): Option[Gen[Json]] =
    schema.`type`.map(typeToGen(_)(schema))

  def fromOneOf(schema: Schema): Option[Gen[Json]] =
    schema.oneOf.map { schemas => for {
      n <- Gen.chooseNum(0, schemas.value.length - 1)
      result <- schemas.value.map(json).get(n.toLong).get
    } yield result
}

  /** Pick some JSON from enum */
  def fromEnum(enum: CommonProperties.Enum): Gen[Json] =
    Gen.oneOf(enum.value)

  /** Generate list of all required properties and few (not all) "usual" properties */
  def getProperties(schema: Schema): Option[Gen[List[(String, Json)]]] = {
    val required = schema.required.map(_.value).getOrElse(List.empty)

    val properties = schema.properties.map {
      case ObjectProperty.Properties(map) =>
        val requiredFields = traverseMap(map.filterKeys(required.contains))(json)
        val nonRequiredFields = traverseMap(map.filterKeys(!required.contains(_)))(json)

        for {
          nonRequired <- nonRequiredFields
          toFilter <- Gen.listOfN(nonRequired.length, Arbitrary.arbBool.arbitrary)
          filtered = nonRequired.zip(toFilter).filter(_._2).map(_._1)
          required <- requiredFields
        } yield filtered ++ required
    }

    // In case Schema has only `required`, without `properties`
    val requiredWithoutSchema = required.filter(key => !schema.properties.map(_.value).getOrElse(Map.empty).keySet.contains(key))
    val requiredFields = requiredWithoutSchema.map(key => JsonGen.json.map { (key, _) }).sequence

    properties.orElse(Some(requiredFields))
  }

  /** Generate list of random additional properties if they're allowed */
  def getAdditionalProperties(schema: Schema): Option[Gen[List[(String, Json)]]] = {
    schema.additionalProperties.flatMap {
      case ObjectProperty.AdditionalProperties.AdditionalPropertiesAllowed(true) =>
        JsonGen.fields.some
      case ObjectProperty.AdditionalProperties.AdditionalPropertiesAllowed(false) =>
        none
      case ObjectProperty.AdditionalProperties.AdditionalPropertiesSchema(schema) =>
        Gen.listOf(Gen.nonEmptyBuildableOf[String, Char](Gen.alphaChar))
          .flatMap(key => traverseMap(key.map(kk => (kk, schema)).toMap)(json))
          .map(fields => fields)
          .some
    }
  }

  def typeToGen(t: CommonProperties.Type)(schema: Schema): Gen[Json] =
    t match {
      case CommonProperties.Type.Null => JsonGen.jsonNull
      case CommonProperties.Type.Array => array(schema)
      case CommonProperties.Type.String => string(schema)
      case CommonProperties.Type.Integer => int(schema)
      case CommonProperties.Type.Number => number(schema)
      case CommonProperties.Type.Boolean => JsonGen.bool
      case CommonProperties.Type.Object => jsonObject(schema)
      case CommonProperties.Type.Union(types) =>
        val schemas = types.map(typeToGen(_)(schema))
        for {
          n <- Gen.chooseNum(0, schemas.size - 1)
          schema <- schemas.toList.apply(n)
        } yield schema
    }

}
