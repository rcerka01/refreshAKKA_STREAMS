package lectures.part2

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}

object Materializing extends App {

  implicit val system  = ActorSystem("Mater")
  implicit val forFutures = system.dispatcher // can use this instead of usual import for Futures

  // MATERIALIZATION is value of a graph after run()

  val source = Source(1 to 10)
  val flow = Flow[Int].map(_ + 1)
  val printSink = Sink.foreach[Int](println)
  val reduceSink = Sink.reduce[Int]((a,b) => a + b) // reduceSink is Sink[Int, Future[Int]]. Future[Int] is materialization value

  //val x: NotUsed = source.to(printSink).run() // moment of MATERIALIZATION. NotUsed same as Unit

  // (!!!!!)
//  val matr = source.to(reduceSink).run() // NotUsed
//  val matr2 = source.runWith(reduceSink) // Future[Int]
//  matr2.map(println)
//
//  // viaMat
//  val matResult = source.viaMat(flow)((a,b) => b) // same:
//  val r = source.viaMat(flow)(Keep.right).toMat(printSink)(Keep.right)  // Future[Done]  // Keep.Left - Source,  Keep.right - Sink
//
//  // sugar
//  val sumSugar = source.runWith(reduceSink) // is equal to:
//  val sum = source.toMat(reduceSink)(Keep.right)
//
//  // backwords
//  printSink.runWith(source) // same thing, but it will use Keep.left for materialized value
//
//  // both ways (sugar)
//  flow.runWith(source, printSink)
//

  // last element of source
  val lastEl = Source(1 to 10).toMat(Sink.last[Int])(Keep.right).run()
  lastEl map println
  val lastEl2 = Source(1 to 10).runWith((Sink.last[Int]))
  lastEl2 map println

  // word counter
  val s ="wef wef wef efw efw ert"
  val stringSource = Source(s.split(" "))
  val stringFlowReduce = Flow[String].reduce((a,b) => a + b) // can't do with reduce
  val stringFlowFold = Flow[String].fold[Int](0) ((a,b) => a + 1)
  val stringPrintSink = Sink.foreach[Int](println)
               // NOT viaMat(!!)
  val c = stringSource.via(stringFlowFold).toMat(Sink.head)(Keep.right).run()
  c map println
  val c2 = stringSource.via(stringFlowFold).runWith(Sink.head)
  c2 map println
                         // viaMat(!!!)                   // toMat(!)
  val c3 = stringSource.viaMat(stringFlowFold)(Keep.left).toMat(Sink.head)(Keep.right).run() // Interesting
  c3 map println

  val c4 = stringFlowFold.runWith(stringSource, Sink.head)._2
  c4 map println
}
