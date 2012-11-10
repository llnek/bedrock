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

import java.util.{Date=>JDate,Properties=>JPS,ResourceBundle}
import org.json.{JSONObject=>JSNO}

import com.zotoh.fwk.util.{CmdLineMust,CmdLineQ,CmdLineSeq}


/**
 * Sets up a repeatable timer.
 *
 * The set of properties:
 *
 * <b>intervalsecs</b>
 * The number of seconds between each trigger, default is 60.
 *
 * @see com.zotoh.bedrock.device.XXXTimer
 *
 * @author kenl
 *
 */
class RepeatingTimer(devMgr:DeviceMgr) extends XXXTimer(devMgr)  {

  private var _intervalMillis= 0L

  /**
   * @return
   */
  def intervalMillis() = _intervalMillis

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.XXXTimer#inizWithQuirks(org.json.JSONObject)
   */
  override def inizWithQuirks(pps:JSNO) {
    super.inizWithQuirks(pps)
    val intv= pps.optInt(XXXTimer.PSTR_INTVSECS, 0)
    tstPosIntArg("interval-secs", intv)
    _intervalMillis= 1000L * intv
  }

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.Poller#wakeup()
   */
  override def wakeup() {
    dispatch( new TimerEvent(true,this))
  }

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.Poller#schedule()
   */
  override def schedule() {
    when() match {
      case Some(w) =>
        scheduleRepeaterWhen( w, intervalMillis())
      case _ =>
        scheduleRepeater( delayMillis(), intervalMillis())
    }
  }

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.Device#supportsConfigMenu()
   */
  override def supportsConfigMenu() = true

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.Device#getCmdSeq(java.util.ResourceBundle, java.util.Properties)
   */
  override def getCmdSeq(rcb:ResourceBundle, pps:JPS) = {
    val q2= new CmdLineQ("delay", bundleStr(rcb, "cmd.delay.start"), "","2") {
      def onRespSetOut(a:String, p:JPS) = {
        p.put("delaysecs", asJObj(asInt(a,2)))
        ""
      }}
    val q1= new CmdLineMust("pintv", bundleStr(rcb, "cmd.repeat.intv"), "","60") {
      def onRespSetOut(a:String, p:JPS) = {
        p.put("intervalsecs", asJObj(asInt(a,60)))
        "delay"
      }}
    Some(new CmdLineSeq(super.getCmdSeq(rcb, pps),Array(q1,q2)) {
      def onStart() = q1.label()
    })
  }

}


