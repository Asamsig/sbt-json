import org.scalatest.{FlatSpec, Matchers}
import play.api.libs.json.Json

import scala.io.Source
import scala.utils.JsonFile

class SbtJsonPluginTests extends FlatSpec with Matchers {
  "JSON Object with one field" should "deserialized with correct field value" in {
    import models.json.foo._

    val json = JsonFile.readeJsonFile("foo")

    val foo = Json.parse(json).as[Foo]

    foo.foo shouldEqual 42
  }

  "Generated code from JSON document of FB post with optional field" should "contain optional field if it was marked as such" in {
    import models.json.fbpost._

    val json =
      """{
        |  "id":"339880699398622_371821532871205",
        |  "created_time":"2012-06-19T07:57:54+0000",
        |  "full_picture":"https:\/\/scontent.xx.fbcdn.net\/v\/t31.0-8\/s720x720\/469692_371821072871251_145095902_o.jpg?oh=8a1be9485002e2d25dbe396a8f1fe176&oe=5A2F45F8"
        |}
        |""".stripMargin

    val post = Json.parse(json).as[Fbpost]

    post.message shouldEqual None
  }

  "Generated code from JSON document of facebook site scala4beginner" should "contain optional message field" in {
    import models.json.facebook._

    val json = JsonFile.readeJsonFile("facebook")

    val fb = Json.parse(json).as[Facebook]

    fb.posts.data.head.message shouldEqual None
  }

  "Generated code from JSON document of HPImageArchive" should "contain url" in {
    import models.json.hpimagearchive._

    val json = Source.fromURL("https://www.bing.com/HPImageArchive.aspx?format=js&idx=0&n=1&mkt=en-US").mkString

    val imageArchive = Json.parse(json).as[HPImageArchive]

    imageArchive.images.head.url should not be empty
  }

  "Generated code from JSON document which contains a Scala type name" should "contain a class name with suffix" in {
    import models.json.list._

    val json = JsonFile.readeJsonFile("list")

    val list = Json.parse(json).as[ListModel]

    list.list shouldEqual Seq(1, 2, 3)
  }
}