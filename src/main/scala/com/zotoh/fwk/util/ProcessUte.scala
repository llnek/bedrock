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

package com.zotoh.fwk.util

import com.zotoh.fwk.util.LoggerFactory.getLogger
import com.zotoh.fwk.util.CoreUte._

import java.lang.management.ManagementFactory
import com.zotoh.fwk.util.StrUte._

/**
 * @author kenl
 *
 */
object ProcessUte {

  private val _log= getLogger(classOf[ProcessUte])
  def tlog() = _log

  /**
   * @param r
   */
  def asyncExec(r:Runnable) {
    if ( r != null) {
      val t=new Thread(r)
      t.setDaemon(true)
      t.start()
    }
  }

  /**
   * @param millisecs
   */
  def safeThreadWait(millisecs:Long) {
    try  {
      if ( millisecs > 0L) { Thread.sleep(millisecs) }
    }
    catch { case _ => }
  }

  /**
   * Block and wait on the object.
   *
   */
  def blockAndWait(lock:AnyRef, waitMillis:Long) {
    lock.synchronized {
      try {
        if (waitMillis > 0L) { lock.wait(waitMillis) } else { lock.wait() }
      }
      catch { case _ => }
    }
  }

  def pid() = {
    val ss = nsb( ManagementFactory.getRuntimeMXBean().getName()).split("@")
    if ( ! isNilSeq(ss)) ss(0) else "???"
  }

  /**
   *
   */
  def blockForever() {
    while (true) try {
      Thread.sleep(5000)
    }
    catch { case _ => }
  }

}

sealed class ProcessUte {}

