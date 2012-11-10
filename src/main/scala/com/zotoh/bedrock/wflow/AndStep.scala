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

/**
 * A "AND" join enforces that all bound activities must return before Join continues.
 *
 * @author kenl
 *
 */
class AndStep protected[wflow](var s:FlowStep, var a:And) extends JoinStep(s,a) {

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.wflow.Step#eval(com.zotoh.bedrock.core.Job)
   */
  def eval(j:Job) = eval_0(j)

  private def eval_0(j:Job) = {
    val c= popCArg()
    val n=_cntr.incrementAndGet()
    tlog().debug("AndStep: size={}, cntr={}, join={}", asJObj(size()), asJObj(_cntr), toString())

    if (n == size()) {
      // all branches have returned, proceed...
      var rc= if(_body == null) nextStep() else _body
      c match { case Some(p) if rc != null => rc.pushCArg(p); case _ => }
      realize()
      rc
    } else {
      null
    }
  }

}
