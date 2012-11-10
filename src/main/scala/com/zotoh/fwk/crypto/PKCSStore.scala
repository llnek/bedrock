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

import scala.collection.JavaConversions._

import com.zotoh.fwk.util.CoreUte._

import java.io.{IOException,InputStream}
import java.security._
import java.security.cert.{CertificateFactory,Certificate}

object PKCSStore {
}

/**
 * @author kenl
 *
 */
case class PKCSStore() extends CryptoStore {

  /**
   * @param bits
   * @throws CertificateException
   * @throws KeyStoreException
   */
  def addPKCS7Entity(bits:InputStream) {
    val certs= CertificateFactory.getInstance( "X.509").generateCertificates(bits)
    certs.toArray().foreach { (c) =>
      _store.setCertificateEntry( newAlias(), c.asInstanceOf[Certificate] )
    }
  }

  /* (non-Javadoc)
   * @see com.zotoh.crypto.CryptoStore#createKeyStore()
   */
  override def createKeyStore() = {
    var ks= KeyStore.getInstance("PKCS12", Crypto.bouncyCastle())
    ks.load(null)
    ks
  }

}
