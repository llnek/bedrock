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

import scala.collection.mutable.ArrayBuffer
import scala.collection.JavaConversions._

import java.util.Locale

import com.zotoh.fwk.util.LoggerFactory._
import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.util.Logger

import org.dasein.cloud.CloudException
import org.dasein.cloud.InternalException
import org.dasein.cloud.network.IpAddress
import org.dasein.cloud.network.VLANSupport
import org.dasein.cloud.network.VLAN
import org.dasein.cloud.network.Subnet
import org.dasein.cloud.network.NetworkInterface
import org.dasein.cloud.identity.ServiceAction


/**
 * @author kenl
 *
 */
class VPC(private val _svc:AWSNetworkSvcs ) extends VLANSupport {

  private def ilog() { _log=getLogger(classOf[VPC]) }
  @transient private var _log:Logger=null
  def tlog() = { if(_log==null) ilog(); _log  }

  override def mapServiceAction(a:ServiceAction) = Array[String]()

  override def getMaxVlanCount() = 0

  override def getVlan(a:String):VLAN = null

  override def getSubnet(id:String):Subnet = null

  override def isSubscribed() = false

  override def createVlan(cidr:String,
        name:String,
        description:String,
        domainName:String,
        dnsServers:Array[String],
        ntpServers:Array[String]):VLAN = null

  override def createSubnet(cidr:String,
          inProviderVlanId:String,
          name:String,
          description:String):Subnet = null

  override def allowsNewSubnetCreation() = true

  override def allowsNewVlanCreation() = true

  override def listVlans() = null

  override def removeVlan(n:String) {}

  def removeSubnet(n:String) {}

  override def isSubnetDataCenterConstrained() = false

  override def isVlanDataCenterConstrained() = false

  override def supportsVlansWithSubnets() = true

  override def listNetworkInterfaces(vmId:String) = null

  override def listSubnets(vlanId:String) = null

  override def getProviderTermForVlan(loc:Locale) = "VPC"

  override def getProviderTermForSubnet(loc:Locale) = "Subnet"

  override def getProviderTermForNetworkInterface(loc:Locale) = "NetworkInterface"

}
