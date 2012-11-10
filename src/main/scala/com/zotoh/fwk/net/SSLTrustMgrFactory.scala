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

import com.zotoh.fwk.util.LoggerFactory._
import com.zotoh.fwk.util.Logger

import java.security.InvalidAlgorithmParameterException
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.cert.CertificateException
import java.security.cert.X509Certificate

import javax.net.ssl.ManagerFactoryParameters
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactorySpi
import javax.net.ssl.X509TrustManager

object SSLTrustMgrFactory {
  
  private val _log=getLogger(classOf[SSLTrustMgrFactory])
  def tlog() = _log
  
  //TrustManager
  private val _mgr = new X509TrustManager() {
    def getAcceptedIssuers() = Array[X509Certificate]()

    def checkClientTrusted( chain:Array[X509Certificate], authType:String) {
      tlog().warn("SkipCheck: CLIENT CERTIFICATE: {}" , chain(0).getSubjectDN())
    }

    def checkServerTrusted( chain:Array[X509Certificate], authType:String) {
      tlog().warn("SkipCheck: SERVER CERTIFICATE: {}" , chain(0).getSubjectDN())
    }
  }

  /**
   * @return
   */
  def getTrustManagers() = Array[TrustManager] ( _mgr )

}

/**
 * @author kenl
 *
 */
class SSLTrustMgrFactory extends TrustManagerFactorySpi {

  /* (non-Javadoc)
   * @see javax.net.ssl.TrustManagerFactorySpi#engineGetTrustManagers()
   */
  override def engineGetTrustManagers() = SSLTrustMgrFactory.getTrustManagers()

  /* (non-Javadoc)
   * @see javax.net.ssl.TrustManagerFactorySpi#engineInit(java.security.KeyStore)
   */
  override def engineInit(ks:KeyStore) {}

  /* (non-Javadoc)
   * @see javax.net.ssl.TrustManagerFactorySpi#engineInit(javax.net.ssl.ManagerFactoryParameters)
   */
  override def engineInit(p:ManagerFactoryParameters) {}

}
