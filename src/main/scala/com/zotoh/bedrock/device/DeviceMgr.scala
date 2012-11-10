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

package  com.zotoh.bedrock.device

import scala.collection.mutable.HashMap

import com.zotoh.fwk.util.LoggerFactory._
import com.zotoh.fwk.util.Logger
import com.zotoh.fwk.util.CoreUte._

import com.zotoh.bedrock.core.AppEngine
import com.zotoh.bedrock.core.Vars

/**
 * Manages all devices.
 *
 * @author kenl
 */
class DeviceMgr(private val _engine:AppEngine) extends Vars {

  private def ilog() {_log=getLogger(classOf[DeviceMgr]) }
  @transient private var _log:Logger=null
  def tlog() = { if(_log==null) ilog(); _log }

  private val _devices= HashMap[String,Device]()

  /**
   * @return
   */
  def engine() = _engine

  /**
   * @return
   */
  def listDevices() =  _devices.values.toSeq

  /**
   * @throws Exception
   */
  def load() {
    _devices.foreach { (t) =>
      t._2.start()
      	t._2 ! "load" 
      }
  }

  /**
   *
   */
  def unload() {
    _devices.foreach { (t) => t._2 ! "unload" }
  }

  /**
   * @param dev
   * @throws Exception
   */
  def add(dev:Device) {
    val id= dev.id()
    tstArg(!hasDevice(id),
      "Device: \"" + id + "\" already exists")
    _devices += Tuple2(id, dev)
  }

  def remove(dev:Device) {
    if (dev != null) { _devices.remove(dev.id()) }
  }

  /**
   * @param id
   * @return
   */
  def hasDevice(id:String) = {
    if (id==null) false else _devices.isDefinedAt(id)
  }

  /**
   * @param id
   * @return
   */
  def device(id:String) = {
    if (id==null) None else _devices.get(id)
  }

  def start() {
    _devices.foreach { (t) => t._2 ! "start" }
  }

  def stop() {
    _devices.foreach { (t) => t._2 ! "stop" }
  }

}

