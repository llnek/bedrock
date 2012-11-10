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


import java.io.IOException

import javax.servlet.ServletConfig
import javax.servlet.ServletContext
import javax.servlet.ServletException
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import org.eclipse.jetty.continuation.Continuation
import org.eclipse.jetty.continuation.ContinuationSupport

import com.zotoh.fwk.util.LoggerFactory._
import com.zotoh.fwk.util.{CoreImplicits,Logger}
import com.zotoh.fwk.util.MetaUte._
import com.zotoh.fwk.io.XData
import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.io.IOUte._
import com.zotoh.fwk.net.HTTPStatus

import com.zotoh.bedrock.core.Vars

/**
 * @author kenl
 *
 */
class WEBServlet(private var _dev:BaseHttpIO=null) extends HttpServlet with Vars with CoreImplicits {

  private def ilog() { _log=getLogger(classOf[WEBServlet]) }
  @transient private var _log:Logger=null
  def tlog() = { if(_log==null) ilog(); _log }

  private val serialVersionUID = -3862652820921092885L
  private var _jettyAsync=false

  /* (non-Javadoc)
   * @see javax.servlet.GenericServlet#destroy()
   */
  override def destroy() {
    tlog().debug("WEBServlet: destroy()")
  }

  /* (non-Javadoc)
   * @see javax.servlet.http.HttpServlet#service(javax.servlet.ServletRequest, javax.servlet.ServletResponse)
   */
  override def service(request:ServletRequest, response:ServletResponse) {
    val rsp= response.asInstanceOf[HttpServletResponse]
    val req= request.asInstanceOf[HttpServletRequest]

    tlog().debug("{}\n{}\n{}",
    "********************************************************************",
      req.getRequestURL(),
      "********************************************************************")

    val evt= HttpHplr.extract( _dev, req)
    if (_jettyAsync) {
      doASyncSvc(evt, req, rsp)
    } else {
      doSyncSvc(evt, req,rsp)
    }
  }

  private def doASyncSvc(evt:HttpEvent, req:HttpServletRequest, rsp:HttpServletResponse) {
    val c = ContinuationSupport.getContinuation(req)
    if (c.isInitial())  {
      try {
        dispREQ(c, evt, req,rsp)
      } catch {
        case e => tlog().errorX("",Some(e))
      }
    }
  }

  private def doSyncSvc(evt:HttpEvent, req:HttpServletRequest, rsp:HttpServletResponse) {
    val w= new SyncWaitEvent( evt )
    val ev = w.innerEvent()

    _dev.holdEvent(w)
    _dev.deviceMgr().engine().scheduler().run( new Runnable(){
      def run() { _dev.dispatch(ev) }
    })

    try {
      w.timeoutMillis(  _dev.waitMillis())
    } finally {
      _dev.releaseEvent(w)
    }

    w.innerEvent().result() match {
      case Some(res) =>
        replyService( res.asInstanceOf[HttpEventResult] , rsp)
      case _ =>
        replyService( new HttpEventResult(HTTPStatus.REQUEST_TIMEOUT), rsp)
    }
  }

  protected def replyService(res:HttpEventResult, rsp:HttpServletResponse) {
    val hdrs= res.headers()
    val data  = res.data()
    var clen=0L
    val sc= res.statusCode()

    try  {
      hdrs.foreach { (t) =>
        if ( !"content-length".eqic(t._1)) {
          rsp.setHeader(t._1,t._2)
        }
      }
      if (res.hasError()) {
        rsp.sendError(sc, res.errorMsg())
      } else {
        rsp.setStatus(sc)
      }
      data match {
        case Some(d) if (d.hasContent()) =>
          clen=d.size()
          copy( d.stream(), rsp.getOutputStream(), clen)
        case _ =>
      }

      rsp.setContentLength( clen.toInt)
    }
    catch {
      case e => tlog().warnX("",Some(e))
    }
  }

  private def dispREQ(ct:Continuation, evt:HttpEvent, req:HttpServletRequest, rsp:HttpServletResponse) {

    ct.setTimeout(_dev.waitMillis())
    ct.suspend(rsp)

    val w= new AsyncWaitEvent(evt,  new JettyAsyncTrigger( rsp, req, _dev) )
    val ev = w.innerEvent()

    w.timeoutMillis(_dev.waitMillis())
    _dev.holdEvent(w)
    _dev.deviceMgr().engine().scheduler().run( new Runnable(){
      def run() { _dev.dispatch(ev) }
    })
  }

  override def init(config:ServletConfig) {
    super.init(config)

    val ctx= config.getServletContext()
    ctx.getAttribute(WEBSERVLET_DEVID) match {
      case x:BaseHttpIO => _dev =x
    }

    block { () =>
      val z= loadClass("org.eclipse.jetty.continuation.ContinuationSupport")
      if (z != null) { _jettyAsync= true }
    }

    block { () =>
      tlog().debug("{}\n{}{}\n{}\n{}{}",
        "********************************************************************",
        "Servlet Container: ",
        ctx.getServerInfo(),
        "********************************************************************",
        "Servlet:iniz() - servlet:" ,
        getServletName())
    }

  }

}
