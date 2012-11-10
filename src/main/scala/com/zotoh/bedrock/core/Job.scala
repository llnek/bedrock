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

import com.zotoh.bedrock.device.Event

object Job {
  def apply(j:Long, e:AppEngine, ev:Event) = new Job(j,e,ev)
}

/**
 * When an event is spawned from a device, a job is created.  The runtime will decide on what processor
 * should handle this job, either by the "processor" property in the device configuration or via the application delegate.
 *
 * @see com.zotoh.bedrock.core.JobData
 *
 * @author kenl
 */
case class Job(private val _jobID:Long,private val _engine:AppEngine) {

  private def ilog() { _log=getLogger(classOf[Job]) }
  @transient private var _log:Logger=null
  def tlog() = {  if(_log==null) ilog(); _log }

  private var _data:JobData= _

  /**
   * @param jobID
   * @param engine
   * @param event
   */
  def this( jobID:Long, eng:AppEngine, ev:Event) {
    this(jobID, eng)
    _data =new JobData(Some(ev))
  }

  /**
   * @param jobID
   * @param engine
   * @param data
   */
  def this( jid:Long, eng:AppEngine, data:JobData) {
    this(jid,eng)
    _data= data
  }

  /**
   * @return
   */
  def engine() = _engine

  /**
   * @param key
   * @param value
   */
  def setSlot(key:Any, value:Any) { _data.setField(key, value) }

  /**
   * @param key
   * @return
   */
  def slot(key:Any) = _data.field(key)

  /**
   * @return
   */
  def event() = _data.event()

  /**
   * @return
   */
  def id() = _jobID

}


