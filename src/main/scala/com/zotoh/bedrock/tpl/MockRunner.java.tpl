package ${PACKAGE_ID};


import java.io.File;
import java.io.InputStream;
import java.util.Properties;

import com.zotoh.fwk.io.IOUte;
import com.zotoh.bedrock.wflow.*;
import com.zotoh.bedrock.core.*;


public class MockRunner {

  public static void main(String[] args) {

    System.getProperties().put("bedrock.pipeline", "com.zotoh.bedrock.wflow.FlowModule");
    System.getProperties().put("log4j.configuration", "${LOG4J.REF}");
    System.setProperty("user.dir", "${APP.DIR}");

    try {

      AppEngine eng= new AppEngine( Module$.MODULE$.pipelineModule().get() );
      Properties props= new Properties();
      InputStream inp= null;
      try {
        inp= IOUte.open(new File("${MANIFEST.FILE}"));
        props.load(inp);
      }
      finally {
        IOUte.close(inp);
      }
      eng.start(props);
    }
    catch (Throwable t) {
      t.printStackTrace();
    }

  }

}

