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

import com.zotoh.bedrock.wflow.Reifier._

/**
 * @author kenl
 *
 */
class Split( private var _join:Join=null) extends Composite {

  protected var _theJoin:Join= null

  /**
   * @param a
   * @return
   */
  def addSplit(a:Activity) = {
    add(a); this
  }

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.wflow.Activity#reify(com.zotoh.bedrock.wflow.Step)
   */
  def reify(cur:FlowStep) = reifySplit(cur, this)

  /**
   * @param a
   * @return
   */
  def withJoin(a:Join) = {
    _join=a; this
  }

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.wflow.Activity#realize(com.zotoh.bedrock.wflow.Step)
   */
  def realize(cur:FlowStep) {
    if ( _join!=null) {
      _join.withBranches( size())
      _theJoin = _join
    } else {
      _theJoin= new NullJoin()
    }
    var s = _theJoin.reify(cur.nextStep())
    cur match {
      case ss:SplitStep =>
        _theJoin match {
          case n:NullJoin => ss.fallThrough()
          case _ =>
        }
        ss.withBranches(reifyInnerSteps(s))
      case _ =>
    }

  }

}

/**
 * @author kenl
 *
 */
class NullJoin extends Join {

  def reify(cur:FlowStep) = reifyNullJoin(cur, this)

  def realize(cur:FlowStep) {}
}

/**
 * @author kenl
 *
 */
class NullJoinStep(s:FlowStep,a:Join) extends JoinStep(s,a) {

  override def eval(j:Job) = null

}
