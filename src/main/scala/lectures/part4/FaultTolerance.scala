package lectures.part4

import akka.actor.ActorSystem
import akka.stream.ActorAttributes
import akka.stream.Supervision.{Resume, Stop}
import akka.stream.javadsl.RestartSource
import akka.stream.scaladsl.{Sink, Source}

import scala.util.Random

object  FaultTolerance extends App {

  implicit val system = ActorSystem("FT")

  // # 1. logging
  // each element is logged only on level DBUG
  val faultySource = Source(1 to 10).map(x => if (x == 6) throw new RuntimeException("It is 6") else x)
  faultySource.log("Tracking elements").to(Sink.ignore)
    //.run()

  // # 2. GRACEFULLY TERMINATE STREAM
  faultySource.recover {
    case _ : RuntimeException => Int.MinValue
  }
    .log("GracefulSource")
    .to(Sink.ignore)
   // .run()

  // # 3. RECOWER WITH ANOTHER STREAM
  faultySource.recoverWithRetries(3, {
    case _: RuntimeException => Source(19 to 99)
  })
    .log("Recover with retry")
    .to(Sink.ignore)
    //.run()

//  // # 4 BACKOFF SUPERVISION
//  // when failed, try to retry with randomnes in time
//  import scala.concurrent.duration._
//  val restartSource = RestartSource.onFailuresWithBackoff(
//    minBackoff = 1 second,
//    maxBackoff = 30 seconds,
//    randomFactor = 0.2,
//  )(() => {
//    val randomNumber = new Random().nextInt(20)
//    Source(1 to 10).map(elem => if (elem == randomNumber) throw new RuntimeException else elem)
//  })
//
//  restartSource
//    .log("restartBackoff")
//    .to(Sink.ignore)
//  // .run()

  // # 5 SUPERVISION STRATEGY
  val numbers = Source(1 to 20) map ( e => if (e == 13)  throw new RuntimeException("Bad luck") else e )
  (numbers withAttributes ActorAttributes.supervisionStrategy {
    // Resume - skip faulty element
    // Stop - stop the stream
    // Restart - Resume + clear state of components (like folds, etc acumulated state will be gone)
    case _ : RuntimeException => Resume
    case _ => Stop
  })
    .log("With superviser")
    .to(Sink.ignore)
    .run()
}
