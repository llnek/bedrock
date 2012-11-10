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

package com.zotoh.fwk.net

import java.io.IOException

import org.jboss.netty.channel.Channel
import org.jboss.netty.channel.ChannelFuture
import org.jboss.netty.channel.ChannelFutureListener
import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.channel.MessageEvent
import org.jboss.netty.handler.codec.http.DefaultHttpResponse
import org.jboss.netty.handler.codec.http.HttpMethod
import org.jboss.netty.handler.codec.http.HttpRequest
import org.jboss.netty.handler.codec.http.HttpResponseStatus
import org.jboss.netty.handler.codec.http.HttpVersion
import org.jboss.netty.handler.stream.ChunkedStream

import com.zotoh.fwk.io.XData


/**
 * @author kenl
 *
 */
class FileServerHdlr(private var _svr:MemFileServer) extends BasicChannelHandler(_svr.channels()) {

  private var _file=""

  /* (non-Javadoc)
   * @see com.zotoh.netio.BasicChannelHandler#onRecvRequest(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.MessageEvent)
   */
  override def onRecvRequest( ctx:ChannelHandlerContext, ev:MessageEvent) = {

    var rc=true
    ev.getMessage() match {
      case msg:HttpRequest =>
        val m=msg.getMethod()
        val uri= msg.getUri()
        val pos= uri.lastIndexOf('/')
        val p = if (pos >=0) {
          uri.substring(pos+1)
        } else { uri }
        tlog().debug("FileServerHdlr: Input Method = {}", m.getName())
        tlog().debug("FileServerHdlr: Input Uri = {}", uri)
        tlog().debug("FileServerHdlr: File = {}", p)

        _file=p

        if (HttpMethod.GET.eq( m)) {
          doGet(ctx,ev)
          rc=false
        }

        rc
    }
  }

  /* (non-Javadoc)
   * @see com.zotoh.netio.BasicChannelHandler#doReqFinal(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.MessageEvent, com.zotoh.core.io.StreamData)
   */
  override def doReqFinal(ctx:ChannelHandlerContext, ev:MessageEvent,
      inData:XData) {
    if (inData  != null) {
      super.doReqFinal(ctx, ev, inData)
      doPut(inData)
    }
  }

  private def doGet(ctx:ChannelHandlerContext, ev:MessageEvent) {

    _svr.getFile(_file) match {
      case Some(data) =>
        if (data.size() == 0L) {
          doReplyError(ctx,ev, HttpResponseStatus.NO_CONTENT);
        } else {
          do_reply_file(ctx,ev,data)
        }
      case _ =>
        doReplyError(ctx,ev, HttpResponseStatus.NOT_FOUND)
    }
  }

  private def do_reply_file(ctx:ChannelHandlerContext, ev:MessageEvent, data:XData) {

    val res= new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK)
    val clen= data.size()
    var c= ev.getChannel()
    val me=this

    if (c == null) {
      c= ctx.getChannel()
    }
    res.setHeader("content-type", "application/octet-stream")
    res.setHeader("content-length", clen.toString )
    c.write(res)
    val f=c.write(new ChunkedStream( data.stream()))
    f.addListener(new ChannelFutureListener() {
      def operationComplete(fff:ChannelFuture) {
        me.write_complete(fff)
      }
    })
  }

  private def write_complete(fff:ChannelFuture) {
    if (isKeepAlive()) {
      fff.getChannel().close()
    }
  }

  private def doPut(in:XData) {
    _svr.saveFile(_file, in)
  }

}

