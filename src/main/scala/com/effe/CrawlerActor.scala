package com.effe

import akka.actor.{Actor, ActorLogging, Props}

import scala.concurrent.ExecutionContextExecutor

object CrawlerActor {
    sealed trait Message

    case class CrawlPage(url: String) extends Message
}

class CrawlerActor extends Actor with ActorLogging {

    import com.effe.ParseActor._
    import CrawlerActor._

    implicit val ec: ExecutionContextExecutor = context.dispatcher

    override def receive: Receive = {
        case CrawlPage(url) =>
            val ParseRef = context.actorOf(Props[ParseActor], "parseActor")
            log.info(s"starting parse $url")
            ParseRef ! ParseMessage(url)
        case msg =>
            log.info(s"$msg get")
    }
}

