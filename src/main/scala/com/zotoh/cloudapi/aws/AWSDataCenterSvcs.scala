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

import java.util.{Locale,ArrayList=>JAL}

import org.dasein.cloud.CloudException
import org.dasein.cloud.InternalException
import org.dasein.cloud.dc.DataCenter
import org.dasein.cloud.dc.DataCenterServices
import org.dasein.cloud.dc.Region

import com.amazonaws.services.ec2.model.AvailabilityZone
import com.amazonaws.services.ec2.model.{Region=>EC2Region}
import com.amazonaws.services.ec2.model.DescribeAvailabilityZonesRequest
import com.amazonaws.services.ec2.model.DescribeAvailabilityZonesResult
import com.amazonaws.services.ec2.model.DescribeRegionsRequest
import com.amazonaws.services.ec2.model.DescribeRegionsResult

object AWSDataCenterSvcs {}


/**
 * @author kenl
 *
 */
case class AWSDataCenterSvcs(private val _aws:AWSCloud) extends DataCenterServices
with AWSService {

  private def ilog() { _log=getLogger(classOf[AWSDataCenterSvcs]) }
  @transient private var _log:Logger=null
  def tlog() = { if(_log==null) ilog(); _log }

  override def getDataCenter(zone:String) = {
    tstEStrArg("zone-name", zone)
    val res= _aws.EC2().describeAvailabilityZones(
      new DescribeAvailabilityZonesRequest().withZoneNames(zone))
    val lst= res.getAvailabilityZones()
    if ( isNilSeq(lst)) null else toDC(lst.get(0))
  }

  override def getProviderTermForDataCenter(loc:Locale) = "availability-zone"

  override def getProviderTermForRegion(loc:Locale) = "region"

  override def getRegion(name:String) = {
    tstEStrArg("region-name", name)
    val res= _aws.EC2().describeRegions(
        new DescribeRegionsRequest().withRegionNames(name))
    val lst= res.getRegions()
    if(isNilSeq(lst)) null else toReg(lst.get(0))
  }

  override def listDataCenters(name:String) = {
    val res= _aws.newEC2(name).describeAvailabilityZones(
      new DescribeAvailabilityZonesRequest())
    res.getAvailabilityZones().map { (p) => toDC(p) }
  }

  override def listRegions() = {
    val res= _aws.EC2().describeRegions()
    res.getRegions().map { (p) => toReg(p) }
  }

  override def cloud() = _aws

  private def toDC(z:AvailabilityZone) = {
    if (z != null) {
      val c= new DataCenter()
      c.setAvailable("available" == z.getState())
      c.setActive(c.isAvailable())
      c.setRegionId(z.getRegionName())
      c.setName(z.getZoneName())
      c.setProviderDataCenterId(c.getName())
      c
    } else { null }
  }

  private def toReg(r:EC2Region) =  {
    if (r != null) {
      val g= new Region()
      g.setProviderRegionId(r.getEndpoint())
      g.setActive(true)
      g.setAvailable(true)
      g.setName(r.getRegionName())
      g
    } else { null }
  }

}
