package lectures.part3

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.stream._
import akka.stream.impl.Compose
import akka.stream.scaladsl.{Broadcast, Concat, Flow, GraphDSL, Merge, Sink, Source, Zip}

object OpenGraphs extends App {
  import GraphDSL.Implicits._

  implicit val system = ActorSystem("OpenGraphSystem")

  val source1 = Source(1 to 10)
  val source2 = Source(11 to 20)

  val sink1 = Sink.foreach[Int](x => println(s"To sink 1 $x"))
  val sink2 = Sink.foreach[Int](x => println(s"To sink 2 $x"))

  val sink = Sink.foreach[Int](println)

  val flow1 = Flow[Int].map(_ + 1)
  val flow2 = Flow[Int].map(_ * 10)

  // 1. Source, that concatenates two Sources
  //  val sourceGraph = RunnableGraph.fromGraph( BECOME:
  val sourceGraph = Source.fromGraph(
    GraphDSL.create() { implicit builder =>
      val concat = builder.add(Concat[Int](2))
      source1 ~> concat
      source2 ~> concat
      //ClosedShape BECOME
      SourceShape(concat.out)
    })

  // sourceGraph.to(sink).run()

  // 2. Sink,  split into two sinks
  //  val sourceGraph = RunnableGraph.fromGraph( BECOME:
  val sinkGraph = Sink.fromGraph(
    GraphDSL.create() { implicit builder =>
      val broadcast = builder.add(Broadcast[Int](2))
      broadcast ~> sink1
      broadcast ~> sink2
      //ClosedShape BECOME
      SinkShape(broadcast.in)
    })

  // source1.to(sinkGraph).run()

  // 3. Flow, composed of two flows
  //  val sourceGraph = RunnableGraph.fromGraph( BECOME:
  val flowGraph = Flow.fromGraph(
    GraphDSL.create() { implicit builder =>
      // EVERYTHING HERE OPERATE ON SHAPES
      val flow1Shape = builder.add(flow1)
      val flow2Shape = builder.add(flow2)
      flow1Shape ~> flow2Shape
      //ClosedShape BECOME
      FlowShape(flow1Shape.in, flow2Shape.out)
    })

 // source1.via(flowGraph).to(sink).run()

  // FUNCTION WIth SOURCE AND STREAM AS PARAMETERS
  def flowFromSourceToSink[A,B](source: Source[A,_], sink: Sink[B,_]): Flow[B, A, _] = {
    Flow.fromGraph(
      GraphDSL.create() { implicit builder =>
        val flow1Shape = builder.add(source)
        val flow2Shape = builder.add(sink)
        FlowShape(flow2Shape.in, flow1Shape.out)
      })
  }

  flowFromSourceToSink(source1, sink1)
  // this is implementation of:
  Flow.fromSinkAndSource(sink, source1)
  // what to do if sink stopped, but source still goes on?
  Flow.fromSinkAndSourceCoupled(sink, source1)
}
