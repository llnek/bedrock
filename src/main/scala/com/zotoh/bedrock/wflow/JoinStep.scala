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
import java.util.concurrent.atomic.AtomicInteger


/**
 * @author kenl
 *
 */
abstract class JoinStep protected( s:FlowStep, a:Join) extends FlowStep(s,a) {

  protected var _body:FlowStep=null
  private var _branches=0
  protected var _cntr=new AtomicInteger()

  /**
   * @param body
   * @return
   */
  def withBody(body:FlowStep) = {
    _body=body; this
  }

  /**
   * @param n
   * @return
   */
  def withBranches(n:Int) = {
    _branches=n; this
  }

  /**
   * @return
   */
  def size() = _branches

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.wflow.FlowStep#postRealize()
   */
  override def postRealize() = { _cntr.set(0); this }

}
