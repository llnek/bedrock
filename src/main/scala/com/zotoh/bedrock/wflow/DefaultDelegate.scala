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

import com.zotoh.bedrock.core.{AppDelegate,AppEngine,Job,Pipeline}

import com.zotoh.bedrock.wflow.Workflow._

/**
 * The default implementation of an application delegate.  If a device configuration
 * has no processor defined (to handle events from that device), the event will be
 * ignored as the runtime has no knowledge of how to handle the event.
 * Therefore, if your application relies on the default delegate, all the
 * devices should have processor defined as part of their configurations.
 *
 * @author kenl
 */
class FlowDelegate( e:AppEngine) extends AppDelegate(e) {

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.core.AppDelegate#newProcess(com.zotoh.bedrock.core.Job)
   */
  def newProcess(j:Job):Pipeline = XFLOW

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.core.AppDelegate#onShutdown()
   */
  override def onShutdown() {}

}

