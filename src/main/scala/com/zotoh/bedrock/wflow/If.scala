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

import com.zotoh.bedrock.wflow.Reifier._

object If {
  def apply(e:BoolExpr, thenCode:Activity, elseCode:Activity) = new If(e,thenCode,elseCode)
  def apply(e:BoolExpr, thenCode:Activity) = new If(e,thenCode)
}

/**
 * @author kenl
 *
 */
class If( e:BoolExpr=null) extends Conditional(e) {

  private var _then:Activity=null
  private var _else:Activity=null

  /**
   * @param e
   * @param thenCode
   * @param elseCode
   */
  def this(e:BoolExpr, thenCode:Activity, elseCode:Activity) {
    this(e)
    _then= thenCode
    _else= elseCode
  }

  /**
   * @param e
   * @param thenCode
   */
  def this(e:BoolExpr, thenCode:Activity) {
    this(e, thenCode, null)
  }

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.wflow.Activity#reify(com.zotoh.bedrock.wflow.Step)
   */
  def reify(cur:FlowStep) = reifyIf(cur, this)

  /**
   * @param elseCode
   * @return
   */
  def withElse(elseCode:Activity) = {
    _else=elseCode; this
  }

  /**
   * @param thenCode
   * @return
   */
  def withThen(thenCode:Activity) = {
    _then=thenCode; this
  }

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.wflow.Activity#realize(com.zotoh.bedrock.wflow.Step)
   */
  def realize(cur:FlowStep) {
    val next= cur.nextStep()
    cur match {
      case s:IfStep =>
        s.withElse( if(_else ==null) next else _else.reify(next) )
        s.withThen( _then.reify(next))
        s.withTest( expr())
    }
        
  }

}
