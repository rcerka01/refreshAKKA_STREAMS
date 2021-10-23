package exercises.part3

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape}
import akka.stream.javadsl.RunnableGraph
import akka.stream.scaladsl.{ Balance, Broadcast, Flow, GraphDSL, Merge, Sink, Source }
import scala.concurrent.duration._

object Graphs extends App {


  implicit val system =  ActorSystem("GraphsSystem")

  val source = Source(1 to 100)
  val source2 = Source(1 to 100).throttle(5, 1.seconds)
  val flow1 = Flow[Int].map(_ + 1)
  val flow2 = Flow[Int].map(_ * 10)
  val sink = Sink.foreach[Int](x => println(s"To Sink1: $x"))
  val sink2 = Sink.foreach[Int](x => println(s"To Sink2: $x"))


  //////////////////////////

  val graph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import akka.stream.scaladsl.GraphDSL.Implicits._

      val balance = builder.add(Balance[Int](2)) // fan-out operator
      val merge = builder.add(Merge[Int](2)) // fan-in operator

      // Feed Source into two Sinks at the same time
//      source ~> broadcast
//      broadcast.out(0) ~> flow1 ~> sink
//      broadcast.out(1) ~> flow2 ~> sink2
// same as:
//      source ~> broadcast ~> flow1 ~> sink
//                broadcast ~> flow2 ~> sink2
      // Two sources merge, then balance, then to two sinks
      source ~> merge ~> balance ~> sink
      source ~> merge
                         balance ~> sink2

      ClosedShape
    }
  )

  //////////////////////////

  graph.run(ActorMaterializer())
}
