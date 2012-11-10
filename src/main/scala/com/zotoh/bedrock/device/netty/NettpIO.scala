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

import org.jboss.netty.channel.Channels.pipeline

import org.jboss.netty.bootstrap.ServerBootstrap
import org.jboss.netty.channel.ChannelPipeline
import org.jboss.netty.channel.ChannelPipelineFactory
import org.jboss.netty.channel.SimpleChannelHandler
import org.jboss.netty.handler.codec.http.HttpRequestDecoder
import org.jboss.netty.handler.codec.http.HttpResponseEncoder
import org.jboss.netty.handler.stream.ChunkedWriteHandler

import com.zotoh.bedrock.device.DeviceMgr
import com.zotoh.bedrock.device.HttpEvent

/**
 * Http Device using JBoss-Netty which is asynchronous by design.
 *
 * The set of properties:
 *
 * @see com.zotoh.bedrock.device.NettyIOTrait
 *
 * @author kenl
 */
class NettpIO(devMgr:DeviceMgr,ssl:Boolean) extends NettyIOTrait(devMgr,ssl) {

  /**
   * @param mgr
   */
  def this(devMgr:DeviceMgr) {
    this(devMgr, false)
  }

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.Device#onStart()
   */
  override def onStart() {
    val boot = onStart_0()
    val me= this

    // from netty examples/tutorials...
    boot.setPipelineFactory(new ChannelPipelineFactory() {
      override def getPipeline() = {
        var pl = org.jboss.netty.channel.Channels.pipeline()
        me.maybeCfgSSL( pl)
        pl.addLast("decoder", new HttpRequestDecoder())
//        pipeline.addLast("aggregator", new HttpChunkAggregator(65536))
        pl.addLast("encoder", new HttpResponseEncoder())
        //pipeline.addLast("deflater", new HttpContentCompressor())
        pl.addLast("chunker", new ChunkedWriteHandler())
        pl.addLast("handler", me.getHandler())
        pl
      }
    })

    onStart_1(boot)
  }

  /**
   * @return
   */
  protected[netty] def getHandler() = new NettpReqHdlr(this)

  /**
   * @return
   */
  protected[netty] def createEvent() = new HttpEvent(this)

}

