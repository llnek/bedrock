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

import scala.collection.mutable.HashMap

import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.util.StrUte._

import java.io.IOException

import com.zotoh.fwk.io.{XData}
import com.zotoh.fwk.net.HTTPStatus


/**
 * @author kenl
 *
 */
case class HttpEventResult() extends EventResult {

  private val serialVersionUID= 568732487697863245L
  private val _headers=HashMap[String,String]()
  private var _data:Option[XData]=None
  private var _text:String= "OK"
  private var _code= 200

  /**
   * @param s
   */
  def this(s:HTTPStatus) {
    this()
    setStatus(s)
  }

  /**
   * Get the result payload data.
   *
   * @return result payload.
   */
  def data() = _data

  /**
   * Set the payload data.
   *
   * @param d data.
   */
  def setData(d:XData) = {
    _data=Some(d)
    this
  }

  /**
   * @param data
   * @throws IOException
   */
  def setData(data:String):HttpEventResult = setData(XData(data))

  /**
   * Set the error message.
   *
   * @param msg message.
   */
  def setErrorMsg(msg:String) = {
    _text= nsb(msg)
    setError(true)
    this
  }

  /**
   * Get the error message to be sent back.
   *
   * @return error message.
   */
  def errorMsg() = {
    if ( hasError()) {
      if (isEmpty(_text)) statusText() else _text
    } else {
      ""
    }
  }

  /**
   * @param s
   */
  def setStatus(s:HTTPStatus) = {
    setStatusText( s.reasonPhrase() )
    setStatusCode(s.code())
    this
  }

  /**
   * Set the HTTP status code to be sent back.
   *
   * @param c status code.
   */
  def setStatusCode(c:Int) = {
    //setError( ! (c >= 200 && c < 300) )
    _code= c
    this
  }

  /**
   * @param s
   */
  def setStatusText(s:String) = { _text= nsb(s); this  }

  /**
   * @return
   */
  def statusText() = _text

  /**
   * Get the HTTP status code to be sent back.
   *
   * @return the code.
   */
  def statusCode() = _code

  /**
   * Get all the internet headers.
   *
   * @return immutable map.
   */
  def headers() = _headers.toMap

  /**
   * Add another internet header, overwrite existing one if same.
   *
   * @param h header key.
   * @param v value.
   */
  def setHeader(h:String, v:String) = {
    if (!isEmpty(h)) { _headers += Tuple2(h, nsb(v)) }
    this
  }

  /**
   *
   */
  def clearAllHeaders() = {
    _headers.clear()
    this
  }

}

