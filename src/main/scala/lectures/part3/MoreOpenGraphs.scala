package lectures.part3

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape, FanOutShape2, UniformFanInShape}
import akka.stream.javadsl.RunnableGraph
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Sink, Source, ZipWith}

import java.util.Date

object MoreOpenGraphs extends App {

  implicit val system = ActorSystem("MOG")

  // - 3x Int inputs
  // find max of 3

  val MaxThreeStaticGraph = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._
    val zip1 = builder.add(ZipWith[Int, Int, Int]((a,b) => Math.max(a,b)))
    val zip2 = builder.add(ZipWith[Int, Int, Int]((a,b) => Math.max(a,b)))

    zip1.out ~> zip2.in0

    UniformFanInShape(zip2.out, zip1.in0, zip1.in1, zip2.in1)
  }

  val source1 = Source(1 to 10)
  val source2 = Source(1 to 10).map(_ => 5)
  val source3 = Source((1 to 10).reverse)

  val sink = Sink.foreach[Int](println)

  val maxThreeGraph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._
      val maxThreeShape = builder.add(MaxThreeStaticGraph)
      source1 ~> maxThreeShape.in(0)
      source2 ~> maxThreeShape.in(1)
      source3 ~> maxThreeShape.in(2)
      maxThreeShape.out ~> sink
      ClosedShape
    }
  )

  // uncomment to run
  // maxThreeGraph.run(ActorMaterializer())

  // Fan-out
  // Different types
  //
  // Process bank transaction
  // - suspicious if more than 10k, return just ID
  // - others amount

  case class Transaction(
                        id: String,
                        source: String,
                        recipient: String,
                        amount: Int,
                        date: Date
                        )
  val trList = List(
    Transaction("654656", "Ray", "Jim", 100, new Date),
    Transaction("654656", "Bil", "Henry", 10000, new Date),
    Transaction("654656", "Gary", "Flin", 70000, new Date)
  )

  val bankProcessorSink = Sink.foreach[Transaction](println)
  val suspiciousTransactionsSink = Sink.foreach[String](id => println(s"Suspicos $id"))

  val transactionGraph = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._
    val broadcast = builder.add(Broadcast[Transaction](2))
    val filter    = builder.add(Flow[Transaction].filter(_.amount > 1000))
    val toId      = builder.add(Flow[Transaction].map(_.id))

    broadcast.out(0) ~> filter ~> toId

    new FanOutShape2(broadcast.in, broadcast.out(1), toId.out)
  }

  val transactionGraphRunnable = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val transactionGraphShape = builder.add(transactionGraph)

      Source(trList) ~> transactionGraphShape.in
      transactionGraphShape.out0 ~> bankProcessorSink
      transactionGraphShape.out1 ~> suspiciousTransactionsSink

      ClosedShape
    }
  )

  transactionGraphRunnable.run(ActorMaterializer())
}
