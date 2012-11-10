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

import com.zotoh.fwk.util.LoggerFactory._
import com.zotoh.fwk.util.Logger
import com.zotoh.fwk.util.SeqNumGen._
import com.zotoh.bedrock.device.Device
import com.zotoh.bedrock.device.Event
import com.zotoh.bedrock.device.MemEvent

/**
 * Creates jobs when an event is generated from a device.
 *
 * @author kenl
 *
 */
class JobCreator protected[core](private val _engine:AppEngine) extends Vars {

  private def ilog() { _log=getLogger(classOf[JobCreator]) }
  @transient private var _log:Logger=null
  def tlog() = { if(_log==null) ilog(); _log }

  /**
   * @return
   */
  def engine() = _engine

  /**
   * @param ev
   */
  def create(ev:Event) {
    val v= ev.device()
    onCreate(
      v,
      v.id().matches(SYS_DEVID_REGEX),
      Job( next(), _engine, ev) )
  }

  /**
   * @param v
   * @param sys
   * @param j
   */
  protected def onCreate(v:Device, sys:Boolean, j:Job) {
    engine().module().onCreate(v,sys,j)
  }

  /**
   * @return
   */
  def createMemJob() = {
    _engine.deviceMgr().device(INMEM_DEVID) match {
      case Some(d) => Job( next(), _engine, MemEvent("", d))
      case _ => None
    }
  }

}
