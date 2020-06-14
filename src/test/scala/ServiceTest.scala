import com.softwaremill.sttp.SttpBackend
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import com.bot4s.telegram.models.User
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import slick.jdbc.H2Profile.api._

class ServiceTest extends AnyFlatSpec with Matchers with MockFactory {
  trait mocks {
    implicit val ec = ExecutionContext.global
    implicit val sttpBackend = mock[SttpBackend[Future, Nothing]]
    implicit val db: Database = Database.forConfig("h2mem1")
    val service = new Service(db)
    Await.result(service.init(), Duration.Inf)
  }

  "Service" should "add users and get list of users" in new mocks {
    val newUser1 = new User(1, false, "User1")
    val newUser2 = new User(2, false, "User2")
    val newUser3 = new User(3, false, "User3")

    service.addUser(1)
    service.addUser(2)
    service.addUser(3)

    val usersListCorrect: String = "1\n2\n3"

    Await.result(service.getUsers().map(_.mkString("\n")), Duration.Inf) shouldBe usersListCorrect
  }

  "Service" should "send messages and get unread" in new mocks {
    val newUser1 = new User(1, false, "User1")
    val newUser2 = new User(2, false, "User2")

    service.addUser(1)
    service.addUser(2)

    service.sendMessage(1, "hello")
    service.sendMessage(1, "))")

    service.sendMessage(2, "bye")
    service.sendMessage(2, "((")

    val messagesToUser1Correct: String = "hello\n))"
    val messagesToUser2Correct: String = "bye\n(("
    val empty: String = ""

    Await.result(service.getMessages(1).map(_.mkString("\n")), Duration.Inf) shouldBe messagesToUser1Correct
    Await.result(service.getMessages(1).map(_.mkString("\n")), Duration.Inf) shouldBe empty
    Await.result(service.getMessages(2).map(_.mkString("\n")), Duration.Inf) shouldBe messagesToUser2Correct
    Await.result(service.getMessages(2).map(_.mkString("\n")), Duration.Inf) shouldBe empty
  }

  "ServiceRest" should "get cat image" in new mocks {
    (sttpBackend.send[Response] _).expects(*).returning(Future.successful(
      com.softwaremill.sttp.Response.ok(Response(List(Data("cat"))))))

    val result: String = Await.result(service.getRandomCat(), Duration.Inf)

    result shouldBe "cat"
  }
}