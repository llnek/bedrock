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

package demo.file

import java.io.File
import com.zotoh.bedrock.device.FileEvent
import com.zotoh.fwk.io.IOUte._
import com.zotoh.bedrock.core.Job
import com.zotoh.bedrock.wflow._
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author kenl
 *
 */
object Demo {

  private val _count= new AtomicInteger()

  def upCount() {
  _count.incrementAndGet()
  }

  def count() = _count.get()

}

class Demo(job:Job) extends Workflow(job) {

  override def onStart() = PTask().withWork( new Work() {
      override def eval(job:Job) {
          val ev= job.event().asInstanceOf[FileEvent]
          val f0= ev.origFilePath()
          val f=ev.file()
          println("Found new file: " + f0)
          println("Content: " + readText(f, "utf-8"))

          Demo.upCount()

          if ( Demo.count() > 3) {
              println("\nPRESS Ctrl-C anytime to end program.\n")
          }
      }
  })


}


