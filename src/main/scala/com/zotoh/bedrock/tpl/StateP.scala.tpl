package ${PACKAGE_ID}

import com.zotoh.bedrock.device.Event
import com.zotoh.bedrock.core.{Job, JobData}
import com.zotoh.bedrock.wflow._


class ${CLASS_NAME}(j:Job) extends Workflow(j) {

  override def onStart = new PTask( w1 )

  private val w1= new Work() {
      def eval(job:Job) {
        val ev= job.event()
        // do your stuff here
        println("hello world")
      }
  }

}

