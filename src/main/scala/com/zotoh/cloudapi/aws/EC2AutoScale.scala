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

import org.dasein.cloud.CloudException
import org.dasein.cloud.InternalException
import org.dasein.cloud.OperationNotSupportedException
import org.dasein.cloud.compute.AutoScalingSupport
import org.dasein.cloud.compute.LaunchConfiguration
import org.dasein.cloud.compute.ScalingGroup
import org.dasein.cloud.compute.VirtualMachineProduct
import org.dasein.cloud.identity.ServiceAction

import com.amazonaws.services.autoscaling.model.{LaunchConfiguration=>EC2LCfg}
import com.amazonaws.services.autoscaling.model.AutoScalingGroup
import com.amazonaws.services.autoscaling.model.CreateAutoScalingGroupRequest
import com.amazonaws.services.autoscaling.model.CreateLaunchConfigurationRequest
import com.amazonaws.services.autoscaling.model.DeleteAutoScalingGroupRequest
import com.amazonaws.services.autoscaling.model.DeleteLaunchConfigurationRequest
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult
import com.amazonaws.services.autoscaling.model.DescribeLaunchConfigurationsRequest
import com.amazonaws.services.autoscaling.model.DescribeLaunchConfigurationsResult
import com.amazonaws.services.autoscaling.model.SetDesiredCapacityRequest
import com.amazonaws.services.autoscaling.model.UpdateAutoScalingGroupRequest


/**
 * @author kenl
 *
 */
class EC2AutoScale(private val _svc:AWSComputeSvcs) extends AutoScalingSupport {

  private def ilog() { _log=getLogger(classOf[EC2AutoScale]) }
  @transient private var _log:Logger=null
  def tlog() = { if(_log==null) ilog(); _log }

  override def mapServiceAction(a:ServiceAction)=Array[String]()

  override def isSubscribed() = true

  override
  def createAutoScalingGroup(name:String, cfgId:String, minServers:Int,
      maxServers:Int, coolDown:Int, zones:String*) = {
    tstEStrArg("autoscale-group-name", name)
    tstNonNegIntArg("max-servers", maxServers)
    tstNonNegIntArg("min-servers", minServers)
    tstNonNegIntArg("cool-down", coolDown)
    tstNEArray("zones", zones)

    _svc.cloud().AutoScale().createAutoScalingGroup(
      new CreateAutoScalingGroupRequest().
      withLaunchConfigurationName(cfgId).
      withAutoScalingGroupName(name).
      withMaxSize(maxServers).
      withMinSize(minServers).
      withDefaultCooldown(coolDown).
      withAvailabilityZones(zones))

    name
  }

  override def createLaunchConfiguration(name:String, ami:String,
      pd:VirtualMachineProduct, fwalls:String*) = {
    tstEStrArg("launch-config-name", name)
    tstEStrArg("image-id", ami)
    tstObjArg("product-type", pd)
    tstNEArray("firewalls", fwalls)
    _svc.cloud().AutoScale().createLaunchConfiguration(
        new CreateLaunchConfigurationRequest().
        withLaunchConfigurationName(name).
        withInstanceType(pd.getProductId()).
        withSecurityGroups(fwalls).
        withImageId(ami))
    name
  }

  override def deleteAutoScalingGroup(name:String) {
    tstEStrArg("autoscale-group-name", name)
    _svc.cloud().AutoScale().deleteAutoScalingGroup(
        new DeleteAutoScalingGroupRequest().
            withAutoScalingGroupName(name))
  }

  override def deleteLaunchConfiguration(name:String) {
    tstEStrArg("launch-config-name",name)
    _svc.cloud().AutoScale().deleteLaunchConfiguration(
        new DeleteLaunchConfigurationRequest().
              withLaunchConfigurationName(name))
  }

  override def getLaunchConfiguration(name:String) = {
    tstEStrArg("launch-config-name", name)
    val res=_svc.cloud().AutoScale().describeLaunchConfigurations(
      new DescribeLaunchConfigurationsRequest().
            withLaunchConfigurationNames(name))
    val lst= res.getLaunchConfigurations()
    if(isNilSeq(lst)) null else toLCfg( lst.get(0) )
  }

  override def getScalingGroup(name:String) = {
    val res=_svc.cloud().AutoScale().describeAutoScalingGroups(
      new DescribeAutoScalingGroupsRequest())
    val lst= res.getAutoScalingGroups()
    if(isNilSeq(lst)) null else toSG( lst.get(0))
  }

  override def  listLaunchConfigurations() = {
    val res=_svc.cloud().AutoScale().describeLaunchConfigurations()
    res.getLaunchConfigurations().map { (e) => toLCfg(e) }
  }

  override  def listScalingGroups() = {
    val res=_svc.cloud().AutoScale().describeAutoScalingGroups()
    res.getAutoScalingGroups().map { (e) =>
      toSG( e)
    }
  }

  override def setDesiredCapacity(group:String, capacity:Int) {
    tstEStrArg("autoscale-group-name", group)
    tstNonNegIntArg("capacity", capacity)
    _svc.cloud().AutoScale().setDesiredCapacity(
        new SetDesiredCapacityRequest().
        withDesiredCapacity(capacity).
        withAutoScalingGroupName(group))
  }

  override def setTrigger(name:String, group:String, statistic:String,
      unitOfMeasure:String, metric:String, periodSecs:Int, lowerThreshold:Double,
      upperThreshold:Double, lowerIncr:Int, lowerIncrAbsolute:Boolean, upperIncr:Int,
      upperIncrAbsolute:Boolean, breachDuration:Int) = {
    throw new OperationNotSupportedException()
  }

  override def updateAutoScalingGroup(group:String, cfgId:String, minServers:Int,
      maxServers:Int, coolDown:Int, zones:String*) {

    _svc.cloud().AutoScale().updateAutoScalingGroup(
      new UpdateAutoScalingGroupRequest().
      withAutoScalingGroupName(group).
      withDefaultCooldown(coolDown).
      withLaunchConfigurationName(cfgId).
      withMaxSize(maxServers).
      withMinSize(minServers).
      withAvailabilityZones(zones))
  }

  private def toSG(g:AutoScalingGroup) = {
    if (g != null) {
      val s= new ScalingGroup()
      s.setCooldown(g.getDefaultCooldown())
      s.setCreationTimestamp(g.getCreatedTime().getTime())
      s.setDescription("")
      s.setMaxServers(g.getMaxSize())
      s.setMinServers(g.getMinSize())
      s.setName(g.getAutoScalingGroupName())
      s.setProviderDataCenterIds(g.getAvailabilityZones().toSeq.toArray[String])
      s.setProviderLaunchConfigurationId(g.getLaunchConfigurationName())
      s.setProviderOwnerId(_svc.cloud().getContext().getAccountNumber())
      s.setProviderRegionId(_svc.cloud().getContext().getRegionId())
      s.setProviderScalingGroupId(s.getName())
      var ls= ArrayBuffer[String]()
      var lst=g.getInstances()
      if (lst != null) for ( i <- 0 until lst.size()) {
        ls += lst.get(i).getInstanceId()
      }
      s.setProviderServerIds(ls.toArray)
      s.setTargetCapacity(g.getDesiredCapacity())
      s
    } else { null }
  }

  private def toLCfg(c:EC2LCfg) = {
    if (c != null) {
      var g= new LaunchConfiguration()
      g.setCreationTimestamp(c.getCreatedTime().getTime())
      g.setName(c.getLaunchConfigurationName())
      var lst= c.getSecurityGroups()
      var ss= if (lst != null) {
        lst.toSeq.toArray[String]
      } else {
        Array[String]()
      }
      g.setProviderFirewallIds(ss)
      g.setProviderImageId(c.getImageId())
      g.setProviderLaunchConfigurationId(g.getName())
      g.setServerSizeId(c.getInstanceType())
      g
    } else { null }

  }

}
