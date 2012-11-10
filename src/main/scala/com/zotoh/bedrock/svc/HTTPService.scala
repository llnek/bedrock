package com.zotoh.bedrock.svc

import com.zotoh.bedrock.device.HttpIOTrait._
import com.zotoh.bedrock.device.{Device, DeviceMgr}
import com.zotoh.bedrock.core.Vars.DT_HTTP



/**
 * Created with IntelliJ IDEA.
 * User: kenl
 * Date: 8/11/12
 * Time: 10:18 PM
 * To change this template use File | Settings | File Templates.
 */

class HTTPService(name:String) extends DeviceService(name,DT_HTTP) {

  def withHost(host:String) = {
    super.withAttrString(PSTR_HOST, host)
    this
  }

  def withPort(port:Int) = {
    super.withAttrInt(PSTR_PORT,port)
    this
  }

  def withServerKey(key:String) = {
    super.withAttrString("type", DT_HTTPS)
    super.withAttrString(PSTR_KEY, key)
    this
  }

  def withServerPwd(pwd:String) = {
    super.withAttrString(PSTR_PWD, pwd)
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
