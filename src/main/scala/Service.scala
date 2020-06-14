import com.softwaremill.sttp._
import com.softwaremill.sttp.json4s.asJson
import scala.concurrent.{ExecutionContext, Future}
import slick.jdbc.H2Profile.api._

case class Response(data: List[Data])
case class Data(link: String)

class Users(tag: Tag) extends Table[Int](tag, "USERS") {
  def id = column[Int]("ID", O.PrimaryKey)
  def * = (id)
}

class Messages(tag: Tag) extends Table[(Int, String)](tag, "MESSAGES") {
  def idTo = column[Int]("ID_TO")
  def message = column[String]("MSG")
  def * = (idTo, message)
}

class Service(val db: Database)(implicit val backend: SttpBackend[Future, Nothing], implicit val ec: ExecutionContext) {
  private implicit val serialization = org.json4s.native.Serialization

  val users = TableQuery[Users]
  val messages = TableQuery[Messages]

  def init(): Future[Unit] = for {
      _ <- db.run(users.schema.createIfNotExists)
      _ <- db.run(messages.schema.createIfNotExists)
    } yield ()

  def addUser(id: Int): Future[Int] = {
    db.run(users.insertOrUpdate(id))
  }

  def getUsers(): Future[Seq[Int]] = {
    db.run(users.result)
  }

  def sendMessage(id: Int, message: String): Future[Unit] = {
    val query = for {
      _ <- messages += (id, message)
    } yield ()
    db.run(query)
  }

  def getMessages(id: Int): Future[Seq[String]] = {
    val query = for {
      all <- messages.filter(_.idTo === id).map(_.message).result
      _ <- messages.filter(_.idTo === id).delete
    } yield all
    db.run(query)
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