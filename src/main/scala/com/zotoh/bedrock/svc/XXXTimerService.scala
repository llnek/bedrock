package com.zotoh.bedrock.svc

import com.zotoh.bedrock.device.XXXTimer._
import com.zotoh.bedrock.device.DeviceMgr

/**
 * Created with IntelliJ IDEA.
 * User: kenl
 * Date: 8/11/12
 * Time: 10:22 PM
 * To change this template use File | Settings | File Templates.
 */

abstract class XXXTimerService(name:String,kind:String) extends DeviceService(name,kind) {

  protected def setDelay(secs:Int) {
    super.withAttrInt(PSTR_DELAYSECS, secs)
  }

  protected def setInterval(secs:Int) {
    super.withAttrInt(PSTR_INTVSECS, secs)
  }

  protected def setWhen(when:String) {
    super.withAttrString(PSTR_WHEN, when)
  }

}
