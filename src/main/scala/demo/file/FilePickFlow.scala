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
import com.zotoh.fwk.io.IOUte
import com.zotoh.bedrock.core.Job
import com.zotoh.bedrock.wflow._

/**
 * @author kenl
 *
 */
object FilePickFlow {

  var _count=0

  def upCount() {
    _count=_count+1
  }

  def count() = {
    _count
  }

}

class FilePickFlow(job:Job) extends Workflow(job) {

  val task1= new Work() {
        def eval(job:Job) {
            val ev= job.event().get.asInstanceOf[FileEvent]
            val f0= ev.origFilePath()
            val f=ev.file()
            println("Found new file: " + f0)
            println("Content: " + IOUte.readText(f, "utf-8"))

            FilePickFlow.upCount()

            if ( FilePickFlow.count() > 3) {
                println("\nPRESS Ctrl-C anytime to end program.\n")
            }
        }
    }

    override def onStart() = {
        new PTask( task1)
    }

}


