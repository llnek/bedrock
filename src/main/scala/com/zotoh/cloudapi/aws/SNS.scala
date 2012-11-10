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
import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.util.Logger
import com.zotoh.fwk.util.StrUte._

import java.util.Locale

import org.dasein.cloud.CloudException
import org.dasein.cloud.DataFormat
import org.dasein.cloud.InternalException
import org.dasein.cloud.platform.EndpointType
import org.dasein.cloud.platform.PushNotificationSupport
import org.dasein.cloud.platform.Subscription
import org.dasein.cloud.platform.Topic
import org.dasein.cloud.identity.ServiceAction

import com.amazonaws.AmazonClientException
import com.amazonaws.AmazonServiceException
import com.amazonaws.services.sns.model.ConfirmSubscriptionRequest
import com.amazonaws.services.sns.model.ConfirmSubscriptionResult
import com.amazonaws.services.sns.model.CreateTopicRequest
import com.amazonaws.services.sns.model.CreateTopicResult
import com.amazonaws.services.sns.model.DeleteTopicRequest
import com.amazonaws.services.sns.model.ListSubscriptionsByTopicRequest
import com.amazonaws.services.sns.model.ListSubscriptionsByTopicResult
import com.amazonaws.services.sns.model.ListSubscriptionsRequest
import com.amazonaws.services.sns.model.ListSubscriptionsResult
import com.amazonaws.services.sns.model.ListTopicsRequest
import com.amazonaws.services.sns.model.ListTopicsResult
import com.amazonaws.services.sns.model.PublishRequest
import com.amazonaws.services.sns.model.PublishResult
import com.amazonaws.services.sns.model.SubscribeRequest
import com.amazonaws.services.sns.model.SubscribeResult
import com.amazonaws.services.sns.model.UnsubscribeRequest
import com.amazonaws.services.sns.model.{Topic=>EC2Topic,Subscription=>EC2Subs}


/**
 * @author kenl
 *
 */
class SNS(private val _svc:AWSPlatformSvcs) extends PushNotificationSupport with AWSAPI {

  private def ilog() { _log=getLogger(classOf[SNS]) }
  @transient private var _log:Logger=null
  def tlog() = { if(_log==null) ilog(); _log }

  override def mapServiceAction(a:ServiceAction) = Array[String]()

  override def confirmSubscription(topic:String, token:String, authUnsubscribe:Boolean) = {
    val res=_svc.cloud().SNS().confirmSubscription(
      new ConfirmSubscriptionRequest().
      withAuthenticateOnUnsubscribe(
        if(authUnsubscribe) "true" else "false").
      withTopicArn(topic).
      withToken(token))
    res.getSubscriptionArn()
  }

  override def createTopic(topic:String) = {
    tstEStrArg("topic-name", topic)
    val res=_svc.cloud().SNS().createTopic(
        new CreateTopicRequest().withName(topic))
    val t= new Topic()
    t.setActive(true)
    t.setName(res.getTopicArn())
    t.setProviderOwnerId(_svc.cloud().getContext().getAccountNumber())
    t.setProviderRegionId(_svc.cloud().getContext().getRegionId())
    t.setProviderTopicId(t.getName())
    t
  }

  override def getProviderTermForSubscription(loc:Locale) = "subscription"

  override def getProviderTermForTopic(loc:Locale) = "topic"

  override def isSubscribed() = {
    try {
      _svc.cloud().SNS().listSubscriptions()
      true
    }
    catch {
      case e:AmazonServiceException =>
        if (testForNotSubError(e, "SubscriptionCheckFailed"
          ,"AuthFailure"
          ,"SignatureDoesNotMatch"
          ,"InvalidClientTokenId", "OptInRequired"))  {
          false
        } else { throw e }
      case e => throw e
    }
  }

  override def listSubscriptions(optionalTopicId:String) = {
    val rc=ArrayBuffer[Subscription]()
    var token=""
    do {
      val t = if (isEmpty(optionalTopicId)) {
        listSubs(token)
      } else {
        listSubsWithTopic(optionalTopicId, token)
      }
      token=t._2
      (rc /: t._1) { (b, e) => b += toSub( e); b }
    }
    while (!isEmpty(token))
    rc
  }

  private def listSubsWithTopic(topic:String, nextToken:String) = {
    val req= new ListSubscriptionsByTopicRequest().withTopicArn(topic)
    if (!isEmpty(nextToken)) { req.withNextToken(nextToken) }
    val res=_svc.cloud().SNS().listSubscriptionsByTopic(req)
    ( res.getSubscriptions(), res.getNextToken() )
  }

  private def listSubs(nextToken:String) =  {
    val req= new ListSubscriptionsRequest()
    if (!isEmpty(nextToken)) { req.withNextToken(nextToken) }
    val res = _svc.cloud().SNS().listSubscriptions( req)
    ( res.getSubscriptions(), res.getNextToken() )
  }

  override def listTopics() = {
    val rc=ArrayBuffer[Topic]()
    var token=""
    do {
      val req= new ListTopicsRequest()
      if (!isEmpty(token)) { req.setNextToken(token) }
      val res= _svc.cloud().SNS().listTopics(req)
      token= res.getNextToken()
      (rc /: res.getTopics()) { (b, e) => b += toTopic(e); b }
    }
    while (!isEmpty(token))
    rc
  }

  override def publish(topic:String, subject:String, message:String) = {
    tstEStrArg("message", message)
    tstEStrArg("topic-name", topic)
    tstEStrArg("subject", subject)
    val res=_svc.cloud().SNS().publish(
        new PublishRequest().withTopicArn(topic).
        withSubject(subject).withMessage(message))
    res.getMessageId()
  }

  override def removeTopic(topic:String) = {
    tstEStrArg("topic-name", topic)
    _svc.cloud().SNS().deleteTopic(
        new DeleteTopicRequest().withTopicArn(topic))
  }

  override def subscribe(topic:String, pt:EndpointType, fmt:DataFormat,
      endpt:String) {
    tstEStrArg("topic-name", topic)
    tstEStrArg("endpoint", endpt)
    val res=_svc.cloud().SNS().subscribe(
        new SubscribeRequest().
        withProtocol( toProtocol(fmt, pt)).
        withTopicArn(topic).
        withEndpoint(endpt))
    res.getSubscriptionArn()
    //TODO strange, shouldn't it return subscription-id
  }

  override def unsubscribe(subscription:String) {
    tstEStrArg("subscription-name", subscription)
    _svc.cloud().SNS().unsubscribe(
        new UnsubscribeRequest().withSubscriptionArn(subscription))
  }

  private def toTopic(t:EC2Topic) = {
    if (t != null) {
      val p= new Topic()
      p.setName(t.getTopicArn())
      p.setActive(true)
      p.setDescription(p.getName())
      p.setProviderOwnerId(_svc.cloud().getContext().getAccountNumber())
      p.setProviderTopicId(p.getName())
      p.setProviderRegionId(_svc.cloud().getContext().getRegionId())
      p
    } else { null }
  }

  private def toSub(s:EC2Subs) = {
    if (s != null) {
      val ss= new Subscription()
      ss.setName(s.getSubscriptionArn())
      ss.setProviderOwnerId(s.getOwner())
      ss.setProviderRegionId(_svc.cloud().getContext().getRegionId())
      ss.setProviderSubscriptionId(ss.getName())
      ss.setProviderTopicId(s.getTopicArn())
      ss.setEndpoint(s.getEndpoint())
      var t= toDataFmt(s.getProtocol())
      if (t != null) {
        ss.setEndpointType( t._2)
        ss.setDataFormat(t._1)
      }
      ss
    } else { null }
  }

}
