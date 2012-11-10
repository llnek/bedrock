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
import com.zotoh.bedrock.core.Job
import com.zotoh.bedrock.device.JmsEvent
import com.zotoh.bedrock.wflow._
import java.util.concurrent.atomic.AtomicInteger


/**
 * @author kenl
 *
 */
object Demo {
  private var _count= new AtomicInteger()
  def upCount() { _count.incrementAndGet() }
  def count() = _count.get()
}

class Demo(job:Job) extends Workflow(job) {

  override def onStart() = PTask().withWork( new Work() {
    override def eval(job:Job) {

      val ev= job.event().asInstanceOf[JmsEvent]
      val msg= ev.msg()

      println("-> Correlation ID= " + msg.getJMSCorrelationID())
      println("-> Msg ID= " + msg.getJMSMessageID())
      println("-> Type= " + msg.getJMSType())

      msg match {
        case t:TextMessage =>
          println("-> Text Message= " + t.getText())
      }

      Demo.upCount()

      if (Demo.count() > 3) {
              println("\nPRESS Ctrl-C anytime to end program.\n")
      }

    }} )

}

class DemoPreamble(j:Job) extends Workflow(j) {
  override def onStart() = PTask().withWork( new Work() {
      override def eval(j:Job ) {
          println("Demo receiving JMS messages..." )
      }
  })
}


