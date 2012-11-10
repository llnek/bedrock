package com.zotoh.bedrock.svc

import com.zotoh.bedrock.device.{Device, DeviceMgr, FeedIO}
import com.zotoh.bedrock.core.Vars.DT_ATOM

import com.zotoh.fwk.util.JSONUte._

/**
 * Created with IntelliJ IDEA.
 * User: kenl
 * Date: 8/11/12
 * Time: 7:23 PM
 * To change this template use File | Settings | File Templates.
 */

class RSSAtomService(name:String) extends XXXTimerService(name, DT_ATOM) {
  private val _urls= newJSA()

  def withUrl(url:String) = {
    _urls.put(url)
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
    _ps.put("urls", FeedIO.PSTR_URLS)
    super.end
  }

}
