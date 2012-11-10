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


import com.zotoh.bedrock.core.Job

/**
 * @author kenl
 *
 */
class SwitchStep protected[wflow]( s:FlowStep, a:Activity) extends FlowStep(s,a) {

  private val _cs= HashMap[Any,FlowStep]()
  private var _expr:SwitchChoiceExpr =null
  private var _def:FlowStep=null

  /**
   * @param cs
   * @return
   */
  def withChoices(cs:Map[Any,FlowStep]) = {
    cs.foreach((t) => _cs += t)
    this
  }

  /**
   * @param def
   * @return
   */
  def withDef(dft:FlowStep) = {
    _def=dft; this
  }

  /**
   * @param e
   * @return
   */
  def withExpr(e:SwitchChoiceExpr) = {
    _expr=e; this
  }

  def choices() = _cs.toMap

  def dft() = _def

  override def eval(j:Job) = {
    val m= _expr.eval(j)
    val a= if(m==null) null else _cs.get(m)
    // if no match, try default?
    val ns= a match {
      case Some(f) => f
      case _ => _def
    }
    popCArg() match {
      case Some(p) if ns != null => ns.pushCArg(p)
      case _ =>
    }
    realize()
    ns
  }

}
