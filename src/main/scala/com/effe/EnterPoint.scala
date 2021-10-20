package com.effe

import akka.actor.{ActorSystem, Props}
import com.effe.CrawlerActor.CrawlPage

object EnterPoint extends App {
    val system = ActorSystem.create("system")
    val crawl = system.actorOf(Props[CrawlerActor], "crawl")
    crawl ! CrawlPage("https://copymanga.com/comic/wangxianglaoshi")
}
