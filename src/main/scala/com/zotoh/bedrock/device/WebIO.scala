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

import com.zotoh.fwk.util.StrUte._
import org.json.{JSONObject=>JSNO}

/**
 * @author kenl
 *
 */
class WebIO(devMgr:DeviceMgr) extends BaseHttpIO(devMgr,false) with Weblet {

  private var _contextPath=""

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.Device#inizWithProperties(org.json.JSONObject)
   */
  override def inizWithQuirks(pps:JSNO) {
    super.inizWithQuirks(pps)
  }

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.Device#onStart()
   */
  override def onStart() = {}

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.Device#onStop()
   */
  override def onStop() = {}

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.Weblet#setContextPath(java.lang.String)
   */
  override def setContextPath(path:String) = {
    _contextPath=nsb(path)
    this
  }

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.Weblet#getContextPath()
   */
  def contextPath() = _contextPath

}
