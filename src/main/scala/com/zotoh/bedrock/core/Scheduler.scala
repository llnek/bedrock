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

import java.util.{Properties,Timer=>JTimer,TimerTask=>JTTask}
import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.util.StrUte._
import com.zotoh.fwk.util.LoggerFactory._
import com.zotoh.fwk.util.{Logger,CoreImplicits}

/**
 * The Scheduler manages a set of threadpools.  Applications can add more groups and pick and choose which processor
 * runs in which group.
 *
 * @author kenl
 */
class Scheduler protected[core](private val _engine:AppEngine) extends Vars with CoreImplicits {

  private def ilog() { _log=getLogger(classOf[Scheduler]) }
  @transient private var _log:Logger=null
  def tlog() = { if(_log==null) ilog(); _log }

  val EVENT_CORE= "#events#"
  val WAIT_CORE= "#wait#"
  val WORK_CORE= "#worker#"

  protected val _holdQ= HashMap[Long,Runnable]()
  protected val _runQ= HashMap[Long,Runnable]()
  protected val _parQ= HashMap[Long,Long]()
  protected val _cores= HashMap[String,TCore]()
  private var _timer:JTimer=null

  /**
   * @param core
   * @param w
   */
  def run(core:String, w:Runnable) {
    val cc = _cores.get( if (isEmpty(core)) WORK_CORE else core ) match {
      case Some(c) => c
      case _ =>
        tlog().warn("Scheduler: unknown core: {}", core)
        _cores(WORK_CORE)
    }
    preRun(w)
    cc.schedule(w)
  }

  def delay(w:Runnable, delayMillis:Long) {

    val cc = nsb( engine().module().xrefCore(w) )
    val me= this

    if (delayMillis == 0L) { _engine.scheduler().run(cc, w)  }
    else if (delayMillis < 0L) { hold(cc, w) }
    else {
      _engine.scheduler().addTimer(new JTTask() {
        def run() { me.wakeup(cc,w) }
      }, delayMillis)

      tlog().debug("Scheduler: delaying eval on core: {} for process: {}, wait-millis: {}" ,
        cc, w, asJObj(delayMillis))
    }

  }

  def hold(core:String, w:Runnable) {
    engine().module().xrefPID(w) match {
      case pid:Long if pid >= 0L =>  hold(pid,w)
      case _ =>
    }
  }

  /**
   * @param pid
   * @param w
   */
  def hold(pid:Long, w:Runnable) {
    _runQ.remove(pid)
    _holdQ.put(pid, w)
    tlog().debug("Scheduler: moved to pending wait, process: {}", w)
  }

  def wakeup(core:String,w:Runnable) {
    engine().module().xrefPID(w) match {
      case pid:Long if pid >= 0L =>  wakeAndRun(core, pid,w)
      case _ =>
    }
  }


  /**
   * @param core
   * @param pid
   * @param w
   */
  def wakeAndRun(core:String, pid:Long, w:Runnable) {
    _holdQ.remove(pid)
    _runQ.put(pid, w)
    run(core, w)
    tlog().debug("Scheduler: waking up process: {}", w)
  }

  /**
   * @param w
   */
  def reschedule(w:Runnable) {
    if (w != null) {
      tlog().debug("Scheduler: restarting runnable: {}" , w)
      run(WAIT_CORE, w)
    }
  }

  private def preRun(w:Runnable) {
    engine().module().xrefPID(w) match {
      case n:Long if n >= 0L  =>
        _holdQ.remove( n )
        _runQ += Tuple2( n, w)
      case _ =>
    }
  }

  /**
   * @return
   */
  def engine() = _engine

  /**
   * @param w
   */
  def run(w:Runnable) { run(  WORK_CORE,  w) }

  /**
   * @param id
   * @param threads
   */
  def addCore(id:String, threads:Int=2) {
    val c= TCore(id)
    _cores += Tuple2(id, c)
    c.start(threads)
  }

  protected[core] def iniz() {
    val ps= engine().quirks()
    val e=asInt(trim(ps.gets(TDS_EVENTS)), 2)
    val w=asInt( trim(ps.gets(TDS_WORK)), 4)
    val t= asInt( trim(ps.gets(TDS_WAIT)), 2)
    _timer= new JTimer("scheduler-timer", true)
    _cores.clear()
    addCore(EVENT_CORE, e)
    addCore(WAIT_CORE, t)
    addCore(WORK_CORE, w)
  }

  def addTimer(t:JTTask, delay:Long) {
    _timer.schedule(t, delay)
  }

}
