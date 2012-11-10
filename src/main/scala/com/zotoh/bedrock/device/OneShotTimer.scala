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

import java.util.{Date=>JDate,Properties=>JPS,ResourceBundle}

import com.zotoh.fwk.util.{CmdLineQ,CmdLineSeq}
import com.zotoh.fwk.util.CoreUte._

/**
 * A timer which only fires once.
 *
 * The set of properties:
 *
 * @see com.zotoh.bedrock.device.XXXTimer
 *
 * @author kenl
 *
 */
class OneShotTimer(devMgr:DeviceMgr) extends XXXTimer(devMgr) {

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.Poller#wakeup()
   */
  override def wakeup() {
    try {
      dispatch( new TimerEvent( false,this))
    } finally {
      this ! "stop"
    }
  }

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.Poller#schedule()
   */
  override def schedule() {
    when() match {
      case Some(w) => scheduleTriggerWhen(w)
      case _ => scheduleTrigger( delayMillis() )
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
    val q1= new CmdLineQ("delay", bundleStr(rcb,"cmd.delay.start"), "","0") {
      def onRespSetOut(a:String, p:JPS) = {
        p.put("delaysecs", asJObj(asInt(a,0)))
        ""
      }}
    Some(new CmdLineSeq(super.getCmdSeq(rcb, pps),Array(q1)) {
      override def onStart() = q1.label()
    })
  }

}

