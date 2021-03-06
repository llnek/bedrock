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

import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.util.StrUte._
import java.security.SecureRandom
import java.io.{ByteArrayOutputStream=>BAOS}

import org.apache.commons.codec.binary.Base64
import org.bouncycastle.crypto.KeyGenerationParameters
import org.bouncycastle.crypto.engines.DESedeEngine
import org.bouncycastle.crypto.generators.DESedeKeyGenerator
import org.bouncycastle.crypto.modes.CBCBlockCipher
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher
import org.bouncycastle.crypto.params.DESedeParameters
import org.bouncycastle.crypto.params.KeyParameter

object BCOfuscator {
  private def genkey() {
    // create 2 things for the generation of a key
    // 1. random gen
    // 2. key length in bits
    // DESede key must be 192 or 128 bits long only
    var kg = new DESedeKeyGenerator()
    kg.init(
      new KeyGenerationParameters( Crypto.secureRandom(), DESedeParameters.DES_EDE_KEY_LENGTH*8) )
    Base64.encodeBase64( kg.generateKey())
  }

}

/**
 * Obfuscation using BouncyCastle.
 *
 * @author kenl
 *
 */
sealed case class BCOfuscator() extends BaseOfuscator {

  /* (non-Javadoc)
   * @see com.zotoh.core.crypto.BaseOfuscator#unobfuscate(java.lang.String)
   */
  def unobfuscate(encrypted:String) = decrypt(encrypted)

  /* (non-Javadoc)
   * @see com.zotoh.core.crypto.BaseOfuscator#obfuscate(java.lang.String)
   */
  def obfuscate(clearText:String) = encrypt(clearText)

  private def decrypt(encrypted:String) = {
    if (isEmpty(encrypted)) { encrypted } else {
      val cipher= new PaddedBufferedBlockCipher(
        new CBCBlockCipher(new DESedeEngine()))
      // init the cipher with the key, for encryption
      cipher.init(false, new KeyParameter( key()))
      val p = Base64.decodeBase64(encrypted)
      val out = new Array[Byte](1024)
      val baos = new BAOS()
      var c= cipher.processBytes(p, 0, p.length, out, 0)
      if (c > 0) {
        baos.write(out, 0, c)
      }
      c = cipher.doFinal(out,0)
      if (c > 0) {
        baos.write(out, 0, c)
      }
      asString( baos.toByteArray())
    }
  }

  private def encrypt(clearText:String) = {
    if (isEmpty(clearText)) { clearText } else {
      val cipher= new PaddedBufferedBlockCipher(
        new CBCBlockCipher(new DESedeEngine()))
      // init the cipher with the key, for encryption
      cipher.init(true, new KeyParameter( key()))
      val out = new Array[Byte](4096)
      val baos = new BAOS()
      val p = asBytes(clearText)
      var c= cipher.processBytes(p, 0, p.length, out, 0)
      if (c > 0) {
        baos.write(out, 0, c)
      }
      c = cipher.doFinal(out,0)
      if (c > 0) {
        baos.write(out, 0, c)
      }
      Base64.encodeBase64String(  baos.toByteArray())
    }
  }


}
