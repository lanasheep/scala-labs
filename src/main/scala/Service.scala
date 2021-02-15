import com.softwaremill.sttp._
import com.softwaremill.sttp.json4s.asJson
import scala.concurrent.{ExecutionContext, Future}
import slick.jdbc.H2Profile.api._

case class Response(data: List[Data])
case class Data(link: String)

class Users(tag: Tag) extends Table[(Int, String)](tag, "USERS") {
  def id = column[Int]("ID", O.PrimaryKey)
  def login = column[String]("LOGIN")
  def * = (id, login)
}

class Messages(tag: Tag) extends Table[(Int, String)](tag, "MESSAGES") {
  def idTo = column[Int]("ID_TO")
  def message = column[String]("MSG")
  def * = (idTo, message)
}

class Links(tag: Tag) extends Table[(Int, String)](tag, "LINKS") {
  def id = column[Int]("ID")
  def link = column[String]("LINK")
  def *  = (id, link)
}

class Service(val db: Database)(implicit val backend: SttpBackend[Future, Nothing], implicit val ec: ExecutionContext) {
  private implicit val serialization = org.json4s.native.Serialization

  val users = TableQuery[Users]
  val messages = TableQuery[Messages]
  val links = TableQuery[Links]

  def init(): Future[Unit] = for {
      _ <- db.run(users.schema.createIfNotExists)
      _ <- db.run(messages.schema.createIfNotExists)
      _ <- db.run(links.schema.createIfNotExists)
    } yield ()

  def getId(login: String): Future[Int] = {
    db.run(users.filter(_.login === login).map(_.id).result).map(_.head)
  }

  def addUser(id: Int, login: String): Future[Unit] = {
    db.run(users.insertOrUpdate((id, login))).map(_ => Unit)
  }

  def getUsers(): Future[Seq[String]] = {
    db.run(users.map(_.login).result)
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

  def getRandomCat(id: Int): Future[String] = {
    val request: RequestT[Id, Response, Nothing] = sttp
      .header("Authorization", "Client-ID 653027b508dec6b")
      .get(uri"https://api.imgur.com/3/gallery/search?q=cats")
      .response(asJson[Response])

    for {
      link <- backend.send(request).map { response =>
        scala.util.Random.shuffle(response.unsafeBody.data).head.link
      }
      _ <- db.run(links += (id, link))
    } yield link
  }

  def getStatistics(id: Int): Future[Seq[String]] = {
    db.run(links.filter(_.id === id).map(_.link).result)
  }
}