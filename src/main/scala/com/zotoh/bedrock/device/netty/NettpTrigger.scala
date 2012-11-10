/*??
 * COPYRIGHT (C) 2010-2012 CHERIMOIA LLC. ALL RIGHTS RESERVED.
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


package com.zotoh.bedrock.device.netty

import org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1

import scala.collection.JavaConversions._
import java.io.InputStream

import org.jboss.netty.channel.Channel
import org.jboss.netty.channel.ChannelFuture
import org.jboss.netty.channel.ChannelFutureListener
import org.jboss.netty.channel.ChannelFutureProgressListener
import org.jboss.netty.channel.MessageEvent
import org.jboss.netty.handler.codec.http.DefaultHttpResponse
import org.jboss.netty.handler.codec.http.HttpResponse
import org.jboss.netty.handler.codec.http.HttpResponseStatus
import org.jboss.netty.handler.stream.ChunkedStream

import com.zotoh.fwk.io.XData
import com.zotoh.fwk.io.IOUte._
import com.zotoh.bedrock.device.AsyncTrigger
import com.zotoh.bedrock.device.EventResult
import com.zotoh.bedrock.device.Device
import com.zotoh.bedrock.device.HttpEventResult
import com.zotoh.fwk.net.HTTPStatus

/**
 * Triggers a response back to client - netty IO.
 *
 * @author kenl
 */
class NettpTrigger(dev:Device) extends AsyncTrigger(dev)  {

  private var _msgEvt:MessageEvent=null
  private var _cb:NettpMsgCB=null

  /**
   * @param cb
   * @param e
   */
  def this( cb:NettpMsgCB, e:MessageEvent) {
    this( cb.device())
    _cb=cb
    _msgEvt= e
  }

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.AsyncWaitTrigger#resumeWithResult(com.zotoh.bedrock.device.EventResult)
   */
  override def resumeWithResult(rs:EventResult) {
    rs match {
      case res:HttpEventResult =>
        try {
          reply(res)
        } catch {
          case e => _cb.tlog().errorX("", Some(e))
        }
    }
  }

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.AsyncWaitTrigger#resumeWithError()
   */
  override def resumeWithError() {
    resumeWithResult(
      new HttpEventResult(HTTPStatus.INTERNAL_SERVER_ERROR) )
  }

  private def reply(res:HttpEventResult) {
    val rsp = new DefaultHttpResponse(HTTP_1_1,
          new HttpResponseStatus(res.statusCode(), res.statusText() ))
    var clen = 0L
    val ch= _msgEvt.getChannel()

    res.headers().foreach { (t) => rsp.setHeader( t._1,t._2) }
    val inp= res.data() match {
      case Some(d) if(d.hasContent()) =>
        clen= d.size()
        d.stream()
      case _ => null
    }
    rsp.setHeader("content-length", clen.toString)
    _cb.setCookie(rsp)

    //TODO: this throw NPE some times !
    var cf:ChannelFuture = try { ch.write(rsp) } catch {
      case e => tlog().errorX("",Some(e)); null
      case _ => null
    }

    if (inp != null) {
      cf= ch.write(new ChunkedStream(inp))
      val me=this
      cf.addListener(new ChannelFutureProgressListener() {
        override def operationComplete(f:ChannelFuture) {
          close(inp)
          if (!me._cb.isKeepAlive()) {
            f.addListener(ChannelFutureListener.CLOSE)
          }
        }
        override def operationProgressed(
            f:ChannelFuture, amount:Long, current:Long, total:Long) {}
      })
    } else {
      _cb.preEnd(cf)
    }

  }

}

