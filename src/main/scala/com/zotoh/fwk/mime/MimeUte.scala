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

package com.zotoh.fwk.mime

import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.util.StrUte._
import com.zotoh.fwk.io.IOUte._
import com.zotoh.fwk.util.LoggerFactory._
import com.zotoh.fwk.util.{CoreImplicits,Logger}

import java.io.InputStream
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * This is a utility class that provides various MIME related functionality.
 *
 * @author kenl
 *
 */
object MimeUte extends MimeConsts with CoreImplicits {
  private var _log=getLogger(classOf[MimeUte])
  def tlog() = _log

  /**
   * @param cType
   * @return
   */
  def isSigned(cType:String) = {
    val ct= nsb(cType).lc
    //tlog().debug("MimeUte:isSigned: ctype={}", cType);
    ( ct.indexOf("multipart/signed") >=0 ) ||
          (isPKCS7mime(ct) && (ct.indexOf("signed-data") >=0) )
  }

  /**
   * @param cType
   * @return
   */
  def isEncrypted(cType:String) = {
    val ct= nsb(cType).lc
    //tlog().debug("MimeUte:isEncrypted: ctype={}", cType);
    (isPKCS7mime(ct)  &&  (ct.indexOf("enveloped-data") >= 0) )
  }

  /**
   * @param cType
   * @return
   */
  def isCompressed(cType:String) = {
    val ct= nsb(cType).lc
    //tlog().debug("MimeUte:isCompressed: ctype={}", cType);
    (ct.indexOf("application/pkcs7-mime") >= 0 ) &&
        (ct.indexOf("compressed-data") >= 0 )
  }

  /**
   * @param cType
   * @return
   */
  def isMDN(cType:String) = {
    val ct= nsb(cType).lc
    //tlog().debug("MimeUte:isMDN: ctype={}", cType);
    (ct.indexOf("multipart/report") >=0) &&
        (ct.indexOf("disposition-notification") >= 0)
  }

  /**
   * @param obj
   * @return
   * @throws Exception
   */
  def maybeAsStream(obj:Any) = {
    obj match {
      case b:Array[Byte] =>  asStream(b)
      case i:InputStream =>  i
      case s:String =>  asStream(asBytes(s))
      case _ => null
    }
  }

  /**
   * @param u
   * @return
   */
  def urlDecode(u:String) = {
    if (u==null) null else
    try {
      URLDecoder.decode(u, "UTF-8")
    }
    catch {
      case _ => null
    }

  }

  /**
   * @param u
   * @return
   */
  def urlEncode(u:String) = {
    if (u==null) null else
    try {
      URLEncoder.encode(u, "UTF-8")
    }
    catch {
      case _ => null
    }
    
  }

  private def isPKCS7mime(s:String) = {
    (s.indexOf("application/pkcs7-mime") >=0) ||
      (s.indexOf("application/x-pkcs7-mime") >=0)
  }

}

sealed class MimeUte {}

