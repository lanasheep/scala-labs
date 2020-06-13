package bot

import com.bot4s.telegram.api.RequestHandler
import com.bot4s.telegram.clients.{FutureSttpClient}
import com.bot4s.telegram.future.{Polling, TelegramBot}
import com.softwaremill.sttp.okhttp.OkHttpFutureBackend
import com.softwaremill.sttp.json4s.asJson
import scala.collection.mutable.Set
import scala.collection.mutable.Map
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import cats.instances.future._
import cats.syntax.functor._
import com.softwaremill.sttp._
import com.bot4s.telegram.future.Polling
import com.bot4s.telegram.api.declarative.Commands

case class Response(data: List[Data])
case class Data(link: String)

class Service(implicit val backend: SttpBackend[Future, Nothing], implicit val ec: ExecutionContext) {
  private implicit val serialization = org.json4s.native.Serialization

  val request: RequestT[Id, Response, Nothing] = sttp
    .header("Authorization", "Client-ID 653027b508dec6b")
    .get(uri"https://api.imgur.com/3/gallery/search?q=cats")
    .response(asJson[Response])

  def getRandomCat() = backend.send(request).map { response =>
    scala.util.Random.shuffle(response.unsafeBody.data).head.link
  }
}

class Bot(override val client: RequestHandler[Future], val service: Service) extends TelegramBot
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

    val token = "750606265:AAHK36Mu8tKZwwIOVweydES81XheJPG-cx8"
    val service = new Service()
    val bot = new Bot(new FutureSttpClient(token), service)
    Await.result(bot.run(), Duration.Inf)
  }
}