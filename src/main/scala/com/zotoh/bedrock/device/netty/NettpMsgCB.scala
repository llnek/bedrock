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

import scala.math._

import scala.collection.JavaConversions._

import com.zotoh.fwk.util.LoggerFactory._
import com.zotoh.fwk.util.StrUte._
import org.jboss.netty.handler.codec.http.HttpHeaders.{isKeepAlive => JbossIsKeepAlive}
import org.jboss.netty.handler.codec.http.HttpHeaders.Names.COOKIE
import org.jboss.netty.handler.codec.http.HttpHeaders.Names.SET_COOKIE

import java.io.File
import java.io.IOException
import java.io.{OutputStream,ByteArrayOutputStream=>BAOS}
import java.util.Set

import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.channel.ChannelFuture
import org.jboss.netty.channel.ChannelFutureListener
import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.channel.MessageEvent
import org.jboss.netty.handler.codec.http.Cookie
import org.jboss.netty.handler.codec.http.CookieDecoder
import org.jboss.netty.handler.codec.http.CookieEncoder
import org.jboss.netty.handler.codec.http.HttpChunk
import org.jboss.netty.handler.codec.http.HttpRequest
import org.jboss.netty.handler.codec.http.HttpResponse

import com.zotoh.fwk.io.XData
import com.zotoh.fwk.io.IOUte._
import com.zotoh.fwk.util.Logger
import com.zotoh.bedrock.device.AsyncWaitEvent
import com.zotoh.bedrock.device.NIOCB
import com.zotoh.bedrock.device.Device
import com.zotoh.bedrock.device.Event
import com.zotoh.bedrock.device.HttpEvent
import com.zotoh.bedrock.device.WaitEvent

/**
 * Callback used to work with the netty asynchronous IO framework.
 *
 * @author kenl
 */
class NettpMsgCB protected[netty](private val _dev:NettpIO) extends NIOCB {

  private def ilog() { _log=getLogger(classOf[NettpMsgCB]) }
  @transient private var _log:Logger=null
  def tlog() = { if(_log==null) ilog(); _log }

  @transient private var _os:OutputStream= _
  private var _clen:Long=0L
  private var _request:HttpRequest= _
  private var _keepAlive=false

  private var _event:HttpEvent= _
  private var _fout:File= _
  private var _cookie:CookieEncoder= _

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.NIOCB#destroy()
   */
  override def destroy() {
    close(_os)
    _os=null
    _fout=null
    _request= null
    _event= null
    _cookie= null
  }

  /**
   * @return
   */
  def isKeepAlive() = _keepAlive

  /**
   * @return
   */
  def cookie() = _cookie

  /**
   * @return
   */
  def device() = _dev

  /**
   * @param rsp
   */
  protected[netty] def setCookie(rsp:HttpResponse) = {
    if (_cookie != null) {
      rsp.addHeader(SET_COOKIE, _cookie.encode())
    }
    this
  }

  /**
   * @return
   */
  def event() = _event

  /**
   * @param future
   */
  def preEnd(future:ChannelFuture) {
    // Close the non-keep-alive connection after the write operation is done.
    if ( ! _keepAlive ) {
      future.addListener(ChannelFutureListener.CLOSE)
    }
  }

  /**
   * @param ctx
   * @param e
   * @throws Exception
   */
  def onREQ(ctx:ChannelHandlerContext, ev:MessageEvent) {
    if (this._request != null) {
      throw new Exception("NettpReqHdlr: onREQ: expected to be called once only")
    }
    this._request = ev.getMessage().asInstanceOf[HttpRequest]
    tlog().debug("NettpReqHdlr: URI=> {}" , _request.getUri() )
    _event= NettpHplr.extract(_dev, _request)
    _event.setSSL( _dev.isSSL())
    _keepAlive = JbossIsKeepAlive( _request)

    if (_request.isChunked()) {
      tlog().debug("NettpReqHdlr: request is chunked")
    } else {
      sockBytes(_request.getContent())
      onMsgFinal(ev)
    }
  }

  /**
   * @param ctx
   * @param ev
   * @throws Exception
   */
  def onChunk(ctx:ChannelHandlerContext, ev:MessageEvent) {
    val chunk = ev.getMessage().asInstanceOf[HttpChunk]
//    HttpChunkTrailer trailer;
    sockBytes(chunk.getContent())
    if (chunk.isLast()) {
      onMsgFinal(ev)
    }
  }

  private def onMsgFinal(ev:MessageEvent) {
    val data= XData()
    if (_os.isInstanceOf[BAOS]) {
      data.resetMsgContent(_os)
    }
    else if (_fout != null) {
      data.resetMsgContent(_fout)
    }

    close(_os)
    _os=null
    _event.setData(data)

    // Encode the cookie.
    val cookie = _request.getHeader(COOKIE)
    val me= this

    if ( ! isEmpty(cookie)) {
      val cookies = new CookieDecoder().decode(cookie)
      if (!cookies.isEmpty()) {
        // Reset the cookies if necessary.
        val enc = new CookieEncoder(true)
        cookies.foreach{(c) => enc.addCookie(c) }
        _cookie= enc
      }
    }

    val w= new AsyncWaitEvent( _event, new NettpTrigger(me, ev) )
    val evt = w.innerEvent()

    w.timeoutMillis( _dev.waitMillis())
    _dev.holdEvent(w)
    _dev.deviceMgr().engine().scheduler().run( new Runnable(){
      def run() { me._dev.dispatch(evt) }
    })
  }

  private def sockBytes(cbuf:ChannelBuffer) {
    var c=0
    if (cbuf != null) do {
      c=cbuf.readableBytes()
      if (c > 0) {
        sockit_down(cbuf, c)
      }
    } while (c > 0)
  }

  private def sockit_down(cbuf:ChannelBuffer, count:Int) {
    val bits= new Array[Byte](4096)
    val thold= _dev.threshold()
    var total=count
    while (total > 0) {
      val len = min(4096, total)
      cbuf.readBytes(bits, 0, len)
      _os.write(bits, 0, len)
      total = total-len
    }
    _os.flush()
    if (_clen >= 0L) { _clen += count }
    if (_clen > 0L && _clen > thold) {
      swap()
    }
  }

  private def swap() {
    val baos= _os.asInstanceOf[BAOS]
    val t= newTempFile(true)
    _os=t._2
    _os.write(baos.toByteArray())
    _os.flush()
    _clen= -1L
    _fout= t._1
  }



}

