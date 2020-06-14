import com.softwaremill.sttp._
import com.softwaremill.sttp.json4s.asJson
import scala.collection.mutable.SortedSet
import scala.collection.mutable.Map
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{ExecutionContext, Future}

case class Response(data: List[Data])
case class Data(link: String)

class Service(implicit val backend: SttpBackend[Future, Nothing], implicit val ec: ExecutionContext) {
  private implicit val serialization = org.json4s.native.Serialization

  val users: SortedSet[Int] = SortedSet[Int]()
  val messages: Map[Int, ArrayBuffer[String]] = Map[Int, ArrayBuffer[String]]()

  def addUser(id: Int) {
    users += id
  }

  def getUsers(): String = {
    users.mkString("\n")
  }

  def sendMessage(id: Int, message: String) {
    if (!messages.contains(id)) {
      messages(id) = ArrayBuffer()
    }
    messages(id) += message
  }

  def getMessages(id: Int): String = {
    if (!messages.contains(id)) {
      messages(id) = ArrayBuffer()
    }
    val result = messages(id).mkString("\n")
    messages(id).clear()
    result
  }

  def getRandomCat(): Future[String] = {
    val request: RequestT[Id, Response, Nothing] = sttp
      .header("Authorization", "Client-ID 653027b508dec6b")
      .get(uri"https://api.imgur.com/3/gallery/search?q=cats")
      .response(asJson[Response])

    backend.send(request).map { response =>
      scala.util.Random.shuffle(response.unsafeBody.data).head.link
    }
  }
}

