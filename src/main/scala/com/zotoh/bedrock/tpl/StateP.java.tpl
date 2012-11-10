package ${PACKAGE_ID};

import com.zotoh.bedrock.device.Event;
import com.zotoh.bedrock.core.JobData;
import com.zotoh.bedrock.core.Job;
import com.zotoh.bedrock.wflow.*;


public class ${CLASS_NAME} extends Workflow {

  private Work task1=new Work() {
      public void eval(Job job ) {
          Event ev= job.event().get();
          // do your stuff here
          System.out.println("hello world");
      }
  };

  @Override
  public Activity onStart() {

      return new PTask( task1);

  }

  public ${CLASS_NAME}(Job job) {
      super(job);
  }

}

