package lectures.part2

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object Intro extends App {

  implicit val system: ActorSystem = ActorSystem("IntroStream")

  val source = Source(1 to 10)
  val flow = Flow[Int] map (x => x + 1)
  val sink = Sink.foreach[Int](println)

  val graph = source.to(sink)

  source
    .via(flow)
    .to(sink)
    .run()

  // NULLs ARE NOT ALLOWED (!!!) Exception thrown
  //Source.single[String](null) // illegal
  // so use Options

  // SOURCES
  Source.single(1)
  Source(List(1,2,3))
  Source.empty[Int]
  Source(Stream.from(1)) // INFINITE. Not Akka Stream WITH Collection
  Source.fromFuture(Future(1))

  // SINKS
  Sink.ignore
  Sink.foreach(println)
  Sink.head[Int]
  Sink.fold[Int, Int](0)((a,b) => a + b)

  // FLOWS
  Flow[Int].map(_ + 1)
  Flow[Int].take(2)
  Flow[Int].filter(_ > 3)
  // there is no .flatMap

  // SUGARs
  Source(1 to 10).via(Flow[Int].map(x => x + 2)).run()
  // same: Source(1 to 10).map(x => x +2)
  Source(1 to 10).to(Sink.foreach[Int](println)).run()
  // same : Source(1 to 10).runForeach(println)



  // ex
  case class Person(name: String)
  val george = Person("George")
  val ray = Person("Ray")
  val beatrise = Person("Beatrise")
  val bill = Person("Bill")
  val josephine = Person("Josephine")

  Source(List(george, ray, beatrise, bill, josephine))
    .via(Flow[Person].filter(_.name.length > 5).take(2))
    .to(Sink.fold[List[Person], Person](Nil)((a,b) => a :+ b))
    .run() // TODO how to get result???
    //.to(Sink.foreach[Person](x => println(x.name)))

  // with sugar
  Source(List(george, ray, beatrise, bill, josephine)).filter(_.name.length > 5).take(2).runForeach(println)

}
