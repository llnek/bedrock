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

package com.zotoh.fwk.net

import org.jboss.netty.channel.group.ChannelGroup
import com.zotoh.fwk.io.XData

/**
 * @author kenl
 *
 */
class HttpResponseHdlr(g:ChannelGroup) extends BasicChannelHandler(g) {

  private var _cb:HttpMsgIO = _

  /**
   * @param cb
   * @return
   */
  def bind(cb:HttpMsgIO) = {
    _cb= cb; this
  }

  override def doResFinal(code:Int, reason:String, out:XData) {
    if (_cb != null)  {
      _cb.onOK(code, reason, out)
    }
  }

  override def onResError(code:Int, reason:String) {
    if (_cb != null) {
      _cb.onError(code, reason)
    }
  }


}

