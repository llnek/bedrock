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

package demo.tcpip

import java.io.BufferedInputStream
import java.io.InputStream

import com.zotoh.fwk.util.ByteUte
import com.zotoh.bedrock.core.Job
import com.zotoh.bedrock.device.TCPEvent
import com.zotoh.bedrock.wflow._


/**
 * @author kenl
 *
 */
class DemoServer(job:Job) extends Workflow(job) {

  val task1= new Work() {
      override def eval(job:Job) {
          val ev= job.event().asInstanceOf[TCPEvent]
          def sockBin(ev:TCPEvent) {
            val bf= new BufferedInputStream( ev.sockIn())
            var buf= new Array[Byte](4);
            var clen=0
            bf.read(buf)
            clen=ByteUte.readAsInt(buf)
            buf= new Array[Byte](clen)
            bf.read(buf)
            println("TCP Server Received: " + new String(buf) )
          }
          sockBin(ev)
          // add a delay into the workflow before next step
          setResult( new Delay(1500))
      }
  }

  val task2= new Work() {
      override def eval(job:Job) {
          println("\nPRESS Ctrl-C anytime to end program.\n")
      }
  }

  override def onStart() = new PTask(task1).chain(new PTask(task2))

}

class DemoPreamble(j:Job) extends Workflow(j) {
  override def onStart() = new PTask( new Work() {
      override def eval(j:Job ) {
        println("Demo sending & receiving messages via tcpip..." )
      }
  })

}


