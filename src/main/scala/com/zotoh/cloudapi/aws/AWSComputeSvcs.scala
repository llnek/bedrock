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

import org.dasein.cloud.compute.AbstractComputeServices
import org.dasein.cloud.compute.AutoScalingSupport
import org.dasein.cloud.compute.MachineImageSupport
import org.dasein.cloud.compute.SnapshotSupport
import org.dasein.cloud.compute.VirtualMachineSupport
import org.dasein.cloud.compute.VolumeSupport

import com.zotoh.fwk.util.CoreUte._

object AWSComputeSvcs {}

/**
 * @author kenl
 *
 */
case class AWSComputeSvcs(private val _aws:AWSCloud) extends AbstractComputeServices with AWSService {

  private def ilog() { _log=getLogger(classOf[AWSComputeSvcs]) }
  @transient private var _log:Logger=null
  def tlog() = { if(_log==null) ilog(); _log }

  override def getAutoScalingSupport() = new EC2AutoScale(this)

  override def getImageSupport() = new AMImage(this)

  override def getSnapshotSupport() = new EBSSnapshot(this)

  override def getVirtualMachineSupport() = new EC2Instance(this)

  override def getVolumeSupport() = new EBSVolume(this)

  override def hasAutoScalingSupport() = true

  override def hasImageSupport() = true

  override def hasSnapshotSupport() = true

  override def hasVirtualMachineSupport() = true

  override def hasVolumeSupport() = true

  override def cloud() = _aws

}
