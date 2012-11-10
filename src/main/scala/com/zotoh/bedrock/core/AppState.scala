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
import com.zotoh.fwk.util.JSONUte._
import org.json.{JSONObject=>JSNO}


/**
 * Holds state information which may need to be made persistent in case of a Stateful processor.
 *
 * @author kenl
 */
class AppState (private val _pipe:Pipeline) {

  private def ilog() { _log=getLogger(classOf[AppState]) }
  @transient private var _log:Logger= null
  def tlog() = { if(_log==null) ilog(); _log }

  private var _trackObj:Option[Any]=None
  private var _keyObj:Option[Any]=None
  private var _json= newJSON()

  /**
   * @param tracker
   */
  def setTracker(tracker:Any) { _trackObj=Some(tracker) }

  /**
   * @return
   */
  def tracker():Option[Any] = _trackObj

  /**
   * @return
   */
  def hasKey() = { _keyObj != None }

  /**
   * @param key
   */
  def setKey(key:Any) { _keyObj= Some(key) }

  /**
   * @return
   */
  def key():Option[Any] = { _keyObj  }

  /**
   * @return
   */
  def root() = _json

  /**
   * @param obj
   */
  def setRoot(obj:JSNO) { _json=obj }

  /**
   * @return
   */
  def pipeline() = _pipe

}
