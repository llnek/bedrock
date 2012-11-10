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

package com.zotoh.bedrock.device.netty

import com.zotoh.fwk.util.LoggerFactory._
import com.zotoh.fwk.util.Logger
import com.zotoh.fwk.util.StrUte._

import org.jboss.netty.handler.codec.http.HttpHeaders.isKeepAlive
import org.jboss.netty.handler.codec.http.HttpHeaders.setContentLength
import org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONNECTION
import org.jboss.netty.handler.codec.http.HttpHeaders.Values.WEBSOCKET
import org.jboss.netty.handler.codec.http.HttpMethod.GET
import org.jboss.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN
import org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1

import java.security.MessageDigest
import java.io.{ByteArrayOutputStream=>BAOS,IOException}

import org.jboss.netty.buffer.ChannelBufferInputStream
import org.jboss.netty.channel.Channels
import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.channel.Channel
import org.jboss.netty.channel.ChannelFuture
import org.jboss.netty.channel.ChannelFutureListener
import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.channel.ChannelPipeline
import org.jboss.netty.channel.ChannelStateEvent
import org.jboss.netty.channel.ChildChannelStateEvent
import org.jboss.netty.channel.ExceptionEvent
import org.jboss.netty.channel.MessageEvent
import org.jboss.netty.channel.SimpleChannelUpstreamHandler
import org.jboss.netty.handler.codec.http.DefaultHttpResponse
import org.jboss.netty.handler.codec.http.HttpHeaders
import org.jboss.netty.handler.codec.http.HttpHeaders.Names
import org.jboss.netty.handler.codec.http.HttpHeaders.Values
import org.jboss.netty.handler.codec.http.HttpMethod
import org.jboss.netty.handler.codec.http.HttpRequest
import org.jboss.netty.handler.codec.http.HttpResponse
import org.jboss.netty.handler.codec.http.HttpResponseStatus
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame
import org.jboss.netty.handler.codec.http.websocketx.WebSocketServerHandshaker
import org.jboss.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory
import org.jboss.netty.handler.ssl.SslHandler
import org.jboss.netty.util.CharsetUtil
import org.jboss.netty.handler.codec.http.websocketx.ContinuationWebSocketFrame
import org.jboss.netty.handler.codec.http.websocketx.CloseWebSocketFrame
import org.jboss.netty.handler.codec.http.websocketx.BinaryWebSocketFrame
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame
import org.jboss.netty.handler.codec.http.websocketx.PingWebSocketFrame
import org.jboss.netty.handler.codec.http.websocketx.PongWebSocketFrame

import com.zotoh.bedrock.device.AsyncWaitEvent
import com.zotoh.bedrock.device.Event
import com.zotoh.bedrock.device.WaitEvent

object WebSockHdlr {

  def toBytes(frame:WebSocketFrame) = {
    var buf= frame.getBinaryData()
    var baos= new BAOS(4096)
    if (buf != null) try {
      val s= new ChannelBufferInputStream(buf)
      val bits= new Array[Byte](4096)
      var c=0
      do {
        c= s.read(bits)
        if (c > 0) { baos.write(bits, 0,c) }
      } while (c > 0)
    } catch {
      case e => new WebSockHdlr().tlog().errorX("",Some(e))
    }

    baos.toByteArray()
  }

}

/**
 * @author kenl
 *
 */
class WebSockHdlr protected[netty](private val _dev:WebSockIO, private val _pathUri:String) extends SimpleChannelUpstreamHandler {

  private def ilog() {_log=getLogger(classOf[WebSockHdlr]) }
  @transient private var _log:Logger=null
  def tlog() = { if(_log==null) ilog(); _log }

  private val SEC_WEBSOCKET_ACCEPT="Sec-WebSocket-Accept"
  private val SEC_WEBSOCKET_KEY= "Sec-WebSocket-Key"
  private val MAX_FRAME_SIZE = 1024 * 16

  private var _handshaker:WebSocketServerHandshaker=null

  private def this () {
    this(null,null)
  }

  /* (non-Javadoc)
   * @see org.jboss.netty.channel.SimpleChannelUpstreamHandler#channelClosed(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelStateEvent)
   */
  override def channelClosed(ctx:ChannelHandlerContext, e:ChannelStateEvent) {
    var c= e.getChannel()
    if (c ==null)  { c= ctx.getChannel() }

    tlog().debug("NettpReqHdlr: ChannelClosed - ctx {}, channel {}",
          ctx, if (c==null) "?" else c.toString())

    _dev.popOneChannel(c)
    super.channelClosed(ctx, e)
  }

  /* (non-Javadoc)
   * @see org.jboss.netty.channel.SimpleChannelUpstreamHandler#channelConnected(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelStateEvent)
   */
  override def channelConnected(ctx:ChannelHandlerContext, e:ChannelStateEvent) {
    var c= e.getChannel()
    if (c ==null)  { c= ctx.getChannel() }

    tlog().debug("NettpReqHdlr: ChannelConnected - ctx {}, channel {}",
          ctx, if(c==null) "?" else c.toString())

    super.channelConnected(ctx, e)
  }

  /* (non-Javadoc)
   * @see org.jboss.netty.channel.SimpleChannelUpstreamHandler#channelDisconnected(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelStateEvent)
   */
  override def channelDisconnected(ctx:ChannelHandlerContext,
      e:ChannelStateEvent) {
    var c= e.getChannel()
    if (c ==null)  { c= ctx.getChannel() }

    tlog().debug("NettpReqHdlr: ChannelDisconnected - ctx {}, channel {}",
          ctx, if(c==null) "?" else c.toString())

    super.channelDisconnected(ctx, e)
  }

  /* (non-Javadoc)
   * @see org.jboss.netty.channel.SimpleChannelUpstreamHandler#channelOpen(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelStateEvent)
   */
  override def channelOpen(ctx:ChannelHandlerContext, e:ChannelStateEvent) {
    var c= e.getChannel()
    if (c ==null)  { c= ctx.getChannel() }

    tlog().debug("NettpReqHdlr: ChannelOpen - ctx {}, channel {}",
          ctx, if(c==null) "?" else c.toString())

    _dev.pushOneChannel(c)
    super.channelOpen(ctx, e)
  }

  /* (non-Javadoc)
   * @see org.jboss.netty.channel.SimpleChannelUpstreamHandler#childChannelClosed(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChildChannelStateEvent)
   */
  override def childChannelClosed(ctx:ChannelHandlerContext,
      e:ChildChannelStateEvent) {
    var c= e.getChannel()
    if (c ==null)  { c= ctx.getChannel() }

    tlog().debug("NettpReqHdlr: ChildChannelClosed - ctx {}, channel {}",
          ctx, if(c==null) "?" else c.toString())

    _dev.popOneChannel(c)
    super.childChannelClosed(ctx, e)
  }

  /* (non-Javadoc)
   * @see org.jboss.netty.channel.SimpleChannelUpstreamHandler#childChannelOpen(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChildChannelStateEvent)
   */
  override def childChannelOpen(ctx:ChannelHandlerContext,
      e:ChildChannelStateEvent) {
    var c= e.getChannel()
    if (c ==null)  { c= ctx.getChannel() }

    tlog().debug("NettpReqHdlr: ChildChannelOpen - ctx {}, channel {}",
          ctx, if(c==null) "?" else c.toString())

    _dev.pushOneChannel(c)
    super.childChannelOpen(ctx, e)
  }

  /* (non-Javadoc)
   * @see org.jboss.netty.channel.SimpleChannelUpstreamHandler#messageReceived(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.MessageEvent)
   */
  override def messageReceived(ctx:ChannelHandlerContext, e:MessageEvent) {
    e.getMessage() match {
      case x:WebSocketFrame => handleWebSocketFrame(ctx, x)
      case x:HttpRequest => handleHttpRequest(ctx, x)
    }
  }

  /* (non-Javadoc)
   * @see org.jboss.netty.channel.SimpleChannelUpstreamHandler#exceptionCaught(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ExceptionEvent)
   */
  override def exceptionCaught(ctx:ChannelHandlerContext, e:ExceptionEvent) {
    tlog().errorX("", Some(e.getCause()) )
    e.getChannel().close()
  }

  private def handleHttpRequest(ctx:ChannelHandlerContext, req:HttpRequest) {
    val ssl= ctx.getPipeline().get(classOf[SslHandler]) != null
    val mtd= req.getMethod()
    val uri= req.getUri()

    tlog().debug("WebSockHdlr: request uri= {}", uri)

    if ( GET != mtd) {
      tlog().debug("WebSockHdlr: expecting /GET, got {}", mtd)
      sendHttpResponse(ctx, req, new DefaultHttpResponse(HTTP_1_1,  FORBIDDEN))
    } else {
      doHandshake(ctx,req,ssl)
    }
  }

  private def doHandshake(ctx:ChannelHandlerContext, req:HttpRequest, ssl:Boolean) {
    val wsf = new WebSocketServerHandshakerFactory(
          getWebSocketLocation(req,ssl), null, false)
    val hs = wsf.newHandshaker(req)
    val me=this
    val cc=ctx.getChannel()

    if (hs == null) {
      wsf.sendUnsupportedWebSocketVersionResponse(cc)
    } else {
      _handshaker=hs
      hs.handshake(cc, req).addListener(new ChannelFutureListener(){
        override def operationComplete(f:ChannelFuture) {
          if (!f.isSuccess()) {
            Channels.fireExceptionCaught(f.getChannel(), f.getCause())
          }else{
            me.maybeSSL(ctx)
          }
        }
      })
    }
  }

  private def maybeSSL(ctx:ChannelHandlerContext) {
    // check if ssl ?
    // get the SslHandler in the current pipeline.
    val ssl= ctx.getPipeline().get(classOf[SslHandler])
    if (ssl != null) {
      val cf= ssl.handshake()
      cf.addListener(new ChannelFutureListener(){
        override def operationComplete(f:ChannelFuture) {
          if (!f.isSuccess()) {
            f.getChannel().close()
          }
        }
      })
    }

  }

  private def handleWebSocketFrame(ctx:ChannelHandlerContext, frame:WebSocketFrame) {
    val cc= ctx.getChannel()
    val data= frame match {
      case x:CloseWebSocketFrame =>
        _handshaker.close(cc, x)
        null
      case x:PingWebSocketFrame =>
        cc.write(new PongWebSocketFrame(frame.getBinaryData()))
        null
      case x:BinaryWebSocketFrame =>  WebSockHdlr.toBytes(frame)
      case x:TextWebSocketFrame => x.getText()
      case x:ContinuationWebSocketFrame =>
        if (!x.isFinalFragment()) null else {
          x.getAggregatedText()
        }
    }

    if (data != null) {
      val w= new AsyncWaitEvent(
        new WebSockEvent(data,_dev), new WebSockTrigger(ctx,_dev) )
      val ev = w.innerEvent()
      w.timeoutMillis(_dev.waitMillis())
      _dev.holdEvent(w)
      _dev.deviceMgr().engine().scheduler().run( new Runnable(){
        def run() { _dev.dispatch(ev) }
      })
    }

  }

  private def sendHttpResponse(ctx:ChannelHandlerContext, req:HttpRequest, res:HttpResponse) {
    // generate an error page if response status code is not OK (200).
    if (res.getStatus().getCode() != 200) {
      res.setContent(ChannelBuffers.copiedBuffer(res.getStatus().toString(),
          CharsetUtil.UTF_8))
      setContentLength(res, res.getContent().readableBytes())
    }

    // send the response and close the connection if necessary.
    val f = ctx.getChannel().write(res)
    if ( !isKeepAlive(req) || res.getStatus().getCode() != 200) {
      f.addListener(ChannelFutureListener.CLOSE)
    }
  }

  private def getWebSocketLocation(req:HttpRequest,ssl:Boolean) = {
    ( if (ssl)  "wss://" else "ws://" ) +
      req.getHeader(HttpHeaders.Names.HOST) + _pathUri
  }


}

