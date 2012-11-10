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

package com.zotoh.bedrock.device

import scala.collection.JavaConversions._

import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.util.StrUte._

import java.util.{Hashtable=>JHT,Properties=>JPS,ResourceBundle}

import javax.jms.Connection
import javax.jms.ConnectionFactory
import javax.jms.Destination
import javax.jms.Message
import javax.jms.MessageConsumer
import javax.jms.MessageListener
import javax.jms.Queue
import javax.jms.QueueConnection
import javax.jms.QueueConnectionFactory
import javax.jms.QueueReceiver
import javax.jms.QueueSession
import javax.jms.Session
import javax.jms.Topic
import javax.jms.TopicConnection
import javax.jms.TopicConnectionFactory
import javax.jms.TopicSession
import javax.jms.TopicSubscriber
import javax.naming.Context
import javax.naming.InitialContext

import org.json.{JSONObject=>JSNO}

import com.zotoh.fwk.util.{CoreImplicits,CmdLineMust,CmdLineQ,CmdLineSeq}
import com.zotoh.fwk.util.WWID._


object JmsIO {

val PSTR_CTXTFAC= "contextfactory"
val PSTR_CONNFAC= "connfactory"
val PSTR_JNDIUSER= "jndiuser"
val PSTR_JNDIPWD= "jndipwd"
val PSTR_JMSUSER= "jmsuser"
val PSTR_JMSPWD= "jmspwd"
val PSTR_DURABLE= "durable"
val PSTR_PROVIDER= "providerurl"
val PSTR_DESTINATION= "destination"

}

/**
 * A JMS client receiver.  The message is not confirmed by default unless an error occurs.  Therefore, the application is
 * responsible for the confirmation to messages.
 *
 * The set of properties:
 *
 * <b>contextfactory</b>
 * The class name of the context factory to be used as part of InitContext().
 * <b>connfactory</b>
 * The name of the connection factory.
 * <b>jndiuser</b>
 * The JNDI username, if any.
 * <b>jndipwd</b>
 * The JNDI user password, if any.
 * <b>jmsuser</b>
 * The username needed for your JMS server.
 * <b>jmspwd</b>
 * The password for your JMS server.
 * <b>durable</b>
 * Set to boolean true if message is persistent.
 * <b>providerurl</b>
 * The provider URL.
 * <b>destination</b>
 * The name of the destination.
 *
 * @see com.zotoh.bedrock.device.Device
 *
 * @author kenl
 *
 */
class JmsIO(devMgr:DeviceMgr) extends Device(devMgr) with CoreImplicits {

  private var _JNDIPwd=""
  private var _JNDIUser=""
  private var _connFac=""
  private var _ctxFac=""
  private var _url=""
  private var _dest=""
  private var _jmsUser=""
  private var _jmsPwd=""

  private var _conn:Option[Connection]=None
  private var _durable=false

  private def onMessage(original:Message) {
    var msg=original
    try {
      dispatch(new JmsEvent(msg,this))
      msg=null
    } catch {
      case e => tlog().error("", e)
    } finally {
      if (msg!=null) block { () => msg.acknowledge() }
    }
  }

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.Device#inizWithProperties(org.json.JSONObject)
   */
  override def inizWithQuirks(pps:JSNO) {
    _ctxFac= trim( pps.optString(JmsIO.PSTR_CTXTFAC))
    _connFac= trim( pps.optString(JmsIO.PSTR_CONNFAC))
    _JNDIUser= trim( pps.optString(JmsIO.PSTR_JNDIUSER))
    _JNDIPwd= trim( pps.optString(JmsIO.PSTR_JNDIPWD))
    _jmsUser= trim( pps.optString(JmsIO.PSTR_JMSUSER))
    _jmsPwd= trim( pps.optString(JmsIO.PSTR_JMSPWD))
    _durable= pps.optBoolean(JmsIO.PSTR_DURABLE)
    _url= trim( pps.optString(JmsIO.PSTR_PROVIDER))
    _dest= trim( pps.optString(JmsIO.PSTR_DESTINATION))
  }

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.Device#onStart()
   */
  override def onStart() { inizConn() }

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.Device#onStop()
   */
  override def onStop() {
    _conn match {
      case Some(c) => using(c) { (c) => }
      case _ =>
    }
    _conn=null
  }

  private def inizConn() {

    val vars= new JHT[String,String]()

    if (!isEmpty(_ctxFac))
    {  vars.put(Context.INITIAL_CONTEXT_FACTORY, _ctxFac) }

    if (!isEmpty(_url))
    {  vars.put(Context.PROVIDER_URL, _url) }

    if (!isEmpty(_JNDIPwd))
    {  vars.put("jndi.password", _JNDIPwd) }

    if (!isEmpty(_JNDIUser))
    {  vars.put("jndi.user", _JNDIUser) }

    val ctx= new InitialContext(vars)
    ctx.lookup(_connFac) match {
      case obj:QueueConnectionFactory => inizQueue(ctx, obj)
      case obj:TopicConnectionFactory => inizTopic(ctx, obj)
      case obj:ConnectionFactory => inizFac(ctx, obj)
      case _ =>
        throw new Exception("JmsIO: unsupported JMS Connection Factory")
    }
    _conn match {
      case Some(c) => c.start()
      case _ =>
    }
  }

  private def inizFac(ctx:Context, obj:Any) {

    val f= obj.asInstanceOf[ConnectionFactory]
    val c= ctx.lookup(_dest)
    val me=this
    val conn= if ( ! isEmpty(_jmsUser)) {
      f.createConnection( _jmsUser, _jmsPwd)
    } else {
      f.createConnection()
    }
    _conn=Some(conn)
    c match {
      case x:Destination =>
        //TODO ? ack always ?
        conn.createSession(false, Session.CLIENT_ACKNOWLEDGE).
        createConsumer(x).
        setMessageListener(new MessageListener(){
          def onMessage(m:Message) { me.onMessage(m) }
        })
      case _ =>
        throw new Exception("JmsIO: Object not of Destination type")
    }
  }

  private def inizTopic(ctx:Context, obj:Any) {

    val f= obj.asInstanceOf[TopicConnectionFactory]
    val me=this
    val conn = if ( ! isEmpty(_jmsUser)) {
      f.createTopicConnection(_jmsUser, _jmsPwd)
    } else {
      f.createTopicConnection()
    }
    _conn=Some(conn)
    val s= conn.createTopicSession(false, Session.CLIENT_ACKNOWLEDGE)
    val t= ctx.lookup(_dest).asInstanceOf[Topic]
    val b = if (_durable) {
      s.createDurableSubscriber(t, newWWID())
    } else {
      s.createSubscriber(t)
    }
    b.setMessageListener( new MessageListener(){
      def onMessage(m:Message) { me.onMessage(m) }
    })
  }

  private def inizQueue(ctx:Context, obj:Any) {

    val f= obj.asInstanceOf[QueueConnectionFactory]
    val q= ctx.lookup(_dest).asInstanceOf[Queue]

    val conn = if ( ! isEmpty(_jmsUser)) {
      f.createQueueConnection(_jmsUser, _jmsPwd)
    } else {
      f.createQueueConnection()
    }
    _conn=Some(conn)
    val s= conn.createQueueSession(false, Session.CLIENT_ACKNOWLEDGE)
    val r= s.createReceiver(q)
    val me=this
    r.setMessageListener(new MessageListener() {
      def onMessage(m:Message) { me.onMessage(m) }
    })
  }

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.Device#supportsConfigMenu()
   */
  override def supportsConfigMenu() = true

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.Device#getCmdSeq(java.util.ResourceBundle, java.util.Properties)
   */
  override def getCmdSeq(rcb:ResourceBundle, pps:JPS) = {
    val q9= new CmdLineMust("dest", bundleStr(rcb, "cmd.jms.dest")) {
      def onRespSetOut(a:String, p:JPS)= {
        p.put(JmsIO.PSTR_DESTINATION, a)
        ""
      }}
    val q8= new CmdLineMust("provurl", bundleStr(rcb, "cmd.jms.purl")) {
      def onRespSetOut(a:String, p:JPS) = {
        p.put(JmsIO.PSTR_PROVIDER, a)
        "dest"
      }}
    val q7= new CmdLineQ("durable", bundleStr(rcb, "cmd.jms.store"), "y/n","n") {
      def onRespSetOut(a:String, p:JPS) = {
        p.put(JmsIO.PSTR_DURABLE, asJObj("Yy".has(a) ))
        "provurl"
      }}
    val q6= new CmdLineQ("jmspwd", bundleStr(rcb, "cmd.jms.pwd")) {
      def onRespSetOut(a:String, p:JPS) = {
        p.put(JmsIO.PSTR_JMSPWD, a)
        "durable"
      }}
    val q5= new CmdLineQ("jmsuser", bundleStr(rcb,"cmd.jms.user")) {
      def onRespSetOut(a:String, p:JPS) = {
        p.put(JmsIO.PSTR_JMSUSER, a)
        "jmspwd"
      }}
    val q4= new CmdLineQ("jndipwd", bundleStr(rcb,"cmd.jndi.pwd")) {
      def onRespSetOut(a:String, p:JPS) = {
        p.put(JmsIO.PSTR_JNDIPWD, a)
        "jmsuser"
      }}
    val q3= new CmdLineQ("jndiuser", bundleStr(rcb,"cmd.jndi.user")) {
      def onRespSetOut(a:String, p:JPS) = {
        p.put(JmsIO.PSTR_JNDIUSER, a)
        "jndipwd"
      }}
    val q2= new CmdLineMust("conn", bundleStr(rcb,"cmd.jms.conn")) {
      def onRespSetOut(a:String, p:JPS) = {
        p.put(JmsIO.PSTR_CONNFAC, a)
        "jndiuser"
      }}
    val q1= new CmdLineMust("ctx", bundleStr(rcb,"cmd.jms.ctx")) {
      def onRespSetOut(a:String, p:JPS) = {
        p.put(JmsIO.PSTR_CTXTFAC, a)
        "conn"
      }}
    Some(new CmdLineSeq(super.getCmdSeq(rcb, pps),
      Array(q1,q2,q3,q4,q5,q6,q7,q8,q9)) {
      def onStart()= q1.label()
    })
  }

}

