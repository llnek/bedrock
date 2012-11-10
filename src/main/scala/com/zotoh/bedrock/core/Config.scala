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

package com.zotoh.bedrock.core

import scala.collection.mutable.HashMap

import org.json.{JSONObject=>JSNO}
import java.io.InputStream
import java.net.URL

import com.zotoh.fwk.util.LoggerFactory._
import com.zotoh.fwk.util.{Logger,CoreImplicits,JSONUte}
import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.util.StrUte._
import com.zotoh.fwk.io.IOUte._
import com.zotoh.fwk.util.JSONUte._

import com.zotoh.bedrock.device.Device
import com.zotoh.bedrock.impl.DefaultDeviceFactory._
import com.zotoh.bedrock.impl.DefaultDeviceFactory
import com.zotoh.bedrock.impl.CustomDeviceFactory

object Config {}

/**
 * Handles the reading and parsing of the device config file in JSON format.
 *
 * @author kenl
 */
sealed case class Config protected[core](private val _engine:AppEngine) extends Vars with CoreImplicits {

  private def ilog() { _log= getLogger(classOf[Config]) }
  @transient private var _log:Logger=null
  def tlog() = {  if(_log==null) ilog(); _log }

  private val _devHandlers=HashMap[String,DeviceFactory]()
  private var _cusDevFac:CustomDeviceFactory=null
  private var _devFac:DeviceFactory=null

  iniz()

  /**
   * @return
   */
  def engine() = _engine

  def dftDevFacttory() = _devFac

  def cusDevFactory() = _cusDevFac


  /**
   * @param cfgFile
   */
  def parse(cfgFile:URL) {
    using(cfgFile.openStream()) { (inp) =>
      JSONUte.read(inp) match {
        case root =>
          onDevHdlers( root.optJSONObject(CFGKEY_DEVHDLRS) )
          onDevices( root.optJSONObject(CFGKEY_DEVICES) )
          onCores( root.optJSONObject(CFGKEY_CORES) )
      }
//      onSys()
    }
  }

  /**
   *
   */
  protected[core] def onSys() {
    val obj= newJSON()
    // we add an internal event source that can generate in-memory events
    obj.put(CFGKEY_TYPE, DT_MEMORY)
    obj.put(DEV_STATUS, true)
    addDev(INMEM_DEVID, DT_MEMORY, obj)

    if ( !engine().isEmbedded()) {
      // check for shutdown port
      cfgShutdownHook(engine().quirk(SHUTDOWN_PORT) match {
        case s:String => s
        case _ => ""
      } )
    }
  }

  // expect format = "host:port"
  private def cfgShutdownHook(ps:String) {
    val pos= if (isEmpty(ps)) -1 else ps.lastIndexOf(':')
    var h=""
    val port=asInt( trim( if (pos >= 0) {
        h=ps.substring(0,pos); ps.substring(pos+1)
      } else {
        ps
    }), -1)

    if (port > 0)    {
      val obj=newJSON()
      obj.put(CFGKEY_TYPE, DT_HTTP)
      obj.put(DEV_STATUS, true)
      obj.put(CFGKEY_HOST, h)
      obj.put(CFGKEY_SOCTOUT, 5)
      obj.put(CFGKEY_PORT, port)
      obj.put("async", true)
      addDev(SHUTDOWN_DEVID, DT_HTTP, obj)
    }

  }

  def onDevHdlers(top:JSNO) {
    val fac= new CustomDeviceFactory( engine().deviceMgr() )
    val keys = if (top != null) top.keys() else null
    if (keys !=null) while (keys.hasNext()) {
      val t= keys.next() match { case s:String => s}
      fac.add( t, top.optString(t) )
      _devHandlers += Tuple2(t, fac)
    }
  }

  def onCores(obj:JSNO) {
    val it= if (obj != null) obj.keys() else null
    if (it != null) while(it.hasNext() ) {
      val key= it.next() match { case s:String => s}
      onOneCore(key, obj.getJSONObject(key) )
    }
  }

  private def onOneCore(id:String, obj:JSNO) {
    val n= obj.optInt(CFGKEY_THDS)
    if (n > 0) { engine().scheduler().addCore(id, n) }
  }

  private def onDevices(top:JSNO) {
    val it= if (top==null) null else top.keys()
    if (it != null) while(it.hasNext()) {
      val key= it.next() match { case s:String => s}
      onOneDevice(key, top.getJSONObject(key) )
    }
  }

  private def onOneDevice(id:String, obj:JSNO) {
    val t = trim( obj.optString(CFGKEY_TYPE))
    addDev(id, t, obj)
  }

  def addDev(id:String, dt:String, obj:JSNO) = {
    val fac= _devHandlers.get(dt) match {
      case Some(fac) => fac
      case _ =>
        throw new InstantiationException(
          "Config: no device-factory found for type: " + dt)
    }

    try {
      fac.newDevice(id, obj) match {
        case Some(dev) =>
          engine().deviceMgr().add(dev)
          Some(dev)
        case _ =>
          None
      }
    } catch {
      case t => tlog().warn("",t); None
    }
//    if (dev==null) {
//      throw new InstantiationException("Failed to create device type: " + type) ;
//    }
  }

  private def iniz() {
    val dm=engine().deviceMgr()
    _cusDevFac= new CustomDeviceFactory( dm )
    _devFac= new DefaultDeviceFactory( dm )

    listDefaultTypes().foreach { (h) =>
      _devHandlers += Tuple2(h, _devFac)
    }
  }

}
