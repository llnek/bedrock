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

import java.util.Locale

import org.dasein.cloud.CloudException
import org.dasein.cloud.InternalException
import org.dasein.cloud.identity.ShellKeySupport
import org.dasein.cloud.identity.ServiceAction
import org.dasein.cloud.identity.SSHKeypair

import com.amazonaws.AmazonServiceException
import com.amazonaws.services.ec2.model.CreateKeyPairRequest
import com.amazonaws.services.ec2.model.CreateKeyPairResult
import com.amazonaws.services.ec2.model.DeleteKeyPairRequest
import com.amazonaws.services.ec2.model.DescribeKeyPairsRequest
import com.amazonaws.services.ec2.model.DescribeKeyPairsResult
import com.amazonaws.services.ec2.model.KeyPair
import com.amazonaws.services.ec2.model.KeyPairInfo


/**
 * @author kenl
 *
 */
class Keypair(private val _svc:AWSIdentitySvcs) extends ShellKeySupport with AWSAPI {

  private def ilog() { _log=getLogger(classOf[Keypair]) }
  @transient private var _log:Logger=null
  def tlog() = { if(_log==null) ilog(); _log  }

  override def mapServiceAction(a:ServiceAction) = Array[String]()

  override def createKeypair(name:String) = {
    tstEStrArg("keypair-name", name)
    val res= _svc.cloud().EC2().createKeyPair(
      new CreateKeyPairRequest().withKeyName(name))
    val p= res.getKeyPair()
    if (p != null) {
      val kp=new SSHKeypair()
      kp.setPrivateKey(asBytes(p.getKeyMaterial()))
      kp
    } else {
      null
    }
  }

  override def deleteKeypair(name:String) {
    tstEStrArg("keypair-name", name)
    _svc.cloud().EC2().deleteKeyPair(
            new DeleteKeyPairRequest().withKeyName(name))
  }

  override def getFingerprint(name:String) = {
    tstEStrArg("keypair-name", name)
    try {
      val res= _svc.cloud().EC2().describeKeyPairs(
              new DescribeKeyPairsRequest().withKeyNames(name))
      val lst=  res.getKeyPairs()
      val p= if(isNilSeq(lst)) null else lst.get(0)
      if (p==null) "" else p.getKeyFingerprint()
    }
    catch {
      case e:AmazonServiceException =>
        if (!testSafeNonExistError(e, "InvalidKeyPair.NotFound")) {
          throw e
        }
      case e => throw e
    }
    ""
  }

  def getKeypair(id:String):SSHKeypair = null

  override def isSubscribed() = true

  override def getProviderTermForKeypair(loc:Locale) = "keypair"

  override def  list() = {
    val res= _svc.cloud().EC2().describeKeyPairs(new DescribeKeyPairsRequest())
    res.getKeyPairs().map { (e) =>
      val kp=new SSHKeypair()
      kp.setName(e.getKeyName())
      kp
    }
  }

}
