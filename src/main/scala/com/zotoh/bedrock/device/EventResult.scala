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
import com.zotoh.fwk.util.Logger


/**
 * Base class for results which are to be sent back to the client.
 *
 * @author kenl
 */
abstract class EventResult protected() {

  private def ilog() { _log=getLogger(classOf[EventResult]) }
  private val serialVersionUID= -874578359743L
  @transient private var _log:Logger=null
  def tlog() = { if(_log==null) ilog(); _log }
  private var _hasError=false

  /**
   * @return
   */
  def hasError() = _hasError


  /**
   * @param b
   */
  protected def setError(b:Boolean) { _hasError=b  }

}
