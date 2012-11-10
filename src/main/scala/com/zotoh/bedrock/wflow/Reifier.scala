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


/**
 * @author kenl
 *
 */
object Reifier {

  /**
   * @return a Nihil Step which does nothing but indicates end of flow.
   */
  def reifyZero(f:Workflow) = new NihilStep(f)

  /**
   * @param cur
   * @param a
   * @return
   */
  def reifyAsyncWait(cur:FlowStep, a:AsyncWait) = {
    post_reify( new AsyncWaitStep(cur,a)).asInstanceOf[AsyncWaitStep]
  }

  /**
   * @param cur
   * @param a
   * @return
   */
  def reifyDelay(cur:FlowStep, a:Delay) = {
    post_reify( new DelayStep(cur,a)).asInstanceOf[DelayStep]
  }

  /**
   *
   * @param cur
   * @param a
   * @return
   */
  def reifyPTask(cur:FlowStep, a:PTask) = {
    post_reify( new PTaskStep(cur,a)).asInstanceOf[PTaskStep]
  }

  /**
   * @param cur
   * @param a
   * @return
   */
  def reifySwitch(cur:FlowStep, a:Switch) = {
    post_reify( new SwitchStep(cur,a)).asInstanceOf[SwitchStep]
  }

  /**
   * @param cur
   * @param a
   * @return
   */
  def reifyIf(cur:FlowStep, a:If) = {
    post_reify( new IfStep(cur,a)).asInstanceOf[IfStep]
  }

  /**
   * @param cur
   * @param a
   * @return
   */
  def reifyBlock(cur:FlowStep, a:Block) = {
    post_reify( new BlockStep(cur,a)).asInstanceOf[BlockStep]
  }

  /**
   * @param cur
   * @param a
   * @return
   */
  def reifySplit(cur:FlowStep, a:Split) = {
    post_reify( new SplitStep(cur,a)).asInstanceOf[SplitStep]
  }

  /**
   * @param cur
   * @param a
   * @return
   */
  def reifyOrJoin(cur:FlowStep, a:Or) = {
    post_reify( new OrStep(cur,a)).asInstanceOf[OrStep]
  }

  /**
   * @param cur
   * @param a
   * @return
   */
  def reifyNullJoin(cur:FlowStep, a:NullJoin) = {
    post_reify( new NullJoinStep(cur,a)).asInstanceOf[NullJoinStep]
  }

  /**
   * @param cur
   * @param a
   * @return
   */
  def reifyAndJoin(cur:FlowStep, a:And) = {
    post_reify( new AndStep(cur,a)).asInstanceOf[AndStep]
  }

  /**
   * @param cur
   * @param a
   * @return
   */
  def reifyWhile(cur:FlowStep, a:While) = {
    post_reify(new WhileStep(cur,a)).asInstanceOf[WhileStep]
  }

  /**
   * @param cur
   * @param a
   * @return
   */
  def reifyFor(cur:FlowStep, a:For) = {
    post_reify(new ForStep(cur,a)).asInstanceOf[ForStep]
  }

  private def post_reify(s:FlowStep) = {
    s.realize(); s
  }

}
