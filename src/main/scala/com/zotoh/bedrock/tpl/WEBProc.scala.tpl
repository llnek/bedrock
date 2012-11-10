package ${PACKAGE_ID}


import com.zotoh.bedrock.core.Job
import com.zotoh.bedrock.device.{ ServletEvent, ServletEventResult }
import com.zotoh.bedrock.wflow._


class WEBProcessor (j:Job) extends Workflow(j) {

  val task1= new Work() {
      override def eval(job : Job ) {
              val res= new ServletEventResult();
              val ev= job.event();
              res.setData("<html><body>Bonjour!</body></html>");
              ev.setResult(res) ;
      }
  }

  override def onStart : Activity = {
          new PTask(task1)
  }

}

