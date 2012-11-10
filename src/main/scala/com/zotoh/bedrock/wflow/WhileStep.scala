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
class WhileStep(s:FlowStep, a:While) extends ConditionalStep(s,a) {

  private var _body:FlowStep=null

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.wflow.Step#eval(com.zotoh.bedrock.core.Job)
   */
  def eval(j:Job) = {
    var rc:FlowStep= this
    val c= popCArg() // data from previous async call ?
    if ( ! doTest(j)) {
      tlog().debug("WhileStep: test-condition == false")
      rc= nextStep()
      c match { case Some(p) if rc != null => rc.pushCArg(p) ; case _ => }
      realize()
    } else {
      tlog().debug("WhileStep: looping - eval body")
      c match { case Some(p) => _body.pushCArg(p) ; case _ => }
      var s=_body.eval(j)
      if (s != this) { _body=s }
    }
    rc
  }

  /**
   * @param body
   * @return
   */
  def withBody(body:FlowStep) = {
    _body=body; this
  }

}
