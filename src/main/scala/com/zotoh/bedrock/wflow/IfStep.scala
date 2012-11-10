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
class IfStep( s:FlowStep, a:If) extends ConditionalStep(s,a) {

  private var _then:FlowStep=null
  private var _else:FlowStep=null

  /**
   * @param s
   * @return
   */
  def withElse(s:FlowStep) = {
    _else=s; this
  }

  /**
   * @param s
   * @return
   */
  def withThen(s:FlowStep) = {
    _then=s; this
  }

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.wflow.Step#eval(com.zotoh.bedrock.core.Job)
   */
  def eval(j:Job) = {
    val c= popCArg()   // data pass back from previous async call?
    var b = doTest(j)
    tlog().debug("If: test {}", (if(b) "OK" else "FALSE"))
    val rc = if(b) _then else _else
    c match {
      case Some(p) if rc != null => rc.pushCArg(p)
      case _ =>
    }
    realize()
    rc
  }

}

