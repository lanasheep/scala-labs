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

class Bot(override val client: RequestHandler[Future], val service: Service) extends TelegramBot
  with Polling
  with Commands[Future] {

  onCommand("/start") { implicit msg =>
    msg.from match {
      case None => reply("error").void
      case Some(user) => {
        val id = user.id
        service.addUser(id)
        reply(s"you are registered\nyour id is ${id}").void
      }
    }
  }

  onCommand("/users") { implicit msg =>
    reply(s"users:\n${service.getUsers()}").void
  }

  onCommand("/send") { implicit msg =>
    withArgs { args =>
      if (args.size != 2) reply("error").void
      else {
        val id = args(0).toInt
        val message = args(1).toString
        service.sendMessage(id, message)
        reply("ok").void
      }
    }
  }

  onCommand("/check") { implicit msg =>
    msg.from match {
      case None => reply("error").void
      case Some(user) => {
        val id = user.id
        reply(s"unread messages:\n${service.getMessages(id)}").void
      }
    }
  }

  onCommand("/cat") { implicit msg =>
    service.getRandomCat.flatMap(link => reply(link)).void
  }
}

object Bot {
  def main(args: Array[String]): Unit = {
    implicit val ec: ExecutionContext = ExecutionContext.global
    implicit val backend = OkHttpFutureBackend(
      SttpBackendOptions.Default.socksProxy("ps8yglk.ddns.net", 11999)
    )

    val token = ""
    val service = new Service()
    val bot = new Bot(new FutureSttpClient(token), service)
    Await.result(bot.run(), Duration.Inf)
  }
}