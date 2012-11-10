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


/**
 * @author kenl
 *
 */
class BlockStep protected[wflow](var s:FlowStep, var a:Block) extends FlowStep(s,a) {

  protected var _steps:IterWrapper =null

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.wflow.Step#eval(com.zotoh.bedrock.core.Job)
   */
  def eval(j:Job) = {
    // data pass back from previous async call?
    val c= popCArg()
    if ( ! _steps.isEmpty()) {
      val n=_steps.next()
      c match { case Some(p) => n.pushCArg(p) ; case _ => }
      n.eval(j)
    } else {
      val rc=nextStep()
      c match { case Some(p) if rc != null => rc.pushCArg(p) ; case _ => }
      realize()
      rc
    }
  }

  /**
   * @param wrap
   * @return
   */
  def withSteps(wrap:IterWrapper) = {
    _steps=wrap; this
  }

}
