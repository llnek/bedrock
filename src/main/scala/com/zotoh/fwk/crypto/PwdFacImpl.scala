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

package com.zotoh.fwk.crypto

import scala.math._

import java.security.SecureRandom

import com.zotoh.fwk.util.LoggerFactory._
import com.zotoh.fwk.util.Logger
import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.util.ByteUte._

/**
 * Implementation of passwords.
 *
 * @author kenl
 *
 */
object PwdFacImpl {

  private def ilog() { _log = getLogger(classOf[PwdFacImpl]) }
  private var _log:Logger=null
  def tlog() = { if(_log==null) ilog(); _log }

  private val PCHS= "abcdefghijklmnopqrstuvqxyz" +
  "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
  "`1234567890-_~!@#$%^&*()"
  private val s_pwdChars= PCHS.toCharArray()

  private val ACHS= "abcdefghijklmnopqrstuvqxyz" +
  "1234567890-_ABCDEFGHIJKLMNOPQRSTUVWXYZ"
  private val s_asciiChars= ACHS.toCharArray()

  /**
   * @param length
   * @return
   */
  protected[crypto] def createStrong(length:Int) = createXXX(length, s_pwdChars)

  /**
   * @param length
   * @return
   */
  protected[crypto] def createRandom(length:Int) = createXXX(length, s_asciiChars)

  /**
   * @param length
   * @param chars
   * @return
   */
  private def createXXX(length:Int, chars:Array[Char]):String = {
    if (length < 0) { return null }
    if (length==0) { return "" }
    val str = new Array[Char](length)
    try {
      val r = SecureRandom.getInstance("SHA1PRNG")
      val bits= new Array[Byte](4)
      val cl= chars.length
      for (i <- 0 until length) {
        r.nextBytes(bits)
        str(i)= chars( abs( readAsInt(bits) % cl) )
      }
    }
    catch {
      case e => tlog().warnX("", Some(e))
    }
    new String(str)
  }
}

sealed class PwdFacImpl {}

