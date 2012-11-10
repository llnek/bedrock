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


import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.util.StrUte._
import com.zotoh.fwk.io.IOUte._
import com.zotoh.fwk.io.XData

import java.io.{IOException,InputStream,OutputStream,ByteArrayOutputStream=>BAOS}
import javax.servlet.http.HttpServletRequest

/**
 * Helper functions to get data from a Http request.
 *
 * @author kenl
 */
object HttpHplr {

  /**
   * @param dev
   * @param req
   * @return
   * @throws IOException
   */
  def extract(dev:BaseHttpIO, req:HttpServletRequest ):HttpEvent = {

    val clen= req.getContentLength()
    val ev= new ServletEvent(dev)
    val thold= dev.threshold()

    ev.setContentType(req.getContentType()).setContentLength( clen)

    req.getContextPath()

    ev.setMethod( req.getMethod() ).
    setServletPath( req.getServletPath()).
    setSSL( nsb(req.getScheme()).lc.has("https") ).
    setScheme(req.getScheme()).
    setUrl( nsb( req.getRequestURL()) ).
    setUri( req.getRequestURI() ).
    setQueryString( req.getQueryString() ).
    setProtocol(req.getProtocol())

    var en= req.getHeaderNames()
    var s=""
    while (en.hasMoreElements()) {
      s=nsb( en.nextElement())
      ev.setHeader( s, req.getHeader(s) )
    }

    en= req.getParameterNames()
    while (en.hasMoreElements()) {
      s= nsb( en.nextElement())
      ev.addParam(s, req.getParameterValues(s))
    }

    en= req.getAttributeNames()
    while (en.hasMoreElements()) {
      s=nsb( en.nextElement())
      ev.addAttr(s, req.getAttribute(s))
    }

    if (clen > 0) {
      grabPayload(ev, req.getInputStream(), clen, thold)
    }

    ev
  }

  private def grabPayload(ev:HttpEvent, inp:InputStream, clen:Long, thold:Long) {
    val t = if (clen > thold) { newTempFile(true) } else {
      (null, new BAOS(4096))
    }
    using(t._2) { (os) =>
      copy(inp, os, clen)
      ev.setData( new XData( if(t._1==null) os else t._1))
    }
  }

}


