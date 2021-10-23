package lectures.part5

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.stream.{ActorMaterializer, KillSwitches, OverflowStrategy}
import akka.stream.scaladsl.{BroadcastHub, Flow, Keep, MergeHub, Sink, Source}
import akka.util.Timeout

import scala.concurrent.duration._

object DynamicStreamHandling extends App {
  implicit val system = ActorSystem("DSH")
  implicit val disp = system.dispatcher

  // # 1 DYNAMIC STREAM STOPPING STARTING
  // Kill Switch

  val ksFlow = KillSwitches.single[Int]

  val counter = Source(Stream.from(1)).throttle(1, 1.second).log("Counter")
  val sink = Sink.ignore

  val ks = counter
    .viaMat(ksFlow)(Keep.right)
    .to(sink)
  //  .run()

//  system.scheduler.scheduleOnce(3.seconds) {
//    ks.shutdown()
//  }

  // for multiple streams
  val anotherCounter =  Source(Stream.from(1)).throttle(1, 1.second).log("Another counter")
  val sharedKs = KillSwitches.shared("sharedKs")

//  counter.via(sharedKs.flow).runWith(Sink.ignore)
//  anotherCounter.via(sharedKs.flow).runWith(Sink.ignore)
//  system.scheduler.scheduleOnce(3.seconds) {
//    sharedKs.shutdown()
//  }

  // # 2 DYNAMICLY ADD FUN-IN and FUN-OUT
  // Mergehub

  val dynamicMerge = MergeHub.source[Int]
  val matrSink = dynamicMerge.to(Sink.foreach[Int](println)).run()

//  Source(1 to 10).runWith(matrSink)
//  counter.runWith(matrSink)

  val dynamicBroadcast = BroadcastHub.sink[Int]
  val matrSource = Source (1 to 100) runWith dynamicBroadcast

  ///////////// challange, copied

  val merge = MergeHub.source[String]
  val bcast = BroadcastHub.sink[String]
  val (publisherPort, subscriberPort) = merge.toMat(bcast)(Keep.both).run()

  subscriberPort.runWith(Sink.foreach(e => println(s"I received: $e")))
  subscriberPort.map(string => string.length).runWith(Sink.foreach(n => println(s"I got a number: $n")))

  Source(List("Akka", "is", "amazing")).runWith(publisherPort)
  Source(List("I", "love", "Scala")).runWith(publisherPort)
  Source.single("STREEEEEEAMS").runWith(publisherPort)

}
