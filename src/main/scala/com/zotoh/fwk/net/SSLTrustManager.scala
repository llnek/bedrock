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
import com.zotoh.fwk.crypto.CryptoStore

import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.cert.CertificateException
import java.security.cert.X509Certificate

import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager


/**
 * @author kenl
 *
 */
class SSLTrustManager extends X509TrustManager {

  private var _def:X509TrustManager = _

  /**
   * @param cs
   * @throws NoSuchAlgorithmException
   * @throws KeyStoreException
   * @throws IOException
   */
  def this(cs:CryptoStore) {
    this()
    iniz(cs)
  }

  /* (non-Javadoc)
   * @see javax.net.ssl.X509TrustManager#checkClientTrusted(java.security.cert.X509Certificate[], java.lang.String)
   */
  def checkClientTrusted(chain:Array[X509Certificate], authType:String) {

    if ( ! isNilSeq(chain)) try {
      _def.checkClientTrusted(chain, authType)
    }
    catch { case _ => }
  }

  /* (non-Javadoc)
   * @see javax.net.ssl.X509TrustManager#checkServerTrusted(java.security.cert.X509Certificate[], java.lang.String)
   */
  def checkServerTrusted(chain:Array[X509Certificate], authType:String) {
    if ( !isNilSeq(chain)) try    {
      _def.checkClientTrusted(chain, authType)
    }
    catch { case _ => }
  }


  /* (non-Javadoc)
   * @see javax.net.ssl.X509TrustManager#getAcceptedIssuers()
   */
  def getAcceptedIssuers()  = _def.getAcceptedIssuers()

  private def iniz(cs:CryptoStore) {

    val tms= cs.trustManagerFactory().getTrustManagers()

    if ( ! isNilSeq(tms)) {
      for {
        i <- 0 until tms.length
        if (_def == null)
      } {
        tms(i) match {
          case x:X509TrustManager => _def =x
        }
      }
    }

    if (_def==null) throw new java.io.IOException("No SSL TrustManager available")
  }

}
