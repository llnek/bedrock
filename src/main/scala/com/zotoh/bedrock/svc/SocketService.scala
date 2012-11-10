package com.zotoh.bedrock.svc

import com.zotoh.bedrock.device.{Device, DeviceMgr}
import com.zotoh.bedrock.device.TcpIO._
import com.zotoh.bedrock.core.Vars.DT_TCP

/**
 * Created with IntelliJ IDEA.
 * User: kenl
 * Date: 8/11/12
 * Time: 10:16 PM
 * To change this template use File | Settings | File Templates.
 */

class SocketService(name:String) extends DeviceService(name, DT_TCP) {

  def withBacklog(bklog:Int) = {
    super.withAttrInt(PSTR_BACKLOG, bklog)
    this
  }

  def withHost(host:String) = {
    super.withAttrString(PSTR_HOST, host)
    this
  }

  def withPort(port:Int) = {
    super.withAttrInt(PSTR_PORT, port)
    this
  }

  def withTimeout(millis:Int) = {
    super.withAttrInt(PSTR_SOCTIMEOUT, millis)
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
