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
import java.util.GregorianCalendar
import java.util.Locale

import org.dasein.cloud.CloudException
import org.dasein.cloud.InternalException
import org.dasein.cloud.OperationNotSupportedException
import org.dasein.cloud.Tag
import org.dasein.cloud.compute.Architecture
import org.dasein.cloud.compute.VirtualMachine
import org.dasein.cloud.compute.{VirtualMachineProduct=>VMP}
import org.dasein.cloud.compute.VirtualMachineSupport
import org.dasein.cloud.compute.VmStatistics
import org.dasein.cloud.identity.ServiceAction

import com.amazonaws.AmazonClientException
import com.amazonaws.AmazonServiceException
import com.amazonaws.services.cloudwatch.model.Datapoint
import com.amazonaws.services.cloudwatch.model.Dimension
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult
import com.amazonaws.services.ec2.model.DescribeInstancesRequest
import com.amazonaws.services.ec2.model.DescribeInstancesResult
import com.amazonaws.services.ec2.model.GetConsoleOutputRequest
import com.amazonaws.services.ec2.model.GetConsoleOutputResult
import com.amazonaws.services.ec2.model.GroupIdentifier
import com.amazonaws.services.ec2.model.Instance
import com.amazonaws.services.ec2.model.MonitorInstancesRequest
import com.amazonaws.services.ec2.model.Placement
import com.amazonaws.services.ec2.model.RebootInstancesRequest
import com.amazonaws.services.ec2.model.Reservation
import com.amazonaws.services.ec2.model.RunInstancesRequest
import com.amazonaws.services.ec2.model.RunInstancesResult
import com.amazonaws.services.ec2.model.StartInstancesRequest
import com.amazonaws.services.ec2.model.StopInstancesRequest
import com.amazonaws.services.ec2.model.TerminateInstancesRequest
import com.amazonaws.services.ec2.model.UnmonitorInstancesRequest


/**
 * @author kenl
 *
 */
class EC2Instance(private val _svc:AWSComputeSvcs ) extends VirtualMachineSupport with AWSAPI {

  private def ilog() { _log=getLogger(classOf[EC2Instance]) }
  @transient private var _log:Logger=null
  def tlog() = { if(_log==null) ilog(); _log }

  override def mapServiceAction(a:ServiceAction) = Array[String]()

  override def boot(server:String) {
    tstEStrArg("instance-id", server)
    _svc.cloud().EC2().startInstances(
      new StartInstancesRequest().withInstanceIds(server))
  }

  override def clone(server:String, toDcId:String, name:String,
      desc:String, powerOn:Boolean, fwalls:String* ):VirtualMachine = {
    throw new OperationNotSupportedException("EC2 instance cannot be cloned.")
  }

  override def disableAnalytics(server:String) {
    tstEStrArg("instance-id", server)
    _svc.cloud().EC2().unmonitorInstances(
        new UnmonitorInstancesRequest().withInstanceIds(server))
  }

  override
  def enableAnalytics(server:String) {
    tstEStrArg("instance-id", server)
    _svc.cloud().EC2().monitorInstances(
        new MonitorInstancesRequest().withInstanceIds(server))
  }

  override def getConsoleOutput(server:String) = {
    tstEStrArg("instance-id", server)
    val res=_svc.cloud().EC2().getConsoleOutput(
        new GetConsoleOutputRequest().withInstanceId(server))
    if (res==null) null else res.getOutput()
  }

  override def getProduct(productId:String) = findProduct(productId)

  override def getProviderTermForServer(loc:Locale) = "instance"

  override def getVMStatistics(server:String, from:Long, to:Long) = {
    tstPosLongArg("from-timestamp", from)
    tstPosLongArg("to-timestamp", to)
    tstEStrArg("instance-id", server)
    val req= new GetMetricStatisticsRequest()
    val cal= new GregorianCalendar()
    cal.setTimeInMillis(from)
    req.setStartTime(cal.getTime())
    cal.setTimeInMillis(to)
    req.setEndTime(cal.getTime())
    req.setDimensions( List(new Dimension().withName("InstanceId").withValue(server) ))
    req.setNamespace("AWS")
    req.setPeriod(60)
    req.setMetricName("")
    req.setStatistics(List("Average", "Minimum", "Maximum"))
    val res= _svc.cloud().CW().getMetricStatistics(req)
    val lst= res.getDatapoints()
    null.asInstanceOf[VmStatistics]
  }

  override def getVMStatisticsForPeriod(server:String,
      begin:Long, end:Long) = {
    Array[VmStatistics]().toSeq
  }

  override def getVirtualMachine(server:String) = {
    tstEStrArg("instance-id", server)
    val res= _svc.cloud().EC2().describeInstances(
        new DescribeInstancesRequest().withInstanceIds(server))
    val lst= res.getReservations()
    val r= if(isNilSeq(lst)) null else lst.get(0)
    if (r == null) null else {
      val li= r.getInstances()
      if (isNilSeq(li)) null else toVM( r.getOwnerId(), li.get(0))
    }
  }

  override def isSubscribed() = {
    try {
      _svc.cloud().EC2().describeInstances()
      true
    }
    catch {
      case e:AmazonServiceException =>
        if ( testForNotSubError(e, "SignatureDoesNotMatch")) { false } else { throw e }
      case e => throw e
    }
  }

  // we embed the region as part of zone => region|zone
  override def launch(ami:String, pd:VMP,
      zone:String, name:String, descOrUserData:String, keypair:String,
      vpcId:String, monitoring:Boolean,
      asImageSandbox:Boolean, fwalls:String*):VirtualMachine = {

    launch(ami,pd,zone,name,descOrUserData,keypair,vpcId,monitoring,asImageSandbox, fwalls.toArray)
  }

  // we embed the region as part of zone => region|zone
  override  def  launch(ami:String, pd:VMP,
      zone:String, name:String, descOrUserData:String, keypair:String,
      vpcId:String, monitoring:Boolean, asImageSandbox:Boolean, fwalls:Array[String],
      tags:Tag*):VirtualMachine = {
    tstEStrArg("image-id", ami)
    tstObjArg("product-type", pd)
    tstEStrArg("keypair", keypair)
    tstEStrArg("zone", zone)
    val req=new RunInstancesRequest().
    withInstanceType(pd.getProductId()).
    withImageId(ami).
    withKeyName(keypair).
    withMaxCount(1).
    withMinCount(1).
    withMonitoring(monitoring)

    if (!isNilSeq(fwalls)) {
      req.withSecurityGroups(fwalls.toSeq)
    }

    val ss= zone.split("\\|")
    _svc.cloud().setAWSSite(ss(0))
    if (ss.length > 1) {
      req.withPlacement(new Placement().withAvailabilityZone( trim( ss(1))) )
    }
    if (!isEmpty(descOrUserData)) {
      req.withUserData(descOrUserData)
    }

    val res=_svc.cloud().EC2().runInstances(req)
    val r= res.getReservation()
    if (r == null) null else {
      val lst= r.getInstances()
      if(isNilSeq(lst)) null else toVM( r.getOwnerId(), lst.get(0))
    }
  }

  override def listFirewalls(server:String) = {
    tstEStrArg("instance-id", server)
    val res= _svc.cloud().EC2().describeInstances(
        new DescribeInstancesRequest().withInstanceIds(server))
    val lst= res.getReservations()
    val r= if(isNilSeq(lst)) null else lst.get(0)
    if (r != null) {
      r.getGroups().map { (e) => e.getGroupName() }
    } else {
      List[String]()
    }
  }

  override def listProducts(arch:Architecture) = {
    arch match {
      case Architecture.I32 =>  listProducts(32)
      case Architecture.I64 => listProducts(64)
      case _ => List[VMP]()
    }
  }

  override def listVirtualMachines() = {
    val res=_svc.cloud().EC2().describeInstances()
    res.getReservations().flatMap { (rr) =>
      rr.getInstances().map { (e) => toVM(rr.getOwnerId(), e) }
    }
  }

  // beware: you can only stop an EBS-backed ami(vm)
  override def pause(server:String) {
    tstEStrArg("instance-id", server)
    _svc.cloud().EC2().stopInstances(
        new StopInstancesRequest().withInstanceIds(server))
  }

  override def reboot(server:String) {
    tstEStrArg("instance-id", server)
    _svc.cloud().EC2().rebootInstances(
        new RebootInstancesRequest().withInstanceIds(server))
  }

  override def supportsAnalytics() = true

  override def terminate(server:String) {
    tstEStrArg("instance-id", server)
    _svc.cloud().EC2().terminateInstances(
        new TerminateInstancesRequest().withInstanceIds(server))
  }

  private def toVM(owner:String, r:Instance) = {
    if (r != null) {
      var vm=new VirtualMachine()
      vm.setPersistent(true);// if EBS backed, yes (i think)
      vm.setProviderOwnerId(owner)
      vm.setCurrentState(toVmState(r.getState().getName()));
      vm.setArchitecture(toArch(r.getArchitecture()))
      vm.setClonable(false)
      vm.setCreationTimestamp(r.getLaunchTime().getTime())
      vm.setDescription("")
      vm.setImagable(true)
      vm.setLastBootTimestamp(vm.getCreationTimestamp())
      vm.setLastPauseTimestamp(0L)
      vm.setName(r.getInstanceId())
      vm.setPausable(false); // only if EBS backed
      vm.setPlatform(toPlat(r.getPlatform()))
      vm.setPrivateDnsAddress(r.getPrivateDnsName())
      vm.setPrivateIpAddresses( Array(nsb(r.getPrivateIpAddress())))
      vm.setProduct( findProduct( r.getInstanceType()))
      vm.setProviderAssignedIpAddressId(r.getPublicDnsName())
      vm.setPublicDnsAddress(r.getPublicDnsName())
      vm.setPublicIpAddresses(Array(nsb(r.getPublicIpAddress())))
      vm.setRebootable(true)
      vm.setTerminationTimestamp(0L)
      vm.setProviderDataCenterId(
          if(r.getPlacement()==null) "" else r.getPlacement().getAvailabilityZone())
      vm.setProviderMachineImageId(r.getImageId())
      vm.setProviderRegionId( _svc.cloud().getContext().getRegionId())
      vm.setProviderVirtualMachineId(r.getInstanceId())

      var s=r.getRootDeviceType()
      if ("ebs"==s) { vm.setPausable(true) }
      vm.addTag("rootdevicetype", s)
      vm
    } else { null }
  }

}
