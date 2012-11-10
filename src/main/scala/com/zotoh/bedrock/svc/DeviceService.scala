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


package com.zotoh.bedrock.svc

import scala.collection.JavaConversions._

import com.zotoh.bedrock.device.{Event, DeviceMgr}
import com.zotoh.bedrock.device.Device._

import com.zotoh.fwk.util.JSONUte._
import com.zotoh.bedrock.core.Vars



class DeviceService(name:String, kind:String ) extends Vars {

  private var _handler:EventHandler = null
  protected val _ps= newJSON()


  _ps.put("type", kind)
  _ps.put(DEVID, name)

  def id() = _ps.getString(DEVID)

  def setProc(cz:String) {
    _ps.put(DEV_PROC, cz)
  }

  def withAttrString(attr:String, value:String) {
    _ps.put(attr, value)
  }

  def withAttrBool(attr:String, value:Boolean) {
    _ps.put(attr, value)
  }

  def withAttrDouble(attr:String, value:Double) {
    _ps.put(attr, value)
  }

  def withAttrLong(attr:String, value:Long) {
    _ps.put(attr, value)
  }

  def withAttrInt(attr:String, value:Int) {
    _ps.put(attr, value)
  }

  def withAttrList(attr:String, value:List[_]) {
    _ps.put(attr, newJSA(value.toIterable) )
  }

  def withAttrMap(attr:String, value:Map[_,_] ) {
    _ps.put(attr, newJSON(value) )
  }

  def setHandler( f: EventHandler ) {
    _handler = f
  }

  def end = {
  }

  def reify(dm:DeviceMgr) {
      dm.engine().config().addDev( id(), _ps.getString("type"), _ps) match {
        case Some(d) => d.bindHandler(_handler)
        case _ =>
      }
  }

}
