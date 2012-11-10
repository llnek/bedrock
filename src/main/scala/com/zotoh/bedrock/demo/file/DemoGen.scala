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

import java.util.{Date=>JDate}
import java.io.File

import com.zotoh.fwk.util.DateUte.fmtDate
import com.zotoh.fwk.io.IOUte.writeFile
import com.zotoh.fwk.util.CoreUte._

import com.zotoh.bedrock.device.FilePicker
import com.zotoh.bedrock.core.Job
import com.zotoh.bedrock.wflow._

/**
 * @author kenl
 * Create a new file every n secs
 *
 */
class DemoGen(job:Job) extends Workflow(job) {

  override def onStart() = PTask().withWork( new Work() {
      override def eval(job:Job) {
          val s= "Current time is " + fmtDate(new JDate())
          job.engine().deviceMgr().device("picker") match {
            case p:FilePicker =>
              writeFile( new File(p.srcDir().get, uid()+".txt"), s, "utf-8")
            case _ =>
          }
      }
  })

}

class DemoGenPreamble(j:Job) extends Workflow(j) {
  override def onStart() = PTask( new Work() {
    override def eval(j:Job ) {
      println("Demo file directory monitoring - picking up new files")
    }
  })

}


