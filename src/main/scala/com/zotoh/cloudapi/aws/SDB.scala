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
import scala.collection.mutable.{HashMap,ArrayBuffer,LinkedList}

import com.zotoh.fwk.util.LoggerFactory._
import com.zotoh.fwk.util.Logger
import com.zotoh.fwk.util.StrUte._
import com.zotoh.fwk.util.CoreUte._

import java.util.{Locale,Set=>JS,HashSet=>JHS,HashMap=>JHM}

import org.dasein.cloud.CloudException
import org.dasein.cloud.InternalException
import org.dasein.cloud.platform.KeyValueDatabase
import org.dasein.cloud.platform.KeyValueDatabaseSupport
import org.dasein.cloud.platform.KeyValuePair
import org.dasein.cloud.identity.ServiceAction

import com.amazonaws.AmazonClientException
import com.amazonaws.AmazonServiceException
import com.amazonaws.services.simpledb.model.Attribute
import com.amazonaws.services.simpledb.model.CreateDomainRequest
import com.amazonaws.services.simpledb.model.DeleteAttributesRequest
import com.amazonaws.services.simpledb.model.DeleteDomainRequest
import com.amazonaws.services.simpledb.model.DomainMetadataRequest
import com.amazonaws.services.simpledb.model.DomainMetadataResult
import com.amazonaws.services.simpledb.model.GetAttributesRequest
import com.amazonaws.services.simpledb.model.GetAttributesResult
import com.amazonaws.services.simpledb.model.Item
import com.amazonaws.services.simpledb.model.ListDomainsRequest
import com.amazonaws.services.simpledb.model.ListDomainsResult
import com.amazonaws.services.simpledb.model.PutAttributesRequest
import com.amazonaws.services.simpledb.model.ReplaceableAttribute
import com.amazonaws.services.simpledb.model.SelectRequest
import com.amazonaws.services.simpledb.model.SelectResult


/**
 * @author kenl
 *
 */
class SDB(private val _svc:AWSPlatformSvcs) extends KeyValueDatabaseSupport with AWSAPI {

  private def ilog() { _log=getLogger(classOf[SDB]) }
  @transient private var _log:Logger=null
  def tlog() = { if(_log==null) ilog(); _log  }

  override def addKeyValuePairs(domain:String, item:String, vals:KeyValuePair*) {
    modifyKVPairs(false, domain, item, vals)
  }

  override def createDatabase(domain:String, desc:String) = {
    tstEStrArg("domain-name", domain)
    _svc.cloud().SDB().createDomain(
        new CreateDomainRequest().withDomainName(domain))
    domain
  }

  override def mapServiceAction(a:ServiceAction) = Array[String]()

  override def getDatabase(domain:String) = {
    tstEStrArg("domain-name", domain)
    var res=_svc.cloud().SDB().domainMetadata(
      new DomainMetadataRequest().withDomainName(domain))
    toKVD(domain,res)
  }

  override def getKeyValuePairs(domain:String, item:String,
      consistentRead:Boolean) = {
    tstEStrArg("domain-name", domain)
    tstEStrArg("item-name", item)
    var res=_svc.cloud().SDB().getAttributes(
      new GetAttributesRequest().withDomainName(domain).
        withItemName(item).withConsistentRead(consistentRead))
    toKPs( res.getAttributes() )
  }

  override def getProviderTermForDatabase(loc:Locale) = "simpledb"

  override def isSubscribed() = {
    try {
      _svc.cloud().SDB().listDomains(
        new ListDomainsRequest().withMaxNumberOfDomains(10))
      true
    }
    catch {
      case e:AmazonServiceException =>
        if (testForNotSubError(e,  "SubscriptionCheckFailed", "AuthFailure"
            ,"SignatureDoesNotMatch"
            ,"InvalidClientTokenId", "OptInRequired"))  {
          false
        } else { throw e }
      case e => throw e
    }
  }

  override def isSupportsKeyValueDatabases() = true

  override def list() = {
    val rc= ArrayBuffer[String]()
    var token=""
    do {
      val req=new ListDomainsRequest().withMaxNumberOfDomains(100)
      if (!isEmpty(token)) { req.setNextToken(token) }
      val res= _svc.cloud().SDB().listDomains(req)
      token=res.getNextToken()
      (rc /: res.getDomainNames()) { (b, n) => b += n; b }
    }
    while ( !isEmpty(token))
    rc
  }

  override def query(qry:String, consistentRead:Boolean) = {
    var rc= new JHM[String,JS[KeyValuePair]]()
    var token=""
    if (!isEmpty(qry)) do {
      val req=new SelectRequest().withSelectExpression(qry).
        withConsistentRead(consistentRead)
      if (!isEmpty(token)) { req.setNextToken(token) }
      val res=_svc.cloud().SDB().select(req)
      token= res.getNextToken()
      (rc /: res.getItems()) { (m,itm) =>
        m.put(itm.getName(), toKPs(itm.getAttributes()) )
        m
      }
    }
    while (!isEmpty(token))
    rc
  }

  override def removeDatabase(domain:String) {
    tstEStrArg("domain-name", domain)
    _svc.cloud().SDB().deleteDomain(
        new DeleteDomainRequest().withDomainName(domain))
  }

  override def removeKeyValuePairs(domain:String, item:String,
      vals:KeyValuePair*) {
    tstEStrArg("domain-name", domain)
    tstEStrArg("item-name", item)
    _svc.cloud().SDB().deleteAttributes(
      new DeleteAttributesRequest().withAttributes(toAtts(vals)).
      withItemName(item).withDomainName(domain))
  }

  override def removeKeys(domain:String, item:String, keys:String*) {
    tstEStrArg("domain-name", domain)
    tstEStrArg("item-name", item)
    _svc.cloud().SDB().deleteAttributes(
      new DeleteAttributesRequest().withAttributes(toAtts(keys)).
      withItemName(item).withDomainName(domain))
  }

  override def replaceKeyValuePairs(domain:String,item:String, vals:KeyValuePair*) {
    modifyKVPairs(true, domain, item, vals)
  }

  private def modifyKVPairs(replace:Boolean, domain:String, item:String, vals:Seq[KeyValuePair]) {
    tstEStrArg("domain-name", domain)
    tstEStrArg("item-name", item)
    tstNEArray("keyvalue-pairs", vals)
    _svc.cloud().SDB().putAttributes(
      new PutAttributesRequest().withAttributes( toRAtts(replace, vals)).
      withItemName(item).withDomainName(domain))
  }

  private def toRAtts(replace:Boolean, vals:Seq[KeyValuePair]) = {
    val rc= ArrayBuffer[ReplaceableAttribute]()
    if (vals != null) (rc /: vals) { (b, kp) =>
      b += new ReplaceableAttribute().
        withValue(kp.getValue()).
        withName(kp.getKey()).
        withReplace(replace)
      b
    }
    rc.toList
  }

  private def toAtts(vals:Seq[KeyValuePair]) = {
    val rc= ArrayBuffer[Attribute]()
    if (vals != null) (rc /: vals) { (b, kp) =>
      b += new Attribute().withValue(kp.getValue()).withName(kp.getKey())
      b
    }
    rc.toSeq
  }

  private def toAtts(keys:Seq[String]) =  {
    val rc=ArrayBuffer[Attribute]()
    if (keys != null) (rc /: keys) { (b,k) =>
      b += new Attribute().withName(k)
      b
    }
    rc.toList
  }

  private def toKPs(atts:Seq[Attribute]) = {
    val rc= new JHS[KeyValuePair]()
    if (atts != null) (rc /: atts) { (b, a) =>
      val p=new KeyValuePair()
      p.setValue(a.getValue())
      p.setKey(a.getName())
      b.add(p)
      b
    }
    rc
  }

  private def toKVD(domain:String,res:DomainMetadataResult) =  {
    if (res != null) {
      val db=new KeyValueDatabase()
      db.setDescription(domain)
      db.setItemCount(res.getItemCount())
      db.setItemSize( res.getItemNamesSizeBytes().toInt)
      db.setKeyCount(res.getAttributeNameCount())
      db.setKeySize( res.getAttributeNamesSizeBytes().toInt)
      db.setKeyValueCount(res.getAttributeValueCount())
      db.setKeyValueSize(res.getAttributeValuesSizeBytes().toInt)
      db.setName(domain)
      db.setProviderDatabaseId(db.getName())
      db.setProviderOwnerId(_svc.cloud().getContext().getAccountNumber())
      db.setProviderRegionId(_svc.cloud().getContext().getRegionId())
      db
    } else { null }
  }

}

