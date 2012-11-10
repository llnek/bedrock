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

import org.dasein.cloud.network.AbstractNetworkServices
import org.dasein.cloud.network.DNSSupport
import org.dasein.cloud.network.FirewallSupport
import org.dasein.cloud.network.IpAddressSupport
import org.dasein.cloud.network.LoadBalancerSupport
import org.dasein.cloud.network.VLANSupport

import com.zotoh.fwk.util.CoreUte._

object AWSNetworkSvcs {}


/**
 * @author kenl
 *
 */
case class AWSNetworkSvcs(private val _aws:AWSCloud) extends AbstractNetworkServices
with AWSService {

  private def ilog() { _log=getLogger(classOf[AWSNetworkSvcs]) }
  @transient private var _log:Logger=null
  def tlog() = { if(_log==null) ilog(); _log }

  override def getDnsSupport() = new DNSRoute53(this)

  override def getFirewallSupport() = new SecurityGroup(this)

  override def getIpAddressSupport() = new ElasticIP(this)

  override def getLoadBalancerSupport() = new ElasticLoadBalancer(this)

  override def getVlanSupport() = new VPC(this)

  override def hasDnsSupport() = true

  override def hasFirewallSupport() = true

  override def hasIpAddressSupport() = true

  override def hasLoadBalancerSupport() = true

  override def hasVlanSupport() = true

  override def cloud() = _aws

}
