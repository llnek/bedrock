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

import com.zotoh.bedrock.device.Event
import com.zotoh.bedrock.core.Job



object AdhocFlow {

}

/**
 * Internal user only.
 *
 * @author kenl
 *
 */
class AdhocFlow( j:Job) extends Workflow(j) {
  /* (non-Javadoc)
   * @see com.zotoh.bedrock.wflow.MiniWFlow#onStart()
   */
  override def onStart() = {
    val me=this
    PTask().withWork( new Work() {
      override def eval(j:Job) { 
        j.event() match {
          case Some(e) =>
          case _ =>
        }
        
        // this will fail!!!
//        _cb.handleEvent( _cb.getEventType().cast( job.getEvent()) )
    }})
  }

}
