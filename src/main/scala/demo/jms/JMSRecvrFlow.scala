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

package demo.jms

import javax.jms.{TextMessage,Message}

import com.zotoh.bedrock.device.JmsEvent
import com.zotoh.bedrock.core.Job
import com.zotoh.bedrock.wflow._


/**
 * @author kenl
 *
 */
object JMSRecvrFlow {
  private var _count=0
  def upCount() {
    _count=_count+1
  }
  def count() = { _count }

}

class JMSRecvrFlow(job:Job) extends Workflow(job) {

  val task1= new Work() {
      override def eval(job:Job) {
            val ev= job.event().get.asInstanceOf[JmsEvent]
            val msg= ev.msg()

            println("-> Correlation ID= " + msg.getJMSCorrelationID())
            println("-> Msg ID= " + msg.getJMSMessageID())
            println("-> Type= " + msg.getJMSType())

            if (msg.isInstanceOf[TextMessage]) {
                val t= msg.asInstanceOf[TextMessage]
                println("-> Text Message= " + t.getText())
            }

            JMSRecvrFlow.upCount()

            if (JMSRecvrFlow.count() > 3) {
                    println("\nPRESS Ctrl-C anytime to end program.\n")
            }

      }
  }

    override def onStart() = {
        new PTask(task1)
    }















}

class JMSRecvrFlowPreamble(j:Job) extends Workflow(j) {
    override def onStart() = {
        new PTask( new Work() {
            def eval(j:Job) {
                    println("Demo receiving JMS messages..." )
            }
        })
    }
}


