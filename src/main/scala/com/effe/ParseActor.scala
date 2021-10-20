package com.effe

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.effe.DownloadImg.DownloadBook
import org.jsoup.Jsoup
import org.openqa.selenium.firefox.{FirefoxDriver, FirefoxOptions}
import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.{By, Keys, PageLoadStrategy}

import scala.concurrent.ExecutionContextExecutor
import scala.jdk.CollectionConverters._

object ParseActor {
    sealed trait Message

    case class ParsePage(url: String) extends Message

    case class ParseTask(id: Int, url: String) extends Message

    case class ParseTaskReply(id: Int, res: Vector[String], title: String) extends Message
}

class ParseActor extends Actor with ActorLogging {

    import com.effe.ParseActor._

    implicit val ec: ExecutionContextExecutor = context.dispatcher

    override def receive: Receive = {
        case ParsePage(url) =>
            val senderRef = sender()
            // create a firefox driver with headless
            val driver: FirefoxDriver = initWebDriver()
            val res: Vector[String] = parseHtml(getSourceCode(driver, url))
            driver.quit()
            senderRef ! "page parse done"
            val childrenRef = for (i <- 1 to res.length) yield context.actorOf(Props[ParseBookActor], s"book_$i")
            context.become(withParseBook(childrenRef, 0, 1, Map()))
        case (title: String, links: Vector[String]) =>
            val downloadRef = context.actorOf(Props[DownloadImg], "downloader")
            downloadRef ! DownloadBook(title, links)
    }

    def withParseBook(childrenRef: Seq[ActorRef], bookIndex: Int, childIndex: Int, requestMap: Map[Int, ActorRef]): Receive = {
        case url: String =>
            log.info(s"start parsing book $bookIndex in parseActor$childIndex")
            val originalSender = sender()
            val task = ParseTask(bookIndex, url)
            val childRef = childrenRef(childIndex)
            childRef ! task
            val nextChildIndex = childIndex + 1
            val nextBook = bookIndex + 1
            val newRequest = requestMap + (bookIndex -> originalSender)
            context.become(withParseBook(childrenRef, nextBook, nextChildIndex, newRequest))
        case ParseTaskReply(id, links, title) =>
            log.info(s"I have received a reply for book $title with res $links")
            val originalSender = requestMap(id)
            originalSender ! (title, links)
            context.become(withParseBook(childrenRef, bookIndex, childIndex, requestMap - id))
    }

    class ParseBookActor extends Actor with ActorLogging {
        override def receive: Receive = {
            case ParseTask(id, url) =>
                log.info(s"I have received book link $id")
                val (title, links) = findAllImg(url)
                sender() ! ParseTaskReply(id, links, title)
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

    private def findAllImg(url: String): (String, Vector[String]) = {
        val driver: FirefoxDriver = initWebDriver()

        driver.get(url)
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
