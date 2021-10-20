package com.effe

import akka.actor.{Actor, ActorLogging}
import org.jsoup.Jsoup
import org.openqa.selenium.PageLoadStrategy
import org.openqa.selenium.firefox.{FirefoxDriver, FirefoxOptions}

import scala.concurrent.ExecutionContextExecutor
import scala.jdk.CollectionConverters._

object ParseActor {
    sealed trait Message

    case class ParseMessage(url: String) extends Message
}

class ParseActor extends Actor with ActorLogging {

    import com.effe.ParseActor.ParseMessage

    implicit val ec: ExecutionContextExecutor = context.dispatcher

    override def receive: Receive = {
        case ParseMessage(url) =>
            val senderRef = sender()
            // create a firefox driver with headless
            val driver: FirefoxDriver = {
                val firefoxOptions: FirefoxOptions = new FirefoxOptions
                firefoxOptions.setPageLoadStrategy(PageLoadStrategy.NORMAL).setHeadless(true)
                System.setProperty("webdriver.gecko.driver", "/home/effe/geckodriver")
                new FirefoxDriver(firefoxOptions)
            }
            val res: Vector[String] = parseHtml(getSourceCode(driver, url))
            driver.quit()
            senderRef ! "page parse done"
    }

    /*
    get page source code using driver
     */
    private def getSourceCode(driver: FirefoxDriver, url: String): String = {
        driver.get(url)
        Option(driver.getPageSource).getOrElse("")
    }

    /*
    get the manga book link list using Jsoup
     */
    private def parseHtml(sourceCode: String): Vector[String] = {
        val parseDoc = Jsoup.parse(sourceCode)
        val links = parseDoc.select("div.tab-content #default全部 ul:first-child a").asScala
        val allLinks = for (link <- links) yield "https://copymanga.com" + link.attr("href")
        allLinks.toVector
    }
}
