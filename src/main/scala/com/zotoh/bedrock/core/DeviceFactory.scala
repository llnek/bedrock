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


package com.zotoh.bedrock.core;

import com.zotoh.fwk.util.LoggerFactory._
import com.zotoh.fwk.util.Logger
import com.zotoh.fwk.util.CoreUte._

import org.json.{JSONObject=>JSNO}

import com.zotoh.bedrock.device.DeviceMgr
import com.zotoh.bedrock.device.Device

/**
 * The role of a DeviceFactory is to create devices of a certain type.  If an application wants to introduce a new device type, the
 * application must also implement a device factory which can create those new type(s).
 *
 * @author kenl
 */
abstract class DeviceFactory protected(private val _devMgr:DeviceMgr) extends Vars {

  private def ilog() { _log=getLogger(classOf[DeviceFactory]) }
  @transient private var _log:Logger=null
  def tlog() = { if(_log==null) ilog(); _log }

  /**
   * @param id device name (id)
   * @param deviceProperties
   * @return
   */
  def newDevice(id:String, pps:JSNO) = {
    val dt= pps.optString("type")
    if (! pps.has(DEVID)) {
      pps.put(DEVID, id)
    }
    onNewDevice(deviceMgr(), dt, pps) match {
      case Some(dev) => dev.configure(pps); Some(dev)
      case _ => None
    }
  }

  /**
   * @return
   */
  def deviceMgr():DeviceMgr = _devMgr

  protected def onNewDevice(dm:DeviceMgr, dt:String, pps:JSNO):Option[Device]

}

