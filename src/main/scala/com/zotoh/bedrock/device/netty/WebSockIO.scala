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

import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.util.StrUte._

import java.util.{Properties=>JPS,ResourceBundle}

import org.jboss.netty.bootstrap.ServerBootstrap
import org.jboss.netty.channel.ChannelPipeline
import org.jboss.netty.channel.ChannelPipelineFactory
import org.jboss.netty.handler.codec.http.HttpChunkAggregator
import org.jboss.netty.handler.codec.http.HttpRequestDecoder
import org.jboss.netty.handler.codec.http.HttpResponseEncoder

import org.json.{JSONObject=>JSNO}

import com.zotoh.fwk.util.{CmdLineMust,CmdLineQ,CmdLineSeq}
import com.zotoh.bedrock.device.DeviceMgr

/**
 * A HTTP device implementing the WebSocket protocol via Jboss/netty.
 *
 * The set of properties:
 *
 * <b>uri</b>
 * The Http URI request path.
 *
 * @see com.zotoh.bedrock.device.NettyIOTrait
 *
 * @author kenl
 *
 */
class WebSockIO(devMgr:DeviceMgr) extends NettyIOTrait(devMgr) {

  private var _pathUri=""

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.BaseHttpIO#inizWithProperties(org.json.JSONObject)
   */
  override def inizWithQuirks(pps:JSNO) {
    super.inizWithQuirks(pps)
    val cpath= trim(pps.optString("uri"))
    tstEStrArg("uri-path", cpath)
    _pathUri= cpath
  }

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.Device#onStart()
   */
  override def onStart() {
    val boot = onStart_0()
    val dev=this
    boot.setPipelineFactory(new ChannelPipelineFactory() {
      override def getPipeline() = {
        val pl = org.jboss.netty.channel.Channels.pipeline()
        dev.maybeCfgSSL( pl)
        pl.addLast("decoder", new HttpRequestDecoder())
        pl.addLast("aggregator", new HttpChunkAggregator(65536))
        pl.addLast("encoder", new HttpResponseEncoder())
        pl.addLast("handler", new WebSockHdlr(dev,_pathUri))
        pl
      }
    })

    onStart_1(boot)
  }

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.HttpIOTrait#supportsConfigMenu()
   */
  override def supportsConfigMenu() = true

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.HttpIOTrait#getCmdSeq(java.util.ResourceBundle, java.util.Properties)
   */
  override def getCmdSeq(rcb:ResourceBundle, pps:JPS) = {
    val q1= new CmdLineMust("uri", bundleStr(rcb, "cmd.uri.fmt")) {
      def onRespSetOut(a:String, p:JPS) = {
        p.put("uri", a)
        ""
      }}
    Some(new CmdLineSeq(super.getCmdSeq(rcb, pps), Array(q1)) {
      def onStart() = q1.label()
    })

  }


}
