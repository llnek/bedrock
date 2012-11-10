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
import com.zotoh.fwk.util.StrUte._

import java.util.Collection
import java.util.Collections
import java.util.{Properties=>JPS}

import org.dasein.cloud.DataFormat
import org.dasein.cloud.compute.Architecture
import org.dasein.cloud.compute.MachineImageState
import org.dasein.cloud.compute.MachineImageType
import org.dasein.cloud.compute.Platform
import org.dasein.cloud.compute.SnapshotState
import org.dasein.cloud.compute.{VirtualMachineProduct=>VMPdt}
import org.dasein.cloud.compute.VmState
import org.dasein.cloud.compute.VolumeState
import org.dasein.cloud.network.LbProtocol
import org.dasein.cloud.platform.EndpointType
import org.json.JSONException
import org.json.{JSONObject=>JSNO}

import com.amazonaws.{AmazonServiceException=>AMZSvcEx}
import com.zotoh.cloudapi.core.CloudAPI._
import com.zotoh.cloudapi.core.CloudAPI
import com.zotoh.fwk.util.{CoreImplicits,JSONUte}

/**
 * @author kenl
 *
 */
trait AWSAPI extends CloudAPI with AWSVars with CoreImplicits {

  private def cfgVMP( t:(Int,String,Int,String,String,Int)) = {
     val p=new VMPdt()
     p.setCpuCount(t._1)
     p.setDescription(t._2)
     p.setDiskSizeInGb(t._3)
     p.setName(t._4)
     p.setProductId(t._5)
     p.setRamInMb(t._6)
     p
  }

  private var _regions:JSNO= _

  /**
   * @param lp
   * @return
   */
  def strProtocol(lp:LbProtocol) = {
    lp match {
      case LbProtocol.HTTP | LbProtocol.HTTPS => "HTTP"
      case LbProtocol.RAW_TCP => "TCP"
      case _ => ""
    }
  }

  /**
   * @param txt
   * @return
   */
  def toLbProtocol(txt:String) = {
    txt match {
      case "HTTPS" => LbProtocol.HTTPS
      case "HTTP" => LbProtocol.HTTP
      case "TCP" => LbProtocol.RAW_TCP
      case _ => null
    }
  }

  /**
   * @param e
   * @param codes
   * @return
   */
  def testSafeNonExistError(e:AMZSvcEx, codes:String*) = {
    val ec=e.getErrorCode()
    codes.exists { (c) => ec == c }
  }

  /**
   * @param e
   * @param codes
   * @return
   */
  def testForNotSubError(e:AMZSvcEx, codes:String*) = {
    val ec=e.getErrorCode()
    e.getStatusCode() match {
      case 401 | 403 => true
      case n if ("SignatureDoesNotMatch"==ec) => true
      case _ => codes.exists((c) => c == ec)
    }
  }

  /**
   * @param s
   * @return
   */
  def toPlat(s:String) = {
    s match {
      case PT_WINDOWS => Platform.WINDOWS
      case _ => Platform.UBUNTU
    }
  }

  /**
   * @param s
   * @return
   */
  def toArch(s:String) = {
    s match {
      case AWS_32BIT => Architecture.I32
      case _ => Architecture.I64
    }
  }

  /**
   * @param s
   * @return
   */
  def toVmState(s:String) = {
    s match {
      case "running" =>  VmState.RUNNING
      case "terminating" | "stopping" => VmState.STOPPING
      case "stopped" =>  VmState.PAUSED
      case "shutting-down" => VmState.STOPPING
      case "terminated" => VmState.TERMINATED
      case "rebooting" => VmState.REBOOTING
      case "pending" | _ => VmState.PENDING
    }
  }

  /**
   * @param str
   * @return
   */
  def toSnapState(str:String) = {
    str match {
      case "available" => SnapshotState.AVAILABLE
      case "deleting" | "deleted" => SnapshotState.DELETED
      case _ => SnapshotState.PENDING
    }
  }

  /**
   * @param str
   * @return
   */
  def toVolState(str:String) = {
    str match {
      case "available" => VolumeState.AVAILABLE
      case "deleting" | "deleted" => VolumeState.DELETED
      case _ => VolumeState.PENDING
    }
  }

  /**
   * @param s
   * @return
   */
  def toImageType(s:String) = {
    s match {
      case "ebs" =>  MachineImageType.VOLUME
      case _ => MachineImageType.STORAGE
    }
  }

  /**
   * @param s
   * @return
   */
  def toImageState(s:String) = {
    s match {
      case"available" => MachineImageState.ACTIVE
      case "deleting" | "deleted" => MachineImageState.DELETED
      case _ => MachineImageState.PENDING
    }
  }

  /**
   * @param fmt
   * @param t
   * @return
   */
  def toProtocol(fmt:DataFormat, t:EndpointType) = {
    t match {
      case EndpointType.AWS_SQS => "sqs"
      case EndpointType.HTTPS => "https"
      case EndpointType.HTTP => "http"
      case EndpointType.EMAIL =>
        if (DataFormat.JSON==fmt) {  "email-json" }
        else { "email" }
      case _ => ""
    }
  }

  /**
   * @param protocol
   * @return
   */
  def toDataFmt(protocol:String):(DataFormat,EndpointType) = {
    protocol match {
      case "email-json" => (DataFormat.JSON, EndpointType.EMAIL)
      case "email" =>  (DataFormat.PLAINTEXT, EndpointType.EMAIL)
      case "https" => (DataFormat.JSON, EndpointType.HTTPS)
      case "http" => (DataFormat.JSON, EndpointType.HTTP)
      case "sqs" => (DataFormat.JSON, EndpointType.AWS_SQS)
      case _ => null
    }
  }

  /* (non-Javadoc)
   * @see com.zotoh.cloudapi.core.CloudAPI#listProducts(int)
   */
  def listProducts(bits:Int):Seq[VMPdt] = {
    bits match {
      case 64 => sort(x86_64.values.toSeq)
      case 32 => sort(i386.values.toSeq)
      case _ => sort(i386_x64.values.toSeq)   // everything
    }
  }

  /* (non-Javadoc)
   * @see com.zotoh.cloudapi.core.CloudAPI#listProductIds(int)
   */
  def listProductIds(bits:Int) = {
    val lst= ArrayBuffer[String]()
    val c = bits match {
      case 64 => x86_64
      case 32 => i386
      case _ => i386_x64
    }
    sort(c.values.toSeq).map { (p) => p.getProductId() }
  }

  /**
   * @param pid
   * @return
   */
  def findProduct(pid:String) = {
    if( pid==null) null else i386_x64(pid)
  }

  private val i386= Map(
    "m1.small"-> cfgVMP(
         (1, "i386/1CU/1.7GB/160GB", 160, "Small Instance", "m1.small", 1700) ),

    "c1.medium" -> cfgVMP(
         (5, "i386/5CU/1.7GB/350GB", 350, "High-CPU Medium Instance", "c1.medium", 1700)),

    "t1.micro" -> cfgVMP(
         (2, "i386/2CU/613MB/EBS", 0, "Micro Instance", "t1.micro", 613))
   )

  private val x86_64= Map(
    "t1.micro" -> cfgVMP(
        (2, "x86_64/2CU/613MB/EBS", 0, "Micro Instance", "t1.micro", 613)),

    "m1.large" -> cfgVMP(
        (4, "x86_64/4CU/7.5GB/850GB", 850, "Large Instance", "m1.large", 7500)),

    "m1.xlarge" -> cfgVMP(
        (8, "x86_64/8CU/15GB/1690GB", 1690, "Extra Large Instance", "m1.xlarge", 15000)),

    "m2.xlarge" -> cfgVMP(
        (7, "x86_64/6.5CU/17.1GB/420GB", 420, "High-Memory Extra Large Instance", "m2.xlarge", 17100)),

    "m2.2xlarge" -> cfgVMP(
        (13, "x86_64/13CU/34.2GB/850GB", 850, "High-Memory Double Extra Large Instance", "m2.2xlarge", 34200)),

    "m2.4xlarge" -> cfgVMP(
        (26, "x86_64/26CU/68.4GB/1690GB", 1690, "High-Memory Quadruple Extra Large Instance", "m2.4xlarge", 68400)),

    "c1.xlarge" -> cfgVMP(
        (20, "x86_64/20CU/7GB/1690GB", 1690, "High-CPU Extra Large Instance", "c1.xlarge", 7000)),

    "cc1.4xlarge" -> cfgVMP(
        (34,
        "x86_64/33.5CU/23GB/1690GB (quad-core \"Nehalem\" architecture)",
        1690,
        "Cluster Compute Quadruple Extra Large Instance",
        "cc1.4xlarge", 23000)),

    "cg1.4xlarge" -> cfgVMP(
        (34,
        "x86_64/33.5CU/22GB/1690GB (quad-core \"Nehalem\" architecture)",
        1690,
        "Cluster GPU Quadruple Extra Large Instance",
        "cg1.4xlarge",
        23000))
  )

  private val i386_x64= Map() ++ x86_64 ++ i386

  /* (non-Javadoc)
   * @see com.zotoh.cloudapi.core.CloudAPI#listRegions()
   */
  override def listRegions() = {
    val it = if (_regions == null) null else _regions.keys()
    val rc=ArrayBuffer[String]()
    if (it != null) while (it.hasNext()) {
      rc += nsb(it.next())
    }
    rc.toSeq
  }

  /* (non-Javadoc)
   * @see com.zotoh.cloudapi.core.CloudAPI#setRegionsAndZones(org.json.JSONObject)
   */
  def setRegionsAndZones(regions:JSNO) {
    tstObjArg("regions object", regions)
    _regions=JSONUte.read( JSONUte.asString(regions) )
  }

  /* (non-Javadoc)
   * @see com.zotoh.cloudapi.core.CloudAPI#listDatacenters(java.lang.String)
   */
  override def listDatacenters(region:String) = {
    val rc= ArrayBuffer[String]()
    if (!isEmpty(region) && _regions != null) try {
      val j= _regions.optJSONObject(region)
      val it2= if (j == null) null else j.keys()
      if (it2 != null) while (it2.hasNext()) {
        rc +=  nsb(it2.next())
      }
    }
    catch { case _ => }
    rc.toSeq
  }


}
