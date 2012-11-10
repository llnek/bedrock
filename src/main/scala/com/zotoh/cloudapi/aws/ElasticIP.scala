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
import scala.collection.mutable.ArrayBuffer


import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.util.LoggerFactory._
import com.zotoh.fwk.util.Logger
import com.zotoh.fwk.util.StrUte._

//import java.util.Collections
import java.util.Locale

import org.dasein.cloud.CloudException
import org.dasein.cloud.InternalException
import org.dasein.cloud.OperationNotSupportedException
import org.dasein.cloud.network.AddressType
import org.dasein.cloud.network.IpAddress
import org.dasein.cloud.network.IpAddressSupport
import org.dasein.cloud.network.IpForwardingRule
import org.dasein.cloud.network.Protocol
import org.dasein.cloud.identity.ServiceAction

import com.amazonaws.AmazonClientException
import com.amazonaws.AmazonServiceException
import com.amazonaws.services.ec2.model.Address
import com.amazonaws.services.ec2.model.AllocateAddressRequest
import com.amazonaws.services.ec2.model.AllocateAddressResult
import com.amazonaws.services.ec2.model.AssociateAddressRequest
import com.amazonaws.services.ec2.model.DescribeAddressesRequest
import com.amazonaws.services.ec2.model.DescribeAddressesResult
import com.amazonaws.services.ec2.model.DisassociateAddressRequest
import com.amazonaws.services.ec2.model.ReleaseAddressRequest


/**
 * @author kenl
 *
 */
class ElasticIP(private val _svc:AWSNetworkSvcs) extends IpAddressSupport with AWSAPI {

  private def ilog() {  _log=getLogger(classOf[ElasticIP]) }
  @transient private var _log:Logger=null
  def tlog() = { if(_log==null) ilog(); _log }

  override def mapServiceAction(a:ServiceAction)= Array[String]()

  override def assign(addr:String, server:String)  {
    tstEStrArg("instance-id", server)
    tstEStrArg("ip-addr", addr)
    _svc.cloud().EC2()
      .associateAddress(
          new AssociateAddressRequest().
          withInstanceId(server).withPublicIp(addr))
  }

  override def forward(addr:String, pubPort:Int, p:Protocol, prvPort:Int,
      server:String):String = {
    throw new OperationNotSupportedException()
  }

  override def getIpAddress(ipAddr:String) = {
    tstEStrArg("ip-addr", ipAddr)
    val res=_svc.cloud().EC2().describeAddresses(
        new DescribeAddressesRequest().withPublicIps(ipAddr))
    val lst= res.getAddresses()
    if (isNilSeq(lst)) null else toIPAddr( lst.get(0) )
  }

  override def getProviderTermForIpAddress(loc:Locale) = "elastic-ip"

  override def isAssigned(t:AddressType) = {
    AddressType.PUBLIC == t
  }

  override def isForwarding() = false

  override def isRequestable(t:AddressType) = {
    AddressType.PUBLIC == t
  }

  override def isSubscribed() = {
    try {
      _svc.cloud().EC2().describeAddresses()
      true
    }
    catch {
      case e:AmazonServiceException =>
        if ( testForNotSubError(e, "SignatureDoesNotMatch")) { false }
        else { throw e }
      case e => throw e
    }
  }

  override def listPrivateIpPool(unassignedOnly:Boolean) = List[IpAddress]()

  override def listPublicIpPool(unassignedOnly:Boolean) = {
    val res= _svc.cloud().EC2().describeAddresses(new DescribeAddressesRequest())
    (ArrayBuffer[IpAddress]() /: res.getAddresses()) { (rc, a) =>
      if ( unassignedOnly && !isEmpty(a.getInstanceId())) {}
      else {
        rc += toIPAddr( a)
      }
      rc
    }
  }

  override def listRules(addr:String) = {
    throw new OperationNotSupportedException()
  }

  override def releaseFromPool(addr:String) {
    tstEStrArg("public-ip", addr)
    _svc.cloud().EC2().releaseAddress(
      new ReleaseAddressRequest().withPublicIp(addr))
  }

  override def releaseFromServer(addr:String) {
    tstEStrArg("public-ip", addr)
    _svc.cloud().EC2().disassociateAddress(
      new DisassociateAddressRequest().withPublicIp(addr))
  }

  override  def request(t:AddressType) = {
    if ( AddressType.PUBLIC != t) {
      throw new IllegalArgumentException("Expecting type: PUBLIC, got: " + t)
    }

    // i forgot,do we need to do this?
    //_svc.cloud().setAWSSite( _svc.cloud().getContext().getRegionId() )

    val res= _svc.cloud().EC2().allocateAddress(new AllocateAddressRequest())
    res.getPublicIp()
  }

  override def stopForward(addr:String) {
    throw new OperationNotSupportedException()
  }

  private def toIPAddr(a:Address) = {
    if (a != null) {
      val p=new IpAddress()
      p.setRegionId( _svc.cloud().getContext().getRegionId())
      p.setAddressType(AddressType.PUBLIC)
      p.setServerId(a.getInstanceId())
      p.setAddress(a.getPublicIp())
      p.setIpAddressId(p.getAddress())
      p
    } else { null }

  }

}
