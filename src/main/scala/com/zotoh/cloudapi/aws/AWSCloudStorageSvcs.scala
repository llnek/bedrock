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

import org.dasein.cloud.storage.AbstractStorageServices

import com.zotoh.fwk.util.CoreUte._

object AWSCloudStorageSvcs {}


/**
 * @author kenl
 *
 */
case class AWSCloudStorageSvcs(private val _aws:AWSCloud) extends AbstractStorageServices with AWSService {

  private def ilog() { _log=getLogger(classOf[AWSCloudStorageSvcs]) }
  @transient private var _log:Logger=null
  def tlog() = { if(_log==null) ilog(); _log }

  override def getBlobStoreSupport() = new S3(this)

  override def cloud() = _aws

}
