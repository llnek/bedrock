package ${PACKAGE_ID};

import com.zotoh.bedrock.core.AppEngine;
import com.zotoh.bedrock.core.Job;
import com.zotoh.bedrock.wflow.*;
import com.zotoh.bedrock.device.Event;

class ${CLASS_NAME} extends FlowDelegate {

  def ${CLASS_NAME}( AppEngine  eng) {
      super(eng);
  }

  def Workflow  newProcess(Job job) {
      def ev= job.event().get();

  // You decide on how to react to this job by returning a workflow object back to the
  // engine.  How you decide is up to you, it can be simply be based on event type,
  // or content specific -
  // that is, take a look inside the event and based on its content, determine
  // what workflow to invoke.  Or you may have one giant workflow which takes
  // care of everything!.

      return new ${PROC_CLASS_NAME}(job);
  }

  public void onShutdown() {
      // add specific code to handle shutdown
  }

}

