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


package com.zotoh.bedrock.device.netty


import scala.math._

import scala.collection.JavaConversions._

import java.io.{IOException,OutputStream}
import java.security.MessageDigest

import org.apache.commons.codec.binary.Base64
import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.handler.codec.http.HttpRequest
import org.jboss.netty.handler.codec.http.QueryStringDecoder

import com.zotoh.bedrock.device.HttpEvent
import com.zotoh.bedrock.http.UriUte

/**
 * Helper functions to get data from Http requests coming from netty.
 *
 * @author kenl
 */
object NettpHplr {

  /**
   * @param key
   * @return
   */
  def calcHybiSecKeyAccept(key:String) = {
    // add fix GUID according to
    // http://tools.ietf.org/html/draft-ietf-hybi-thewebsocketprotocol-10
    val k = key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"
    Base64.encodeBase64String( MessageDigest.getInstance("SHA-1").
              digest(k.getBytes("utf-8")) )
  }

  /**
   * @param dev
   * @param req
   * @return
   */
  def extract( dev:NettpIO, req:HttpRequest) = {
    val ev= dev.createEvent()
    var uri= req.getUri()
    // for REST, we want to inspect the URL more closely by breaking up into paths.
    // this is really to deal with possible matrix type parameters
    if ( !ev.setUriChain( UriUte.toPathChain(uri) )) {
      grabParams(ev, uri)
    }

    ev.setProtocol ( req.getProtocolVersion().getProtocolName()).
    setMethod( req.getMethod().getName())

    val pos = uri.indexOf("?")
    if (pos >= 0) {
      ev.setQueryString( uri.substring(pos+1) )
      uri= uri.substring(0,pos)
    }
    ev.setUri( uri )
    req.getHeaders().foreach { (en) =>
      ev.setHeader(en.getKey(), en.getValue())
    }
    ev
  }

  /**
   * @param cbuf
   * @param os
   * @throws IOException
   */
  def content(cbuf:ChannelBuffer, os:OutputStream) {
    //int pos= cbuf.readerIndex();
    val bits= new Array[Byte](4096)
    var clen= cbuf.readableBytes()
    while (clen > 0) {
      val len = min(4096, clen)
      cbuf.readBytes(bits, 0, len)
      os.write(bits, 0, len)
      clen = clen-len
    }
    os.flush()
  }

  private def grabParams(ev:HttpEvent, uri:String ) {
    val params = new QueryStringDecoder( uri).getParameters()
    if (params != null) params.foreach{ (t) =>
      ev.addParam(t._1, t._2.toSeq )
    }
  }

}
