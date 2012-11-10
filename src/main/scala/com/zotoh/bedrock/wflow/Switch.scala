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

import scala.collection.mutable.HashMap

import com.zotoh.bedrock.wflow.Reifier._

/**
 * @author kenl
 *
 */
class Switch(private var _expr:SwitchChoiceExpr=null) extends Activity {

  private val _choices= HashMap[Any,Activity]() 
  private var _def:Activity=null

  /**
   * @param e
   * @return
   */
  def withExpr(e:SwitchChoiceExpr) = { _expr=e;  this }

  /**
   * @param matcher
   * @param body
   * @return
   */
  def withChoice(matcher:Any, body:Activity) = {
    _choices += Tuple2(matcher, body)
     this
  }

  /**
   * @param a
   * @return
   */
  def withDef(a:Activity) = {
    _def=a
     this
  }

  override def reify(cur:FlowStep) = reifySwitch(cur, this)

  def realize(cur:FlowStep)  {
    val next= cur.nextStep()
    cur match {
      case s:SwitchStep =>
        val t= HashMap[Any,FlowStep]()
        _choices.foreach { (en) =>
          t += Tuple2(en._1, en._2.reify(next))
        }
        s.withChoices(t.toMap)
        if (_def != null) {
          s.withDef( _def.reify(next))
        }
        s.withExpr(_expr)
    }
        
  }

}
