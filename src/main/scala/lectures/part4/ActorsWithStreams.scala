package lectures.part4

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.Timeout

import scala.concurrent.duration._

object ActorsWithStreams extends App {

  implicit val system = ActorSystem("AWS")

  // ACTOR AS FLOW
  class SimpleActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case s: String =>
        log.info(s"Recieved string: $s")
        sender() ! s"$s$s"
      case n: Int=>
        log.info(s"Recieved Int: $n")
        sender() ! (n * 2)
      case _ =>
    }
  }

  val actor = system.actorOf(Props[SimpleActor], "myactor")

  val numbSource = Source(1 to 10)

  implicit val timeout = Timeout(1.second)
  val actorBasedFlow = Flow[Int].ask[Int](parallelism = 4)(actor)

  // numbSource.via(actorBasedFlow).to(Sink.ignore).run()
  // equivalent:
  // numbSource.ask[Int](parallelism = 4)(actor).to(Sink.ignore).run()


  // ACTOR AS SOURCE
  val actorSource = Source.actorRef[Int](bufferSize = 4, overflowStrategy = OverflowStrategy.dropHead)
  val matrActorRef = actorSource.to(Sink.foreach[Int]( x => println(s"Actor source to sink: $x"))).run()
//  matrActorRef ! 10 // cool!!!
//  matrActorRef ! akka.actor.Status.Success("compleate") // stop actor
//  matrActorRef ! 20

  // ACTOR AS Sink
  case object StreamInt
  case object StreamAsk
  case object StreamComplete
  case class StreamFiled(ex: Throwable)

  class Destination extends Actor with ActorLogging {
    override def receive: Receive = {
      case StreamInt =>
        log.info("Stream initialized")
        sender() ! StreamAsk // important
      case StreamComplete =>
        log.info("Stream compleated")
      case ex: StreamFiled =>
        log.info(s"Stream failed: ${ex.toString}")
      case msg =>
        log.info(s"Message recieved $msg")
        sender() ! StreamAsk // important
    }
  }

  val destinationActor = system.actorOf(Props[Destination])

  val actorSink = Sink.actorRefWithAck[Int](
    destinationActor,
    onInitMessage = StreamInt,
    onCompleteMessage = StreamComplete,
    ackMessage = StreamAsk,
    onFailureMessage = thr => StreamFiled(thr)
  )

  Source(1 to 10).to(actorSink).run()



}
