package lectures.part2

// BACKPRESSURE PROTOCOL
// IT WILL:
// - try to slow down
// - buffer
// - drop elements
// - kill stream
// STREAM ARE STARTED FROM RIGHT - SINK
// IF SINK IS SLOW, IT TELLS UPPER FLOW TO SLOW DOWN. AND UPPER...

import akka.actor.ActorSystem
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{ Flow, Sink, Source }

import scala.concurrent.duration.DurationInt

object Backpressure extends App {

  implicit val system = ActorSystem("BP")

  val fastSource = Source(1 to 1000)
  val slowSink = Sink.foreach[Int](x => {
    Thread.sleep(1000)
    println(x)
  })

 // fastSource.runWith(slowSink) // this is natural, as it happens on same actor. NOT BACKPRESSURE
 // fastSource.async.runWith(slowSink) // BACKPRESSURE protocol kicks in

  // BUFFER
  val flow = Flow[Int].map( x => {
    println(s"Incomming: $x")
    x + 1
  })
  val bufferedFlow = flow.buffer(10, overflowStrategy = OverflowStrategy.dropHead)
  fastSource.async
    .via(bufferedFlow).async
    .to(slowSink)
    //.run()
  // oldest element in buffer dropped.
  // It receive all elements at beginning, but most in this case was dropped
  // first elements will process, and only last 10 will be guaranteed

  // OTHER OverflowStrategies:
  // - dropHead - drops oldest
  // - dropTail - drops newest
  // - dropNew - new element added (keeps buffer)
  // - dropBuffer - drops buffer
  // - backpressure - emit BP signal
  // - fail - terminate stream

  // THROTTLE
  fastSource
    .throttle(2, 1.seconds)  // 2 elem per second
    .runWith(slowSink)
}
