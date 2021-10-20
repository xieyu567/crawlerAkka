name := "crawlerAkka"

version := "0.1"

scalaVersion := "2.13.6"
val akkaVersion = "2.6.17"
val akkaHttpVersion = "10.2.6"

libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,

    "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
    "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
    "org.scalatest" %% "scalatest" % "3.2.9" % Test,

    "org.seleniumhq.selenium" % "selenium-firefox-driver" % "4.0.0",
    "org.seleniumhq.selenium" % "selenium-java" % "4.0.0",
    "org.jsoup" % "jsoup" % "1.14.3",
    "commons-io" % "commons-io" % "2.11.0",
)