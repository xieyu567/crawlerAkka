package com.effe

import javax.imageio.ImageIO
import java.io.File
import java.net.URL
import org.apache.commons.io.FileUtils

import scala.io.Source

object Test extends App {
    val url = "https://mirror277.mangafuna.xyz:12001/comic/wangxianglaoshi/e78a2/8e310920-1b64-11eb-a727-00163e0ca5bd.jpg!kb_w_read_large"
    val targetFile = new File("/home/effe/1.jpg")
    FileUtils.copyURLToFile(new URL(url), targetFile)
//    println(Source.fromURL(url))
}
