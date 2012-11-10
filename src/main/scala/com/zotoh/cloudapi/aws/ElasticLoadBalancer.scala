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
import org.dasein.cloud.ProviderContext
import org.dasein.cloud.network.LbAlgorithm
import org.dasein.cloud.network.LbListener
import org.dasein.cloud.network.LbProtocol
import org.dasein.cloud.network.LoadBalancer
import org.dasein.cloud.network.LoadBalancerAddressType
import org.dasein.cloud.network.LoadBalancerState
import org.dasein.cloud.network.LoadBalancerSupport
import org.dasein.cloud.identity.ServiceAction

import com.amazonaws.AmazonClientException
import com.amazonaws.AmazonServiceException
import com.amazonaws.services.elasticloadbalancing.model.CreateLoadBalancerRequest
import com.amazonaws.services.elasticloadbalancing.model.CreateLoadBalancerResult
import com.amazonaws.services.elasticloadbalancing.model.DeleteLoadBalancerRequest
import com.amazonaws.services.elasticloadbalancing.model.DeregisterInstancesFromLoadBalancerRequest
import com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersRequest
import com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersResult
import com.amazonaws.services.elasticloadbalancing.model.DisableAvailabilityZonesForLoadBalancerRequest
import com.amazonaws.services.elasticloadbalancing.model.EnableAvailabilityZonesForLoadBalancerRequest
import com.amazonaws.services.elasticloadbalancing.model.Instance
import com.amazonaws.services.elasticloadbalancing.model.Listener
import com.amazonaws.services.elasticloadbalancing.model.ListenerDescription
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription
import com.amazonaws.services.elasticloadbalancing.model.RegisterInstancesWithLoadBalancerRequest


/**
 * @author kenl
 *
 */
class ElasticLoadBalancer(private val _svc:AWSNetworkSvcs) extends LoadBalancerSupport with AWSAPI {

  private def ilog() { _log=getLogger(classOf[ElasticLoadBalancer]) }
  @transient private var _log:Logger=null
  def tlog() = { if(_log==null) ilog(); _log }

  private val _protocols= List(LbProtocol.HTTP, LbProtocol.RAW_TCP)
  private val _algos= List(LbAlgorithm.ROUND_ROBIN)

  override def mapServiceAction(a:ServiceAction) = Array[String]()

  override def addDataCenters(balancer:String, zones:String*) {
    tstEStrArg("load-balancer-name", balancer)
    _svc.cloud().ELB().enableAvailabilityZonesForLoadBalancer(
        new EnableAvailabilityZonesForLoadBalancerRequest().
          withAvailabilityZones(zones.toList).
          withLoadBalancerName(balancer))
  }

  override def addServers(balancer:String, servers:String*) {
    tstEStrArg("load-balancer-name", balancer)
    val lst= servers.map { (s) => new Instance(s) }
    _svc.cloud().ELB().registerInstancesWithLoadBalancer(
        new RegisterInstancesWithLoadBalancerRequest().
          withInstances(lst.toSeq).
          withLoadBalancerName(balancer))
  }

  override def create(name:String, desc:String, addrIgnoredByAWS:String,
    zones:Array[String], lis:Array[LbListener], servers:Array[String]) = {

    tstEStrArg("load-balancer-name", name)
    tstObjArg("listeners", lis)
    tstObjArg("zones", zones)
    val lst = if (lis!=null) lis.map { (lb) =>
      val ln= new Listener()
      ln.setProtocol(strProtocol( lb.getNetworkProtocol()))
      ln.setInstancePort(lb.getPrivatePort())
      ln.setLoadBalancerPort(lb.getPublicPort())
      ln
    } else {
      Array[Listener]()
    }
    val res= _svc.cloud().ELB().createLoadBalancer(
        new CreateLoadBalancerRequest().
          withLoadBalancerName(name).
          withListeners(lst.toSeq).
          withAvailabilityZones(zones.toSeq))
    res.getDNSName()
  }

  override def getAddressType() = LoadBalancerAddressType.DNS

  override def getLoadBalancer(balancer:String) = {
    tstEStrArg("load-balancer-name", balancer)
    val res=_svc.cloud().ELB().describeLoadBalancers(
      new DescribeLoadBalancersRequest().withLoadBalancerNames(balancer))
    val lst = res.getLoadBalancerDescriptions()
    if (isNilSeq(lst)) null else toELB( lst.get(0) )
  }

  override def getMaxPublicPorts() = 0

  override def getProviderTermForLoadBalancer(loc:Locale) = "load-balancer"

  override def isAddressAssignedByProvider() = true

  override def isDataCenterLimited() = true

  override def isSubscribed() = {
    try {
      _svc.cloud().ELB().describeLoadBalancers(new DescribeLoadBalancersRequest())
      true
    }
    catch {
      case e:AmazonServiceException =>
        if (testForNotSubError(e,"SubscriptionCheckFailed"
              ,"AuthFailure","SignatureDoesNotMatch"
              ,"OptInRequired","InvalidClientTokenId" )) { false }
        else { throw e }
      case e => throw e
    }
  }

  override def listLoadBalancers() = {
    val res= _svc.cloud().ELB().describeLoadBalancers(new DescribeLoadBalancersRequest())
    res.getLoadBalancerDescriptions().map { (e) => toELB(e) }
  }

  override def listSupportedAlgorithms() = _algos

  override def listSupportedProtocols() = _protocols

  override def remove(balancer:String) {
    tstEStrArg("load-balancer-name", balancer)
    _svc.cloud().ELB().deleteLoadBalancer(new DeleteLoadBalancerRequest()
      .withLoadBalancerName(balancer))
  }

  override def removeDataCenters(balancer:String, zones:String*) {
    tstEStrArg("load-balancer-name", balancer)
    if (!isNilSeq(zones)) {
      _svc.cloud().ELB().disableAvailabilityZonesForLoadBalancer(
        new DisableAvailabilityZonesForLoadBalancerRequest().
        withLoadBalancerName(balancer).
        withAvailabilityZones(zones))
    }
  }

  override def removeServers(balancer:String, servers:String*) {
    tstEStrArg("load-balancer-name", balancer)
    if (!isNilSeq(servers)) {
      val rc= servers.map( (e) => new Instance(e))
      _svc.cloud().ELB().deregisterInstancesFromLoadBalancer(
        new DeregisterInstancesFromLoadBalancerRequest().
        withLoadBalancerName(balancer).
        withInstances(rc))
    }
  }

  override def requiresListenerOnCreate() = true

  override def requiresServerOnCreate() = false

  override def supportsMonitoring() = true

  private def toELB(desc:LoadBalancerDescription) = {

    if (desc != null) {
      val x= _svc.cloud().getContext()
      val b= new LoadBalancer()
      b.setCreationTimestamp(desc.getCreatedTime().getTime())
      b.setProviderRegionId(x.getRegionId())
      b.setAddressType(LoadBalancerAddressType.DNS)
      b.setCurrentState(LoadBalancerState.ACTIVE)
      b.setProviderOwnerId(x.getAccountNumber())
      b.setName(desc.getLoadBalancerName())
      b.setDescription(b.getName())
      b.setProviderLoadBalancerId(b.getName())
      b.setAddress(desc.getDNSName())
      // zones
      if (true) {
        val lst= desc.getAvailabilityZones()
        if (!isNilSeq(lst)) {
          b.setProviderDataCenterIds(lst.toSeq.toArray)
        }
      }
      // servers
      if (true) {
        val lst = desc.getInstances()
        val s = if (!isNilSeq(lst)) {
          lst.map { (n) => n.getInstanceId() }.toArray
        } else {
          Array[String]()
        }
        b.setProviderServerIds(s)
      }
      // listeners/ports
      if (true) {
        val lst= desc.getListenerDescriptions()
        val rc= if (lst != null) {
          lst.map { (e) => toLis( e)  }.toArray
        } else {
          Array[LbListener]()
        }
        b.setListeners(rc)
        val pports = rc.map { (e) => e.getPublicPort() }
        b.setPublicPorts(pports)
      }

      // unsupported
      desc.getHealthCheck();
      desc.getPolicies()
      desc.getSourceSecurityGroup()
      desc.getCanonicalHostedZoneName()
      b
    } else { null }
  }

  private def toLis(desc:ListenerDescription) = {

    if (desc != null) {
      var rc= new LbListener()
      rc.setAlgorithm(LbAlgorithm.ROUND_ROBIN)
      rc.setNetworkProtocol(toLbProtocol( desc.getListener().getProtocol()))
      rc.setPublicPort(desc.getListener().getLoadBalancerPort())
      rc.setPrivatePort(desc.getListener().getInstancePort())
      rc
    } else { null }
  }



}
