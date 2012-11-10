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

import com.zotoh.fwk.io.XData
import com.zotoh.fwk.util.StrUte._

/**
 * Events generated by a POP3 device.
 *
 * @author kenl
 */
class POP3Event(
  private val _msg:XData,
  private var _hdrs:String,
  dev:Device) extends Event(dev) {

  private val serialVersionUID = -5093250293811551815L
  _hdrs=nsb(_hdrs)

  /**
   * @return
   */
  def msg() = _msg

  /**
   * @return
   */
  def headers() = _hdrs

  /**
   * @return
   */
  def subject() = getLine("subject:")

  /**
   * @return
   */
  def receiver() = getLine("to:")

  /**
   * @return
   */
  def from() = getLine("from:")

  /**
   * @param key
   * @return
   */
  private def getLine(key:String) = {
    _hdrs.split("\\r\\n").find { (s) =>
      s.lc.has(key)
    }
  }

}

