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

import scala.math._

import java.lang.System._

import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.util.CoreUte
import com.zotoh.fwk.util.LoggerFactory._
import com.zotoh.fwk.util.{Logger,CoreImplicits}
import com.zotoh.fwk.util.ProcessUte._
import com.zotoh.fwk.util.StrUte._

import java.io.InputStream
import java.io.OutputStream
import java.util.{Locale,Properties=>JPS,Collections,ArrayList=>JAL}

import org.dasein.cloud.AsynchronousTask
import org.dasein.cloud.CloudException
import org.dasein.cloud.CloudProvider
import org.dasein.cloud.InternalException
import org.dasein.cloud.OperationNotSupportedException
import org.dasein.cloud.compute.Architecture
import org.dasein.cloud.compute.MachineImage
import org.dasein.cloud.compute.MachineImageFormat
import org.dasein.cloud.compute.MachineImageSupport
import org.dasein.cloud.compute.Platform
import org.dasein.util.CalendarWrapper
import org.dasein.cloud.identity.ServiceAction

import com.amazonaws.AmazonClientException
import com.amazonaws.AmazonServiceException
import com.amazonaws.services.ec2.model.BundleInstanceRequest
import com.amazonaws.services.ec2.model.BundleInstanceResult
import com.amazonaws.services.ec2.model.BundleTask
import com.amazonaws.services.ec2.model.BundleTaskError
import com.amazonaws.services.ec2.model.CreateImageRequest
import com.amazonaws.services.ec2.model.CreateImageResult
import com.amazonaws.services.ec2.model.DeregisterImageRequest
import com.amazonaws.services.ec2.model.DescribeBundleTasksRequest
import com.amazonaws.services.ec2.model.DescribeBundleTasksResult
import com.amazonaws.services.ec2.model.DescribeImageAttributeRequest
import com.amazonaws.services.ec2.model.DescribeImageAttributeResult
import com.amazonaws.services.ec2.model.DescribeImagesRequest
import com.amazonaws.services.ec2.model.DescribeImagesResult
import com.amazonaws.services.ec2.model.Filter
import com.amazonaws.services.ec2.model.Image
import com.amazonaws.services.ec2.model.ImageAttribute
import com.amazonaws.services.ec2.model.LaunchPermission
import com.amazonaws.services.ec2.model.LaunchPermissionModifications
import com.amazonaws.services.ec2.model.ModifyImageAttributeRequest
import com.amazonaws.services.ec2.model.RegisterImageRequest
import com.amazonaws.services.ec2.model.RegisterImageResult
import com.amazonaws.services.ec2.model.S3Storage
import com.amazonaws.services.ec2.model.Storage
import com.amazonaws.services.ec2.util.S3UploadPolicy
import com.zotoh.cloudapi.core.Vars


/**
 * @author kenl
 *
 */
class AMImage(private val _svc:AWSComputeSvcs)
extends MachineImageSupport with CoreImplicits with AWSVars with Vars with AWSAPI {

  private def ilog() { _log=getLogger(classOf[AMImage]) }
  @transient private var _log:Logger=null
  def tlog() = { if(_log==null) ilog(); _log }

  override def mapServiceAction(act:ServiceAction) = Array[String]()

  override def downloadImage(ami:String, out:OutputStream) {
    throw new OperationNotSupportedException()
  }

  override def getMachineImage(ami:String) = {
    tstEStrArg("image-id", ami)
    val res= _svc.cloud().EC2().
      describeImages(new DescribeImagesRequest().withImageIds(ami))
    val lst= res.getImages()
    if( isNilSeq(lst)) null else toMI( lst.get(0) )
  }

  override def getProviderTermForImage(loc:Locale) = "image"

  override def hasPublicLibrary() = true

  override def imageVirtualMachine(vmId:String, name:String, desc:String) = {
    tstEStrArg("description", desc)
    tstEStrArg("vm-id", vmId)
    tstEStrArg("name", name)
    val task = new AsynchronousTask[String]()
    asyncExec(new Runnable() {
      def run() {
        try {
          val res= _svc.cloud().EC2().
            createImage(new CreateImageRequest().withInstanceId(vmId).
            withName(name).withDescription(desc))
          task.completeWithResult( res.getImageId() )
        }
        catch{
          case e => task.complete(e)
        }
      }
    })
    task
  }

  override def imageVirtualMachineToStorage(vmId:String,
      name:String, desc:String, dir:String) = {
    tstEStrArg("directory", dir)
    tstEStrArg("name", name)
    tstEStrArg("instance-id", vmId)
    val pps= _svc.cloud().getContext().getCustomProperties()
    val pwd= pps.gets(P_PWD)
    val uid= pps.gets(P_ID)
    val p= new S3UploadPolicy(uid, pwd, dir, name, 60*12)
    val s3= new S3Storage().
      withAWSAccessKeyId(uid).withBucket(dir).withPrefix(name).
      withUploadPolicy(p.getPolicyString()).
      withUploadPolicySignature(p.getPolicySignature())
    val res= _svc.cloud().EC2().bundleInstance(
      new BundleInstanceRequest().withInstanceId(vmId).
        withStorage(new Storage().withS3(s3)))
    val t= res.getBundleTask()
    val bid = if(t==null) null else t.getBundleId()

    if (isEmpty(bid)) {
      throw new CloudException("Bundle Id is empty")
    }
    val task = new AsynchronousTask[String]()
    val manifest = (dir + "/" + name + ".manifest.xml")
    asyncExec( new Runnable() {
      def run() {
        try { waitForBundle(bid, manifest, task) } catch {
          case e => task.complete(e)
        }
    }})
    task
  }

  override def installImageFromUpload(fmt:MachineImageFormat,
      s:InputStream):String = {
    throw new OperationNotSupportedException()
  }

  override def isImageSharedWithPublic(ami:String) = {
    val m= getMachineImage(ami)
    if(m==null) false else "true" == m.getTag("public")
  }

  override def isSubscribed() = {
    try {
      _svc.cloud().EC2().describeImages(
          new DescribeImagesRequest().
            withOwners(_svc.cloud().getContext().getAccountNumber()))
      true
    }
    catch {
      case e:AmazonServiceException =>
        if (testForNotSubError(e)) false else {  throw  e }
      case e => throw e
    }
  }

  override def listMachineImages() = {
    listAMIs( _svc.cloud().EC2().
      describeImages(new DescribeImagesRequest()) )
  }

  override def listMachineImagesOwnedBy(owner:String) = {
    tstEStrArg("owner-id", owner)
    listAMIs( _svc.cloud().EC2().
      describeImages( new DescribeImagesRequest().withOwners(owner)) )
  }

  override def listShares(ami:String) = {
    tstEStrArg("image-id", ami)
    val res=_svc.cloud().EC2().
      describeImageAttribute( new DescribeImageAttributeRequest().withImageId(ami))
    val attr= res.getImageAttribute()
    if(attr!=null) {
      attr.getLaunchPermissions().map { (p) => p.getUserId() }
    } else {
      List[String]()
    }
  }

  override def listSupportedFormats() = {
    Collections.singletonList(MachineImageFormat.AWS)
  }

  override def registerMachineImage(location:String) = {
    tstEStrArg("image-location", location)
    val res= _svc.cloud().EC2().
      registerImage( new RegisterImageRequest().withImageLocation(location))
    res.getImageId()
  }

  override def remove(ami:String) {
    tstEStrArg("image-id", ami)
    try {
      _svc.cloud().EC2().
          deregisterImage( new DeregisterImageRequest().withImageId(ami))
    }
    catch {
      case e:AmazonServiceException =>
        if ( ! testSafeNonExistError(e, "InvalidAMIID.NotFound")) {
          throw e
        }
      case e => throw e
    }
  }

  override def searchMachineImages(keyword:String,
      pl:Platform, arch:Architecture) = {

    val fs= ArrayBuffer[Filter]()
    val _kw =nsb(keyword)

    fs += new Filter("state", List("available"))
    if (pl!= null && pl.isWindows()) {
      fs += new Filter("platform", List("windows") )
    }
    if (arch != null) {
      fs += new Filter("architecture", List(
           if(Architecture.I32==arch) "i386" else "x86_64" ))
    }

    val res= _svc.cloud().EC2().describeImages(
        new DescribeImagesRequest().withFilters(fs))

    ( ArrayBuffer[MachineImage]() /: res.getImages()) { (rc, g) =>
      val ok = if (!isEmpty(_kw)) {
        hasWithin(_kw, Array(nsb(g.getDescription()),
          nsb(g.getName()), nsb(g.getImageId())))
      } else {
        true
      }
      if (ok) { rc += toMI(g) }
      rc
    }
  }

  override def shareMachineImage(ami:String, acct:String, allow:Boolean) {
    tstEStrArg("image-id", ami)
    val perms= new LaunchPermissionModifications()
    val req= new ModifyImageAttributeRequest().withImageId(ami)
    val lp=new LaunchPermission()
    val lst= List( if(isEmpty(acct)) lp.withGroup("all") else lp.withUserId(acct) )
    if (allow) {   perms.setAdd(lst)   }
    else { perms.setRemove(lst) }
    req.setLaunchPermission(perms)
    _svc.cloud().EC2().modifyImageAttribute(req)
  }

  override def supportsCustomImages() = true

  override def supportsImageSharing() = true

  override def supportsImageSharingWithPublic() = true

  override def transfer(fr:CloudProvider, iid:String):String = {
    throw new OperationNotSupportedException()
  }

  private def listAMIs(res:DescribeImagesResult) = {
    res.getImages().map { (p) => toMI(p) }
  }

  private def toMI(i:Image) = {
    if (i != null) {
      val m= new MachineImage()
      m.setProviderRegionId(_svc.cloud().getContext().getRegionId())
      m.setArchitecture( toArch(i.getArchitecture()) )
      m.setCurrentState( toImageState(i.getState() ))
      m.setProviderMachineImageId(i.getImageId())
      m.setName(i.getName())
      m.setProviderOwnerId(i.getOwnerId())
      m.setSoftware("")
      m.setType( toImageType(i.getRootDeviceType() ))

      m.addTag("manifest-location", nsb( i.getImageLocation()))
      m.addTag("hypervisor", nsb(i.getHypervisor()))
      m.addTag("alias", nsb(i.getImageOwnerAlias()))
      m.addTag("kernel", nsb(i.getKernelId()))
      m.addTag("public", if(i.getPublic()) "true" else "false")
      m.addTag("ramdisk", nsb(i.getRamdiskId()))
      m.addTag("root-dev-name", nsb(i.getRootDeviceName()))
      m.addTag("state-reason", nsb(i.getStateReason()))
      m.addTag("virtualization-type", nsb(i.getVirtualizationType()))

      m.setDescription(i.getDescription())
      m.setPlatform(
          if (nsb(i.getPlatform()).lc.has("windows") )
           Platform.WINDOWS else Platform.UBUNTU)
      m
    } else { null }
  }

  private def waitForBundle(bid:String, manifest:String, task:AsynchronousTask[String]) {
    var failurePoint = -1L
    while( ! task.isComplete() ) {
      val res= _svc.cloud().EC2().describeBundleTasks(
          new DescribeBundleTasksRequest().withBundleIds(bid))
      val lst= res.getBundleTasks()
      val t:BundleTask = if(isNilSeq(lst)) null else lst.get(0)
      if (t != null) {
        val bar=asDouble(t.getProgress(), 0.0)
        // pending | waiting-for-shutdown | storing | canceling | complete | failed
        t.getState() match {
          case "pending" | "waiting-for-shutdown" => task.setPercentComplete(0.0)
          case "complete" => onBundleComplete(manifest, task)
          case "bundling" => task.setPercentComplete(min(50.0, bar/2))
          case "storing" => task.setPercentComplete(min(100.0, 50.0 + bar/2))
          case "failed" => failurePoint= onBundleFailure(failurePoint, t, task)
          case _ => task.setPercentComplete(0.0)
        }
      }
      if (!task.isComplete()) {
        safeThreadWait(1500)
      }
    }
  }

  private def onBundleFailure(lastFailed:Long, t:BundleTask, task:AsynchronousTask[String]) = {
    val e= t.getBundleTaskError()
    var msg= if(e==null) null else e.getMessage()
    var lastF= lastFailed

    if( isEmpty(msg)) {
      if( lastF == -1L ) {
        lastF = currentTimeMillis()
      }
      if( (currentTimeMillis() - lastF) > (CalendarWrapper.MINUTE * 2) ) {
        msg = "Bundle failed without further information."
      }
    }
    if ( !isEmpty(msg) ) {
      task.complete(new CloudException(msg))
    }
    lastF
  }

  private def onBundleComplete(manifest:String, task:AsynchronousTask[String]) {
    task.setPercentComplete(99.0)
    val imageId = registerMachineImage(manifest)
    task.setPercentComplete(100.00)
    task.completeWithResult(imageId)
  }

}
