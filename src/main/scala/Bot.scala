package bot

import cats.instances.future._
import cats.syntax.functor._
import com.bot4s.telegram.api.RequestHandler
import com.bot4s.telegram.api.declarative.Commands
import com.bot4s.telegram.clients.{FutureSttpClient}
import com.bot4s.telegram.future.{Polling, TelegramBot}
import com.softwaremill.sttp.SttpBackendOptions
import com.softwaremill.sttp.okhttp.OkHttpFutureBackend
import scala.collection.mutable.Set
import scala.collection.mutable.Map
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

class Bot(override val client: RequestHandler[Future]) extends TelegramBot
  with Polling
  with Commands[Future] {
  val users: Set[Int] = Set[Int]()
  val messages: Map[Int, ArrayBuffer[String]] = Map[Int, ArrayBuffer[String]]()

  onCommand("/start") { implicit msg =>
    msg.from match {
      case None => reply("error").void
      case Some(user) => {
        users += user.id
        reply(s"you are registered\nyour id is ${user.id}").void
      }
    }
  }

  onCommand("/users") { implicit msg =>
    reply(s"users:\n${users.mkString("\n")}").void
  }

  onCommand("/send") { implicit msg =>
    withArgs { args =>
      if (args.size != 2) reply("error").void
      else {
        val id = args(0).toInt
        val message = args(1).toString
        if (!messages.contains(id)) {
          messages(id) = ArrayBuffer()
        }
        messages(id) += message
        reply("ok").void
      }
    }
  }

  onCommand("/check") { implicit msg =>
    msg.from match {
      case None => reply("error").void
      case Some(user) => {
        val id = user.id
        if (!messages.contains(id)) {
          messages(id) = ArrayBuffer()
        }
        reply(s"unread messages:\n${messages(id).mkString("\n")}")
        messages(id).clear()
        reply("").void
      }
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
    val bot = new Bot(new FutureSttpClient(token))
    Await.result(bot.run(), Duration.Inf)
  }
}