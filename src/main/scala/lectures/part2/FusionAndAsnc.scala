package lectures.part2

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Sink, Source}
import scala.concurrent.duration._

object FusionAndAsnc extends App {

  // ALL STREAM MATERIALIZATION IS HAPPENING IN SINGLE ACTOR (single thread)
  // THAT'S CALLED FUSION
  // IT IS GREAT FOR SIMPLE COMPUTATION, BUT NOT GOOD FOR COMPLEX
  // FOR COMPLEX USE ASYNC. THEN FLOWS ARE COMPUTED IN DIFFERENT ACTORS AND CONSOLIDATED IN ONE SINGLE SINK

  implicit val system: ActorSystem = ActorSystem("FusionAndAsync")

  val source = Source(1 to 1000)
  val flow1 = Flow[Int].map(_ + 10)
  val flow2 = Flow[Int].map(_ * 10)
  val sink = Sink.foreach[Int](println)

  val slowFlow1 = Flow[Int].map(x => {
    Thread.sleep(1000)
    x + 10
  })

  val slowFlow2 = Flow[Int].map(x => {
    Thread.sleep(1000)
    x * 10
  })

  // great
//  source
//    .via(flow1)
//    .via(flow2)
//    .to(sink)
//    .run()

  // shit
//    source
//      .via(slowFlow1)
//      .via(slowFlow2)
//      .to(sink)
//      .run()

  // better
  // async boundary
//  source
//    .via(slowFlow1).async
//    .via(slowFlow2).async
//    .to(sink)
//    .run()

  // ordering guaranty
  Source(1 to 3)
    .map(x => { println(s"First: $x"); x }).async
    .map(x => { println(s"Second: $x"); x }).async
    .map(x => { println(s"Third: $x"); x }).async
    .runWith(Sink.ignore)

//  First: 1
//  First: 2
//  Second: 1
//  Second: 2
//  Third: 1
//  First: 3
//  Third: 2
//  Second: 3
//  Third: 3

  // EACH FLOW (EACH ACTOR) ARE GETTING EACH STREAM ELEMENT IN ORDER
  // (BECAUSE EACH ARE PERFORMED ON SEPARATE ACTOR)
  // WITHOUT async IT IS JUST NATURAL GUARANTEED ORDER


}
