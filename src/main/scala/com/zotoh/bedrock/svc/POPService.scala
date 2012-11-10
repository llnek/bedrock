package com.zotoh.bedrock.svc

import com.zotoh.bedrock.device.{Device, DeviceMgr}
import com.zotoh.bedrock.device.PopIO._
import com.zotoh.bedrock.core.Vars.DT_POP3


/**
 * Created with IntelliJ IDEA.
 * User: kenl
 * Date: 8/11/12
 * Time: 10:15 PM
 * To change this template use File | Settings | File Templates.
 */

class POPService(name:String) extends XXXTimerService(name,DT_POP3) {

  def withHost(host:String) = {
    super.withAttrString(PSTR_HOST,host)
    this
  }

  def withPort(port:Int) = {
    super.withAttrInt(PSTR_PORT,port)
    this
  }

  def withUser(user:String) = {
    super.withAttrString(PSTR_USER,user)
    this
  }

  def withPwd(pwd:String) = {
    super.withAttrString(PSTR_PWD,pwd)
    this
  }

  def withDelay(secs:Int) = {
    super.setDelay(secs)
    this
  }

  def withWhen(when:String) = {
    super.setWhen(when)
    this
  }

  def withInterval(secs:Int) = {
    super.setInterval(secs)
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
