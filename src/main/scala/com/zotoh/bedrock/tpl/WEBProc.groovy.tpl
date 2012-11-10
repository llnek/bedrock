package ${PACKAGE_ID};


import com.zotoh.bedrock.core.Job;
import com.zotoh.bedrock.device.ServletEvent;
import com.zotoh.bedrock.device.ServletEventResult;
import com.zotoh.bedrock.wflow.*;


class WEBProcessor extends Workflow {

  private def task1= new Work() {
      public void eval(Job job ) throws Exception {
          def res= new ServletEventResult();
          def ev= job.event().get();
          res.setData("<html><body>Bonjour!</body></html>");
          ev.setResult(res) ;
      }
  };

  def Activity onStart() {

      return new PTask( task1);
  }



  def WEBProcessor(Job j) {
      super(j);
  }


}

