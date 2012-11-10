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

import java.net.{InetAddress,ServerSocket,Socket}
import java.util.{Properties=>JPS,ResourceBundle}
import java.io.IOException

import com.zotoh.fwk.util.{CmdLineMust,CmdLineQ,CmdLineSeq}
import com.zotoh.fwk.net.NetUte._
import com.zotoh.fwk.net.NetUte
import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.util.ProcessUte._
import com.zotoh.fwk.util.StrUte._
import org.json.{JSONObject=>JSNO}


object TcpIO {
  val PSTR_SOCTIMEOUT="soctoutmillis"
  val PSTR_HOST="host"
  val PSTR_PORT="port"
  val PSTR_BACKLOG="backlog"
}

/**
 * A general TCP socket device.
 *
 * The set of properties:
 *
 * <b>host</b>
 * The hostname where this device is running on, default is localhost.
 * <b>port</b>
 * The port no.
 * <b>soctoutmillis</b>
 * The socket time out in milliseconds, default is 0.
 * <b>backlog</b>
 * The tcp backlog, default is 100.
 * <b>binary</b>
 * Set to boolean true if data is to be treated as binary.
 * <b>encoding</b>
 * Set to character encoding if the data is text, default is utf-8.
 *
 *
 * @see com.zotoh.bedrock.device.Device
 *
 * @author kenl
 *
 */
class TcpIO(devMgr:DeviceMgr) extends Device(devMgr) {

  private var _ssoc:ServerSocket=null
  private var _socTOutMillis=0
  private var _port=0
  private var _backlog=0

  private var _binary=false
  private var _host=""
  private var _enc=""

  /**
   * @return
   */
  def host() = _host

  /**
   * @return
   */
  def port() = _port

  /**
   * @return
   */
  def isBinary() = _binary

  /**
   * @return
   */
  def encoding() = _enc

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.Device#inizWithQuirks(org.json.JSONObject)
   */
  override def inizWithQuirks(pps:JSNO) {
    val socto= pps.optInt( TcpIO.PSTR_SOCTIMEOUT , 0)
    val host= trim(pps.optString( TcpIO.PSTR_HOST ))
    val port= pps.optInt( TcpIO.PSTR_PORT ,-1)
    val enc= trim( pps.optString("encoding") )
    val bin= pps.optBoolean("binary")
    val blog= pps.optInt( TcpIO.PSTR_BACKLOG ,100)

    _host = if (isEmpty(host)) "localhost" else host
    _enc = if (isEmpty(enc)) "utf-8" else enc
    _binary = bin

    tstNonNegIntArg("tcp-port", port)
    _port= port

    tstNonNegIntArg("tcp-backlog", blog)
    _backlog= blog

    tstNonNegIntArg("socket-timeout-millis", socto)
    _socTOutMillis= socto

  }

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.Device#onStart()
   */
  protected def onStart() {
    _ssoc= createSvrSockIt()
    val me=this
    asyncExec( new Runnable() {
      def run() {
        while ( me._ssoc != null) try {
          me.sockItDown( me._ssoc.accept() )
        }
        catch {
           // tlog().warn("", e) ;
          case _ => me.closeSoc()
        }
      }
    })
  }

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.Device#onStop()
   */
  protected def onStop() { closeSoc() }

  private def closeSoc() {
    NetUte.close(_ssoc)
    _ssoc=null
  }

  private def sockItDown(s:Socket) {
    val ev= new TCPEvent(this, s)
    ev.setSocketTimeout(_socTOutMillis)
    ev.setEncoding(_enc)
    ev.setBinary(_binary)
    dispatch(ev)
  }

  private def createSvrSockIt() = {

    val ip= if(isEmpty(_host)) InetAddress.getLocalHost() else InetAddress.getByName(_host)
    var soc= new ServerSocket(_port, _backlog, ip)
    var s:ServerSocket=null
    try {
      soc.setReuseAddress(true)
      s=soc
      soc=null
    } finally {
      NetUte.close(soc)
    }
    tlog().debug("TCP: opened server socket: {} on host: {}" , asJObj(_port) , _host)
    s
  }

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.Device#supportsConfigMenu()
   */
  override def supportsConfigMenu() = true

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.Device#getCmdSeq(java.util.ResourceBundle, java.util.Properties)
   */
  override def getCmdSeq(rcb:ResourceBundle, pps:JPS) = {
    pps.put("backlog", asJObj(100))
    pps.put("soctoutmillis", asJObj(0))
    pps.put("binary", asJObj(true))
    pps.put("encoding", "utf-8")
    val q2= new CmdLineMust("port", bundleStr(rcb,"cmd.port")) {
      def onRespSetOut(a:String, p:JPS) = {
        p.put("port", asJObj(asInt(answer, -1)))
        ""
      }}
    val q1= new CmdLineMust("host", bundleStr(rcb, "cmd.host"), "", localHost()) {
      def onRespSetOut(a:String, p:JPS) = {
        p.put("host", a)
        "port"
      }};
    Some(new CmdLineSeq(super.getCmdSeq(rcb, pps), Array(q1,q2)) {
      def onStart() = q1.label()
    })
  }

}

