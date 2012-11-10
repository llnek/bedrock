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

import scala.math._

import scala.collection.mutable.HashMap
import scala.collection.JavaConversions._

import com.zotoh.fwk.util.LoggerFactory._
import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.util.{Logger,StrArr}
import com.zotoh.fwk.util.StrUte._
import com.zotoh.fwk.io.IOUte
import org.jboss.netty.handler.codec.http.HttpHeaders.Names.COOKIE

import java.io.{ByteArrayOutputStream=>BAOS,OutputStream}
import java.io.{IOException,File}
import java.util.{Properties=>JPS}
import java.util.Set

import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.channel.Channel
import org.jboss.netty.channel.ChannelEvent
import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.channel.ChannelStateEvent
import org.jboss.netty.channel.ExceptionEvent
import org.jboss.netty.channel.MessageEvent
import org.jboss.netty.channel.SimpleChannelHandler
import org.jboss.netty.channel.group.ChannelGroup
import org.jboss.netty.handler.codec.http.Cookie
import org.jboss.netty.handler.codec.http.CookieDecoder
import org.jboss.netty.handler.codec.http.CookieEncoder
import org.jboss.netty.handler.codec.http.DefaultHttpResponse
import org.jboss.netty.handler.codec.http.HttpChunk
import org.jboss.netty.handler.codec.http.HttpHeaders
import org.jboss.netty.handler.codec.http.HttpMessage
import org.jboss.netty.handler.codec.http.HttpRequest
import org.jboss.netty.handler.codec.http.HttpResponse
import org.jboss.netty.handler.codec.http.HttpResponseStatus
import org.jboss.netty.handler.codec.http.HttpVersion

import com.zotoh.fwk.io.XData
import com.zotoh.fwk.io.IOUte._

/**
 * @author kenl
 *
 */
class BasicChannelHandler( private var _grp:ChannelGroup) extends SimpleChannelHandler {

  @transient private var _log=getLogger(classOf[BasicChannelHandler])
  def tlog() = _log

  private var _thold= HttpUte.dftThreshold()
  private var _clen=0L
  private var _cookies:CookieEncoder= _
  private var _fOut:File = _
  private var _os:OutputStream = _

  private var _props= new JPS()
  private var _keepAlive=false

  /**
   * @return
   */
  //def cookie() = _cookies

  /**
   * @return
   */
  def isKeepAlive() = _keepAlive

  /* (non-Javadoc)
   * @see org.jboss.netty.channel.SimpleChannelHandler#channelClosed(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelStateEvent)
   */
  override def channelClosed(ctx:ChannelHandlerContext, ev:ChannelStateEvent) {

    val c= maybeGetChannel(ctx,ev)

    tlog().debug("BasicChannelHandler: channelClosed - ctx {}, channel {}", ctx, if(c==null) "?" else c )

    if (c != null) { _grp.remove(c) }

    super.channelClosed(ctx, ev)
  }

//  @Override
//  public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent ev)
//          throws Exception {
//    Channel c= maybeGetChannel(ctx,ev)
//    tlog().debug("BasicChannelHandler: channelConnected - ctx {}, channel {}",
//                ctx, (c==null ? "?" : c.toString()) )
//    super.channelConnected(ctx, ev)
//  }

//  @Override
//  public void channelDisconnected(ChannelHandlerContext ctx,
//          ChannelStateEvent ev) throws Exception {
//    Channel c= maybeGetChannel(ctx,ev)
//    tlog().debug("BasicChannelHandler: channelDisconnected - ctx {}, channel {}",
//                ctx, (c==null ? "?" : c.toString()) )
//    super.channelDisconnected(ctx, ev)
//  }

  /* (non-Javadoc)
   * @see org.jboss.netty.channel.SimpleChannelHandler#channelOpen(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelStateEvent)
   */
  override def channelOpen(ctx:ChannelHandlerContext, ev:ChannelStateEvent) {
    val c= maybeGetChannel(ctx,ev)

    tlog().debug("BasicChannelHandler: channelOpen - ctx {}, channel {}", ctx, if (c==null) "?" else c)

    if (c != null) { _grp.add(c) }

    super.channelOpen(ctx, ev)
  }

  /* (non-Javadoc)
   * @see org.jboss.netty.channel.SimpleChannelHandler#exceptionCaught(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ExceptionEvent)
   */
  override def exceptionCaught(ctx:ChannelHandlerContext, ev:ExceptionEvent) {

    tlog().errorX("", Some(ev.getCause()))

    val c= maybeGetChannel(ctx, ev)
    if (c != null) try {
        c.close()
    } finally {
        _grp.remove(c)
    }
//    super.exceptionCaught(ctx, e)
  }

//  @Override
//  public void handleDownstream(ChannelHandlerContext ctx, ChannelEvent e)
//          throws Exception {
//    super.handleDownstream(ctx,e)
//  }

//  @Override
//  public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e)
//          throws Exception {
//    super.handleUpstream(ctx,e)
//  }

  /**
   * @param ctx
   * @param ev
   * @return
   * @throws IOException
   */
  protected def onRecvRequest( ctx:ChannelHandlerContext, ev:MessageEvent) = true
  // false to stop further processing

  /* (non-Javadoc)
   * @see org.jboss.netty.channel.SimpleChannelHandler#messageReceived(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.MessageEvent)
   */
  override  def messageReceived(ctx:ChannelHandlerContext, ev:MessageEvent) {

    val msg = ev.getMessage()

    msg match {
      case x:HttpChunk => // no op
      case x:HttpMessage =>
        msg_recv_0(x)
        _os= new BAOS(4096)
        _props.clear()
    }

    msg match {
      case res:HttpResponse =>
        val s= res.getStatus()
        val r= s.getReasonPhrase()
        val c= s.getCode()

        tlog().debug("BasicChannelHandler: got a response: code {} {}", asJObj(c), asJObj(r))

        _props.add("reason", r).add("dir", -1).add("code", c)
        if (c >= 200 && c < 300) {
          onRes(s,ctx,ev)
        } else {
          onResError(ctx,ev)
        }
//        else if (code >= 300 && code < 400) {
//          // TODO: handle redirect
//        }
      case req:HttpRequest =>
        tlog().debug("BasicChannelHandler: got a request: ")
        onReqIniz(ctx,ev)
        _keepAlive = HttpHeaders.isKeepAlive(req)
        _props.put("dir", asJObj(1))
        if ( onRecvRequest(ctx,ev) ) {
          onReq(ctx,ev)
        }
      case x:HttpChunk =>
        onChunk(ctx,ev)
      case _ =>        
        throw new IOException( "BasicChannelHandler:  unexpected msg type: " + 
            safeGetClzname(msg))
    }
  }

  private def onReq(ctx:ChannelHandlerContext, ev:MessageEvent) {
    ev.getMessage() match {
      case msg:HttpRequest =>
        if (msg.isChunked()) {
          tlog().debug("BasicChannelHandler: request is chunked")
        } else {
          sockBytes(msg.getContent())
          onMsgFinal(ctx,ev)
        }
    }
  }

  private def onRes(rc:HttpResponseStatus, ctx:ChannelHandlerContext, ev:MessageEvent) {
    ev.getMessage() match {
      case msg:HttpResponse =>
        onResIniz(ctx,ev)
        if (msg.isChunked()) {
          tlog().debug("BasicChannelHandler: response is chunked")
        } else {
          sockBytes(msg.getContent())
          onMsgFinal(ctx,ev)
        }
    }
  }

  protected def onReqIniz(ctx:ChannelHandlerContext, ev:MessageEvent) {
    ev.getMessage() match {
      case msg:HttpRequest =>
        val m= msg.getMethod().getName()
        val uri= msg.getUri()
        tlog().debug("BasicChannelHandler: onReqIniz: Method {}, Uri {}", m, uri)
        onReqPreamble(m, uri, iterHeaders(msg))
    }
  }

  protected def onResIniz(ctx:ChannelHandlerContext, ev:MessageEvent) {
    ev.getMessage() match {
      case msg:HttpResponse =>
        onResPreamble( iterHeaders(msg))
    }
  }

  protected def onReqPreamble(mtd:String, uri:String, h:Map[String,StrArr] ) {}
  protected def onResPreamble(h:Map[String,StrArr]) {}

  protected def doReqFinal(code:Int, r:String, out:XData) {}
  protected def doResFinal(code:Int, r:String, out:XData) {}
  protected def onResError(code:Int, r:String) {}

  private def onResError(ctx:ChannelHandlerContext, ev:MessageEvent) {
    val cc= maybeGetChannel(ctx,ev)
    onResError( _props.geti("code"), _props.gets("reason"))
    if (!isKeepAlive()) {
      cc.close()
    }
  }

  private def sockBytes(cb:ChannelBuffer) {
    var c=0
    if (cb != null) do {
      c=cb.readableBytes()
      if (c > 0) { sockit(cb, c) }
    } while(c >0)
  }

  private def sockit(cb:ChannelBuffer, count:Int) {

    val bits= new Array[Byte](4096)
    var total=count

//    tlog().debug("BasicChannelHandler: socking it down {} bytes", count)

    while (total > 0) {
      val len = min(4096, total)
      cb.readBytes(bits, 0, len)
      _os.write(bits, 0, len)
      total = total-len
    }

    _os.flush()

    if (_clen >= 0L) { _clen += count }

    if (_clen > 0L && _clen > _thold) {
        swap()
    }
  }

  private def swap() {
    _os match {
      case baos:BAOS =>
        val t= newTempFile(true)
        t._2.write(baos.toByteArray())
        t._2.flush()
        _os=t._2
        _clen= -1L
        _fOut= t._1
    }
  }

  protected def doReplyError(ctx:ChannelHandlerContext, ev:MessageEvent, err:HttpResponseStatus) {
    doReplyXXX(ctx,ev,err)
  }

  private def doReplyXXX(ctx:ChannelHandlerContext, ev:MessageEvent, s:HttpResponseStatus) {
    val res= new DefaultHttpResponse(HttpVersion.HTTP_1_1, s)
    val c= maybeGetChannel(ctx,ev)
    res.setChunked(false)
    res.setHeader("content-length", "0")
    c.write(res)
    if (! isKeepAlive()) {
      c.close()
    }
  }

  protected def doReqFinal(ctx:ChannelHandlerContext, ev:MessageEvent, inData:XData) {
    doReplyXXX(ctx,ev,HttpResponseStatus.OK)
  }

  private def onMsgFinal(ctx:ChannelHandlerContext, ev:MessageEvent) {
    val dir = _props.geti("dir")
    val out= on_msg_final(ev)
    if ( dir > 0) {
      doReqFinal(ctx,ev,out)
    } else if (dir < 0) {
      doResFinal( _props.geti("code"), _props.gets("reason"),out)
    }
  }

  private def on_msg_final(ev:MessageEvent) = {
    val data= XData()
    if (_fOut != null) {
      data.resetMsgContent(_fOut)
    } else {
      data.resetMsgContent(_os)
    }

    IOUte.close(_os)
    _fOut=null
    _os=null

    data
  }

  /**
   * @param t
   * @return
   */
  def withThreshold(t:Long) = {
    _thold=t; this
  }


  /**
   * @param ctx
   * @param ev
   * @return
   */
  protected def maybeGetChannel(ctx:ChannelHandlerContext, ev:ChannelEvent) = {
    val cc= ev.getChannel()
    if (cc==null) ctx.getChannel()  else cc
  }

  private def msg_recv_0(msg:HttpMessage) {
    val s= msg.getHeader(COOKIE)
    if ( ! isEmpty(s)) {
      val cookies = new CookieDecoder().decode(s)
      val enc = new CookieEncoder(true)
      cookies.foreach { (c) =>  enc.addCookie(c)  }
      _cookies= enc
    }
  }

  private def onChunk(ctx:ChannelHandlerContext, ev:MessageEvent) {
    ev.getMessage() match {
      case msg:HttpChunk =>
        sockBytes(msg.getContent())
        if (msg.isLast()) {
          onMsgFinal(ctx,ev)
        }
    }
  }

  protected def iterHeaders(msg:HttpMessage) = {
    val hdrs= HashMap[String,StrArr]()

    msg.getHeaderNames().foreach { (n) =>
      hdrs += n -> StrArr().add( msg.getHeaders(n).toSeq )
    }

    if (tlog().isDebugEnabled()) {
      val dbg=new StringBuilder(1024)
      hdrs.foreach { (t) =>
        dbg.append(t._1).append(": ").append(t._2).append("\r\n")
      }
      tlog().debug("HttpResponseHdlr: headers\n{}", dbg.toString )
    }

    hdrs.toMap
  }

}
