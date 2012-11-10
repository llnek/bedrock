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

package com.zotoh.bedrock.device

import com.zotoh.fwk.util.LoggerFactory._
import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.util.Logger

/**
 * After a device generates an event, in most case, it has to wait for the downstream application code to
 * process this event, after which a result will be pass back to the device for replying back to the client.
 *
 * The WaitEvent class is used by devices to put the event on hold until the result is back from the application.
 *
 * @author kenl
 */
abstract class WaitEvent protected(private val _event:Event) {

  @transient private var _log= getLogger(classOf[WaitEvent])
  def tlog() = _log
  private var _res:Option[EventResult]=None

  _event.bindWait(this)

  /**
   * @param res
   */
  def resumeOnEventResult(res:EventResult):Unit

  /**
   * @param millisecs
   */
  def timeoutMillis(millisecs:Long):WaitEvent

  /**
   * @param secs
   */
  def timeoutSecs(secs:Int):WaitEvent

  /**
   * @return
   */
  def innerEvent() = _event

  /**
   * @return
   */
  def id() =  _event.id()

  /**
   * Set the result directly.
   *
   * @param obj the result.
   */
  def setEventResult(obj:EventResult) {
    _res= Some(obj)
    this
  }

  /**
   * Get the result.
   *
   * @return the result.
   */
  def result() = _res

}

