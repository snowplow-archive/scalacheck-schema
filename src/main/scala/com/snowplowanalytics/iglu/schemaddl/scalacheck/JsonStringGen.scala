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

import java.time.{Instant, ZoneOffset}
import java.time.format.DateTimeFormatter
import java.util.Locale

import cats._
import cats.implicits._
import com.snowplowanalytics.iglu.schemaddl.jsonschema.properties._
import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.cats.implicits._

/** String generators for JSON Schema formats */
object JsonStringGen {

  def forFormat(format: StringProperty.Format): Gen[String] = {
    format match {
      case StringProperty.Format.UriFormat => uriGen
      case StringProperty.Format.Ipv4Format => ipv4Gen
      case StringProperty.Format.Ipv6Format => ipv6Gen
      case StringProperty.Format.EmailFormat => emailGen
      case StringProperty.Format.HostNameFormat => hostnameGen
      case StringProperty.Format.UuidFormat => Gen.uuid.map(_.toString)
      case StringProperty.Format.CustomFormat(_) => Arbitrary.arbitrary[String]
      case StringProperty.Format.DateTimeFormat =>
        val format = DateTimeFormatter
          .ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
          .withZone(ZoneOffset.UTC)
        Gen.calendar.suchThat(_.getWeekYear < 10000).map { c =>
          format.format(Instant.ofEpochMilli(c.getTimeInMillis))
        }
      case StringProperty.Format.DateFormat =>
        val format = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        Gen.calendar.map(c => format.format(Instant.ofEpochMilli(c.getTimeInMillis)))
    }
  }

  def hostnameGen: Gen[String] = {
    val toplevelDomains = List("com", "org", "edu", "gov", "ru", "nl", "co.uk", "me", "us",
      "name", "biz", "online", "pro", "tel")
    for {
      n     <- Gen.frequency((10, Gen.const(1)), (3, Gen.const(2)), (2, Gen.const(3)))
      label <- Gen.listOfN(n, Gen.alphaNumStr.suchThat(s => s.length < 63 && s.length > 2))
      top   <- Gen.oneOf(toplevelDomains)
    } yield (label :+ top).mkString(".")
  } :| "Hostname Generator"

  def emailGen: Gen[String] = {
    for {
      host       <- hostnameGen
      nameLength <- Gen.chooseNum(3, 63)
      name       <- Gen.listOfN(nameLength, Gen.alphaNumChar).map(_.mkString)
    } yield name + "@" + host
  } :| "Email Generator"

  def uriGen: Gen[String] = {
    val schemes = List("http", "https", "ftp")

    val queryParams = for {
      n         <- Gen.chooseNum(1, 10)
      maxLength <- Gen.chooseNum(3, 40)
      item       = Gen.listOfN(maxLength, Gen.alphaNumChar).map(_.mkString)
      pairs     <- Gen.listOfN(n * 2, item.map(x => (x, x)))
    } yield "?" + pairs.map { case (k, v) => s"$k=$v" }.mkString("&")

    val queryPath = for {
      n         <- Gen.chooseNum(1, 10)
      maxLength <- Gen.chooseNum(3, 40)
      item       = Gen.listOfN(maxLength, Gen.alphaNumChar).map(_.mkString)
      pairs     <- Gen.listOfN(n, item)
      endpoint  <- Gen.alphaNumStr
      end       <- Gen.frequency((1, queryParams.map(qp => s"/$endpoint$qp").map(_.some)), (10, none[String]))
    } yield pairs.mkString("/") ++ end.getOrElse("")

    for {
      scheme      <- Gen.oneOf(schemes)
      user        <- Gen.oneOf(Gen.const(none[String]), Gen.alphaNumStr.suchThat(_.length > 3).map(u => s"$u@".some))
      host        <- Gen.oneOf(hostnameGen, ipv4Gen)
      portOrSlash <- Gen.oneOf(Gen.const("/"), Gen.chooseNum(2, 65535).map(p => s":$p/"))
      path        <- Gen.oneOf(queryParams, queryPath)
    } yield s"$scheme://${user.getOrElse("")}$host$portOrSlash$path"
  } :| "URI Generator"

  def ipv4Gen: Gen[String] = {
    val num = Gen.numChar.map(_.toString)
    def range(min: Int, max: Int) = Gen.choose(min.toChar, max.toChar).map(_.toString)
    val genDecOctet = Gen.oneOf(
      num,
      range(49, 57) |+| num,
      Gen.const("1") |+| num |+| num,
      Gen.const("2") |+| range(48, 52) |+| num,
      Gen.const("25") |+| range(48, 51)
    )
    Gen.listOfN(4, genDecOctet).map(_.mkString("."))
  } :| "IPv4 Generator"

  def ipv6Gen: Gen[String] = {
    val h16 = timesBetween(min = 1, max = 4, genHexDigit.map(_.toString))
    val ls32 = Gen.oneOf(h16 |+| Gen.const(":") |+| h16, ipv4Gen)
    val h16colon = h16 |+| Gen.const(":")
    val :: = Gen.const("::")

    Gen.oneOf(
      times(6, h16colon) |+| ls32,
      :: |+| times(5, h16colon) |+| ls32,
      opt(h16) |+| :: |+| times(4, h16colon) |+| ls32,
      opt(atMost(1, h16colon) |+| h16) |+| :: |+| times(3, h16colon) |+| ls32,
      opt(atMost(2, h16colon) |+| h16) |+| :: |+| times(2, h16colon) |+| ls32,
      opt(atMost(3, h16colon) |+| h16) |+| :: |+| opt(h16colon) |+| ls32,
      opt(atMost(4, h16colon) |+| h16) |+| :: |+| ls32,
      opt(atMost(5, h16colon) |+| h16) |+| :: |+| h16,
      opt(atMost(6, h16colon) |+| h16) |+| ::
    )
  } :| "IPv6 Generator"

  def genHexDigit: Gen[Char] = Gen.oneOf(
    Seq('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'))

  private def timesBetween[T: Monoid](min: Int, max: Int, g: Gen[T]): Gen[T] =
    for {
      n <- Gen.choose(min, max)
      l <- Gen.listOfN(n, g).suchThat(_.length == n)
    } yield l.fold(Monoid[T].empty)(_ |+| _)

  private def atMost[T: Monoid](n: Int, g: Gen[T]): Gen[T] =
    timesBetween(min = 0, max = n, g)

  private def opt[T](g: Gen[T])(implicit ev: Monoid[T]): Gen[T] =
    Gen.oneOf(g, Gen.const(ev.empty))

  private def times[T: Monoid](n: Int, g: Gen[T]): Gen[T] =
    Gen.listOfN(n, g).suchThat(_.length == n).map(_.reduce(_ |+| _))

}
