/*??
 * COPYRIGHT (C) 2012 CHERIMOIA LLC. ALL RIGHTS RESERVED.
 *
 * THIS IS FREE SOFTWARE; YOU CAN REDISTRIBUTE IT AND/OR
 * MODIFY IT UNDER THE TERMS OF THE APACHE LICENSimport static com.zotoh.core.util.LoggerFactory.getLogger;

E,
 * VERSION 2.0 (THE "LICENSE").
 *
 * THIS LIBRARY IS DISTRIBUTED IN THE HOPE THAT IT WILL BE USEFUL,
 * BUT WITHOUT ANY WAimport com.zotoh.core.util.Logger;
import com.zotoh.bedrock.core.DeviceFactory;
RRANTY; WITHOUT EVEN THE IMPLIED WARRANTY OF
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

import com.zotoh.bedrock.core.{AppEngine,Job,Pipeline,AppState}
import com.zotoh.fwk.util.{SeqNumGen}
import com.zotoh.fwk.util.CoreUte._
import com.zotoh.bedrock.wflow.Reifier._



object Workflow {
  val XFLOW= new Workflow() {
    override def onStart() = new Nihil()
  }
}

/**
 * @author kenl
 *
 */
abstract class Workflow protected(private val _theJob:Job) extends Pipeline {

  private var _active:Boolean=false
  private val _pid= nextPID()
  private var _state:AppState=null

  /**
   * @return
   */
  override def engineQuirks() = job().engine().quirks()

  private def this() {
    this(null)
  }


  /**
   * @return
   */
  override def pid() = _pid

  /**
   * @return
   */
  override def engine() = job().engine()

  /**
   * @return
   */
  override def curState() = if (_state == null) None else Some(_state) 

  /**
   * @return
   */
  override def isActive() = _active

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.core.Pipeline#getJob()
   */
  override def job() = _theJob

  /**
   * @return
   */
  protected[wflow] def nextAID() = SeqNumGen.next()

  /**
   * @return
   */
  protected def mkState() =     new AppState(this)
  

  /**
   *
   */
  protected def onEnd() = {}

  /**
   * @param e
   * @return
   */
  protected[wflow] def onError(e:Exception):Activity =  {
    tlog().errorX("", Some(e))
    null
  }

  /**
   * @return
   */
  protected def onStart():Activity

  /**
   *
   */
  protected def preStart() {}

  /**
   *
   */
  override def start() {
    tlog().debug("Workflow: {} => pid : {} => starting" , toString() , asJObj(pid()))
    val s1= reifyZero( this)

    preStart()
    mkState()

    val s= onStart() match {
      case n:Nihil => reifyZero(this)
      case null => reifyZero(this)
      case a1:Activity => a1.reify(s1)
    }

    try {
      engine().scheduler().run("", s)
    } finally {
      _active=true
    }
  }

  /**
   *
   */
  override def stop() {
    try {
      onEnd()
    } catch {
      case e:Throwable => tlog().errorX("", Some(e))
    }
    tlog().debug("Workflow: {} => end" , toString )
  }

  private def nextPID() = SeqNumGen.next()

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  override def toString() = getClass().getSimpleName() + "(" + _pid + ")"

}
