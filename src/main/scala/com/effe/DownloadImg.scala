package com.effe

import akka.actor.{Actor, ActorLogging}
import org.apache.commons.io.FileUtils

import java.io.File
import java.net.URL

object DownloadImg {
    sealed trait Message

    case class DownloadBook(title: String, links: Vector[String]) extends Message
}

class DownloadImg extends Actor with ActorLogging {

    import com.effe.DownloadImg._

    override def receive: Receive = {
        case DownloadBook(title, links) =>
            links.zipWithIndex.foreach { i =>
                val targetFile = new File(s"downloaded/$title/${i._2}.jpg")
                FileUtils.copyURLToFile(new URL(i._1), targetFile)
            }
            log.info(s"downloaded book $title")
    }
}
