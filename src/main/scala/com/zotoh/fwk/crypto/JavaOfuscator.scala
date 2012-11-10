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

import org.apache.commons.codec.binary.Base64
import javax.crypto.spec.SecretKeySpec
import javax.crypto.Cipher

import java.io.{ByteArrayOutputStream=>BAOS}

import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.util.StrUte._

object JavaOfuscator {} 


/**
 * Obfuscation using SUN-java.
 *
 * @author kenl
 *
 */
sealed case class JavaOfuscator() extends BaseOfuscator  {

  /* (non-Javadoc)
   * @see com.zotoh.core.crypto.BaseOfuscator#unobfuscate(java.lang.String)
   */
  def unobfuscate(encoded:String) = decrypt(encoded)

  /* (non-Javadoc)
   * @see com.zotoh.core.crypto.BaseOfuscator#obfuscate(java.lang.String)
   */
  def obfuscate(clearText:String) = encrypt(clearText)

  private def encrypt(clearText:String) = {
    if (isEmpty(clearText)) { clearText } else {
      val c= getCipher(Cipher.ENCRYPT_MODE)
      val baos = new BAOS()
      val p = asBytes(clearText)
      val out= new Array[Byte]( max(4096, c.getOutputSize(p.length)) )
      var n= c.update(p, 0, p.length, out, 0)
      if (n > 0) { baos.write(out, 0, n) }
      n = c.doFinal(out,0)
      if (n > 0) { baos.write(out, 0, n) }
      Base64.encodeBase64URLSafeString(baos.toByteArray())
    }
  }

  private def decrypt(encoded:String) = {
    if (isEmpty(encoded)) { encoded } else {
      val c= getCipher(Cipher.DECRYPT_MODE)
      val baos = new BAOS()
      val p = Base64.decodeBase64(encoded)
      val out= new Array[Byte]( max(4096, c.getOutputSize(p.length)) )
      var n= c.update(p, 0, p.length, out, 0)
      if (n > 0) { baos.write(out, 0, n) }
      n = c.doFinal(out,0)
      if (n > 0) { baos.write(out, 0, n) }
      asString(baos.toByteArray())
    }
  }

  private def getCipher(mode:Int) = {
    val key= new SecretKeySpec( super.key(), algo())
    val c= Cipher.getInstance( algo())
    c.init(mode, key)
    c
  }

}
