package lectures.part4

import akka.actor.ActorSystem
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Flow, Sink, Source}

import java.util.Date

object AdvancedBackpressure extends App {

  implicit val system = ActorSystem("AB")

  // # SIMPLE BUFFER
  Flow[Int].map( _ * 2 ).buffer(10, OverflowStrategy.dropHead)

  // setup
  case class PagerEvent(descr: String, date: Date, inst: Int = 1)
  case class Notification(email: String, pagerEvent: PagerEvent)

  val events = List(
    PagerEvent("happen1", new Date),
    PagerEvent("happen2", new Date),
    PagerEvent("happen3", new Date),
    PagerEvent("happen4", new Date),
  )

  val eventSource = Source(events)
  val onCall = "ray@email"

  def sendEmail(notif: Notification) = println(s"Dear ${notif.email},you have ${notif.pagerEvent}")

  val notifSink = Flow[PagerEvent].map(n => Notification(onCall, n))
    .to(Sink.foreach[Notification](sendEmail))

  // # STANDARD
  // eventSource.to(notifSink).run()

  // # 1. ADVANCED BACKPRESSURE
  def sendEmailSlow(notif: Notification) = {
    Thread.sleep(1000)
    println(s"Dear ${notif.email},you have ${notif.pagerEvent}")
  }

  val agrNotifFlow = Flow[PagerEvent]
      .conflate((e1, e2) => {
        val inst = e1.inst + e2.inst
        PagerEvent(s"Here are $inst instances.", new Date, inst)
      })
      .map ( r => Notification(onCall, r))

  // here because sink was so slow all 4 events are agregated
  // good alternative to backpressure
  eventSource.via(agrNotifFlow).async.to(Sink.foreach[Notification](sendEmailSlow)).run()


  // # 2. SLOW PRODUCER, FAST CONSUMER
  // extrapolate / expand

  import scala.concurrent.duration._
  val slowSource = Source(Stream.from(1)).throttle(1, 1.second)
  val sink = Sink.foreach[Int](println)
  // you can set how to do that. here element are just copied till next elem produced
  val extrapolate = Flow[Int].extrapolate( e => Iterator.continually(e)) // fast flow. It adds elemnts during waiting time. Extrapolate

  slowSource.via(extrapolate).to(sink).run()

  //   Flow[Int].expand() will do same thing, just extrapolator works when there is demand, but expand all the time


}
