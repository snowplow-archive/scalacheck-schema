# ScalaCheck Schema

ScalaCheck generators for various Iglu-compatible schema formats.

## Installation

The latest version of ScalaCheck Schema is 0.2.0, which is built against Scala 2.12.x.

If you're using SBT, add the following lines to your build file:

```scala
libraryDependencies += "com.snowplowanalytics" %% "scalacheck-schema" % "0.2.0"
```

## Usage

```scala
import com.snowplowanalytics.iglu.schemaddl.jsonschema.Schema
import com.snowplowanalytics.iglu.schemaddl.jsonschema.circe.implicits._
import com.snowplowanalytics.iglu.schemaddl.scalacheck.JsonGenSchema
import io.circe.literal._

val schemaJson: Json = json"""{"type": ["integer", "string"], "maxLength": 10}"""
val schemaObject: Schema = Schema.parse(schemaJson).getOrElse(throw new RuntimeException("Invalid JSON Schema"))
val jsonGen: Gen[Json] = JsonGenSchema.json(schemaObject)
```

Or you can generate jsons from an existing Schema in an Iglu Registry:

```scala
import com.snowplowanalytics.iglu.client.resolver.Resolver
import com.snowplowanalytics.iglu.core.{SchemaKey, SchemaVer}
import com.snowplowanalytics.iglu.schemaddl.scalacheck.{IgluSchemas, JsonGenSchema}

val jsonGen: EitherT[IO, String, Gen[Json]] = for {
  // define a resolver using Iglu Central as registry
  r <- EitherT.right(Resolver.init[IO](0, None, Registry.IgluCentral))
  // schema we want to generate jsons from
  schemaKey = SchemaKey("com.snowplowanalytics.snowplow", "geolocation_context", "jsonschema", SchemaVer(1, 1, 0))
  // get the schema from Iglu Central
  schemaJson <- EitherT(IgluSchemas.lookup[IO](r, schemaKey))
  // parse as JSON Schema AST
  schemaObject <- EitherT.fromEither(IgluSchemas.parseSchema(schemaJson))
} yield JsonGenSchema.json(schemaObject)
```

## Copyright and License

Snowplow scalacheck-schema is copyright 2018-2019 Snowplow Analytics Ltd.

Licensed under the **[Apache License, Version 2.0][license]** (the "License");
you may not use this software except in compliance with the License.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.


[vagrant-install]: http://docs.vagrantup.com/v2/installation/index.html
[virtualbox-install]: https://www.virtualbox.org/wiki/Downloads

[travis]: https://travis-ci.org/snowplow-incubator/scalacheck-schema
[travis-image]: https://travis-ci.org/snowplow-incubator/scalacheck-schema.png?branch=master

[license-image]: http://img.shields.io/badge/license-Apache--2-blue.svg?style=flat
[license]: http://www.apache.org/licenses/LICENSE-2.0

[release-image]: http://img.shields.io/badge/release-0.1.0-rc1-blue.svg?style=flat
[releases]: https://github.com/snowplow-incubator/scalacheck-schema/releases
