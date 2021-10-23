package lectures.part3

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape, OverflowStrategy}
import akka.stream.scaladsl.{Flow, GraphDSL, Merge, MergePreferred, RunnableGraph, Source}

object Cycles extends App {

  // RISK OF DEADLOCKING

  implicit val system = ActorSystem("GC")

  val acceleratorGraph = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    val sourceShap = builder.add(Source(1 to 10))
    // val mergeShap = builder.add(Merge[Int](2))
    // change to, to avoid lock:
    val mergeShap = builder.add(MergePreferred[Int](1))
    val flowShape = builder.add(Flow[Int].map ( x => {
      println(s"Accelerating: $x")
      x + 1
    }))

    // cycle deadlock
    sourceShap ~> mergeShap ~> flowShape
    // mergeShap <~ flowShape
    // and to:
    mergeShap.preferred <~ flowShape
    // it prioritise accelerator

    ClosedShape
  }

  // RunnableGraph.fromGraph(acceleratorGraph).run()

  // Buffered solution to deadlock:
  val bufferedAcceleratorGraph = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    val sourceShap = builder.add(Source(1 to 100))
    val mergeShap = builder.add(Merge[Int](2))
    val flowShape = builder.add(Flow[Int].buffer(10, OverflowStrategy.dropHead).map (x => {
      println(s"Accelerating: $x")
      Thread.sleep(100)
      x
    }))

    // cycle deadlock
    sourceShap ~> mergeShap ~> flowShape
    mergeShap <~ flowShape

    ClosedShape
  }

  RunnableGraph.fromGraph(bufferedAcceleratorGraph).run()

}
