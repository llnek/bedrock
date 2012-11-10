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

package com.zotoh.bedrock.device

import java.util.{Timer=>JTimer,TimerTask=>JTTask}
import com.zotoh.fwk.util.CoreUte._

/**
 * For Asynchronous event handling.  This class wraps the actual event and will <b>wait</b> for the downstream
 * application until the response is ready.  In case the application takes too long to respond, a timer is set and a
 * time out will occur upon expiry.
 *
 * @author kenl
 */
class AsyncWaitEvent(ev:Event,private val _trigger:AsyncWaitTrigger) extends WaitEvent(ev) {

  private var _timer:JTimer = null

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.WaitEvent#resumeOnEventResult(com.zotoh.bedrock.device.EventResult)
   */
  override def resumeOnEventResult(res:EventResult) {
    if(_timer!=null) { _timer.cancel() }
    _timer=null
    innerEvent().device().releaseEvent(this)
    setEventResult(res)
    _trigger.resumeWithResult(res)
  }

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.WaitEvent#timeoutMillis(long)
   */
  override def timeoutMillis(millisecs:Long) = {
    _timer = new JTimer(true)
    _timer.schedule(
            new JTTask() {
              def run() { onExpiry() }
            },
      millisecs)
    this
  }


  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.WaitEvent#timeoutSecs(int)
   */
  override def timeoutSecs(secs:Int) = timeoutMillis(1000L * secs)

  private def onExpiry() {
    innerEvent().device().releaseEvent(this)
    _timer=null
    _trigger.resumeWithError()
  }

}

