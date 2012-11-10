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

import com.zotoh.bedrock.core.FAsyncResumeToken

/**
 * @author kenl
 *
 */
class AsyncResumeToken(var s:FlowStep) extends FAsyncResumeToken[FlowStep](s) {

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.core.FAsyncResumeToken#resume(java.lang.Object)
   */
  override def resume(resultArg:Option[Any]) {
    val p=_proc.nextStep()
    p.pushCArg( if (resultArg.isEmpty) null else resultArg.get )
    p.workflow().engine().scheduler().reschedule(p)
  }


}

