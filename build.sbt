name := "refreshAKKA-STREAMS"

version := "0.1"

scalaVersion := "2.13.6"

val akkaVersion = "2.6.16"
val scalaTestVersion = "3.2.10"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "org.scalatest" %% "scalatest" % scalaTestVersion
)
