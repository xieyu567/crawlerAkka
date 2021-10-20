import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import com.effe.CrawlerActor
import com.effe.CrawlerActor.CrawlPage
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

class LinkSpec extends TestKit(ActorSystem("LinkSpec"))
    with ImplicitSender with AnyWordSpecLike with BeforeAndAfterAll {

    override def afterAll(): Unit = {
        TestKit.shutdownActorSystem(system)
    }

    "parse test" should {
        "send back a message" in {
            within(11 second) {
                val crawlerActor = system.actorOf(Props[CrawlerActor])
                crawlerActor ! CrawlPage("https://copymanga.com/comic/wangxianglaoshi")
                expectMsgType[Vector[String]]
            }
        }
    }
}
