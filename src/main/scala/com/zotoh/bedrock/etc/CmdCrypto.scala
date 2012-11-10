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

package com.zotoh.bedrock.etc

import java.util.{Date=>JDate,Properties=>JPS}
import java.io.File

import com.zotoh.fwk.io.IOUte._
import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.util.DateUte._
import com.zotoh.fwk.util.StrUte._

import com.zotoh.fwk.crypto.{PEM,PwdFactory}
import com.zotoh.fwk.util.{CmdLineQ,CmdLineSeq,CoreImplicits}
//import com.zotoh.fwk.util.WWID._
import com.zotoh.fwk.crypto.Crypto._
import com.zotoh.fwk.crypto.Crypto
import com.zotoh.fwk.crypto.CertFormat

import com.zotoh.bedrock.core.CmdHelpError

/**
 * (Internal use only).
 *
 * @author kenl
 */
class CmdCrypto(home:File,cwd:File) extends Cmdline(home,cwd) with CoreImplicits {

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.etc.Cmdline#getCmds()
   */
  override def cmds() = Array("crypto")

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.etc.Cmdline#eval(java.lang.String[])
   */
  override def eval(args:Seq[String]) {

    if (args==null || args.length < 2) {
      throw new CmdHelpError()
    }

    val s2= if(args.length > 2) args(2) else ""
    //s0= args[0],
    args(1) match {
      case "encrypt" => encrypt(s2)
      case "testjce" => testjce()
      case x:String if (x.swic("generate/")) => generate(x.substring(9))
      case _ => throw new CmdHelpError()
    }
  }

  private def testjce() {
    try {
      Crypto.testJCEPolicy()
      println("%s".format( bundleStr(_rcb, "cmd.jce.ok")) )
    } catch {
      case e =>
        println("%s\n%s\n%s\n%s".format(
            bundleStr(_rcb, "cmd.jce.error1"),
            bundleStr(_rcb, "cmd.jce.error2"),
            bundleStr(_rcb, "cmd.jce.error3"),
            bundleStr(_rcb, "cmd.jce.error4")) )
    }
  }

  private def encrypt(txt:String) {
    assertAppDir()
    println("\n" + PwdFactory.mk(txt).encoded())
  }

  private def generate(arg:String) {
    val appdir= cwd()
    arg match {
      case "password" => generatePassword()
      case "serverkey" => keyfile(appdir)
      case "csr" => csrfile(appdir)
      case _ => throw new CmdHelpError()
    }
  }

  private def keyfile(appdir:File) {

    val props= new JPS()
    var bOK=true
    keyFileInput() match {
      case Some(s) if (s.start(props).isCanceled()) => bOK=false
    }

    if (!bOK) { return }

    val mths= asInt(trim(props.gets("months")), 12)
    val size= asInt(trim(props.gets("size")),1024)
    val pwd= trim(props.gets("pwd"))

    val out= new File( trim(props.gets("fn")) )
    val start= new JDate()

    mkSSV1PKCS12(uid(), start,
      addMonths(start, mths),
        "CN=" + trim(props.gets("cn")) +
        ", OU="+ trim(props.gets("ou")) +
        ", O=" + trim(props.gets("o")) +
        ", L=" + trim(props.gets("l")) +
        ", ST=" + trim(props.gets("st")) +
        ", C=" + trim(props.gets("c")) ,
        pwd, size, out)
  }

  private def csrfile(appdir:File) {

    val props= new JPS()
    var bOK=true
    csrInput() match {
      case Some(s) if (s.start(props).isCanceled()) => bOK=false
    }
    if (!bOK) { return }

    val t=mkCSR( asInt(trim(props.gets("size")),1024),
        "CN="+ trim(props.gets("cn")) +
        ", OU="+ trim(props.gets("ou")) +
        ", O="+ trim(props.gets("o")) +
        ", L="+ trim(props.gets("l")) +
        ", ST="+ trim(props.gets("st")) +
        ", C="+ trim(props.gets("c")), PEM)
    val fn= trim(props.gets("fn"))

    writeFile(new File(fn), t._1)
    writeFile( new File(fn + ".key"), t._2)
  }

  private def generatePassword() {
    println("\n" + PwdFactory.mkRandomText(16))
  }

  // create the set of questions to prompt during the creation of server key
  private def keyFileInput() = {
    val q10= new CmdLineQ("fname", bundleStr(rcb(), "cmd.save.file"), "", "test.p12") {
      def onRespSetOut(a:String, p:JPS) = {
        p.put("fn", a)
        ""
      }}
    val q9= new CmdLineQ("pwd", bundleStr(rcb(),"cmd.key.pwd")) {
      def onRespSetOut(a:String, p:JPS) = {
        p.put("pwd", a)
        "fname"
      }}
    val q8= new CmdLineQ("duration", bundleStr(rcb(),"cmd.key.duration"), "", "12") {
      def onRespSetOut(a:String, p:JPS) = {
        p.put("months", a)
        "pwd"
      }}

    val p= csrInput() match {
      case Some(s) => s.remove("fname"); Some(s)
      case _ => None
    }
    Some(new CmdLineSeq(p, Array(q8,q9,q10)) {
      def onStart() = q8.label()
    })

  }


  // create the set of questions to prompt during the creation of CSR
  private def csrInput() = {
    val q8= new CmdLineQ("fname", bundleStr(rcb(), "cmd.save.file"), "", "test.csr") {
      def onRespSetOut(a:String, p:JPS) = {
        p.put("fn", a)
        ""
      }}
    val q7= new CmdLineQ("size", bundleStr(rcb(), "cmd.key.size"), "", "1024") {
      def onRespSetOut(a:String, p:JPS) = {
        p.put("size", a)
        "fname"
      }}
    val q6= new CmdLineQ("c", bundleStr(rcb(), "cmd.dn.c"), "", "US") {
      def onRespSetOut(a:String,  p:JPS) = {
        p.put("c", a)
        "size"
      }}
    val q5= new CmdLineQ("st", bundleStr(rcb(),"cmd.dn.st")) {
      def onRespSetOut(a:String,  p:JPS) = {
        p.put("st", a)
        "c"
      }}
    val q4= new CmdLineQ("loc", bundleStr(rcb(),"cmd.dn.loc")) {
      def onRespSetOut(a:String, p:JPS) = {
        p.put("l", a)
        "st"
      }}
    val q3= new CmdLineQ("o", bundleStr(rcb(),"cmd.dn.org") ) {
      def onRespSetOut(a:String,  p:JPS) = {
        p.put("o", a)
        "loc"
      }}
    val q2= new CmdLineQ("ou", bundleStr(rcb(), "cmd.dn.ou")) {
      def onRespSetOut(a:String,  p:JPS) = {
        p.put("ou", a)
        "o"
      }}
    val q1= new CmdLineQ("cn", bundleStr(rcb(), "cmd.dn.cn")) {
      def onRespSetOut(a:String, p:JPS) = {
        p.put("cn", a)
        "ou"
      }}
    Some(new CmdLineSeq( Array(q1, q2,q3,q4,q5,q6,q7,q8)) {
      def onStart() = "cn"
    })
  }



}


