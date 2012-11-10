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

import scala.math._

import java.util.concurrent.{LinkedBlockingQueue,RejectedExecutionHandler}
import java.util.concurrent.{ThreadPoolExecutor,TimeUnit}

import com.zotoh.fwk.util.{LoggerFactory,Logger}
import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.util.StrUte._

object TCore {}

/**
 * A Thread pool.
 *
 * @author kenl
 */
sealed case class TCore(var name:String) extends RejectedExecutionHandler {

  private val serialVersionUID = 404521678153694367L

  private def ilog() {  _log=LoggerFactory.getLogger(classOf[TCore]) }
  @transient private var _log:Logger=null
  def tlog() = {  if(_log==null) ilog(); _log }

  private var _scd:ThreadPoolExecutor=null
  private val _id= nsb(name)

  /**
   * @param tds
   */
  def start(tds:Int = 0) { activate( max(2, tds) ) }

  /**
   * @param work
   */
  def schedule(work:Runnable) {
//    if (tlog().isDebugEnabled())
//      tlog().debug("TCore: about to run work: " + work) ;
    _scd.execute(work)
  }

  /* (non-Javadoc)
   * @see java.util.concurrent.RejectedExecutionHandler#rejectedExecution(java.lang.Runnable, java.util.concurrent.ThreadPoolExecutor)
   */
  def rejectedExecution(r:Runnable, x:ThreadPoolExecutor) {
    //TODO: deal with too much work for the core...
    tlog().warn("TCore: \"{}\" rejected work - threads/queue are max'ed out" , _id);
  }

  private def activate(tds:Int) {
    _scd = new ThreadPoolExecutor(tds, tds, 5000,
        TimeUnit.MILLISECONDS, new LinkedBlockingQueue[Runnable](),
        TFac(_id) , this )
    tlog().info("TCore: \"{}\" activated with threads = {}" , _id , asJObj(tds))
  }

}

