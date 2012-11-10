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

object PopIO {
  val PSTR_HOST="host"
  val PSTR_PORT="port"
  val PSTR_USER="user"
  val PSTR_PWD="pwd"
  val PSTR_DELMSG="deletemsg"
  val PSTR_SSL="ssl"
}

import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.util.StrUte._

import java.util.{Properties=>JPS,ResourceBundle}
import java.lang.System._

import javax.mail.Flags
import javax.mail.Folder
import javax.mail.Message
import javax.mail.Provider
import javax.mail.Session
import javax.mail.Store
import javax.mail.internet.MimeMessage

import org.json.{JSONObject=>JSNO}

import com.zotoh.fwk.crypto.PwdFactory
import com.zotoh.fwk.util.{CoreImplicits,CmdLineMust,CmdLineQ,CmdLineSeq}
import com.zotoh.fwk.io.XData
import com.zotoh.fwk.io.IOUte._


/**
 * A device acting as a POP3 client which pings the POP3 server periodically to check for new emails.
 *
 * The set of properties:
 *
 * <b>host</b>
 * The POP3 hostname.
 * <b>port</b>
 * The POP3 port, default is 110.
 * <b>ssl</b>
 * Set to boolean true if the connection to the POP3 server is secured, default is true.
 * <b>user</b>
 * The POP3 user login id.
 * <b>pwd</b>
 * The POP3 login password.
 * <b>deletemsg</b>
 * Set to boolean true is the message is marked as deleted, default is false.
 *
 * @see com.zotoh.bedrock.device.RepeatingTimer
 *
 * @author kenl
 *
 */
class PopIO(devMgr:DeviceMgr) extends ThreadedTimer(devMgr) with CoreImplicits {

//  private final String ST_IMAP= "com.sun.mail.imap.IMAPStore" ;
//  private final String ST_IMAPS=  "com.sun.mail.imap.IMAPSSLStore"
  private val ST_POP3S=  "com.sun.mail.pop3.POP3SSLStore"
  private val ST_POP3=  "com.sun.mail.pop3.POP3Store"
  private val MOCK_STORE="bedrock.pop3.mockstore"
//  private final String CTL= "content-length"
  private val POP3S="pop3s"
  private val POP3="pop3"

  private var _user=""
  private var _pwd=""
  private var _host=""
  private var _storeImpl=""
  private var _port=0
  private var _ssl=false
  private var _delete=false

  private var _pop:Store = _
  private var _fd:Folder = _

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.RepeatingTimer#inizWithProperties(org.json.JSONObject)
   */
  override def inizWithQuirks(pps:JSNO) {
    super.inizWithQuirks(pps)

    val mock1= nsb(deviceMgr().engine().quirk(MOCK_STORE))
    val mock2= getProperties().getProperty(MOCK_STORE)

    //safePutProp( "provider", "com.zotoh.bedrock.mock.mail.MockPop3Store");

    val host= trim(pps.optString(PopIO.PSTR_HOST) )
    val port= pps.optInt(PopIO.PSTR_PORT, 110)

    _storeImpl = trim(pps.optString("provider") )

    _delete= pps.optBoolean(PopIO.PSTR_DELMSG, false)
    _ssl= pps.optBoolean(PopIO.PSTR_SSL, false)

    _user = trim(pps.optString(PopIO.PSTR_USER) )
    _pwd= trim(pps.optString(PopIO.PSTR_PWD) )

    if (!isEmpty(_pwd)) {
      _pwd= PwdFactory.mk(_pwd).text()
    }

    tstPosIntArg("pop3-port", port)
    //tstEStrArg("host", host);
    _host= host
    _port=port

    // this is really for testing only, points to a mock store
    if (!isEmpty(mock2)) {
      _storeImpl=mock2
    } else if (!isEmpty(mock1)) {
      _storeImpl=mock1
    }
  }

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.ThreadedTimer#preLoop()
   */
  override def preLoop() {
    _pop=null
    _fd= null
  }

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.ThreadedTimer#endLoop()
   */
  override def endLoop() { closePOP() }

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.ThreadedTimer#onOneLoop()
   */
  override def onOneLoop() {
    if ( conn() ) {
      try {
        scanPOP()
      }
      catch {
        case e => tlog().warn("",e)
      }
      finally {
        closeFolder()
      }
    }
  }

  private def scanPOP() {
    openFolder()
    grabMsgs()
  }

  private def grabMsgs() {

    val cnt= _fd.getMessageCount()

    tlog().debug("PopIO: count of new messages: {}" , asJObj(cnt))
    if (cnt <= 0)
    return

    val hds=new StringBuilder(512)
    val me=this
    _fd.getMessages().foreach { (m) =>
      val mm= m.asInstanceOf[MimeMessage]
      //TODO
      //_fd.getUID(mm)
      // read all the header lines
      hds.setLength(0)
      val en= mm.getAllHeaderLines()
      while (en.hasMoreElements()) {
        //s= (String) en.nextElement()
//        if (s.toLowerCase().indexOf(CTL) >= 0) {}
//        else
        hds.append(en.nextElement()).append("\r\n")
      }
      try {
        dispatch(new POP3Event(
          readBytes( mm.getRawInputStream()),
          hds.toString(), me))
      } finally {
        if (_delete) { mm.setFlag(Flags.Flag.DELETED, true) }
      }
    }
  }

  private def conn() = {

    if (_pop ==null || !_pop.isConnected())
    try {
      val session = Session.getInstance(new JPS(), null)
      val ps= session.getProviders()
      val uid= if (isEmpty(_user)) null else _user
      val pwd= if(isEmpty(_pwd)) null else _pwd
      var key= ST_POP3
      var sn= POP3

      closePOP()

      if (_ssl) {
        key = ST_POP3S
        sn= POP3S
      }

      var sun = ps.find { (p) => key == p.getClassName() } match {
        case Some(p) => p
        case _ => null
      }

      if ( ! isEmpty(_storeImpl)) {
        // this should never happen , only in testing
        sun= new Provider(Provider.Type.STORE, "pop3", _storeImpl, "test", "1.0.0")
        sn=POP3
      }

      session.setProvider(sun)

      val st= session.getStore(sn)
      var f = if ( st != null) {
        st.connect(_host, _port, uid , pwd)
        st.getDefaultFolder()
      } else { null }

      if (f != null) { f= f.getFolder("INBOX") }
      if (f==null || !f.exists()) {
        throw new Exception("POP3: Cannot find inbox")
      }

      _pop= st
      _fd= f

    } catch {
      case e => tlog().warn("",e)
      closePOP()
    }

    _pop != null && _pop.isConnected()
  }

  private def closePOP() {

    closeFolder()
    try {
      if (_pop != null) _pop.close()
    }
    catch {
      case e => tlog().warn("", e)
    }
    _pop=null
    _fd=null
  }

  private def closeFolder() {
    try {
      if (_fd != null && _fd.isOpen()) _fd.close(true)
    } catch {
      case e => tlog().warn("", e)
    }
  }

  private def openFolder() {
    if ( _fd != null && !_fd.isOpen()) {
      _fd.open(Folder.READ_WRITE)
    }
  }

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.RepeatingTimer#supportsConfigMenu()
   */
  override def supportsConfigMenu() = true

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.RepeatingTimer#getCmdSeq(java.util.ResourceBundle, java.util.Properties)
   */
  override def getCmdSeq(rcb:ResourceBundle, pps:JPS) = {
    val q6= new CmdLineQ("delmsg", bundleStr(rcb, "cmd.pop3.delete"), "y/n", "n") {
      def onRespSetOut(a:String, p:JPS) = {
        p.put("deletemsg", asJObj("Yy".has(a)))
        ""
      }}
    val q5= new CmdLineMust("pwd", bundleStr(rcb, "cmd.pwd")) {
      def onRespSetOut(a:String, p:JPS) = {
        p.put("pwd", a)
        "delmsg"
      }}
    val q4= new CmdLineMust("user", bundleStr(rcb, "cmd.user")) {
      def onRespSetOut(a:String, p:JPS) = {
        p.put("user", a)
        "pwd"
      }}
    val q3= new CmdLineQ("ssl", bundleStr(rcb, "cmd.use.ssl"), "y/n","n") {
      def onRespSetOut(a:String, p:JPS) = {
        p.put("ssl", asJObj("Yy".has(a)))
        "user"
      }}
    val q2= new CmdLineMust("port", bundleStr(rcb, "cmd.pop3.port", "","110")) {
      def onRespSetOut(a:String, p:JPS) = {
        p.put("port", asJObj(asInt(a, 110)))
        "ssl"
      }}
    val q1= new CmdLineMust("host", bundleStr(rcb, "cmd.pop3.host")) {
      def onRespSetOut(a:String, p:JPS) = {
        p.put("host", a)
        "port"
      }}
    Some(new CmdLineSeq( super.getCmdSeq(rcb, pps), Array(q1,q2,q3,q4,q5,q6)) {
      def onStart() = q1.label()
    })
  }

}
