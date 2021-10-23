package exercises.part3

import akka.actor.ActorSystem
import akka.stream.{ClosedShape, UniformFanInShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, MergePreferred, RunnableGraph, Sink, Source, Zip}

object GraphCyclesFibonachi extends App {

  implicit val system = ActorSystem("FNAS")

  val graph = RunnableGraph.fromGraph(
    GraphDSL.create() {  implicit builder =>
      import GraphDSL.Implicits._

      val source = Source.single(1)
      val sourc2 = Source.single(1)

      val sink = Sink.foreach[Int](println)

      val mergeShape = builder.add(MergePreferred[(Int, Int)](1))
      val zipWithShape = builder.add(Zip[Int, Int])

      val flowShape = builder.add(Flow[(Int, Int)].map((a) => {
        Thread.sleep(100)
        (a._1 + a._2, a._1)
      }))

      val broadcast = builder.add(Broadcast[(Int, Int)](2))
      val extract =   builder.add(Flow[(Int, Int)].map(_._1))

      zipWithShape.out ~> mergeShape ~> flowShape ~> broadcast ~> extract
                          mergeShape.preferred    <~ broadcast

      val fanInShape = UniformFanInShape(extract.out, zipWithShape.in0, zipWithShape.in1)

      source ~> fanInShape.in(0)
      sourc2 ~> fanInShape.in(1)
      fanInShape.out ~> sink

      ClosedShape
    }
  )

  graph.run()
}
