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
import scala.collection.mutable.HashMap

import com.zotoh.fwk.util.LoggerFactory._
import com.zotoh.fwk.util.{Logger,CoreImplicits}
import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.util.StrUte._

import java.io.IOException
import java.io.InputStream
import java.security.KeyPair
import java.util.{Properties=>JPS}

import org.dasein.cloud.CloudProvider
import org.dasein.cloud.ProviderContext
import org.dasein.cloud.admin.AdminServices
import org.dasein.cloud.compute.ComputeServices
import org.dasein.cloud.dc.DataCenterServices
import org.dasein.cloud.identity.IdentityServices
import org.dasein.cloud.network.NetworkServices
import org.dasein.cloud.platform.PlatformServices
import org.dasein.cloud.storage.StorageServices

import com.amazonaws.AmazonWebServiceClient
import com.amazonaws.auth.AWSCredentials
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient
import com.amazonaws.services.ec2.AmazonEC2Client
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient
import com.amazonaws.services.rds.AmazonRDSClient
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.AmazonS3EncryptionClient
import com.amazonaws.services.s3.model.EncryptionMaterials
import com.amazonaws.services.simpledb.AmazonSimpleDBClient
import com.amazonaws.services.sns.AmazonSNSClient
import com.amazonaws.services.sqs.AmazonSQSClient
import com.zotoh.cloudapi.core.Vars
import com.zotoh.fwk.io.IOUte._

/**
 * @author kenl
 *
 */
class AWSCloud(ctx:ProviderContext) extends CloudProvider with AWSVars with Vars with CoreImplicits {

  private def ilog() = { _log=getLogger(classOf[AWSCloud]) }
  @transient private var _log:Logger=null
  def tlog() = { if(_log==null) ilog(); _log }

  private var _WEB= Map[String,AmazonWebServiceClient]()

  connect(ctx)
  iniz()

  /**
   * @param region
   * @return
   */
  def newEC2(region:String) = {
    tstEStrArg("region-name", region)
    val ps= getContext().getCustomProperties()
    val cc= new BasicAWSCredentials( ps.gets(P_ID), ps.gets(P_PWD))
    val rc=new AmazonEC2Client(cc)
    setEndpt(rc, "ec2", region)
    rc
  }

  def setAWSSite(region:String) {
    getContext().setRegionId(region)
    _WEB.foreach{ (t) =>
      val rg= t._1 match {
        case "sdb" | "s3" if ("us-east-1"==region) => ""
        case _ => region
      }
      setEndpt(t._2, t._1, rg)
    }
  }

  private def setEndpt(c:AmazonWebServiceClient, key:String,
        region:String, proto:String="https://") {
    val s= proto + key +
      ( if (isEmpty(region)) "" else ("." + region) ) + ".amazonaws.com"
    c.setEndpoint(s)
    if ("ec2" == key) { getContext().setEndpoint(s) }
  }

  override def getAdminServices() = AWSAdminSvcs(this)

  override def getComputeServices() = AWSComputeSvcs(this)

  override def getDataCenterServices() = AWSDataCenterSvcs(this)

  override def getIdentityServices() = AWSIdentitySvcs(this)

  override def getNetworkServices()  = AWSNetworkSvcs(this)

  override def getPlatformServices()  = AWSPlatformSvcs(this)

  override def getStorageServices()  = AWSCloudStorageSvcs(this)

  override def hasComputeServices() = true

  override def hasIdentityServices() = true

  override def hasNetworkServices() = true

  override def hasPlatformServices() = true

  override def hasStorageServices() = true

  override def isConnected() = true

  override def getCloudName() = getContext().getCloudName()

  override def getProviderName() = getContext().getProviderName()

  def SDB()  = {
    _WEB("sdb") match { case x:AmazonSimpleDBClient => x }
  }

  def S3S() = {
    _WEB("s3s") match { case x:AmazonS3EncryptionClient => x }
  }

  def S3() = {
    _WEB("s3") match { case x:AmazonS3Client => x }
  }

  def ELB() = {
    _WEB("elasticloadbalancing") match { case x:AmazonElasticLoadBalancingClient => x }
  }

  def CW()  = {
    _WEB("monitoring") match { case x:AmazonCloudWatchClient => x }
  }

  def AutoScale() = {
    _WEB("autoscaling") match { case x:AmazonAutoScalingClient => x }
  }

  def EC2()  = {
    _WEB("ec2") match { case x:AmazonEC2Client => x }
  }

  def SNS() = {
    _WEB("sns") match { case x:AmazonSNSClient => x }
  }

  def SQS()  = {
    _WEB("sqs") match { case x:AmazonSQSClient => x }
  }

  def RDS() = {
    _WEB("rds") match { case x:AmazonRDSClient => x }
  }

  private def iniz()  {
    mkAWSClients( getContext().getCustomProperties() )
    setAWSSite("us-east-1")
  }

  private def mkAWSClients(ps:JPS) {
    val cc= new BasicAWSCredentials( ps.gets(P_ID), ps.gets(P_PWD))
    _WEB = Map(
    "ec2" -> new AmazonEC2Client(cc),
    "s3" -> new AmazonS3Client(cc),
    "sdb" -> new AmazonSimpleDBClient(cc),
    "elb" -> new AmazonElasticLoadBalancingClient(cc),
    "monitoring" -> new AmazonCloudWatchClient(cc),
    "autoscaling" -> new AmazonAutoScalingClient(cc),
    "sns" -> new AmazonSNSClient(cc),
    "sqs" -> new AmazonSQSClient(cc),
    "rds" -> new AmazonRDSClient(cc),
    "s3s" -> new AmazonS3EncryptionClient(cc,
        new EncryptionMaterials(null.asInstanceOf[KeyPair]))
    )
  }

}

