package ${PACKAGE_ID};

import com.zotoh.bedrock.device.Event;
import com.zotoh.bedrock.core.JobData;
import com.zotoh.bedrock.core.Job;
import com.zotoh.bedrock.wflow.*;

class ${CLASS_NAME} extends Workflow {

  private def task1=new Work() {
      public void eval(Job job) {
          def ev= job.event().get();
          // do your stuff here
          println("hello world");
      }
  };

  def Activity onStart() {

      return new PTask( task1 );

  }

  def ${CLASS_NAME}(Job job) {
      super(job);
  }

}

