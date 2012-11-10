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

import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.util.LoggerFactory._
import com.zotoh.fwk.util.Logger

import java.io.File
import java.util.Locale

import org.dasein.cloud.CloudException
import org.dasein.cloud.InternalException
import org.dasein.cloud.encryption.Encryption
import org.dasein.cloud.storage.BlobStoreSupport
import org.dasein.cloud.storage.CloudStoreObject
import org.dasein.cloud.storage.FileTransfer
import org.dasein.cloud.identity.ServiceAction

/**
 * @author kenl
 *
 */
class S3(private val _svc:AWSCloudStorageSvcs ) extends BlobStoreSupport {

  private def ilog() { _log=getLogger(classOf[S3]) }
  @transient private var _log:Logger=null
  def tlog() = { if(_log==null) ilog(); _log }

  override def mapServiceAction(a:ServiceAction) = Array[String]()

  override def isSubscribed() = true

  override def clear(bucket:String) {}

  override def createDirectory(bucket:String, findFreeName:Boolean) = null

  override def download(cf:CloudStoreObject, fp:File) = null

  override def download(a:String, a1:String, fp:File, enc:Encryption) = null

  override def exists(n:String) = false

  override def exists(a:String, a1:String, a2:Boolean) = 0L

  override def getMaxFileSizeInBytes()= 0L

  override def getProviderTermForDirectory(loc:Locale) = ""

  override def getProviderTermForFile(loc:Locale) = ""

  override def isPublic(a:String, a1:String) = false

  override def listFiles(a:String) = null

  override def makePublic(a:String) {}

  override def makePublic(a:String, a1:String) {}

  override def moveFile(a:String, a1:String, a2:String) {}

  override def removeDirectory(a:String) {}

  override def removeFile(a:String, a1:String, a2:Boolean) {}

  override def renameDirectory(a:String, a1:String, a2:Boolean) = null

  override def renameFile(a:String, a1:String, a2:String) {}

  override def upload(fp:File, a:String, a2:String, a3:Boolean, enc:Encryption) {}

}
