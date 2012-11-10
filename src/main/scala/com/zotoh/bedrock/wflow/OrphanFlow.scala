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

import com.zotoh.fwk.net.HTTPStatus.NOT_IMPLEMENTED
import com.zotoh.bedrock.device.{Event,HttpEvent,HttpEventResult}
import com.zotoh.bedrock.core.Job

/**
 * Deal with jobs which are not handled by any processor.
 * (Internal use only).
 *
 * @author kenl
 */
sealed class OrphanFlow( j:Job) extends Workflow(j) {

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.wflow.MiniWFlow#onStart()
   */
  override def onStart() = {
    val me=this
    PTask().withWork( new Work() {
      override def eval(j:Job) {
        job.event() match {
          case e:HttpEvent => me.handle(e)
          case _ =>
        }
      }
    })
  }

  private def handle(ev:HttpEvent) {
    val res= HttpEventResult().setStatus(NOT_IMPLEMENTED)
//    res.setErrorMsg("Service not implemented")
    ev.setResult(res)
  }

}
