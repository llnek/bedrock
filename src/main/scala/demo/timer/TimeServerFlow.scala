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

package demo.timer

import java.util.{Date=>JDate}

import com.zotoh.bedrock.device.TimerEvent
import com.zotoh.bedrock.core.Job
import com.zotoh.bedrock.wflow._


/**
 * @author kenl
 *
 */
object TimeServerFlow {
  private var _count=0
  def upCount() { _count = _count+1  }
  def count() = { _count }
}

class TimeServerFlow(job:Job) extends Workflow(job) {

    val task1= new Work() {
        def eval(job:Job) {
            val ev= job.event().get.asInstanceOf[TimerEvent]
            if ( ev.isRepeating()) {
                if (TimeServerFlow.count() < 5) {
                    println("-----> repeating-update: " + new JDate())
                    TimeServerFlow.upCount()
                }
                if (TimeServerFlow.count() >= 5) {
                        println("\nPRESS Ctrl-C anytime to end program.\n")
                }
            } else {
                println("-----> once-only!!: " + new JDate())
            }
        }
    }

    override def onStart() = {
        new PTask(task1)
    }

}

class TimeServerFlowPreamble(j:Job) extends Workflow(j) {
    override def onStart() = {
        new PTask( new Work() {
            def eval(j:Job) {
                    println("Demo various timer functions..." )
            }
        })
    }
}



