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

import com.zotoh.fwk.io.XData
import com.zotoh.fwk.net.HTTPStatus
import com.zotoh.fwk.io.IOUte._
import com.zotoh.fwk.util.CoreImplicits

import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpServletRequest

import org.eclipse.jetty.continuation.ContinuationSupport
import org.eclipse.jetty.continuation.Continuation


/**
 * A trigger which works with Jetty's continuation.
 *
 * @author kenl
 */
class JettyAsyncTrigger(
  private var _rsp:HttpServletResponse,
  private var _req:HttpServletRequest,
  dev:Device) extends AsyncTrigger(dev) with CoreImplicits {

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.AsyncWaitTrigger#resumeWithResult(com.zotoh.bedrock.device.EventResult)
   */
  override def resumeWithResult(result:EventResult) {
    val res= result.asInstanceOf[HttpEventResult]
    val c = getCont()
    val hdrs= res.headers()
    val data = res.data()
    try {
      hdrs.foreach { (t) =>
        if ( "content-length".eqic(t._1)) {
        } else {
          _rsp.setHeader(t._1, t._2)
        }
      }
      if (res.hasError()) {
        _rsp.sendError(res.statusCode(), res.errorMsg())
      } else {
        _rsp.setStatus(res.statusCode())
      }
      data match {
        case Some(d) =>
          if (d.hasContent()) {
            val cl=d.size()
            copy(d.stream(), _rsp.getOutputStream(), cl)
            _rsp.setContentLength( cl.toInt)
          } else {
            _rsp.setContentLength(0)
          }
        case _ =>
      }
    }
    catch {
      case e => tlog().error("",e)
    }
    finally {
      c.complete()
    }

  }

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.AsyncWaitTrigger#resumeWithError()
   */
  override def resumeWithError() {
    val s= HTTPStatus.INTERNAL_SERVER_ERROR
    val c = getCont()
    try {
      _rsp.sendError(s.code(), s.reasonPhrase())
    }
    catch {
      case e => tlog().error("",e)
    }
    finally {
      c.complete()
    }
  }

  private def getCont() = {
    ContinuationSupport.getContinuation(_req)
  }

}

