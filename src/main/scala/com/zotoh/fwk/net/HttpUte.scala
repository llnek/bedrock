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

import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.util.StrUte._
import com.zotoh.fwk.util.LoggerFactory._
import com.zotoh.fwk.util.{StrArr,Logger}
import com.zotoh.fwk.io.XData

import java.net.ConnectException
import java.net.URI
import java.net.URISyntaxException

import javax.mail.Header
import javax.net.ssl.SSLContext

import org.jboss.netty.handler.codec.http.HttpMessage


/**
 * @author kenl
 *
 */
object HttpUte {

  private def _log= getLogger(classOf[HttpUte])
  def tlog() = _log

  private val s_td= 8L * 1024 * 1024 // 8 Meg
  private var _client:SSLContext = _

  iniz()

  /**
   * @return
   */
  def clientSSL() = _client

  /**
   * @return
   */
  def dftThreshold() = s_td

  /**
   * @param url
   * @param in
   * @param cb
   * @return
   * @throws Exception
   */
  def simplePOST(url:URI, in:XData, cb:HttpMsgIO):HttpClientr = {

    tstObjArg("post-url", url)
    tstObjArg("data", in)

    val cr= HttpClientr()
    cr.connect(url)
    cr.post(cb,in)
    cr
  }

  /**
   * @param ssl
   * @param host
   * @param port
   * @param uriPart
   * @param in
   * @param cb
   * @return
   * @throws Exception
   */
  def simplePOST(ssl:Boolean, host:String, port:Int,
      uriPart:String, in:XData, cb:HttpMsgIO):HttpClientr = {

    simplePOST(
      new URI( if(ssl) "https" else "http", null,
        host, port, uriPart, null, null),
      in, cb)

  }

  /**
   * @param url
   * @param cb
   * @return
   * @throws Exception
   */
  def simpleGET(url:URI, cb:HttpMsgIO):HttpClientr = {

    tstObjArg("get-url", url)

    val cr= HttpClientr()
    cr.connect(url)
    cr.get(cb)

    cr
  }

  /**
   * @param ssl
   * @param host
   * @param port
   * @param uriPart
   * @param query
   * @param cb
   * @return
   * @throws Exception
   */
  def simpleGET(ssl:Boolean, host:String, port:Int,
      uriPart:String, query:String, cb:HttpMsgIO):HttpClientr = {

    simpleGET(
      new URI( if (ssl) "https" else "http", null,
        host, port, uriPart, query, null), cb )
  }

  /**
   * @param hds
   * @return
   */
  def writeHeaders(hds:Map[String,String] ) = {
    val bd= new StringBuilder(512)
    hds.foreach { (t) =>
      bd.append(t._1).append(": ").append(t._2).append("\r\n")
    }
    bd.toString
  }

  /**
   * @param hds
   * @return
   */
  def writeHeaders(hds:Array[Header]) = {
    val bd= new StringBuilder(512)
    hds.foreach { (h) =>
      bd.append(h.getName()).append(": ").append(h.getValue()).append("\r\n")
    }
    bd.toString
  }

  def wrHds(m:HttpMessage, hds:Map[String,StrArr]) {

    hds.foreach { (t) =>
      t._2.toArray.foreach { (s) =>
        m.addHeader(t._1, nsb(s))
      }
    }
  }

  private def tstConnectRefuse(e:ConnectException) = {
    val s= e.getMessage().lc
    if (s.has("connection") && s.has("refused") &&
        s.has("connect") ) 0 else -1
  }

  private def iniz() {
    try {
      _client= SSLContext.getInstance("TLS")
      _client.init(null, SSLTrustMgrFactory.getTrustManagers(), null)
    } catch {
      case e => throw new RuntimeException("Failed to initialize the client-side SSLContext", e)
    }
  }


}

sealed class HttpUte {}


