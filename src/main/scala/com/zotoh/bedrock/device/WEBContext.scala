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

package com.zotoh.bedrock.device

import com.zotoh.fwk.util.LoggerFactory._
import com.zotoh.fwk.util.Logger
import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.io.IOUte._

import java.io.{File,InputStream}
import java.util.{Properties=>JPS}

import javax.servlet.ServletContext
import javax.servlet.ServletContextEvent
import javax.servlet.ServletContextListener

import com.zotoh.bedrock.core.{Module,AppEngine}
import com.zotoh.bedrock.core.Vars


class WEBContext extends ServletContextListener with Vars {

  private def ilog() { _log=getLogger(classOf[WEBContext]) }
  @transient private var _log:Logger =null
  def tlog() = { if(_log==null) ilog(); _log }

  private var _dev:Option[Device]=None

  /* (non-Javadoc)
   * @see javax.servlet.ServletContextListener#contextInitialized(javax.servlet.ServletContextEvent)
   */
  override def contextInitialized(evt:ServletContextEvent) {

    tlog().debug("WEBContext: contextInitialized()")

    val x= evt.getServletContext()
    var ctx=""
    val m= x.getMajorVersion()
    val n= x.getMinorVersion()

    if (m > 2 || ( m==2 && n > 4)) {
      ctx= x.getContextPath()
    }

    try {
      inizAsJ2EE(x, ctx)
    } catch {
      case e => tlog().errorX("", Some(e)); throw e
    }

    x.setAttribute(WEBSERVLET_DEVID, _dev)
  }

  /* (non-Javadoc)
   * @see javax.servlet.ServletContextListener#contextDestroyed(javax.servlet.ServletContextEvent)
   */
  override def contextDestroyed(e:ServletContextEvent) {
    tlog().debug("WEBContext: contextDestroyed()")
    _dev match {
      case Some(d) => d.deviceMgr().engine().shutdown()
      case _ =>
    }
    _dev=None
  }

  private def inizAsJ2EE(ctx:ServletContext, ctxPath:String) {
    val webinf = new File( niceFPath( ctx.getRealPath("/WEB-INF/")))
    val root= webinf.getParentFile()
    val cfg= new File(root, CFG)
    val props= new JPS()
    using(open( new File(cfg, APPPROPS))) { (inp) =>
      props.load(inp)
    }
    val eng= Module.pipelineModule() match {
      case Some(m) => new AppEngine(m)
      case _ => null      
    }
    eng.startViaContainer(root, props)
    eng.deviceMgr().device(WEBSERVLET_DEVID) match {
      case x:Device => _dev=Some(x)
      case _ =>
    }
    _dev match {
      case x:Weblet => x.setContextPath(ctxPath)
      case _ =>
    }

  }

}
