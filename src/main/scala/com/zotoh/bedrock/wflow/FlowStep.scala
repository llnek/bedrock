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

import com.zotoh.fwk.util.LoggerFactory._
import com.zotoh.fwk.util.Logger
import com.zotoh.fwk.util.StrUte._
import com.zotoh.bedrock.wflow.Reifier._
import com.zotoh.bedrock.core.Job

/**
 * @author kenl
 *
 */
abstract class FlowStep(private var _parent:Workflow) extends Runnable {

  private def ilog() { _log=getLogger(classOf[FlowStep]) }
  @transient private var _log:Logger=null
  def tlog() = { if(_log==null) ilog(); _log }

  private var _defn:Activity=null
  private var _nextPtr:FlowStep=null
  private var _core:String=""
  private var _pid:Long=0L
  private var _closure:Any=null

  /**
   * @param s
   * @param a
   */
  protected def this(s:FlowStep, a:Activity) {
    this( s.workflow())
    _nextPtr=s
    _defn=a
    _pid=_parent.nextAID()
  }

  /**
   * @return
   */
  def pid() = _pid

  /**
   * @param job
   * @return
   */
  def eval(j:Job):FlowStep

  /**
   *
   */
  def realize() = {
    template().realize(this)
    postRealize()
  }

  protected def postRealize() = this

  /**
   * @return
   */
  def nextStep() = _nextPtr

  /**
   * @return
   */
  def template() = _defn

  /**
   * @param c
   */
  def pushCArg(c:Any) = { _closure=c; this }

  /**
   * @return
   */
  def popCArg():Option[Any] = {
    val rc= if (_closure==null) None else Some(_closure)
    _closure=null
    rc
  }

  /**
   *
   */
  protected def clsCArg() = { _closure=null; this }

  /**
   * @param core
   */
  def setCore(core:String) = { _core= nsb(core); this }

  /**
   * @return
   */
  def core() = _core

  /**
   * @return
   */
  def workflow() =  _parent

  /* (non-Javadoc)
   * @see java.lang.Runnable#run()
   */
  override def run() {
    var err:Activity = null
    var rc:FlowStep=null
    var n= nextStep()
    var f= workflow()
    var job= f.job()

    try {
      rc=eval(job)
    }
    catch {
      case e:Exception => err=f.onError(e)
    }

    if (err != null) {
      if (n==null) { n= reifyZero(f) }
      rc= err.reify(n)
    }

    if (rc==null) {
      tlog().debug("FlowStep: rc==null => skip")
      // indicate skip, happens with joins
      return
    }

    val sc= f.engine().scheduler()
    val cn= rc.core()

    rc match {
      case n:NihilStep => f.stop()
      case n:AsyncWaitStep => sc.hold(cn, rc.nextStep())
      case ss:DelayStep => sc.delay(rc.nextStep(), ss.delayMillis())
      case _ => sc.run(cn, rc)
    }
  }

}
