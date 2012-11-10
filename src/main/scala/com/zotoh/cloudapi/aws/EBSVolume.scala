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


import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.util.LoggerFactory._
import com.zotoh.fwk.util.Logger
import com.zotoh.fwk.util.StrUte._

import java.util.{Locale,ArrayList=>JAL}

import org.dasein.cloud.CloudException
import org.dasein.cloud.InternalException
import org.dasein.cloud.compute.Platform
import org.dasein.cloud.compute.Volume
import org.dasein.cloud.compute.VolumeSupport
import org.dasein.cloud.identity.ServiceAction

import com.amazonaws.AmazonClientException
import com.amazonaws.AmazonServiceException
import com.amazonaws.services.ec2.model.AttachVolumeRequest
import com.amazonaws.services.ec2.model.CreateVolumeRequest
import com.amazonaws.services.ec2.model.CreateVolumeResult
import com.amazonaws.services.ec2.model.DeleteVolumeRequest
import com.amazonaws.services.ec2.model.DescribeVolumesRequest
import com.amazonaws.services.ec2.model.DescribeVolumesResult
import com.amazonaws.services.ec2.model.DetachVolumeRequest
import com.amazonaws.services.ec2.model.VolumeAttachment
import com.amazonaws.services.ec2.model.{Volume=>EC2Volume}


/**
 * @author kenl
 *
 */
class EBSVolume(private val _svc:AWSComputeSvcs ) extends VolumeSupport with AWSAPI {

  private def ilog() { _log=getLogger(classOf[EBSVolume]) }
  @transient private var _log:Logger=null
  def tlog() = { if(_log==null) ilog(); _log }

  override def  mapServiceAction(a:ServiceAction) = Array[String]()

  override def attach(vol:String, server:String, dev:String) {
    tstEStrArg("instance-id", server)
    tstEStrArg("volume-id", vol)
    tstEStrArg("dev-id", dev)
    _svc.cloud().EC2().attachVolume(
      new AttachVolumeRequest().withVolumeId(vol).
      withInstanceId(server).withDevice(dev))
  }

  override def create(snapId:String, sizeGB:Int, zone:String) = {
    tstEStrArg("datacenter/zone", zone)
    tstPosIntArg("sizeGB", sizeGB)
    val req=new CreateVolumeRequest().
      withAvailabilityZone(zone).withSize(sizeGB)
    if (!isEmpty(snapId)) {
      req.withSnapshotId(snapId)
    }
    val res=_svc.cloud().EC2().createVolume(req)
    val v= res.getVolume()
    if (v==null) null else v.getVolumeId()
  }

  override def detach(vol:String) {
    tstEStrArg("volume-id", vol)
    _svc.cloud().EC2().detachVolume(
      new DetachVolumeRequest().withVolumeId(vol))
  }

  override def getProviderTermForVolume(loc:Locale) = "volume"

  override def getVolume(vol:String) = {
    tstEStrArg("volume-id", vol)
    val res= _svc.cloud().EC2().describeVolumes(
        new DescribeVolumesRequest().withVolumeIds(vol))
    val lst= res.getVolumes()
    if (isNilSeq(lst)) null else toVol(lst.get(0) )
  }

  override def isSubscribed() = {
    try {
      _svc.cloud().EC2().describeVolumes()
      true
    }
    catch {
      case e:AmazonServiceException =>
        if (testForNotSubError(e)) { false } else { throw e }
      case e => throw e
    }
  }

  override def listPossibleDeviceIds(pl:Platform) = {
    tstObjArg("platform", pl)
    val rc= if( pl.isWindows() ) {
      Array("xvdf","xvdg", "xvdh", "xvdi", "xvdj")
    }
    else {
      Array("/dev/sdf", "/dev/sdg",  "/dev/sdh", "/dev/sdi", "/dev/sdj")
    }
    rc.toSeq
  }

  override def listVolumes() = {
    val res= _svc.cloud().EC2().describeVolumes()
    res.getVolumes().map { (e) => toVol(e) }
  }

  override def remove(vol:String) {
    tstEStrArg("volume-id", vol)
    try {
      _svc.cloud().EC2().deleteVolume(
        new DeleteVolumeRequest().withVolumeId(vol))
    }
    catch {
      case e:AmazonServiceException =>
        if (! testSafeNonExistError(e, "InvalidVolume.NotFound")) { throw e }
      case e => throw e
    }
  }

  private def toVol(v:EC2Volume) = {
    if (v != null) {
      val vol= new Volume()
      vol.setSizeInGigabytes(v.getSize())
      vol.setName(v.getVolumeId())
      vol.setProviderVolumeId(vol.getName())
      vol.setCreationTimestamp(v.getCreateTime().getTime())
      vol.setProviderSnapshotId(v.getSnapshotId())
      vol.setCurrentState( toVolState(v.getState()))
      vol.setProviderDataCenterId(v.getAvailabilityZone())
      vol.setProviderRegionId(_svc.cloud().getContext().getRegionId())
      vol.setProviderVirtualMachineId("")
      var atts=v.getAttachments()
      if (!isNilSeq(atts)) {
        vol.setProviderVirtualMachineId( atts.get(0).getInstanceId())
        vol.setDeviceId( atts.get(0).getDevice())
      }
      vol
    } else { null }

  }

}
