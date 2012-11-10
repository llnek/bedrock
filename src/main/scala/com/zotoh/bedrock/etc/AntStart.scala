/*??
 * COPYRIGHT (C) 2012 CHERIMOIA LLC. ALL RIGHTS RESERVED.
 *
 * THIS IS FREE SOFTWARE; YOU CAN REDISTRIBUTE IT AND/OR
 * MODIFY IT UNDER THE TERMS OF THE APACHE LICENSE,
 * VERSION 2.0 (THE "LICENSE").
 *
 * THIS LIBRARY IS DISTRIBUTED IN THE HOPE THAT IT WILL BE USEFUL,
 * BUT WITHOUT ANY WARRANTY; WITHOUT EVEN THE IMPLIED WARRANTY OF
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.
 *
 * SEE THE LICENSE FOR THE SPECIFIC LANGUAGE GOVERNING PERMISSIONS
 * AND LIMITATIONS UNDER THE LICENSE.
 *
 * You should have received a copy of the Apache License
 * along with this distribution; if not, you may obtain a copy of the
 * License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 ??*/


package com.zotoh.bedrock.etc

import scala.collection.mutable.HashMap

import java.lang.System._

import java.io.{File,InputStream}
import java.util.{Properties=>JPS}

import com.zotoh.fwk.util.{FileUte,Logger,StrUte,CoreUte}
import com.zotoh.fwk.util.LoggerFactory._
import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.util.StrUte._
import com.zotoh.fwk.io.{IOUte}

import com.zotoh.bedrock.util.MiscUte._
import com.zotoh.bedrock.core.{AppEngine, Module, Vars}

/**
 * (Internal use only).
 *
 * @author kenl
 */

object AntStart extends Vars {

  private val _log= getLogger(classOf[AntStart])
  def tlog() = _log

  def main( args:Array[String]) {
    val appdirpos = args.indexOf( "-appdir" )
    val dbg = args.indexOf( "-debug" ) == -1
    val root = if ( appdirpos >= 0) {
      new File( args(appdirpos+1) )
    } else {
      FileUte.cwd()
    }
    try {
      val cfg= new File(root, CFG)
      if ( ! FileUte.isDirRX(cfg)) {
        throw new Exception("AntStart: cannot locate config directory")
      }
      var fp=new File(cfg, APPCONF)
      if (!FileUte.isFileRX(fp)) {
        throw new Exception("AntStart: cannot locate/read app.conf file")
      }
      fp=new File(cfg, "ant.xml")
      if (!FileUte.isFileRX(fp)) {
        throw new Exception("AntStart: cannot locate/read/write ant.xml file")
      }
      fp=new File(cfg, APPPROPS)
      if (!FileUte.isFileRX(fp)) {
        throw new Exception("AntStart: cannot locate/read app.properties file")
      }
      val props= using( IOUte.open(fp)) { (inp) =>
        asQuirks(inp)
      }
      props.add(MANIFEST_FILE, "").add(APP_DIR, niceFPath(root))

      maybeSetKey(new File(new File( root, REALM), KEYFILE))
      getProperties().put(ENG_PROPS, props)

      Module.pipelineModule() match {
        case Some(m) => new AppEngine(m).start(props)
        case _ => throw new Exception("No Module!")
      }

      // engine will block until it stops
      // then we can exit
      if ( dbg) { exit(0) }

    } catch {
      case e => println(e.getMessage())
    }
  }

}

sealed class AntStart {
}


