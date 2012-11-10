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

import com.zotoh.fwk.util.LoggerFactory._
import com.zotoh.fwk.util.Logger
import com.zotoh.fwk.util.CoreUte._

import java.util.Locale

import org.dasein.cloud.CloudException
import org.dasein.cloud.InternalException
import org.dasein.cloud.network.Firewall
import org.dasein.cloud.network.FirewallRule
import org.dasein.cloud.network.FirewallSupport
import org.dasein.cloud.network.Protocol
import org.dasein.cloud.identity.ServiceAction

import com.amazonaws.AmazonServiceException
import com.amazonaws.services.ec2.model.{SecurityGroup=>EC2SG}
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressRequest
import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest
import com.amazonaws.services.ec2.model.CreateSecurityGroupResult
import com.amazonaws.services.ec2.model.DeleteSecurityGroupRequest
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsRequest
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsResult
import com.amazonaws.services.ec2.model.IpPermission
import com.amazonaws.services.ec2.model.RevokeSecurityGroupIngressRequest


/**
 * @author kenl
 *
 */
class SecurityGroup(private val _svc:AWSNetworkSvcs) extends FirewallSupport with AWSAPI {

  private def ilog() { _log=getLogger(classOf[SecurityGroup]) }
  @transient private var _log:Logger=null
  def tlog() = { if(_log==null) ilog(); _log }

  override def mapServiceAction(a:ServiceAction) = Array[String]()

  override def isSubscribed() = true

  override def authorize(group:String, cidr:String, p:Protocol, fromPort:Int,
      toPort:Int) = {
    tstEStrArg("group-name", group)
    tstEStrArg("cidr", cidr)
    tstObjArg("protocol", p)

    val lst= Array(toPerm(group,cidr,p,fromPort,toPort) )
    _svc.cloud().EC2().authorizeSecurityGroupIngress(
        new AuthorizeSecurityGroupIngressRequest(group, lst.toSeq))
    ""
  }

  def createInVLAN(group:String, desc:String, vlanId:String):String = null

  /**
   * returns the Amazon group-id, which is different to group-name.
   */
  override def create(group:String, desc:String) = {
    tstEStrArg("group-description", desc)
    tstEStrArg("group-name", group)
    val res= _svc.cloud().EC2().createSecurityGroup(
      new CreateSecurityGroupRequest(group,desc))
    res.getGroupId()
  }

  override def delete(group:String) {
    tstEStrArg("group-name", group)
    try {
      _svc.cloud().EC2().deleteSecurityGroup(
          new DeleteSecurityGroupRequest().withGroupName(group))
    }
    catch {
      case e:AmazonServiceException =>
        if (!testSafeNonExistError(e, "InvalidGroup.NotFound")) {
          throw e
        }
      case e => throw e
    }
  }

  override def getFirewall(group:String) = {
    getOneFWall(group)._1
  }

  override def getProviderTermForFirewall(loc:Locale) = "security-group"

  override def getRules(group:String) = {
    val rc=ArrayBuffer[FirewallRule]()
    val g= getOneFWall(group)._2
    if (g != null) {
      val lst= g.getIpPermissions()
      if (lst != null) lst.foreach { (e) =>
        rc ++= toRules(group, e)
      }
    }
    rc
  }

  override def list() = {
    val res= _svc.cloud().EC2().describeSecurityGroups(
      new DescribeSecurityGroupsRequest())
    res.getSecurityGroups().map { (g) =>
        toFW(g)._1
    }
  }

  override def revoke(group:String, cidr:String, p:Protocol, fromPort:Int,
      toPort:Int) {
    tstEStrArg("group-name", group)
    tstEStrArg("cidr", cidr)
    tstObjArg("protocol", p)

    val lst= Array(toPerm(group,cidr,p,fromPort,toPort))
    _svc.cloud().EC2()
      .revokeSecurityGroupIngress(
          new RevokeSecurityGroupIngressRequest(group, lst.toSeq))
  }

  private def getOneFWall(group:String) = {
    tstEStrArg("group-name", group)
    val res= _svc.cloud().EC2().describeSecurityGroups( 
      new DescribeSecurityGroupsRequest().withGroupNames(group))
    val lst=  res.getSecurityGroups()
    if(isNilSeq(lst)) null else toFW( lst.get(0) )
  }

  private def toFW(g:EC2SG) : (Firewall,EC2SG) = {
    var w:Firewall= null
    if (g != null) {
      w= new Firewall()
      w.setActive(true)
      w.setAvailable(true)
      w.setProviderFirewallId(g.getGroupId())
      w.setName( g.getGroupName())
      w.setDescription(g.getDescription())
      w.setRegionId(_svc.cloud().getContext().getRegionId())
    }
    (w,g)
  }

  private def toRules(group:String, p:IpPermission) = {
    val rc=ArrayBuffer[FirewallRule]()

    if (p != null) p.getIpRanges().foreach { (s) =>
      val fr= new FirewallRule()
      fr.setFirewallId(group)
      fr.setProtocol(
        Protocol.valueOf(p.getIpProtocol().toUpperCase()))
      fr.setStartPort(p.getFromPort())
      fr.setEndPort(p.getToPort())
      fr.setCidr(s)
      rc += fr
    }

    rc
  }

  private def toPerm(grp:String, cidr:String, p:Protocol, fromPort:Int, toPort:Int) = {
    new IpPermission().withIpProtocol(p.name().toLowerCase()).
    withFromPort(fromPort).withToPort(toPort).
    withIpRanges(cidr)
  }

}
