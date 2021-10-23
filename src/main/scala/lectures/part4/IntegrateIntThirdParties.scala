package lectures.part4

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.pattern.ask
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout

import java.util.Date
import scala.concurrent.Future
import scala.util.Random
import scala.concurrent.duration._
import scala.util.parsing.json.JSON.headOptionTailToFunList

object  IntegrateIntThirdParties extends App {

  // MERGE WITH SOME FUTURE VALUE
  implicit val system = ActorSystem("ASTP")
  //implicit val ec = system.dispatcher // not recomended(!!!)
  // do applicaion.config for this!!
  implicit val dispatcher = system.dispatchers.lookup("dedicated-dispatcher")

  case class PagerEvent(app: String, descr: String, date: Date)

  val eventsSource = Source(List(
    PagerEvent("app1", "happen1", new Date),
    PagerEvent("app2", "happen2", new Date),
    PagerEvent("app3", "happen3", new Date),
    PagerEvent("app2", "happen4", new Date),
  ))

  val oneTypeevents = eventsSource.filter(_.app == "app2")

  ///////////////////////// service
  object PagerService {
    private val engList = List("Ray", "Bob", "Jack")
    private val emails: Map[String, String] = Map(
      "Ray" -> "raymail",
      "Bob" -> "bobmail",
      "jack" -> "jackmail",
    )
    def processEvent(pagerEvent: PagerEvent) = Future {
      val index = (new Random).nextInt(2)
      val engineer = engList(index)
      val email = emails(engineer)
      println(s"Engineer $engineer recieved $pagerEvent")
      email
    }
  }
  //////////////////////////////////

  // run on its own execution dispatcher (not actor one), as it can be ehosted
  val engEmail = oneTypeevents.mapAsync(parallelism = 4)(event => PagerService.processEvent(event)) // (!!!)
  // guaranteed order of elements (.MapAsyncUnordered will not)
  val sink = Sink.foreach[String](e => println(s"Emailed to: $e"))

  //engEmail.to(sink).run()

  // ///////////////////////// service as actor
  class PagerActor extends Actor with ActorLogging {
    private val engList = List("Ray", "Bob", "Jack")
    private val emails: Map[String, String] = Map(
      "Ray" -> "raymail",
      "Bob" -> "bobmail",
      "jack" -> "jackmail",
    )
    def processEvent(pagerEvent: PagerEvent) = {
      val index = (new Random).nextInt(2)
      val engineer = engList(index)
      val email = emails(engineer)
      println(s"Engineer $engineer recieved $pagerEvent")
      email
    }
    override def receive: Receive = {
      case e: PagerEvent =>
        sender() ! processEvent(e)
    }
  }
  //////////////////////////////////

  val actor = system.actorOf(Props[PagerActor])
  implicit val timeout = Timeout(3.second)
  val alternatieemail = oneTypeevents.mapAsync(parallelism = 4)( e => (actor ? e).mapTo[String])
  alternatieemail.to(sink).run()

  // DO not confuse async to MapAsync
}
