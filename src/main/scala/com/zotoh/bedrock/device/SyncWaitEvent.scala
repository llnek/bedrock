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

import com.zotoh.fwk.util.CoreUte._

/**
 * Blocks thread until a result is ready.  This is used in cases where NIO is not
 * wanted - used in conjuction with Apache http IO.
 *
 * @author kenl
 */
class SyncWaitEvent(ev:Event) extends WaitEvent(ev) {

  /**
   * The number of milli-seconds to wait.
   *
   * @param millisecs in millisecs.
   */
  def timeoutMillis(millisecs:Long) = {

    tlog().debug("WaitEvent.timeout() - taking timeout (msecs) : {}" , asJObj(millisecs))

    this.synchronized {
      try  {
        this.wait(millisecs)
      } catch {
        case e:InterruptedException =>
          tlog().warnX("WaitEvent interrupted", Some(e))
      }
    }

    this
  }

  /**
   * The number of seconds to wait.
   *
   * @param secs interval in secs.
   */
  def timeoutSecs(secs:Int) = timeoutMillis( 1000L * secs)

  /**
   * Undo the block, and continue.
   */
  def resume() = {
    val s= toString()
    tlog().debug("SyncWaitEvent: {}.resume()" , s)

    this.synchronized {
      this.notifyAll()
    }

    tlog().debug("SyncWaitEvent: {}.continue()" , s )
    this
  }

  /**
   * Continue and this is the result.
   *
   * @param obj the result.
   */
  def resumeOnEventResult(obj:EventResult) = {
    setEventResult(obj)
    resume()
  }

}
