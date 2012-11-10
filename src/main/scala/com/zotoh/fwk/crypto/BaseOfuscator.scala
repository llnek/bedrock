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

import org.apache.commons.codec.binary.Base64
import java.util.Arrays

import com.zotoh.fwk.util.{Logger,Consts}
import com.zotoh.fwk.util.LoggerFactory._
import com.zotoh.fwk.util.CoreUte._

object BaseOfuscator {

  private val KEY= "ed8xwl2XukYfdgR2aAddrg0lqzQjFhbs"
  private val T3_DES= "TripleDES"
  //AES/ECB/PKCS5Padding/TripleDES
  private val ALGO= T3_DES // default javax supports this
  private var _key:Array[Byte]= null

  setKey( Base64.decodeBase64(KEY))

  /**
   * Set the encryption key for future obfuscation operations.  Typically this is
   * called once only at the start of the main application.
   *
   * @param key
   */
  def setKey(key:Array[Byte]) {
    var len= key.length
    if (T3_DES == ALGO ) {
      if (len < 24) {
        errBadArg("Encryption key length must be 24, using TripleDES")
      }
      else if (len > 24) {
        len=24
      }
    }
    _key= key.slice(0,len)
    //    _key = java.util.Arrays.copyOfRange(key, 0, len)
  }

}

/**
 * @author kenl
 *
 */
abstract class BaseOfuscator protected() extends Consts {

  private def ilog() { _log=getLogger(classOf[BaseOfuscator]) }
  @transient private var _log:Logger= null
  def tlog() = {  if(_log==null) ilog(); _log  }

  /**
   * Decrypt the given text.
   *
   * @param encryptedText
   * @return
   */
  def unobfuscate(encryptedText:String):String

  /**
   * Encrypt the given text string.
   *
   * @param clearText
   * @return
   */
  def obfuscate(clearText:String):String

  /**
   * @return
   */
  protected def algo() = BaseOfuscator.ALGO

  /**
   * @return
   */
  protected def key() = BaseOfuscator._key

}
