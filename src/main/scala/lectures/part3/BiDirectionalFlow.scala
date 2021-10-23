package lectures.part3

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, BidiShape, ClosedShape}
import akka.stream.scaladsl.RunnableGraph
import akka.stream.scaladsl.{Flow, GraphDSL, Sink, Source}

object BiDirectionalFlow extends App {
  implicit val system = ActorSystem("BiStr")

  //  seasor sifer
  def encrypt(n: Int)(txt: String) = txt map (c => ( c + n).toChar)
  def decrypt(n: Int)(txt: String) = txt map (c => ( c - n).toChar)

  println(encrypt(3)("hallo"))
  println(decrypt(3)("kdoor"))

  val biDiCryptoGraph = GraphDSL.create() { implicit builder =>

    val encriptionFlow = builder.add(Flow[String].map(encrypt(3)))
    val decriptionFlow = builder.add(Flow[String].map(decrypt(3)))


    //BidiShape(encriptionFlow.in, encriptionFlow.out, decriptionFlow.in, decriptionFlow.out)
    // or
    BidiShape.fromFlows(encriptionFlow, decriptionFlow)
  }

  val l = List("some", "for", "you", "we", "had", "godd", "time")
  val source = Source(l)
  val encryptedSource = source.map(encrypt(3))

  val graph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val unencryptedShape = builder.add(source)
      val encryptedShape = builder.add(encryptedSource)
      val bidi = builder.add(biDiCryptoGraph)
      val encrSink = Sink.foreach[String](x => println(s"Encrypted $x"))
      val decrSink = Sink.foreach[String](x => println(s"Decripted $x"))

      unencryptedShape ~> bidi.in1  ; bidi.out1 ~> encrSink
      decrSink <~         bidi.out2 ; bidi.in2 <~  encryptedShape

      ClosedShape
    }
  )

  graph.run()
}
