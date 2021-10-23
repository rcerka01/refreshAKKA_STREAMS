package lectures.part5

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Keep, Sink, Source}

import scala.util.{Failure, Success}

object Substreams extends App {
  implicit val system = ActorSystem("SS")

  // # 1 GROUPING STREAM BY CERTAIN FUNCTION
  val wordSource = Source(List("ojpj", "ioji", "Jiij", "popijoi", "Hiu"))
  val groups = wordSource.groupBy(30, w => if (w.isEmpty) "\n" else w.toLowerCase().charAt(0)) // stram of streams
  groups.to(Sink.fold(0)((a,_) => { println(s"counted ${a+1}"); a + 1 })).run()
  // SUBSTREAM WILL HAVE DIFFERENT INSTANCE OF THAT CONSUMER (e.g. fold acc)
  //counted 1
  //counted 1
  //counted 1
  //counted 1
  //counted 1 :(

  // # 2 MERGE SUBSTREAMS BACK
  val textSource = Source(List(
    "I love Akka Streams",
    "this is amazing",
    "learning from Rock the JVM"
  ))

  val result = textSource
    .groupBy(2, s => s.length % 2)
    .map(_.length) // that will make substreams to do paralel computations
    .mergeSubstreamsWithParallelism(2) // then merge them back
    .toMat(Sink.reduce[Int](_ + _))(Keep.right)
    .run()

  implicit val ex = system.dispatcher

  result.onComplete {
    case Success(v) => println(s"Total is $v")
    case Failure(exception) =>
  }

  // # 3 SPLITTING STREAM INTO SUBSTREAMS

  val text =
    "I love Akka Streams\n" +
    "this is amazing\n" +
    "learning from Rock the JVM\n"

  val anotherCount = Source(text.toList) // to characters
    .splitWhen(c => c == "\n")
    .filter(_ != "\n")
    .map(_ => 1)
    .mergeSubstreams
    .toMat(Sink.reduce[Int](_ + _))(Keep.right)
    .run()

  anotherCount.onComplete {
    case Success(v) => println(s"Total for another count is $v")
    case Failure(exception) =>
  }

  // # 4 FLATTENING
  val source = Source(1 to 5).flatMapConcat( x => Source( x to 3 * x)) .runWith(Sink.foreach(println))
  // with merge
  val sourceMerge = Source(1 to 5).flatMapMerge(2,  x => Source( x to 3 * x)) .runWith(Sink.foreach(println))





}
