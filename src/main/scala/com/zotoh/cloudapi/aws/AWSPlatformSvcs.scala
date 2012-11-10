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

import org.dasein.cloud.platform.AbstractPlatformServices
import org.dasein.cloud.platform.CDNSupport
import org.dasein.cloud.platform.KeyValueDatabaseSupport
import org.dasein.cloud.platform.MessageQueueSupport
import org.dasein.cloud.platform.PushNotificationSupport
import org.dasein.cloud.platform.RelationalDatabaseSupport

import com.zotoh.fwk.util.CoreUte._

object AWSPlatformSvcs {}

/**
 * @author kenl
 *
 */
case class AWSPlatformSvcs(private val _aws:AWSCloud) extends AbstractPlatformServices
with AWSService {

  private def ilog() { _log=getLogger(classOf[AWSPlatformSvcs]) }
  @transient private var _log:Logger=null
  def tlog() = { if(_log==null) ilog(); _log }

  override def getCDNSupport() = new CloudFront(this)

  override def getKeyValueDatabaseSupport() = new SDB(this)

  override def getMessageQueueSupport() = new SQS(this)

  override def getPushNotificationSupport() = new SNS(this)

  override def getRelationalDatabaseSupport() = new RDS(this)

  override def hasCDNSupport() = true

  override def hasKeyValueDatabaseSupport() = true

  override def  hasMessageQueueSupport() = true

  override def hasPushNotificationSupport() = true

  override def hasRelationalDatabaseSupport() = true

  override def cloud() = _aws

}
