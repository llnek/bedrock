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

import java.util.{Iterator,Locale}

import org.dasein.cloud.CloudException
import org.dasein.cloud.InternalException
import org.dasein.cloud.network.DNSRecord
import org.dasein.cloud.network.DNSRecordType
import org.dasein.cloud.network.DNSSupport
import org.dasein.cloud.network.DNSZone
import org.dasein.cloud.identity.ServiceAction



/**
 * @author kenl
 *
 */
class DNSRoute53(private val _svc:AWSNetworkSvcs ) extends DNSSupport {

  private def ilog() {  _log=getLogger(classOf[DNSRoute53]) }
  @transient private var _log:Logger=null
  def tlog() = { if(_log==null) ilog(); _log }

  override def mapServiceAction(act:ServiceAction) = Array[String]()

  override def addDnsRecord(a:String, rec:DNSRecordType, a2:String,
      a3:Int, a4:String*) = null

  override def createDnsZone(domain:String, name:String, desc:String) = null

  override def deleteDnsRecords(recs:DNSRecord*) {}

  override def deleteDnsZone(a:String) {}

  override def getDnsZone(a:String) = null

  override def getProviderTermForRecord(loc:Locale) = null

  override def getProviderTermForZone(loc:Locale) = null

  override def isSubscribed() = false

  override def listDnsRecords(a:String, a1:DNSRecordType, a2:String)= null

  override def listDnsZones() = null

}
