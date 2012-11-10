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
import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.util.Logger
import com.zotoh.bedrock.device.NIOCB

import java.io.IOException

import org.jboss.netty.channel.Channel
import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.channel.ChannelStateEvent
import org.jboss.netty.channel.ChildChannelStateEvent
import org.jboss.netty.channel.ExceptionEvent
import org.jboss.netty.channel.MessageEvent
import org.jboss.netty.channel.SimpleChannelHandler
import org.jboss.netty.handler.codec.http.HttpChunk
import org.jboss.netty.handler.codec.http.HttpRequest


/**
 * For netty IO.
 *
 * @author kenl
 */
class NettpReqHdlr(private val _dev:NettpIO) extends SimpleChannelHandler {

  private def ilog() { _log=getLogger(classOf[NettpReqHdlr]) }
  @transient private var _log:Logger=null
  def tlog() = { if(_log==null) ilog(); _log }

  /* (non-Javadoc)
   * @see org.jboss.netty.channel.SimpleChannelHandler#channelOpen(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelStateEvent)
   */
  override def channelOpen(ctx:ChannelHandlerContext, e:ChannelStateEvent) {
    var c= e.getChannel()
    if (c ==null)  { c= ctx.getChannel() }

    tlog().debug("NettpReqHdlr: channelOpen - ctx {}, channel {}",
        ctx, if(c==null) "?" else c.toString())

    _dev.pushOneChannel( c )
    super.channelOpen(ctx, e)
  }

  /* (non-Javadoc)
   * @see org.jboss.netty.channel.SimpleChannelHandler#channelDisconnected(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelStateEvent)
   */
  override def channelDisconnected(ctx:ChannelHandlerContext, e:ChannelStateEvent) {
    var c= e.getChannel()
    if (c ==null)  { c= ctx.getChannel() }

    tlog().debug("NettpReqHdlr: channelDisconnected - ctx {}, channel {}",
          ctx, if(c==null) "?" else c.toString())

    super.channelConnected(ctx, e)
  }

  /* (non-Javadoc)
   * @see org.jboss.netty.channel.SimpleChannelHandler#channelConnected(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelStateEvent)
   */
  override def channelConnected(ctx:ChannelHandlerContext, e:ChannelStateEvent) {
    var c= e.getChannel()
    if (c ==null)  { c= ctx.getChannel() }

    tlog().debug("NettpReqHdlr: channelConnected - ctx {}, channel {}",
          ctx, if(c==null) "?" else c.toString())

    super.channelConnected(ctx, e)
  }

  /* (non-Javadoc)
   * @see org.jboss.netty.channel.SimpleChannelHandler#channelClosed(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelStateEvent)
   */
  override def channelClosed(ctx:ChannelHandlerContext, e:ChannelStateEvent) {
    var c= e.getChannel()
    if (c ==null)  { c= ctx.getChannel() }

    tlog().debug("NettpReqHdlr: channelClosed - ctx {}, channel {}",
        ctx, if(c==null) "?" else c.toString())

    _dev.removeCB(c)
    super.channelClosed(ctx, e)
  }

  override def exceptionCaught(ctx:ChannelHandlerContext, e:ExceptionEvent) {
    var c= e.getChannel()
    if (c== null) { c= ctx.getChannel() }

    tlog().errorX("", Some(e.getCause()))

    if (c != null) {
      _dev.removeCB(c) match {
        case Some(cb) => cb.destroy()
        case _ =>
      }
      c.close()
    }
  }

  /* (non-Javadoc)
   * @see org.jboss.netty.channel.SimpleChannelHandler#messageReceived(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.MessageEvent)
   */
  override def messageReceived(ctx:ChannelHandlerContext, ev:MessageEvent) {
    val msg = ev.getMessage()
    val c= ctx.getChannel()
    msg match {
      case x:HttpRequest =>
        val cb= new NettpMsgCB(_dev)
        _dev.addCB(c, cb)
        cb.onREQ(ctx,ev)
      case x:HttpChunk =>
        _dev.cb(c) match {
          case y:NettpMsgCB => y.onChunk(ctx,ev)
          case _ =>
            throw new IOException("NettpReqHdlr: failed to reconcile http-chunked msg")
        }
      case _ =>
        throw new IOException("NettpReqHdlr:  unexpected msg type: " +
            safeGetClzname(msg))
    }
  }

  override def childChannelClosed(ctx:ChannelHandlerContext,
      e:ChildChannelStateEvent) {
    var c= e.getChannel()
    if (c ==null)  { c= ctx.getChannel() }

    tlog().debug("NettpReqHdlr: ChildChannelClosed - ctx {}, channel {}",
          ctx, if(c==null) "?" else c.toString())

    _dev.popOneChannel(c)
    super.childChannelClosed(ctx, e)
  }

  override def childChannelOpen(ctx:ChannelHandlerContext,
      e:ChildChannelStateEvent) {
    var c= e.getChannel()
    if (c ==null)  { c= ctx.getChannel() }

    tlog().debug("NettpReqHdlr: ChildChannelOpen - ctx {}, channel {}",
          ctx, if(c==null) "?" else c.toString())

    _dev.pushOneChannel(c)
    super.childChannelOpen(ctx, e)
  }


}
