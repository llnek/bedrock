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


import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.util.StrUte._
import com.zotoh.fwk.net.NetUte._
import com.zotoh.fwk.io.IOUte
import com.zotoh.fwk.crypto._

import java.io.{File,IOException,InputStream}
import java.net.{InetAddress,URL,UnknownHostException}
import java.security._
//import java.security.cert.CertificateException
import java.util.{Properties=>JPS,ResourceBundle}
import javax.net.ssl.SSLContext
import org.json.{JSONObject=>JSNO}
import com.zotoh.fwk.util.{CmdLineMust,CmdLineQ,CmdLineSeq}


object HttpIOTrait {

  val PSTR_HOST="host"
  val PSTR_PORT="port"
  val PSTR_KEY="serverkey"
  val PSTR_PWD="serverkeypwd"
  val PSTR_SSLTYPE="flavor"

  /**
   * @param createContext
   * @param sslType
   * @param key
   * @param pwd
   * @return
   * @throws NoSuchAlgorithmException
   * @throws UnrecoverableEntryException
   * @throws KeyStoreException
   * @throws CertificateException
   * @throws IOException
   * @throws KeyManagementException
   */
   def cfgSSL(createContext:Boolean, sslType:String, key:URL, pwd:String)
    = {

    val s= if ( key.getFile().endsWith(".jks") )
      JKSStore() else PKCSStore()

    using(key.openStream()) { (inp) =>
      s.init(pwd )
      s.addKeyEntity(inp, pwd )
    }

    val c= if (createContext) SSLContext.getInstance( sslType ) else null
    if (c!=null) {
      c.init( s.keyManagerFactory().getKeyManagers(),
          s.trustManagerFactory().getTrustManagers(),
          Crypto.secureRandom() )
    }

    (s, c)
  }
}



/**
 * Base class to all HTTP oriented devices.
 *
 * The set of properties:
 *
 * <b>host</b>
 * The hostname to run on - default is localhost.
 * <b>port</b>
 * The port to run on.
 * <b>serverkey</b>
 * The full path pointing to the server key file (p12 or jks) file.  If this value is set, SSL is assumed.
 * <b>serverkeypwd</b>
 * The password for the key file.
 *
 * @see com.zotoh.bedrock.device.Device
 *
 * @author kenl
 *
 */
abstract class HttpIOTrait protected(devMgr:DeviceMgr,private var _secure:Boolean) extends Device(devMgr) {

  private var _keyURL:Option[URL]=None
  private var _keyPwd=""
  private var _host=""
  private var _sslType=""
  private var _port=0

  /**
   * @return
   */
  def isSSL() = _secure

  /**
   * @return
   */
  def port() = _port

  /**
   * @return
   */
  def host() = _host

  /**
   * @return
   */
  def sslType() = _sslType

  /**
   * @return
   */
  def keyURL() = _keyURL

  /**
   * @return
   */
  def keyPwd() = _keyPwd

  /**
   * @return
   * @throws UnknownHostException
   */
  def ipAddr() = {
    if (isEmpty(_host)) InetAddress.getLocalHost() else InetAddress.getByName(_host)
  }

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.Device#inizWithProperties(org.json.JSONObject)
   */
  override def inizWithQuirks(pps:JSNO) = {

    var host = trim(pps.optString("host") )
    val port= pps.optInt("port",-1)
    val pwd= trim(pps.optString("serverkeypwd") )
    val key= trim(pps.optString("serverkey") )
    var sslType= trim(pps.optString("flavor") )

    tstNonNegIntArg("port", port)

    if (_secure && isEmpty(key)) {
      tstEStrArg("ssl-key-file", key)
    }

    if (isEmpty(sslType)) {
      sslType= "TLS"
    }

    if (isEmpty(host)) {
      host= ""
    }

    _sslType = sslType
    _port = port
    _host= host

    if ( !isEmpty(key)) {
      tstEStrArg("ssl-key-file-password", pwd)
      _keyURL = Some(new URL(  if (key.startsWith("file:")) key else asFileUrl(new File(key)) ))
      _keyPwd= PwdFactory.mk(pwd).text()
      _secure=true
    }

    this
  }

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.Device#supportsConfigMenu()
   */
  override def supportsConfigMenu() = true

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.Device#getCmdSeq(java.util.ResourceBundle, java.util.Properties)
   */
  override def getCmdSeq(rcb:ResourceBundle, pps:JPS) = {
    pps.put("soctoutmillis", asJObj(0))
    val q7= new CmdLineQ("wtds", bundleStr(rcb,"cmd.work.thds"), "","8") {
      def onRespSetOut(a:String, p:JPS)= {
        p.put("workers", asJObj(asInt(a,8)))
        ""
      }}
    val q6= new CmdLineQ("wait", bundleStr(rcb,"cmd.async.wait"), "","300") {
      def onRespSetOut(a:String, p:JPS) = {
        p.put("waitmillis", asJObj(asInt(a,300)*1000))
        "wtds"
      }}
    val q5= new CmdLineMust("keypwd", bundleStr(rcb,"cmd.key.pwd")) {
      def onRespSetOut(a:String, p:JPS) = {
        p.put("serverkeypwd", a)
        "wait"
      }}
    val q4= new CmdLineMust("keyfile", bundleStr(rcb, "cmd.key.file")) {
      def onRespSetOut(a:String, p:JPS) = {
        p.put("serverkey", a)
        "keypwd"
      }}
    val q3= new CmdLineQ("ssl", bundleStr(rcb, "cmd.use.ssl"), "y/n","n") {
      def onRespSetOut(a:String, p:JPS) = {
        if ("Yy".has(a)) "keyfile" else "wait"
      }}
    val q2= new CmdLineQ("port", bundleStr(rcb,"cmd.port"), "","8080") {
      def onRespSetOut(a:String, p:JPS) = {
        p.put("port", asJObj(asInt(a,8080)))
        "ssl"
      }}
    val q1= new CmdLineQ("host", bundleStr(rcb, "cmd.host"), "", localHost()) {
      def onRespSetOut(a:String, p:JPS) = {
        p.put("host", a)
        "port"
      }}

    Some(new CmdLineSeq(super.getCmdSeq(rcb, pps) ,Array(q1,q2,q3,q4,q5,q6,q7)){
      def onStart() = q1.label()
    })
  }

}


