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

import scala.collection.JavaConversions._

import com.zotoh.fwk.util.LoggerFactory._
import com.zotoh.fwk.util.Logger
import com.zotoh.fwk.util.CoreUte._

import java.util.Locale

import org.dasein.cloud.CloudException
import org.dasein.cloud.InternalException
import org.dasein.cloud.TimeWindow
import org.dasein.cloud.platform.ConfigurationParameter
import org.dasein.cloud.platform.Database
import org.dasein.cloud.platform.DatabaseConfiguration
import org.dasein.cloud.platform.DatabaseEngine
import org.dasein.cloud.platform.DatabaseProduct
import org.dasein.cloud.platform.DatabaseSnapshot
import org.dasein.cloud.platform.RelationalDatabaseSupport
import org.dasein.cloud.identity.ServiceAction



class RDS(private val _svc:AWSPlatformSvcs) extends RelationalDatabaseSupport {

  private def ilog() { _log=getLogger(classOf[RDS]) }
  @transient private var _log:Logger=null
  def tlog() = { if(_log==null) ilog(); _log }

  override def mapServiceAction(a:ServiceAction) = Array[String]()

  override def getSupportedVersions(eng:DatabaseEngine) = null

  override def addAccess(a:String, a1:String) {}

  override def alterDatabase(a:String, a1:Boolean, a2:String, a3:Int,
      a4:String, a5:String, a6:String, a7:Int, a8:Int,
      a9:TimeWindow, a10:TimeWindow) {
  }

  override def getDefaultVersion(eng:DatabaseEngine) = null

  override def createFromLatest(a:String, a1:String, a2:String,
      a3:String, a4:Int) = null

  override def createFromScratch(a:String, pd:DatabaseProduct,
      a2:String, a3:String, aa:String, a4:Int) = null

  override def createFromSnapshot(a:String, a1:String, a2:String,
      a3:String, a4:String, a5:Int) = null

  override def createFromTimestamp(a:String, a1:String, a2:Long,
      a3:String, a4:String, a5:Int) = null

  override def getConfiguration(n:String) = null

  override def getDatabase(n:String) = null

  override def getDatabaseEngines() = null

  override def getDatabaseProducts(n:DatabaseEngine) = null

  override def getProviderTermForDatabase(loc:Locale) = null

  override def getProviderTermForSnapshot(loc:Locale) = null

  override def getSnapshot(n:String) = null

  override def isSubscribed() = false

  override def isSupportsFirewallRules() = false

  override def isSupportsHighAvailability() = false

  override def isSupportsLowAvailability() = false

  override def isSupportsMaintenanceWindows() = false

  override def isSupportsSnapshots() = false

  override def listAccess(n:String) = null

  override def listConfigurations() = null

  override def listDatabases() = null

  override def listParameters(n:String) = null

  override def listSnapshots(n:String) = null

  override def removeConfiguration(n:String) {}

  override def removeDatabase(n:String) {}

  override def removeSnapshot(n:String) {}

  override def resetConfiguration(n:String, a1:String*) {}

  override def restart(n:String, a1:Boolean) {}

  override def revokeAccess(n:String, a1:String) {}

  override def snapshot(n:String, a1:String) = null

  override def updateConfiguration(n:String, a1:ConfigurationParameter*) {}

}
