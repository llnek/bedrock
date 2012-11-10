package ${PACKAGE_ID};


import java.io.File;
import java.io.InputStream;
import java.util.Properties;

import com.zotoh.fwk.io.IOUte;
import com.zotoh.bedrock.wflow.*;
import com.zotoh.bedrock.core.*;


class MockRunner {

  static void main( args) {

      System.getProperties().put("bedrock.pipeline", "com.zotoh.bedrock.wflow.FlowModule");
      System.getProperties().put("log4j.configuration", "${LOG4J.REF}");
      System.setProperty("user.dir", "${APP.DIR}");

      try {
          def eng= new AppEngine( Module$.MODULE$.pipelineModule().get() ) ,
          props= new Properties() ,
          inp= null;

          try {
              inp= IOUte.open(new File("${MANIFEST.FILE}"));
              props.load(inp);
          } finally {
              IOUte.close(inp);
          }

          eng.start(props);
      }
      catch (Throwable t) {
          t.printStackTrace();
      }
  }

}

