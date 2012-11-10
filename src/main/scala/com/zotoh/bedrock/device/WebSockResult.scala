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

import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.util.StrUte._

/**
 * Result for Websockets.
 *
 * @author kenl
 */
class WebSockResult extends EventResult {

  private val serialVersionUID = 7636284597211747987L
  private var _isText:Boolean=false
  private var _textData=""
  private var _binData=asBytes("")

  /**
   * @param text
   */
  def setData(text:String) {
    _textData=nsb(text)
    _isText=true
  }

  /**
   * @param bits
   */
  def setData(bits:Array[Byte]) {
    _binData=bits
    _isText=false
  }

  /**
   * @return
   */
  def isText() = _isText

  /**
   * @return
   */
  def isBinary() = !isText()

  /**
   * @return
   */
  def text() = _textData

  /**
   * @return
   */
  def binData() =  _binData

}

