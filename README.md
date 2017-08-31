# sbt-json

sbt-json is an sbt plugin that generates Scala case classes for easy, statically typed, and implicit access of JSON data e.g. from API responses.

The plugin makes it possible to access JSON documents in a statically typed way including auto-completion. It takes a sample JSON document as input (either from a file or a URL) and generates Scala types that can be used to read data with the same structure.

sbt-json integrates very well with the [play-json library](https://github.com/playframework/play-json) as it can also generate play-json formats for implicit conversion of a `JsValue` to its Scala representation.

## Prerequisites

0.13.5 <= sbt version < 1.0

## Installation

Install the plugin according to the [sbt documentation](http://www.scala-sbt.org/0.13/docs/Using-Plugins.html).

### Edit `project/plugins.sbt`

    addSbtPlugin("com.github.battermann" % "sbt-json" % "0.2.4")

### Edit `build.sbt`

Edit the `build.sbt` file to enable the plugin and to generate case class sources whenever the compile task is executed:

    enablePlugins(SbtJsonPlugin)

If you want to use play-json add the play-json library dependency:

    libraryDependencies += "com.typesafe.play" %% "play-json" % "2.6.0"

## Settings

| name     | default | description |
| -------- | ------- | ----------- |
| jsonInterpreter | `interpretWithPlayJsonFormats`    | Specifies which interpreter to use. `interpret` and `interpretWithPlayJsonFormats` |
| includeJsValues     | `includeAll`    | Combinator that specifies which JSON values should be in-/excluded for analyzation. `exceptEmptyArrays` and `exceptNullValues`. Example: `includeAll.exceptEmptyArrays` |
| jsonSourcesDirectory  | `src/main/resources/json` | Path containing the `.json` files to analyze. |
| jsonUrls  | `Nil` | List of urls that serve JSON data to be analyzed. |
| jsonOptionals | `Nil` | Specify which fields should be optional, e.g. `jsonOptionals := Seq(("<package_name>", "<class_name>", "<field_name>"))` |

## Example

If you want to analyze JSON data form `https://www.bing.com/HPImageArchive.aspx?format=js&idx=0&n=1&mkt=en-US` and ignore empty arrays, add the following lines to the `build.sbt` file:

    jsonUrls += "https://www.bing.com/HPImageArchive.aspx?format=js&idx=0&n=1&mkt=en-US"
    includeJsValues := includeAll.exceptEmptyArrays

Then use play-json to read the JSON data:

    import play.api.libs.json.Json
    import models.json.hpimagearchive._

    val json = Source.fromURL("https://www.bing.com/HPImageArchive.aspx?format=js&idx=0&n=1&mkt=en-US").mkString
    val imageArchive = Json.parse(json).as[HPImageArchive]
    println(imageArchive.images.head.url)

## Settings in depth

### jsonInterpreter

The `jsonInterpreter` setting specifies if [play-json formats](https://www.playframework.com/documentation/2.6.x/ScalaJsonCombinators#Format) for implicit conversion will be generated. There are two options for this setting available, `interpretWithPlayJsonFormats` (default) and `interpret`. The type of the interpreter function is:

    type Interpreter = Seq[CaseClass] => CaseClassSource

The interpreters are located in `CaseClassToStringInterpreter` and can be set like this in the `build.sbt` file:

    import CaseClassToStringInterpreter._

    jsonInterpreter := interpretWithPlayJsonFormats

### includeJsValues

By default the code generation will fail if the JSON sample contains empty arrays or null values. This follows the fail fast paradigm because some type information might be missing.

To change this behavior you can set `includeJsValues` to ignore empty arrays or null values. The type of this setting is `type Include = JsValue => Boolean` and there are two combinators available as well as a convenient syntax (implicit classes).

Add this import statement to the `build.sbt`:

    import SchemaExtractorOptions._

Configure this setting to ignore empty arrays:

    includeJsValues := includeAll.exceptEmptyArrays

Ignore null values:

    includeJsValues := includeAll.exceptEmptyArrays

Ignore empty arrays as well as null values:

    includeJsValues := includeAll.exceptEmptyArrays.exceptNullValues

### jsonSourcesDirectory

By default all files with a `.json` extension in the directory `src/main/resources/json` will be analyzed. To change the directory set `jsonSourcesDirectory` of type `jva.io.File` to the desired value, e.g.:

    jsonSourcesDirectory := baseDirectory.value / "json"

### jsonUrls

`jsonUrls` is a sequence of strings that represent URLs that serve JSON documents to be analyzed. Add a new URL like this:

    jsonUrls += "https://www.bing.com/HPImageArchive.aspx?format=js&idx=0&n=1&mkt=en-US"

### jsonOptionals

If the JSON documents contain optional fields, they have to be explicitly marked as such. To do this, add a tuple containing the package name, class name, and field name to the `jsonOptionals` setting.

#### Example

Place a file `fbpost.json` containng a JSON document of a facebook post inside the json-sources directory:

    {
        "id":"339880699398622_241628669274112",
        "created_time":"2012-06-19T07:51:06+0000",
        "message":"great information for Linux fan ;-)",
        "full_picture":"https:\/\/scontent.xx.fbcdn.net\/v\/t31.0-8\/s720x720\/177827_10151014484731203_401775304_o.jpg?oh=69db574b81ebe6bfe97a4077b6806775&oe=5A25DA17"
    }

You can inspect the result of the code generation by running the sbt-json task `printJsonModels`:

    [info] /** MACHINE-GENERATED CODE. DO NOT EDIT DIRECTLY */
    [info] package models.json.fbpost
    [info] 
    [info] case class Fbpost(
    [info]   id: String,
    [info]   created_time: String,
    [info]   message: String,
    [info]   full_picture: String
    [info] )
    [info] 
    [info] object Fbpost {
    [info]   import play.api.libs.json.Json
    [info] 
    [info]   implicit val formatFbpost = Json.format[Fbpost]
    [info] }

Here the type of the `message` field is `String`. However, some facebook posts do not contain a message field. Implicit decoding of a JSON document like this will fail:

    {
        "id":"339880699398622_371821532871205",
        "created_time":"2012-06-19T07:57:54+0000",
        "full_picture":"https:\/\/scontent.xx.fbcdn.net\/v\/t31.0-8\/s720x720\/469692_371821072871251_145095902_o.jpg?oh=8a1be9485002e2d25dbe396a8f1fe176&oe=5A2F45F8"
    }

To fix this, the field has to be marked as optional. Add the following line to the `build.sbt` file:

    jsonOptionals += ("fbpost", "Fbpost", "message")

Run `reload` in the sbt console and inspect the generated code again with `printJsonModels`. The `message` field is now of type `Option[String]`:

    [info] /** MACHINE-GENERATED CODE. DO NOT EDIT DIRECTLY */
    [info] package models.json.fbpost
    [info] 
    [info] case class Fbpost(
    [info]   id: String,
    [info]   created_time: String,
    [info]   message: Option[String],
    [info]   full_picture: String
    [info] )
    [info] 
    [info] object Fbpost {
    [info]   import play.api.libs.json.Json
    [info] 
    [info]   implicit val formatFbpost = Json.format[Fbpost]
    [info] }

## Code generation features

### Unification of array types of similar JSON objects

If the JSON objects of an array are not consistent, the generator will unify the type by making all fields optional that are not shared by all objects.

#### Example

The fields `message` and `full_picture` of the generated case class will be optional for this sample JSON document:

    {
        "posts":{
            "data":[
            {
                "id":"339880699398622_185311264930535",
                "created_time":"2012-06-06T08:31:09+0000"
            }
            {
                "id":"339880699398622_347136625339696",
                "created_time":"2012-05-10T06:42:49+0000",
                "message":"Functional Programming and Android Game Development, a Happy Couple"
            },
            {
                "id":"339880699398622_403676613005660",
                "created_time":"2012-05-09T21:24:31+0000",
                "message":"an HTML5 version of the popular Cut the Rope  game with 25 levels is now available online for free\nhttp:\/\/www.cuttherope.ie\/",
                "full_picture":"https:\/\/external.xx.fbcdn.net\/safe_image.php?d=AQC2A2jO1N9RhI2g&url=http\u00253A\u00252F\u00252Fwww.cuttherope.ie\u00252Ffb.png&_nc_hash=AQClKYpmQtwavvP0"
            }
            ],
        "id":"339880699398622"
    }

Generated case classes:

    /** MACHINE-GENERATED CODE. DO NOT EDIT DIRECTLY */
    package models.json.facebook

    case class Facebook(
        posts: Posts,
        id: String
    )

    case class Posts(
        data: Seq[Data]
    )

    case class Data(
        id: String,
        created_time: String,
        full_picture: Option[String],
        message: Option[String]
    )

### Ensure unique names

The generator will search for equal class names which can not be defined within the same scope and append ascending numbers starting with 1.

#### Example

Consider this JSON document:

    {
        "value1": {
            "foo": { "value": 42 }
        },
        "value2": {
            "foo": { "value": "some string" }
        }
    }

The fields of the objects `value1` and `value2` have the same name (`foo`) but different types. Therefore the generated class names will be `Foo` and `Foo1`:

    case class Equalnames(
        value1: Value1,
        value2: Value2
    )

    case class Value1(
        foo: Foo  
    )

    case class Foo(
        value: Double
    )

    case class Value2(
        foo: Foo1
    )

    case class Foo1(
        value: String
    )

### Avoid Scala reserved words and type names

The generator tries to avoid Scala reserved words and type names by appending the suffix `Model` to a class name that is a potential candidate to clash. E.g. `case class List()` will become `case class ListModel()`, or `case class MyClass(this: String)` will become ``case class MyClass(`this`: String)``. (This feature is not yet fully tested and there is still no guaranty that there won't be any clashes.)

### Unify type with exact same structure

If an objects schema has the exact same structure as a schema that was found before, it will be substituted by that schema.

    {
        "geometry": {
            "location": {
                "lat": 37.42291810,
                "lng": -122.08542120
            },
            "viewport": {
                "northeast": {
                    "lat": 37.42426708029149,
                    "lng": -122.0840722197085
                },
                "southwest": {
                    "lat": 37.42156911970850,
                    "lng": -122.0867701802915
                }
            }
        }
    }

Note that there are no case classes `Northeast` and `Southwest` generated. Instead the type of the fields will be declared as `Location`:

    case class Geo(
        geometry: Geometry
    )

    case class Geometry(
        location: Location,
        viewport: Viewport
    )

    case class Location(
        lat: Double,
        lng: Double
    )

    case class Viewport(
        northeast: Location,
        southwest: Location
    )

## Tasks

| name     | description |
| -------- | ----------- |
| printJsonModels | Prints the generated case classes to the console. |
| generateJsonModels | Creates source files containing the generates case classes. |

## Contributing

Contributions are very welcome and highly appreciated. You can contribute by sending pull request, by reporting bugs and feature requests [here](https://github.com/battermann/sbt-json/issues), or by giving feedback and suggestions for improvement.
