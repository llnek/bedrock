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

import java.util.{Locale,ArrayList=>JAL}

import org.dasein.cloud.CloudException
import org.dasein.cloud.InternalException
import org.dasein.cloud.compute.Snapshot
import org.dasein.cloud.compute.SnapshotSupport
import org.dasein.cloud.identity.ServiceAction

import com.amazonaws.AmazonClientException
import com.amazonaws.AmazonServiceException
import com.amazonaws.services.ec2.model.CreateSnapshotRequest
import com.amazonaws.services.ec2.model.CreateSnapshotResult
import com.amazonaws.services.ec2.model.CreateVolumePermission
import com.amazonaws.services.ec2.model.CreateVolumePermissionModifications
import com.amazonaws.services.ec2.model.{Snapshot=>EC2Snapshot}

import com.amazonaws.services.ec2.model.DeleteSnapshotRequest
import com.amazonaws.services.ec2.model.DescribeSnapshotAttributeRequest
import com.amazonaws.services.ec2.model.DescribeSnapshotAttributeResult
import com.amazonaws.services.ec2.model.DescribeSnapshotsRequest
import com.amazonaws.services.ec2.model.DescribeSnapshotsResult
import com.amazonaws.services.ec2.model.ModifySnapshotAttributeRequest



/**
 * @author kenl
 *
 */
class EBSSnapshot(private val _svc:AWSComputeSvcs ) extends SnapshotSupport with AWSAPI {

  private def ilog() { _log=getLogger(classOf[EBSSnapshot]) }
  @transient private var _log:Logger=null
  def tlog() = { if(_log==null) ilog(); _log  }

  override def mapServiceAction(act:ServiceAction) = Array[String]()

  override def create(volId:String, desc:String) = {
    tstEStrArg("volume-id", volId)
    val res = _svc.cloud().EC2().createSnapshot(
      new CreateSnapshotRequest().withVolumeId(volId)
      .withDescription(nsb(desc)))
    val s=res.getSnapshot()
    if (s==null) null else s.getSnapshotId()
  }

  override def getProviderTermForSnapshot(loc:Locale) = "snapshot"

  override def getSnapshot(snap:String) = {
    tstEStrArg("snapshot-id", snap)
    val res= _svc.cloud().EC2().describeSnapshots(
        new DescribeSnapshotsRequest().withSnapshotIds(snap))
    val lst= res.getSnapshots()
    if (isNilSeq(lst)) null else toSnap(lst.get(0))
  }

  override def isPublic(snap:String) = {
    val res= _svc.cloud().EC2().describeSnapshotAttribute(
      new DescribeSnapshotAttributeRequest().withAttribute("createVolumePermission").withSnapshotId(snap))

    val lst= res.getCreateVolumePermissions()
    if (lst!=null) lst.exists((p) => ("all" == p.getGroup())) else false
  }

  override def isSubscribed() = {
    try {
      _svc.cloud().EC2().describeSnapshots(
          new DescribeSnapshotsRequest().withOwnerIds(_svc.cloud().getContext().getAccountNumber()))
      true
    }
    catch {
      case e:AmazonServiceException =>
        if ( testForNotSubError(e)) { false } else { throw e }
      case e => throw e
    }
  }

  override def listShares(snap:String) = {
    val res= _svc.cloud().EC2().describeSnapshotAttribute(
      new DescribeSnapshotAttributeRequest().
          withAttribute("createVolumePermission").
          withSnapshotId(snap))
    res.getCreateVolumePermissions().map { (p) =>  p.getUserId() }
  }

  override def listSnapshots() = {
    val res= _svc.cloud().EC2().describeSnapshots()
    res.getSnapshots().map { (e) => toSnap( e) }
  }

  override def remove(snap:String) {
    tstEStrArg("snapshot-id", snap)
    try {
      _svc.cloud().EC2().deleteSnapshot(
        new DeleteSnapshotRequest().withSnapshotId(snap))
    }
    catch {
      case e:AmazonServiceException =>
        if ( !testSafeNonExistError(e, "InvalidSnapshot.NotFound")) { throw e }
      case e => throw e
    }
  }

  override def shareSnapshot(snapId:String, acct:String, share:Boolean) {
    tstEStrArg("snapshot-id", snapId)
    val perms = new CreateVolumePermissionModifications()
    val cp=new CreateVolumePermission()
    val lst= Array[CreateVolumePermission](if (isEmpty(acct)) cp.withGroup("all") else cp.withUserId(acct))
    if (share) { perms.setAdd(lst.toSeq) }
    else { perms.setRemove(lst.toSeq) }

    _svc.cloud().EC2().modifySnapshotAttribute(
      new ModifySnapshotAttributeRequest().
        withCreateVolumePermission(perms).
        withSnapshotId(snapId))
  }

  override def supportsSnapshotSharing() = true

  override def supportsSnapshotSharingWithPublic() = true

  private def toSnap(s:EC2Snapshot) = {
    if (s != null) {
      val ss= new Snapshot()
      ss.setCurrentState(toSnapState(s.getState()))
      ss.setDescription(s.getDescription());
      ss.setName(s.getSnapshotId())
      ss.setOwner(s.getOwnerId())
      ss.setProviderSnapshotId(ss.getName())
      ss.setRegionId(_svc.cloud().getContext().getRegionId())
      ss.setSizeInGb(s.getVolumeSize())
      ss.setSnapshotTimestamp(s.getStartTime().getTime())
      ss.setVolumeId(s.getVolumeId())
      ss.setProgress(s.getProgress())
      ss
    } else { null }
  }

}
