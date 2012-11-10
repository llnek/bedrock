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

import com.zotoh.bedrock.core.{Pipeline, Job, AppEngine, Module}
import com.zotoh.bedrock.device.Device
import com.zotoh.fwk.util.MetaUte._
import scala.Some
import com.zotoh.fwk.util.LoggerFactory._
import scala.Some
import com.zotoh.fwk.util.Logger
import com.zotoh.fwk.util.StrUte._
import scala.Some
import scala.Tuple2
import java.util.{TimerTask=>JTTask}
import com.zotoh.fwk.util.CoreUte._
import scala.Some
import scala.Tuple2

/**
 * @author kenl
 *
 */
sealed class FlowModule() extends Module {

  private def ilog() { _log=getLogger(classOf[FlowModule]) }
  @transient private var _log:Logger = null
  def tlog() = {  if (_log==null) ilog(); _log  }

  private var _engine:AppEngine= _



  private def isNil(f:Pipeline) = {
    f==null || f == Workflow.XFLOW
  }

  /**
   * @return
   */
  def dftDelegateClass() = "com.zotoh.bedrock.wflow.FlowDelegate"

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.core.Module#getShortName()
   */
  def shortName() = "Flow"

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.core.Module#getName()
   */
  def name() = "Workflow"


  def newPipeline(cz:String, j:Job) = {

    val rc= try {
      Some(loadClass(cz).getConstructor(classOf[Job]).newInstance(j).asInstanceOf[Workflow] )
    } catch {
      case e => tlog().warnX("", Some(e)); None
    }
    rc
  }

  def newPipeline = Some(Workflow.XFLOW)

  def onCreate(v:Device, sys:Boolean, j:Job) {

    val g= _engine.delegate()
    var f:Pipeline = if (sys) {
      new BuiltinFlow(j)
    } else {
      v.pipeline(j) match { case Some(x) => x; case _ => null }
    }
    if (isNil(f)) { f= g.newProcess(j) }
    if (isNil(f)) {
      f=handleOrphansAsFlow(j)
    }
    if (! isNil(f)) {
      f.start()
    }
  }

  private def handleOrphansAsFlow(j:Job) = {
    new OrphanFlow(j)
  }

  def xrefCore(w:Runnable) = {
    w match {
      case s:FlowStep => s.core()
      case _ => ""
    }
  }

  def xrefPID(w:Runnable) = {
    w match {
      case s:FlowStep => s.pid()
      case _ => -1L
    }
  }

  def bind(eng:AppEngine) {
    _engine=eng
  }

}
