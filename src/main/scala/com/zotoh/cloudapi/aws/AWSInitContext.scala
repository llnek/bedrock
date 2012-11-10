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

package com.zotoh.cloudapi.aws

import com.zotoh.fwk.util.LoggerFactory._
import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.util.{Logger}
import com.zotoh.fwk.util.StrUte._

import java.util.{Properties=>JPS}

import org.dasein.cloud.CloudProvider
import org.dasein.cloud.ProviderContext

import com.zotoh.cloudapi.core.Vars
import com.zotoh.fwk.crypto.PwdFactory


/**
 * @author kenl
 *
 */
object AWSInitContext extends AWSVars with Vars {

  private def ilog():Unit = { _log=getLogger(classOf[AWSInitContext]) }
  @transient private var _log:Logger=null
  def tlog() = { if(_log==null) ilog(); _log  }

  /**
   * @param props
   * @return
   */
  def configure(pps:JPS):AWSCloud = {
    tstObjArg("config-props", pps)
//  String acctno= nsb( props.getProperty(P_ACCT)),
//    accessKey= nsb(props.getProperty(P_ID)),
//    secretKey= nsb(props.getProperty(P_PWD))
//  tstEStrArg("cred-secret-key", secretKey);
//  tstEStrArg("cred-access-key", accessKey)
//  tstEStrArg("cred-acct-n#", acctno)

    configure0(new JPS().addAll(pps) )
  }

  /**
   * @param props
   * @param acctno
   * @param accessKey
   * @param secretKey
   * @return
   */
  def configure(pps:JPS, acctno:String, accessKey:String, secretKey:String):AWSCloud = {
//  tstEStrArg("cred-secret-key", secretKey)
//  tstEStrArg("cred-access-key", accessKey)
//  tstEStrArg("cred-acct-n#", acctno)
    tstObjArg("config-props", pps)

    configure0( new JPS().
        addAll(pps).
        add(P_ID, accessKey).
        add(P_PWD, secretKey).
        add(P_ACCT, acctno)
    )
  }

  private def configure0(pps:JPS) = {
    tlog().debug("AWSInitContext: configuring for AWS()")
    var pwd= pps.gets(P_PWD)
    if (!isEmpty(pwd)) try {
      pwd=PwdFactory.mk(pwd).text()
      pps.put(P_PWD, pwd)
    }
    catch { case _ => }
    val rg= nsb( pps.remove(P_REGION) )
    val x= new ProviderContext(pps.gets(P_ACCT).replaceAll("-", ""), "us-east-1")

    x.setProviderName("Amazon")
    x.setCloudName("AWS")
    x.setCustomProperties(pps)

    val c = new AWSCloud(x)
    if (!isEmpty(rg)) { c.setAWSSite(rg) }
    c
  }

}

sealed class AWSInitContext {}
