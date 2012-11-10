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

import scala.math._

import java.util.{Date,Timer=>JTimer,TimerTask=>JTTask}
import com.zotoh.fwk.util.CoreUte._
import org.json.{JSONObject=>JSNO}

/**
 * Base class for all polling devices.
 *
 * @author kenl
 */
abstract class Poller(devMgr:DeviceMgr) extends Device(devMgr) {

  private var _timer:JTimer= _

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.Device#inizWithProperties(org.json.JSONObject)
   */
  override def inizWithQuirks(pps:JSNO) {}

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.Device#onStart()
   */
  override def onStart() {
    _timer= new JTimer(true)
    schedule()
  }

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.Device#onStop()
   */
  protected def onStop() {
    if ( _timer != null) { _timer.cancel() }
    _timer= null
  }

  /**
   * @param w
   */
  protected def scheduleTriggerWhen(w:Date) {
    tstObjArg("date", w)
    if ( _timer != null) {
      _timer.schedule( mkTask(), w)
    }
  }

  /**
   * @param delay
   */
  protected def scheduleTrigger(delay:Long) {
    if ( _timer != null) {
      _timer.schedule( mkTask(), max( 0L, delay ))
    }
  }

  /**
   * @param w
   * @param interval
   */
  protected def scheduleRepeaterWhen(w:Date, interval:Long) {
    tstObjArg("when", w)
    if ( _timer != null) {
      _timer.schedule( mkTask(), w, interval)
    }
  }

  /**
   * @param delay
   * @param interval
   */
  protected def scheduleRepeater(delay:Long, interval:Long) {
    tstPosLongArg("repeat-interval", interval)
    tstNonNegLongArg("delay", delay)
    if ( _timer != null) {
      _timer.schedule( mkTask(), max(0L, delay), interval)
    }
  }

  /**
   *
   */
  protected def schedule():Unit

  /**
   *
   */
  protected def wakeup():Unit

  private def mkTask() = {
    val me=this
    new JTTask() {
      def run() {
        try {
          me.wakeup()
        } catch {
          case e => tlog().warnX("", Some(e))
        }
      }
    }
  }
}

