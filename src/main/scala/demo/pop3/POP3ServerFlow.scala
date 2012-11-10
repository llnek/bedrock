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

package demo.pop3

import com.zotoh.bedrock.device.POP3Event
import com.zotoh.bedrock.core.Job
import com.zotoh.fwk.util.CoreUte._
import com.zotoh.bedrock.wflow._


/**
 * @author kenl
 *
 */
object POP3ServerFlow {
  private var _count=0
  def upCount() { _count=_count+1 }
  def count() = { _count }
}
class POP3ServerFlow(job:Job) extends Workflow(job) {

  val task1= new Work() {
      def eval(job:Job) {
            val e= job.event().get.asInstanceOf[POP3Event]
            val bits=e.msg().bytes()
            println("########################")
            print(e.subject() + "\r\n")
            print(e.from() + "\r\n")
            print(e.receiver() + "\r\n")
            print("\r\n")
            println( asString(bits))

            POP3ServerFlow.upCount()

            if (POP3ServerFlow.count() > 3) {
                    println("\nPRESS Ctrl-C anytime to end program.\n")
            }
      }
  }

    override def onStart() = {
        new PTask(task1)
    }















}

class POP3ServerFlowPreamble(j:Job) extends Workflow(j) {
    override def onStart() = {
        new PTask( new Work() {
            def eval(j:Job) {
                    println("Demo receiving POP3 emails..." )
            }
        })
    }
}


