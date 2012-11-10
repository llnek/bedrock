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

import com.zotoh.fwk.util.CoreUte._
import com.zotoh.bedrock.core.Job
import com.zotoh.bedrock.wflow.Reifier._

/**
 * A For is treated sort of like a while with the test-condition being (i < upperlimit).
 *
 * @author kenl
 *
 */
class For() extends While {

  private var _loopCntr:ForLoopCountExpr=null

  /**
   * @param body
   */
  def this(body:Activity) { this(); withBody(body) }

  /**
   * @param c
   * @return
   */
  def withLoopCount(c:ForLoopCountExpr) = {
    _loopCntr=c; this
  }

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.wflow.While#reify(com.zotoh.bedrock.wflow.Step)
   */
  override def reify(cur:FlowStep) = reifyFor(cur, this)

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.wflow.While#realize(com.zotoh.bedrock.wflow.FlowStep)
   */
  override def realize(cur:FlowStep) {
    cur match {
      case s:ForStep =>
        super.realize(s)
        s.withTest(new ForLoopExpr(s, _loopCntr))
    }
    
  }
}

/**
 * @author kenl
 *
 */
class ForLoopExpr(private val _step:FlowStep, private val _cnt:ForLoopCountExpr) extends BoolExpr {
  private var _started= false
  private var _loop=0

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.wflow.BoolExpr#eval(com.zotoh.bedrock.core.Job)
   */
  def eval(j:Job) = {
    try {
      if (!_started) {
        _loop=_cnt.eval(j)
        _started=true
      }
      _step.tlog().debug("ForLoopExpr: loop {}", asJObj(_loop))
      _loop > 0
    }
    finally {
      _loop -= 1
    }
  }
}


