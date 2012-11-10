package com.zotoh.bedrock.svc

import com.zotoh.bedrock.core.Vars.DT_ONESHOT
import com.zotoh.bedrock.device.{Device, DeviceMgr}

/**
 * Created with IntelliJ IDEA.
 * User: kenl
 * Date: 8/11/12
 * Time: 10:03 PM
 * To change this template use File | Settings | File Templates.
 */

class OnceTimerService(name:String) extends XXXTimerService(name,DT_ONESHOT) {

  def withDelay(secs:Int) = {
    super.setDelay(secs)
    this
  }

  def withWhen(when:String) = {
    super.setWhen(when)
    this
  }

  def withProcessor(cz:String) = {
    super.setProc(cz)
    this
  }

  def withHandler( h: Device.EventHandler ) = {
    super.setHandler(h)
    this
  }

  override def end = {
    super.end
  }


}
