import com.bot4s.telegram.api.RequestHandler
import com.bot4s.telegram.clients.{FutureSttpClient}
import com.bot4s.telegram.future.{Polling, TelegramBot}
import com.softwaremill.sttp.okhttp.OkHttpFutureBackend
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import cats.instances.future._
import cats.syntax.functor._
import com.softwaremill.sttp._
import com.bot4s.telegram.future.Polling
import com.bot4s.telegram.api.declarative.Commands
import slick.jdbc.H2Profile.api._

class Bot(override val client: RequestHandler[Future], val service: Service) extends TelegramBot
  with Polling
  with Commands[Future] {

  onCommand("/start") { implicit msg =>
    msg.from match {
      case None => reply("error").void
      case Some(user) => {
        val id = user.id
        service.addUser(id, user.firstName).flatMap(_ => reply(s"you are registered\nyour id is ${id}")).void
      }
    }
  }

  onCommand("/users") { implicit msg =>
    service.getUsers().map(_.mkString("\n")).flatMap(str => reply(s"users:\n${str}")).void
  }

  onCommand("/send") { implicit msg =>
    withArgs { args =>
      if (args.size != 2) reply("error").void
      else {
        val id = args(0).toInt
        val message = args(1).toString
        service.sendMessage(id, message).flatMap(_ => reply("ok")).void
      }
    }
  }

  onCommand("/check") { implicit msg =>
    msg.from match {
      case None => reply("error").void
      case Some(user) => {
        val id = user.id
        service.getMessages(id).map(_.mkString("\n")).flatMap(str => reply(s"unread messages:\n${str}")).void
      }
    }
  }

  onCommand("/cat") { implicit msg =>
    msg.from match {
      case None => reply("error").void
      case Some(user) => {
        val id = user.id
        service.getRandomCat(id).flatMap(link => reply(link)).void
      }
    }
  }

  onCommand("/stats") { implicit msg =>
    withArgs { args =>
      if (args.isEmpty) {
        msg.from match {
          case None => reply("error").void
          case Some(user) => {
            val id = user.id
            service.getStatistics(id).map(_.mkString("\n")).flatMap(str => reply(s"cats you saw today:\n${str}")).void
          }
        }
      }
      else if (args.size == 1) {
        val anotherUser = args(0).toString
        if (anotherUser forall Character.isDigit) {
          val id = args(0).toInt
          service.getStatistics(id).map(_.mkString("\n")).flatMap(str => reply(s"cats seen by ${id} today:\n${str}")).void
        }
        for {
          idFromLogin <- service.getId(anotherUser)
          ans <- service.getStatistics(idFromLogin).map(_.mkString("\n"))
        } yield reply(s"cats seen by ${anotherUser} today:\n${ans}").void
      }
      else reply("error").void
    }
  }
}

object Bot {
  def main(args: Array[String]): Unit = {
    implicit val ec: ExecutionContext = ExecutionContext.global
    implicit val backend = OkHttpFutureBackend(
      SttpBackendOptions.Default.socksProxy("ps8yglk.ddns.net", 11999)
    )

    val token = ""
    val db: Database = Database.forConfig("h2mem1")
    val service = new Service(db)
    Await.result(service.init(), Duration.Inf)
    val bot = new Bot(new FutureSttpClient(token), service)
    Await.result(bot.run(), Duration.Inf)
  }
}