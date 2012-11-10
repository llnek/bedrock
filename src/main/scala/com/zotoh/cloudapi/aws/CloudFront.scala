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
import com.zotoh.fwk.util.Logger
import com.zotoh.fwk.util.CoreUte._

import java.util.Locale

import org.dasein.cloud.CloudException
import org.dasein.cloud.InternalException
import org.dasein.cloud.platform.CDNSupport
import org.dasein.cloud.platform.Distribution
import org.dasein.cloud.identity.ServiceAction



/**
 * @author kenl
 *
 */
class CloudFront(private val _svc:AWSPlatformSvcs ) extends CDNSupport {

  private def ilog() { _log=getLogger(classOf[CloudFront]) }
  @transient private var _log:Logger=null
  def tlog() = { if(_log==null) ilog(); _log }

  override def mapServiceAction(act:ServiceAction) = Array[String]()

  override def create(bucket:String, name:String, active:Boolean, cnames:String*) = null

  override def delete(a:String) {}

  override def getDistribution(a:String) = null

  override def getProviderTermForDistribution(loc:Locale) = null

  override def isSubscribed() = false

  override def list() = null

  override def update(a:String, a1:String, a2:Boolean, a3:String*) {}

}
