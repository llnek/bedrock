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

import scala.collection.mutable.HashMap

import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.util.MetaUte._
import com.zotoh.fwk.util.StrUte._


import java.lang.reflect.{Constructor=>Ctor}
import org.json.{JSONObject=>JSNO}

import com.zotoh.bedrock.core.{DeviceFactory,Vars}
import com.zotoh.bedrock.device.{Device,DeviceMgr}

/**
 * The device factory for all built-in devices.
 *
 * @author kenl
 */
class CustomDeviceFactory(m:DeviceMgr) extends DeviceFactory(m) with Vars {

  private val _devs= HashMap[String,Ctor[_]]()

  /**
   * @return
   */
  def userDevs() = _devs.keys.toArray

  /**
   * @param dt
   * @param devCz
   * @throws Exception
   */
  def add(dt:String, devCz:String) {

    if (isEmpty(dt) || isEmpty(devCz)) { return }
    if (_devs.isDefinedAt(dt)) {
      errBadArg("Device type: " + dt + " is already defined.")
    }

    var z= loadClass(devCz)
    tstArgIsType("device", z, classOf[Device])

    try {
      var ctor= z.getConstructor(classOf[DeviceMgr])
      _devs += Tuple2(dt, ctor)
    } catch {
      case _ => throw new InstantiationException("Class: " + devCz + " is missing ctor(DeviceManager)")
    }
  }

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.core.DeviceFactory#onNewDevice(com.zotoh.bedrock.device.DeviceManager, java.lang.String, org.json.JSONObject)
   */
  override def onNewDevice(dm:DeviceMgr, dt:String, pps:JSNO) = {
    if ( _devs.isDefinedAt(dt)) {
      Some(_devs(dt).newInstance(dm).asInstanceOf[Device] )
    } else {
      None
    }
  }

}
