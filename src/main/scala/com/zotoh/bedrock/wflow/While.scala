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

object While {}

/**
 * @author kenl
 *
 */
class While( e:BoolExpr = null) extends Conditional(e) {

  private var _body:Activity=null 

  /**
   * @param expr
   * @param body
   */
  def this(expr:BoolExpr, body:Activity) {
    this(expr)
    _body=body
  }

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.wflow.Activity#reify(com.zotoh.bedrock.wflow.Step)
   */
  def reify(cur:FlowStep) = reifyWhile(cur, this)

  /**
   * @param body
   * @return
   */
  def withBody(body:Activity) = {
    _body=body; this
  }

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.wflow.Activity#realize(com.zotoh.bedrock.wflow.Step)
   */
  def realize(cur:FlowStep) {
    cur match {
      case w:WhileStep =>
        if (_body != null) {
          w.withBody(_body.reify(cur))
        }
        w.withTest(expr())
      }
    
    
  }

}
