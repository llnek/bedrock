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

/**
 * @author kenl
 *
 */
case class Delay(private var _delayMillis:Long=0L) extends Activity {

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.wflow.Activity#reify(com.zotoh.bedrock.wflow.Step)
   */
  override def reify(cur:FlowStep) = reifyDelay(cur, this)

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.wflow.Activity#realize(com.zotoh.bedrock.wflow.Step)
   */
  override def realize(cur:FlowStep) {
    cur match {
      case s:DelayStep => s.withDelay(_delayMillis)
    }
    
  }

  /**
   * @return
   */
  def delayMillis() = _delayMillis

  /**
   * @param millis
   * @return
   */
  def withDelay(millis:Long) = {
    _delayMillis=millis; this
  }

}

