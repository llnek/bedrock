/*??
 * COPYRIGHT (C) 2012 CHERIMOIA LLC. ALL RIGHTS RESERVED.
 *
 * THIS IS FREE SOFTWARE; YOU CAN REDISTRIBUTE IT AND/OR
 * MODIFY IT UNDER THE TERMS OF THE APACHE LICENSE,
 * VERSION 2.0 (THE "LICENSE").
 *
 * THIS LIBRARY IS DISTRIBUTED IN THE HOPE THAT IT WILL BE USEFUL,
 * BUT WITHOUT ANY WARRANTY; WITHOUT EVEN THE IMPLIED WARRANTY OF
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.
 *
 * SEE THE LICENSE FOR THE SPECIFIC LANGUAGE GOVERNING PERMISSIONS
 * AND LIMITATIONS UNDER THE LICENSE.
 *
 * You should have received a copy of the Apache License
 * along with this distribution; if not, you may obtain a copy of the
 * License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 ??*/


package demo.rss

import scala.collection.JavaConversions._

import com.sun.syndication.feed.synd.{SyndFeed,SyndEntry}
import com.sun.syndication.feed.module.Module
import com.zotoh.bedrock.device.FeedEvent
import com.zotoh.bedrock.wflow._
import com.zotoh.bedrock.core.Job



/**
 * @author kenl
 *
 */
class FeedReaderFlow(job:Job) extends Workflow(job) {

    val task1=new Work() {
        def eval(job:Job) {

            val ev = job.event().get.asInstanceOf[FeedEvent];
            val feed= ev.feedData()

            println("===> Title: " + feed.getTitle())
            println("===> Author: " + feed.getAuthor())
            println("===> Description: " + feed.getDescription())
            println("===> Pub date: " + feed.getPublishedDate())
            println("===> Copyright: " + feed.getCopyright())
            println("===> Modules used:")

            //val it : Iterator[_] = feed.getModules().iterator().asScala
            feed.getModules().iterator().foreach { a : Any =>
              println( "\t" + a.asInstanceOf[Module].getUri() )
            }

            println("===> Titles of the " + feed.getEntries().size() + " entries:")

            feed.getEntries().iterator().foreach { a : Any =>
              println("\t" + a.asInstanceOf[SyndEntry].getTitle())
            }

            if (feed.getImage() != null) {
                println("===> Feed image URL: " + feed.getImage().getUrl())
            }

            println("\nPRESS Ctrl-C anytime to end program.\n")

        }
    }


    override def onStart() = {
        new PTask(task1)
    }


}


class FeedReaderFlowPreamble(j:Job) extends Workflow(j) {
    override def onStart() = {
        new PTask( new Work() {
            def eval(j:Job) {
              println("Preparing to pull down RSS feeds...")
            }
        })
    }
}

