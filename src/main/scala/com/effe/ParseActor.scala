package com.effe

import akka.actor.{Actor, ActorLogging, Props}
import com.effe.DownloadImg.DownloadBook
import org.jsoup.Jsoup
import org.openqa.selenium.firefox.{FirefoxDriver, FirefoxOptions}
import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.{By, Keys, PageLoadStrategy}

import scala.concurrent.ExecutionContextExecutor
import scala.jdk.CollectionConverters._

object ParseActor {
    sealed trait Message

    case class ParseHomePage(url: String) extends Message

    case class ParseImgTask(url: String) extends Message

    case class ParseTaskReply(title: String, res: Vector[String]) extends Message
}

class ParseActor extends Actor with ActorLogging {

    import com.effe.ParseActor._

    implicit val ec: ExecutionContextExecutor = context.dispatcher

    override def receive: Receive = {
        case ParseHomePage(url) =>
            // create a firefox driver with headless
            val driver: FirefoxDriver = initWebDriver()
            val res: Vector[String] = parseHtml(getSourceCode(driver, url))
            driver.quit()
            log.info("page parse done")
            val childrenRef = context.actorOf(Props[ParseBookActor])
            res.foreach(url => childrenRef ! url)
        case ParseTaskReply(title, links) =>
            val downloadRef = context.actorOf(Props[DownloadImg], "downloader")
            downloadRef ! DownloadBook(title, links)
    }

    class ParseBookActor extends Actor with ActorLogging {
        override def receive: Receive = {
            case ParseImgTask(url) =>
                log.info(s"I have received book link $url")
                val (title, links) = findAllImg(url)
                sender() ! ParseTaskReply(title, links)
        }
    }

    private def initWebDriver() = {
        val firefoxOptions: FirefoxOptions = new FirefoxOptions
        firefoxOptions.setPageLoadStrategy(PageLoadStrategy.NORMAL).setHeadless(true)
        System.setProperty("webdriver.gecko.driver", "src/main/resources/geckodriver")
        new FirefoxDriver(firefoxOptions)
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

    private def findAllImg(bookUrl: String): (String, Vector[String]) = {
        val driver: FirefoxDriver = initWebDriver()

        driver.get(bookUrl)
        val totalImgCount = driver.findElement(By.cssSelector("span.comicCount")).getText.toInt

        /*
        repeat sending keys to refresh page, until refresh all images
         */
        var linksCount = 0
        while (linksCount < totalImgCount) {
            val action = new Actions(driver)
            val keyDown = action.sendKeys(Keys.END).build()
            val keyUp = action.sendKeys(Keys.HOME).build()
            keyUp.perform()
            Thread.sleep(1000)
            keyDown.perform()
            Thread.sleep(1000)
            linksCount = driver.findElements(By.tagName("li")).asScala.length
        }

        val sourceCode = Option(driver.getPageSource).getOrElse("")
        val parseDoc = Jsoup.parse(sourceCode)
        val linkTags = parseDoc.select("li img[data-src]").asScala.toVector
        val title = parseDoc.select("h4.header").text()
        // extract images links
        val imgLinks = linkTags.map(i => i.toString.split(" ")(1).drop(10).dropRight(1))
        driver.quit()
        (title, imgLinks)
    }
}
