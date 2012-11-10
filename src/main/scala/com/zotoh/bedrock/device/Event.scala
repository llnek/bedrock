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
import com.zotoh.fwk.util.SeqNumGen._

/**
 * Base class for all other Events.
 *
 * @author kenl
 */
abstract class Event protected(private val _dev:Device) {

  private val serialVersionUID = -1928500078786458743L
  private def ilog() {  _log=getLogger(classOf[Event]) }
  @transient private var _log:Logger=null
  def tlog() = { if(_log==null) ilog(); _log  }

  private var _waitEvent:Option[WaitEvent]=None
  private var _res:Option[EventResult]=None
  private var _id= next()

  /**
   * By setting a result to this event, the device will resume processing on this event
   * and reply back to the client.
   *
   * @param r
   */
  def setResult(r:EventResult) {
    _res= Some(r)
    _waitEvent match {
      case Some(w) =>
        try {
          w.resumeOnEventResult(r)
        } finally {
          _dev.releaseEvent(w)
          _waitEvent=None
        }
      case _ =>
    }
  }

  /**
   * @return
   */
  def device() = _dev

  /**
   * @return
   */
  def result() = _res

  /**
   * @return
   */
  def id() = _id

  /**
   * Override and do something special when this event is destroyed.
   */
  def destroy() {  }

  /**
   * @param w
   */
  protected[device] def bindWait(w:WaitEvent) {
    _waitEvent=Some(w)
  }

}

