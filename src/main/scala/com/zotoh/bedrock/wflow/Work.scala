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

package com.zotoh.bedrock.wflow

import com.zotoh.bedrock.core.Job
import com.zotoh.fwk.util.LoggerFactory._
import com.zotoh.fwk.util.Logger

/**
 * @author kenl
 *
 */
abstract class Work {

  private def ilog() { _log=getLogger(classOf[Work]) }
  @transient private var _log:Logger=null
  def tlog() = { if(_log==null) ilog(); _log }
  
  private var _curStep:FlowStep=null
  private var _result:Activity=null

  /**
   * @param job
   * @return
   * @throws Exception
   */
  def perform(cur:FlowStep, j:Job) = {
    _curStep=cur
    _result=null
    cur.popCArg() match {
      case Some(p) => eval2(j, Some(p))
      case _ => eval(j)
    }
    _result
  }

  /**
   * @param job
   * @param arg
   * @throws Exception
   */
  def eval2(j:Job, arg:Option[Any]) {
    tlog().debug("eval(2) called!")
  }

  def eval(j:Job):Unit

  /**
   * @param a
   */
  protected def setResult(a:Activity) { _result=a }

  /**
   * @return
   */
  protected def curStep() = _curStep

}
