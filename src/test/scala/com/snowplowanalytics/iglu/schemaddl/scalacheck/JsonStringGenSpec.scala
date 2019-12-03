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

import org.apache.commons.validator.routines.{
  DomainValidator,
  EmailValidator,
  InetAddressValidator,
  UrlValidator
}
import org.specs2.{ScalaCheck, Specification}

class JsonStringGenSpec extends Specification with ScalaCheck {
  def is = s2"""
  hostnameGen generates valid host names $e1
  emailGen generates valid email addresses $e2
  ipv4Gen generates valid IP addresses $e3
  uriGen generates valid URLs $e4
  ipv6Gen generates valid IP addresses $e5
  """

  def e1 =
    prop(DomainValidator.getInstance().isValid(_: String))
      .setGen(JsonStringGen.hostnameGen)

  def e2 =
    prop(EmailValidator.getInstance().isValid(_: String))
      .setGen(JsonStringGen.emailGen)

  def e3 =
    prop(InetAddressValidator.getInstance().isValid(_: String))
      .setGen(JsonStringGen.ipv4Gen)

  def e4 =
    prop(UrlValidator.getInstance().isValid(_: String))
      .setGen(JsonStringGen.uriGen)

  def e5 =
    prop(InetAddressValidator.getInstance().isValid(_: String))
      .setGen(JsonStringGen.ipv6Gen)
}
