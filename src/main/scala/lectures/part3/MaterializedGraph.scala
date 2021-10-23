package lectures.part3

import akka.actor.ActorSystem
import akka.stream.{FlowShape, SinkShape, scaladsl}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Keep, Sink, Source}

import scala.concurrent.Future

object MaterializedGraph extends App {

  implicit val system = ActorSystem("MG")

  val wordSource = Source(List("Sws", "pjplop", "Kookkoko", "klml"))
  val printerSink =  Sink.foreach[String](println)
  val counterSink = Sink.fold[Int, String](0)((a, _) => a + 1)

  // - print out all lower cases
  // - cout shorter than 5

  // initially Sink[String, notUsed]
  // now it is Sink[String, Future[Int]]
  val complexSink =  Sink.fromGraph(
    //  GraphDSL.create() { implicit builder =>  // CHANGE 1
    GraphDSL.create(printerSink, counterSink)((printerMat, counterMat) => counterMat) { implicit builder => ( printerShape, counterShape) =>
      import GraphDSL.Implicits._

      val broadcast = builder.add(Broadcast[String](2))
      val filterUp = builder.add(Flow[String].filter(x => x == x.toLowerCase))
      val filterLess = builder.add(Flow[String].filter(_.length < 5))

//      CHANHE 2
//      broadcast ~> filterUp ~> printerSink
//      broadcast ~> filterLess ~> counterSink
      broadcast ~> filterUp ~> printerShape
      broadcast ~> filterLess ~> counterShape

      SinkShape(broadcast.in)
    }
  )

  import system.dispatcher

  //val future: Future[Int] = wordSource.toMat(complexSink)(Keep.right).run()
  //future map println

  import GraphDSL.Implicits._

  // Enhance flow to return materialised value
  def enhancedFlow[A, B](flow: Flow[A, B, _]): Flow[A, B, Future[Int]] = {
    val sink = Sink.fold[Int, B](0)((a, _) => a + 1)
    Flow.fromGraph({
      GraphDSL.create(sink) { implicit builder => sinkShape =>
      import GraphDSL.Implicits._

        val broadcast = builder.add(Broadcast[B](2))
        val originFlShape = builder.add(flow)

        originFlShape ~> broadcast ~> sinkShape

        FlowShape(originFlShape.in, broadcast.out(1))
      }
    })
  }

  val source = Source(1 to 10)
  val flow = Flow[Int].map(x => x)
  val sink = Sink.ignore

  val result = source.viaMat(enhancedFlow(flow))(Keep.right).toMat(sink)(Keep.left).run()
  result map println



}
