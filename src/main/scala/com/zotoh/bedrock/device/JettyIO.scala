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

import scala.collection.mutable.{HashMap,ArrayBuffer}
import scala.collection.JavaConversions._
import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.util.StrUte._
import com.zotoh.fwk.util.MetaUte._

import java.io.File
import java.util.{Properties=>JPS,ResourceBundle,EnumSet}
import javax.net.ssl.SSLContext

import org.eclipse.jetty.util.ssl.SslContextFactory
import org.eclipse.jetty.server.Connector
import org.eclipse.jetty.server.Handler
import org.eclipse.jetty.server.NCSARequestLog
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.ContextHandlerCollection
import org.eclipse.jetty.server.handler.DefaultHandler
import org.eclipse.jetty.server.handler.HandlerCollection
import org.eclipse.jetty.server.handler.RequestLogHandler
import org.eclipse.jetty.server.nio.SelectChannelConnector
import org.eclipse.jetty.server.ssl.SslSelectChannelConnector
import org.eclipse.jetty.servlet.DefaultServlet
import javax.servlet.DispatcherType;
import org.eclipse.jetty.servlet.FilterHolder
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jetty.util.thread.QueuedThreadPool
import org.eclipse.jetty.webapp.WebAppContext

import org.json.{JSONObject=>JSNO,JSONArray=>JSNA}
import com.zotoh.fwk.util.{CmdLineMust,CmdLineQ,CmdLineSeq}

/**
 * Http IO using Jetty as an embedded web server, using Jetty's continuation to create asynchronicity when handling requests.
 *
 * The set of properties:
 *
 * <b>contextpath</b>
 * The application context path.
 * <b>waitmillis</b>
 * The time this request will be put on hold until a result is ready from the downstream application - default is 5 mins.
 * <b>workers</b>
 * The number of worker threads allocated to Jetty, default is 10.
 * <b>warfile</b>
 * The full path pointing to a web WAR file.  The WAR application must use the servlet provided by this framework.
 * If a WAR is defined, then the following properties are ignored.
 * <b>resbase</b>
 * The full path pointing to the resource base directory.
 * <b>urlpatterns</b>
 * A list of servlet path patterns.
 * <b>filters</b>
 * A list of Filter definitions.  Each Filter definition is another Map of name-value pairs, such as:
 * ----> <b>urlpattern</b> the filter path.
 * ----> <b>class</b> the class for the filter object.
 * ----> <b>params</b> a map of parameters for this filter (key-values).
 *
 * @see com.zotoh.bedrock.device.HttpIOTrait
 *
 * @author kenl
 *
 */
class JettyIO(devMgr:DeviceMgr) extends BaseHttpIO(devMgr,false) with Weblet {

  type SSMSS = (String,String,Map[String,String])

//  private final SMap<JettyIO> _devMap= new SMap<JettyIO>()
  private var _contextPath=""
  private var _warPtr=""
  private var _resDir=""
  private var _logDir=""

  private val _filters=ArrayBuffer[SSMSS]()
  private val _urls=ArrayBuffer[String]()
  private var _jetty:Server=null

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.Weblet#setContextPath(java.lang.String)
   */
  override def setContextPath(path:String) = {
    _contextPath=nsb(path)
    this
  }

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.Weblet#getContextPath()
   */
  override def contextPath() = _contextPath

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.HttpIOTrait#inizWithProperties(org.json.JSONObject)
   */
  override def inizWithQuirks(pps:JSNO) = {
    super.inizWithQuirks(pps)
    _contextPath= trim(pps.optString("contextpath"))
    _logDir= trim(pps.optString("logdir"))
    _resDir= trim(pps.optString("resbase"))
    _warPtr= trim(pps.optString("warfile"))

    if (isEmpty(_warPtr)) {
      var arr= pps.optJSONArray("urlpatterns")
      tstObjArg("servlet-url-patterns", arr)
      for ( i <- 0 until arr.length()) {
        _urls += nsb(arr.get(i))
      }
      arr= pps.optJSONArray("filters")
      if (arr != null) for ( i <- 0 until arr.length()){
        _filters += toFilter( arr.optJSONObject(i))
      }
    }
  }

  private def toFilter(ftr:JSNO):SSMSS = {
    tstObjArg("filter-definition", ftr)
    val ps= ftr.optJSONObject("params")
    val url=ftr.optString("urlpattern")
    val z=ftr.optString("class")
    val m= HashMap[String,String]()
    tstEStrArg("filter-url-pattern", url)
    tstEStrArg("filter-class", z)
    if (ps != null) {
      val it= ps.keys(); while (it.hasNext()) {
        val s= it.next().asInstanceOf[String]
        m += Tuple2( s, nsb( ps.get(s)))
      }
    }
    (z, url, m.toMap)
  }

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.Device#onStart()
   */
  override def onStart() {
    val svr= new Server()
    val cc = if (isSSL()) {
      val c= new SslSelectChannelConnector()
      val t= keyURL() match {
        case Some(u) => HttpIOTrait.cfgSSL(true,sslType(), u, keyPwd())
        case _ => (null,null)
      }
      val fac=c.getSslContextFactory()
      fac.setSslContext( t._2)
      fac.setWantClientAuth(false)
      fac.setNeedClientAuth(false)
      c
    } else {
      new SelectChannelConnector()
    }
    cc.setName(this.id())
    if (!isEmpty(host())) { cc.setHost(host()) }
    cc.setPort(port())
    cc.setThreadPool(new QueuedThreadPool( workers() ))
    cc.setMaxIdleTime(30000)     // from jetty examples
    cc.setRequestHeaderSize(8192)  // from jetty examples
    svr.setConnectors(Array(cc))

    if (isEmpty(_warPtr)) {
      onStart_Servlet(svr)
    } else {
      onStart_War(svr)
    }

    _jetty=svr
    _jetty.start()
  }

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.Device#onStop()
   */
  override def onStop() {
    if (_jetty != null) block { () => _jetty.stop() }
    _jetty=null
  }

  private def onStart_Servlet(svr:Server) {
    //ServletContextHandler x = new ServletContextHandler(ServletContextHandler.SESSIONS | ServletContextHandler.NO_SECURITY)
    val x = new ServletContextHandler(ServletContextHandler.SESSIONS)
    x.setContextPath(_contextPath)
    x.setClassLoader(getCZldr())
    x.setDisplayName(this.id())
    x.setResourceBase( if(isEmpty(_resDir)) "." else _resDir)

    val contexts = new ContextHandlerCollection()
    val handlers=  new HandlerCollection()
    /*
    ResourceHandler rh= new ResourceHandler()
    rh.setDirectoriesListed(false)
    rh.setAliases(false)
    rh.setResourceBase(isEmpty(_resDir) ? "." : _resDir)
    rh.setWelcomeFiles(new String[]{ "index.html" })
    */
    val hs= Array[Handler](contexts, new DefaultHandler(), maybeREQLog() )
    handlers.setHandlers(hs)

    val pms=HashMap[String,String]()
    //pms.put("org.eclipse.jetty.servlet.Default.dirAllowed","false")
    //pms.put("org.eclipse.jetty.servlet.Default.resourceBase", isEmpty(_resDir)?".":_resDir)
    pms.put("resourceBase", if (isEmpty(_resDir)) "." else _resDir)
    var so=new ServletHolder(new DefaultServlet())
    so.setInitParameters(pms)
    x.addServlet(so, "/*")

    // add url patterns
    _urls.foreach { (u) =>
      so=new ServletHolder(new WEBServlet(this))
      so.setInitParameter("server-info", Server.getVersion())
      x.addServlet(so,u)
    }

    // filters
    _filters.foreach{ (t) =>
      var ho=x.addFilter(nsb(t._1), nsb(t._2), EnumSet.noneOf(classOf[DispatcherType]))
        //0 /* Handler.DEFAULT*/)
      t._3.foreach { (t) =>
        ho.setInitParameter(t._1,t._2)
      }
    }
    contexts.addHandler(x)
    svr.setHandler(handlers)
  }

  private def onStart_War(svr:Server) {
    val webapp = new WebAppContext()
    webapp.setAttribute("_#version#_", Server.getVersion())
    webapp.setAttribute("_#device#_", this)
    webapp.setContextPath(_contextPath)
    webapp.setWar(_warPtr)
    webapp.setExtractWAR(true)
    svr.setHandler(webapp)
  }

  private def maybeREQLog():Handler = {
    if (isEmpty(_logDir)) null else {
      val h= new RequestLogHandler()
      val dir=new File(_logDir)
      //dir.mkdirs()
      val path= niceFPath(dir) + "/jetty-yyyy_mm_dd.log"
      tlog().debug("JettyIO: request-log output path {} ", path)
      val requestLog = new NCSARequestLog(path)
      requestLog.setRetainDays(90)
      requestLog.setAppend(true)
      requestLog.setExtended(false)
      requestLog.setLogTimeZone("GMT")

      h.setRequestLog(requestLog)
      h
    }
  }

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.HttpIOTrait#supportsConfigMenu()
   */
  override def supportsConfigMenu() = true

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.HttpIOTrait#getCmdSeq(java.util.ResourceBundle, java.util.Properties)
   */
  override def getCmdSeq(rcb:ResourceBundle, pps:JPS) = {
    pps.put("filters", ArrayBuffer[HashMap[String,Any]]() )
    pps.put("urlpatterns", ArrayBuffer[String]())
    val q7= new CmdLineMust("fcz", bundleStr(rcb, "cmd.http.fcz")) {
      def onRespSetOut(a:String, p:JPS) = {
        val fs= p.get("filters").asInstanceOf[ArrayBuffer[HashMap[String,Any]]]
        val uri=nsb(p.remove("f_uri"))
        val m= HashMap[String,Any]()
        m += Tuple2("urlpattern", uri)
        m += Tuple2("class", a)
        m += Tuple2("params", HashMap[String,String]())
        fs += m
        "fpath"
      }}
    val q6= new CmdLineMust("fpath", bundleStr(rcb, "cmd.http.fpath")) {
      def onRespSetOut(a:String, p:JPS) = {
        if (isEmpty(a)) { p.remove("f_uri"); "" } else {
          p.put("f_uri", a)
          "fcz"
        }
      }}
    val q5= new CmdLineQ("filters", bundleStr(rcb,"cmd.http.filters"), "y/n","n") {
      def onRespSetOut(a:String, p:JPS) = {
        if("Yy".has(a)) "fpath" else ""
      }}
    val q4= new CmdLineMust("spath", bundleStr(rcb,"cmd.http.spath")) {
      def onRespSetOut(a:String, p:JPS) = {
        if (isEmpty(a)) { "filters" } else {
        p.get("urlpatterns").asInstanceOf[ArrayBuffer[String]] += a
        "spath"
        }
      }}
    val q2= new CmdLineQ("base",bundleStr(rcb,"cmd.http.resbase"), "",".") {
      def onRespSetOut(a:String, p:JPS) = {
        p.put("resbase", a)
        "spath"
      }}
    val q1= new CmdLineQ("ctx", bundleStr(rcb, "cmd.http.ctx")) {
      def onRespSetOut(a:String, p:JPS) = {
        p.put("contextpath", a)
        "base"
      }}
    Some(new CmdLineSeq(super.getCmdSeq(rcb, pps),Array(q1,q2,q4,q5,q6,q7)) {
      override def onStart() = q1.label()
    })
  }

}



