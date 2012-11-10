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

/**
 * The AppDelegate is the interface between the runtime and your application.  For example, when an event is spawned and a
 * job is created, the runtime maybe able to determine what processor should handle the job via device configurations.  However,
 * if that is not the case, the runtime will call upon the delegate to decide on how to handle the job.
 *
 * @see com.zotoh.bedrock.impl.DefautDelegate
 *
 * @author kenl
 */
abstract class AppDelegate protected(private val _engine:AppEngine) {

  private def ilog() {  _log=getLogger(classOf[AppDelegate]) }
  @transient private var _log:Logger= null
  def tlog() = {  if(_log==null) ilog(); _log }

  /**
   * Perform any cleanup here.
   */
  protected[core] def onShutdown() = {}

  /**
   * Return a new processor which will own & work on this job.
   *
   * @param j
   * @return
   */
  def newProcess( j:Job):Pipeline

  /**
   * @return
   */
  def engine() =  _engine

}
