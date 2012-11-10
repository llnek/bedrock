package ${PACKAGE_ID}


import java.io.{InputStream, File}
import java.util.Properties
import java.lang.System._

import com.zotoh.bedrock.core._
import com.zotoh.bedrock.wflow._
import com.zotoh.fwk.io.IOUte._
import com.zotoh.fwk.util.CoreUte._


object MockRunner {

  def main(args:Array[String]) {

    getProperties().put("bedrock.pipeline", "com.zotoh.bedrock.wflow.FlowModule")
    getProperties().put("log4j.configuration", "${LOG4J.REF}")
    setProperty("user.dir", "${APP.DIR}")

    try {

      val eng= Module.pipelineModule() match {
        case Some(m) => new AppEngine(m)
        case None => null // should never happen, let it crash
      }

      using(open(new File("${MANIFEST.FILE}")) ) { (inp) =>
        val props= new Properties()
        props.load(inp)
        eng.start(props)
      }

    } catch {
      case e => e.printStackTrace()
    }

  }

}


