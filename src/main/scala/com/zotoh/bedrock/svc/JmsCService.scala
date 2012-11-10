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


package com.zotoh.bedrock.svc

import com.zotoh.bedrock.device.{Device, DeviceMgr, JmsIO}
import com.zotoh.bedrock.core.Vars.DT_JMS

/**
 * Created with IntelliJ IDEA.
 * User: kenl
 * Date: 8/11/12
 * Time: 8:43 PM
 * To change this template use File | Settings | File Templates.
 */

class JmsCService(name:String) extends DeviceService(name,DT_JMS) {

  def withContextFactory(fac:String) = {
    super.withAttrString(JmsIO.PSTR_CTXTFAC, fac)
    this
  }

  def withConnectionFactory(fac:String) = {
    super.withAttrString(JmsIO.PSTR_CONNFAC, fac)
    this
  }

  def withJNDIUser(uid:String) = {
    super.withAttrString(JmsIO.PSTR_JNDIUSER, uid)
    this
  }

  def withJNDIPwd(uid:String) = {
    super.withAttrString(JmsIO.PSTR_JNDIPWD, uid)
    this
  }

  def withJMSUser(uid:String) = {
    super.withAttrString(JmsIO.PSTR_JMSUSER, uid)
    this
  }

  def withJMSPwd(uid:String) = {
    super.withAttrString(JmsIO.PSTR_JMSPWD, uid)
    this
  }

  def withDurable() = {
    super.withAttrBool(JmsIO.PSTR_DURABLE, true)
    this
  }

  def withProviderUrl(url:String) = {
    super.withAttrString(JmsIO.PSTR_PROVIDER, url)
    this
  }

  def withDestination(dest:String) = {
    super.withAttrString(JmsIO.PSTR_DESTINATION, dest)
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
