# ScalaCheck Schema

ScalaCheck generators for various Iglu-compatible schema formats.

## Installation

The latest version of ScalaCheck Schema is 0.1.0, which is cross-built against Scala 2.11.x and 2.12.x.

If you're using SBT, add the following lines to your build file:

```scala
libraryDependencies += "com.snowplowanalytics" %% "scalacheck-schema" % "0.1.0"
```

## Usage

```scala
import org.json4s.jackson.parseJson
import com.snowplowanalytics.iglu.schemaddl.jsonschema.Schema
import com.snowplowanalytics.iglu.schemaddl.jsonschema.json4s.Json4sToSchema._
import com.snowplowanalytics.iglu.schemaddl.scalacheck.JsonGenSchema

val schemaJson = parseJson("""{"type": ["integer", "string"], "maxLength": 10}""")
val schemaObject: Schema = Schema.parse(schemaJson).getOrElse(throw new RuntimeException("Invalid JSON Schema"))
val jsonGen: Gen[JValue] = JsonGenSchema.json(schemaObject)
```

Or you can fetch existing Schema from Iglu Registry:

```scala
import com.snowplowanalytics.iglu.client.Resolver
import com.snowplowanalytics.iglu.core.{SchemaKey, SchemaVer}
import com.snowplowanalytics.iglu.schemaddl.scalacheck.{IgluSchemas, JsonGenSchema}

val resolver: Option[Resolver] = ???    // Can be some custom resolver or none for Iglu Central

for {
  schemaKey <- SchemaKey("com.snowplowanalytics.snowplow", "geolocation_context", "jsonschema", SchemaVer(1, 1, 0))
    .asRight[String]
  // Get schema from Iglu Central
  schemaJson <- IgluSchemas.lookup(None)(schemaKey)
  // Parse as JSON Schema AST
  schemaObject <- IgluSchemas.parseSchema(schemaJson)
  // Create JSON generateor
  gen = schemaObject.map(JsonGenSchema.json)
  // Generate JSON instance compatible with specified schema
  json = gen.sample
} yield json
```

## Copyright and License

Snowplow scalacheck-schema is copyright 2018 Snowplow Analytics Ltd.

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
