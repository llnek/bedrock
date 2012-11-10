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

import scala.collection.mutable.HashMap

import com.zotoh.fwk.util.CoreUte._
import org.json.{JSONObject=>JSNO}

/**
 * @author kenl
 *
 */
trait NIOCB {
  def destroy():Unit
}


/**
 * Base class for all http related devices.  All http devices are by default *async* in nature.
 *
 * The set of properties:
 *
 * <b>soctoutmillis</b>
 * Socket timeout in milliseconds - default is 0
 * <b>workers</b>
 * No. of worker threads allocated for this device - default is 6.
 * <b>thresholdkb</b>
 * The upper limit on payload size before switching to file storage -default is 8Meg.
 * This is especially important when dealing with large http-payloads.
 * <b>waitmillis</b>
 * The time the device will *wait* for the downstream application to hand back a result.  If a timeout occurs, a timeout error will be sent back to
 * the client.  default is 5 mins.
 *
 * @see com.zotoh.bedrock.device.HttpIOTrait
 *
 * @author kenl
 */
abstract class BaseHttpIO protected(devMgr:DeviceMgr,ssl:Boolean) extends HttpIOTrait(devMgr,ssl) {

  private val _cbs= HashMap[Any,NIOCB]()
  private var _socTOutMillis=0L
  private var _thsHold=0L
  private var _waitMillis=0L

  private var _async=true
  private var _workers=0

  /**
   * @return
   */
  def isAsync() = _async

  /**
   * @return The upper limit on the size of payload before switching to use file based storage.
   */
  def threshold() = _thsHold


  /**
   * @return The number of milli-seconds to wait for the downstream application to come back with a result.
   */
  def waitMillis() = _waitMillis

  /**
   * @return Socket time out in millisecs.
   */
  def socetTimeoutMills() = _socTOutMillis

  /**
   * @return The number of inner worker threads for this IO device.
   */
  def workers() = _workers

  /**
   * @param key
   * @return
   */
  def cb(key:Any) = _cbs.get(key)

  /**
   * @param key
   * @param cb
   */
  def addCB(key:Any, cb:NIOCB) {
    _cbs += Tuple2(key, cb)
  }

  /**
   * @param key
   * @return
   */
  def removeCB(key:Any) = _cbs.remove(key)

  override def inizWithQuirks(pps:JSNO) {
    super.inizWithQuirks(pps)
    val socto= pps.optInt("soctoutmillis", 0)  // no timeout
    val nio =pps.optBoolean("async")
    val wks= pps.optInt("workers", 6)
    val thold= pps.optInt("thresholdkb", 8*1024)  // 8 Meg
    val wait= pps.optInt("waitmillis", 300000)   // 5 mins
    tstNonNegIntArg("socket-timeout-millis", socto)
    _socTOutMillis = 1L * socto

    tstNonNegIntArg("threshold", thold)
    _thsHold = 1024L * thold

    tstPosIntArg("wait-millis", wait)
    _waitMillis = 1L *wait

    tstPosIntArg("workers", wks)
    _workers = wks

    if (pps.has("async") && nio==false) {
      _async=false
    }
  }

}


