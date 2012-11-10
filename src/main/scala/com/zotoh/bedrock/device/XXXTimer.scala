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

import java.util.{Date=>JDate}
import org.json.{JSONObject=>JSNO}

import com.zotoh.fwk.util.DateUte._
import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.util.StrUte._

object XXXTimer {
  val PSTR_INTVSECS= "intervalsecs"
  val PSTR_DELAYSECS= "delaysecs"
  val PSTR_WHEN= "when"
}

/**
 * Base class for timer devices.
 *
 * The set of properties:
 *
 * <b>delaysecs</>
 * The activation of this timer is delayed by this value, default is 0
 * <b>when</>
 * The timer is activated on a specific date or datetime.  There are only 2 accepted format.
 * (1) yyyyMMdd , e.g. 20121223
 * (2) yyyyMMddTHH:mm:ss , e.g. 20120721T23:13:54
 *
 * @see com.zotoh.bedrock.device.Device
 *
 * @author kenl
 *
 */
abstract class XXXTimer(devMgr:DeviceMgr) extends Poller(devMgr)  {

  private var _when:Option[JDate] = None
  private var _delayMillis:Long =0L

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.Poller#inizWithProperties(org.json.JSONObject)
   */
  override def inizWithQuirks(pps:JSNO) {
    super.inizWithQuirks(pps)
    // we force a delay, dont really want it to start too early
    val delay= max( pps.optInt(XXXTimer.PSTR_DELAYSECS, 1 ), 1 )
    val when= trim( pps.optString(XXXTimer.PSTR_WHEN))
    if (! isEmpty(when)) {
      val fmt = if ( when.indexOf(":") > 0) {
        "yyyyMMdd'T'HH:mm:ss"
      } else {
        "yyyyMMdd"
      }
      parseDate(when, fmt) match { case d:JDate => setWhen(d) ; case _ => }
    }
    else
    if (delay > 0) {
      setDelayMillis(1000L * delay )
    }
  }

  /**
   * @return
   */
  protected def delayMillis() = _delayMillis

  /**
   * @return
   */
  protected def when() = _when

  /**
   * @param d
   */
  protected def setDelayMillis(d:Long) {
    tstNonNegLongArg("delay-millis", d)
    _delayMillis=d
  }

  /**
   * @param d
   */
  protected def setWhen(d:JDate) = {
    _when=Some(d)
    this
  }

}

