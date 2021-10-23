package lectures.part3

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape}
import akka.stream.javadsl.RunnableGraph
import akka.stream.scaladsl.GraphDSL.Implicits.{SourceArrow, port2flow}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Sink, Source, Zip}

object Graphs extends App {


  implicit val system =  ActorSystem("GraphsSystem")

  val source = Source(1 to 100)
  val flow1 = Flow[Int].map(_ + 1)
  val flow2 = Flow[Int].map(_ * 10)
  val sink = Sink.foreach[(Int, Int)](println)


  //////////////////////////

  val graph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>

      val broadcast = builder.add(Broadcast[Int](2)) // fan-out operator
      val zip = builder.add(Zip[Int, Int]) // fan-in operator

      source ~> broadcast

      broadcast.out(0) ~> flow1 ~> zip.in0
      broadcast.out(1) ~> flow2 ~> zip.in1

      zip.out ~> sink

      ClosedShape

    }
  )

  //////////////////////////

  graph.run(ActorMaterializer())
}
