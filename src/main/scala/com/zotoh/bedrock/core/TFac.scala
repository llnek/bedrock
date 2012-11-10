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

package com.zotoh.bedrock.core

import java.util.concurrent.ThreadFactory
import java.lang.System._

import com.zotoh.fwk.util.SeqNumGen._
import com.zotoh.fwk.util.StrUte._

object TFac {}

/**
 * The default thread factory - from javasoft code.  The reason why
 * we cloned this is so that we can control how the thread-id is
 * traced out. (we want some meaninful thread name).
 *
 * @author kenl
 */
sealed case class TFac(var id:String) extends ThreadFactory {

  private val _group = getSecurityManager() match {
    case sm:SecurityManager => sm.getThreadGroup()
    case _ => Thread.currentThread().getThreadGroup()
  }
  private val _pfx=nsb(id)

  /* (non-Javadoc)
   * @see java.util.concurrent.ThreadFactory#newThread(java.lang.Runnable)
   */
  def newThread(r:Runnable) = {
    val t = new Thread(_group, r, mkTname(), 0)
    t.setPriority(Thread.NORM_PRIORITY)
    t.setDaemon(false)
    t
  }

  private def mkTname() = {    _pfx + nextInt()  }

}
