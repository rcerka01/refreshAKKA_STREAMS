package lectures.part4

import akka.actor.ActorSystem
import akka.pattern.pipe
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.testkit.scaladsl.{TestSink, TestSource}
import akka.testkit.{TestKit, TestProbe}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}

class TestingStreamsSpec extends TestKit(ActorSystem("TestingStreams"))
  with AnyWordSpecLike
  with BeforeAndAfterAll {

  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  "A ssimple stream" should {
    "statisfy basic asertions" in {
      val source = Source(1 to 10)
      val sink = Sink.fold(0)((a: Int, b: Int) => a + b)
      val sumFuture = source.toMat(sink)(Keep.right).run()

      val some = Await.result(sumFuture, Duration.Inf)
      assert(some == 55)
    }

    "integrate with test actors with materialised value" in {
      val source = Source(1 to 10)
      val sink = Sink.fold(0)((a: Int, b: Int) => a + b)

      val probe = TestProbe()
      source.toMat(sink)(Keep.right).run().pipeTo(probe.ref)

      probe.expectMsg(55)
    }

    "integrate with test actors based sink" in {
      val source = Source(1 to 10)
      val flow = Flow[Int].scan[Int](0)(_ + _)
      val streamToTest = source.via(flow)

      val probe = TestProbe()
      val sink = Sink.actorRef(probe.ref, "some")

      streamToTest.to(sink).run()
      probe.expectMsgAllOf(0, 1, 3 , 6 ,10 ,15)
    }

    "Integrate with Stream test kit" in {
      val source = Source(1 to 5) map (_ * 2)
      val sink = TestSink.probe[Int]
      val value = source.runWith(sink)

      value
        .request(5)
        .expectNext(2, 4, 6, 8, 10)
        .expectComplete()
    }

    "integrate with streams TestKit Source" in {
      val sink = Sink.foreach[Int] {
        case 13 => throw new RuntimeException
        case _ =>
      }
      val source = TestSource.probe[Int]
      val matr = source.toMat(sink)(Keep.both).run()
      val publisher = matr._1
      val resultFut = matr._2

      publisher
        .sendNext(1)
        .sendNext(5)
        .sendNext(13)
        .sendComplete()

      resultFut.onComplete {
        case Success(_) => fail("Sink has throw error on 13")
        case Failure(_) =>
      }
    }

    "test flows with a test source and test sink" in {
      val flow = Flow[Int] map (_ * 2)
      val source = TestSource.probe[Int]
      val sink = TestSink.probe[Int]

      val matr = source.via(flow).toMat(sink)(Keep.both).run()
      val (publisher, subscriber) = matr

      publisher
        .sendNext(1)
        .sendNext(5)
        .sendNext(42)
        .sendNext(99)
        .sendComplete()

      subscriber
        .request(4)
        .expectNext(2, 10, 84, 198)
        .expectComplete()
    }
  }

}
