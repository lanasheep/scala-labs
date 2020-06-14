import com.softwaremill.sttp.SttpBackend
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import com.bot4s.telegram.models.User
import scala.collection.mutable.{ArrayBuffer, SortedSet}
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

class ServiceTest extends AnyFlatSpec with Matchers with MockFactory {
  trait mocks {
    implicit val ec = ExecutionContext.global
    implicit val sttpBackend = mock[SttpBackend[Future, Nothing]]

    val service = new Service()
  }

  "Service" should "add users" in new mocks {
    val newUser1 = new User(123, false, "Alice")
    val newUser2 = new User(321, false, "Bob")

    service.addUser(123)
    service.addUser(321)

    val usersCorrect: SortedSet[Int] = SortedSet(123, 321)

    service.users shouldBe usersCorrect
  }

  "Service" should "get list of users" in new mocks {
    val newUser1 = new User(123, false, "A")
    val newUser2 = new User(321, false, "B")
    val newUser3 = new User(789, false, "c")

    service.addUser(123)
    service.addUser(321)
    service.addUser(789)

    val usersListCorrect: String = "123\n321\n789"

    service.getUsers() shouldBe usersListCorrect
  }

  "Service" should "send messages" in new mocks {
    val newUser = new User(123, false, "Alice")

    service.addUser(123)

    service.sendMessage(123, "hello")
    service.sendMessage(123, "))")

    val messagesCorrect: ArrayBuffer[String] = ArrayBuffer("hello", "))")

    service.messages(123) shouldBe messagesCorrect
  }

  "Service" should "get unread messages" in new mocks {
    val newUser = new User(123, false, "Alice")

    service.addUser(123)

    service.sendMessage(123, "hello")
    service.sendMessage(123, "))")

    val messagesListCorrect: String = "hello\n))"

    service.getMessages(123) shouldBe messagesListCorrect
    service.getMessages(123).isEmpty shouldBe true
  }

  "ServiceRest" should "get cat image" in new mocks {
    (sttpBackend.send[Response] _).expects(*).returning(Future.successful(
      com.softwaremill.sttp.Response.ok(Response(List(Data("cat"))))))

    val result: String = Await.result(service.getRandomCat(), Duration.Inf)

    result shouldBe "cat"
  }
}
