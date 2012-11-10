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

import com.zotoh.bedrock.device.Event

object JobData {}

/**
 * JobData is a transient collection of data belonging to a Job.  By default, it has a reference to the original event
 * which spawned the job.
 * If a Processor needs to persist some job data, those data should be encapsulate in a WState object.
 *
 * @see com.zotoh.bedrock.core.Job
 *
 * @author kenl
 */
case class JobData protected[core](private var _event:Option[Event]= None) {

  private val _data= HashMap[Any,Any]()

  /**
   * @param e
   */
  def setEvent(e:Some[Event]) { _event=e }

  /**
   * @return
   */
  def event() = _event

  /**
   * @param key
   * @param value
   */
  def setField( key:Any, value:Any) {
    if ( key != null) {
      _data.put(key, value)
    }
  }

  /**
   * @param key
   * @return
   */
  def field(key:Any) = {
    if (key==null) None else _data.get(key)
  }

  /**
   * @param key
   * @return
   */
  def remove(key:Any) = {
    if (key==null) None else _data.remove(key)
  }

  /**
   *
   */
  def clearAll() { _data.clear() }

}


