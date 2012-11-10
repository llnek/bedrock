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

package com.zotoh.bedrock.impl

import scala.collection.mutable.ArrayBuffer

import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.util.MetaUte._

import org.json.{JSONObject=>JSNO}

import com.zotoh.bedrock.core.{Vars,DeviceFactory}
import com.zotoh.bedrock.{device=>CZBD}
import com.zotoh.bedrock.device._
import com.zotoh.bedrock.device.netty._

object DefaultDeviceFactory extends Vars {
  private val DEVS=Array(
    DT_ONESHOT, DT_REPEAT, DT_HTTP
    , DT_HTTPS
    , DT_JMS, DT_TCP
    ,DT_FILE,  DT_WEBSOC, DT_JETTY, DT_ATOM, DT_REST
    ,DT_POP3, DT_MEMORY
  )

  private val _DEVMAP= Map(
    DT_ONESHOT-> classOf[CZBD.OneShotTimer],
    DT_REPEAT-> classOf[CZBD.RepeatingTimer],
    DT_HTTP-> classOf[CZBD.netty.NettpIO],
    DT_JMS-> classOf[CZBD.JmsIO],
    DT_TCP-> classOf[CZBD.TcpIO],
    DT_FILE-> classOf[CZBD.FilePicker],
    DT_JETTY-> classOf[CZBD.JettyIO],
    DT_ATOM-> classOf[CZBD.FeedIO],
    DT_REST-> classOf[CZBD.netty.RestIO],
    DT_POP3-> classOf[CZBD.PopIO],
    DT_WEBSOC-> classOf[CZBD.netty.WebSockIO]
  )

  /**
   * @return
   */
  def  devCZMap() = _DEVMAP.toMap

  /**
   * @param type
   * @return
   */
  def devCZ(dt:String) = {
    _DEVMAP.get( if ("https" == dt) "http" else dt )
  }

  /**
   * @return
   */
  def listDefaultTypes() = {
    (DT_WEB_SERVLET :: DefaultDeviceFactory.DEVS.toList).toSeq
  }
}

/**
 * The device factory for all built-in devices.
 *
 * @author kenl
 */
class DefaultDeviceFactory(m:DeviceMgr) extends DeviceFactory(m) with Vars {

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.core.DeviceFactory#onNewDevice(com.zotoh.bedrock.device.DeviceManager, java.lang.String, org.json.JSONObject)
   */
  override def onNewDevice(dm:DeviceMgr, dt:String, pps:JSNO) = {
    val x= dt match {
      case DT_WEB_SERVLET => new WebIO(dm)
      case DT_ONESHOT => new OneShotTimer(dm)
      case DT_REPEAT => new RepeatingTimer( dm )
      case DT_HTTPS => new NettpIO( dm, true)
      case DT_HTTP => new NettpIO( dm)
      case DT_WEBSOC => new WebSockIO( dm)
      case DT_JETTY => makeDev(dm, "com.zotoh.bedrock.device.JettyIO")
      case DT_TCP => new TcpIO( dm)
      case DT_JMS => makeDev(dm, "com.zotoh.bedrock.device.JmsIO")
      case DT_POP3 => new PopIO( dm)
      case DT_ATOM => makeDev(dm, "com.zotoh.bedrock.device.FeedIO")
      case DT_REST => new RestIO( dm)
      case DT_FILE => new FilePicker( dm)
      case DT_MEMORY => new MemDevice( dm)
      case _ => 
        throw new Exception("Unknown device type: " + dt)
    }
    Some(x)
  }

  // we don't want to pull in jars unnecessarily
  private def makeDev(dm:DeviceMgr, cz:String) = {
    loadClass(cz).getConstructor(classOf[DeviceMgr]).newInstance(dm).asInstanceOf[Device]
  }

}
