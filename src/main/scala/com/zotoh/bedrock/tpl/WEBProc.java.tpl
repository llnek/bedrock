package ${PACKAGE_ID};


import com.zotoh.bedrock.core.Job;
import com.zotoh.bedrock.device.*;
import com.zotoh.bedrock.wflow.*;


public class WEBProcessor extends Workflow {

  private Work task1= new Work() {
      public void eval(Job job ) throws Exception {
          ServletEventResult res= new ServletEventResult();
          Event ev= job.event().get();
          res.setData("<html><body>Bonjour!</body></html>");
          ev.setResult(res) ;
      }
  };

  protected Activity onStart() {

      return new PTask( task1);
  }



  public WEBProcessor(Job j) {
      super(j);
  }


}

