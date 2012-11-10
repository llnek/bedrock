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

import scala.collection.JavaConversions._

import com.zotoh.fwk.util.LoggerFactory._
import com.zotoh.fwk.util.Logger
import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.util.StrUte._
import com.zotoh.fwk.util.FileUte._

import java.io.File
import java.util.{Locale,ResourceBundle}

import com.zotoh.bedrock.core.AppDirError
import com.zotoh.bedrock.core.CmdHelpError
import com.zotoh.bedrock.core.Vars

/**
 * (Internal use only).
 *
 * @author kenl
 */
object AppRunner extends Vars {

  private val _RCB= inizBundle()

  private val _log= getLogger(classOf[AppRunner])
  def tlog() = _log

  /**
   * @param args
   */
  def main(args:Array[String]) {
    var cloud=false
    try {
      cloud = args.exists((a) => "cloud" == a)
      if ( !parseArgs(args)) {
        throw new CmdHelpError()
      }
    }
    catch {
      case e:AppDirError =>
        println("You must run the command in the application directory.")
      case e:CmdHelpError =>
        if (cloud) { usage_cloud() } else  { usage() }
      case e =>
        tlog().error("", e)
        println(e.getMessage())
    }
  }

  private def usage_cloud() {
    println(mkString('=',78))
    println("> bedrock <commands & options>")
    println("> cloud-related commands")
    println("> ----------------------")
    val a=Array(

      Array("cloud configure", "Set cloud info & credential."),
      Array("cloud sshinfo", "Set SSH info."),

      Array("cloud install <ver> <host:dir>", "Install Bedrock to host:target-dir."),
      Array("cloud app/deploy  <host:dir>", "Deploy app to host:target-dir."),
      Array("cloud app/run  <host:dir>", "Deploy and run app."),

      Array("cloud sync <regions|datacenters>", "Get latest set of Regions or Zones."),

      Array("cloud image/set <image-id>", "Set default Image."),
      Array("cloud image/*",  "Launch an Image."),

      Array("cloud ip/list", "List Elastic IPAddrs."),
      Array("cloud ip/bind <ipaddr> <vm-id>", "Assign IPAddr to VM."),
      Array("cloud ip/+", "Add a new IPAddr."),
      Array("cloud ip/- <ipaddr>", "Remove a IPAddr."),

      Array("cloud vm/list", "List Virtual Machines."),
      Array("cloud vm/set <vm-id>", "Set default VM."),
      Array("cloud vm/? [vm-id]", "Describe a VM."),
      Array("cloud vm/* [vm-id]", "Start a VM."),
      Array("cloud vm/! [vm-id]", "Stop a VM."),
      Array("cloud vm/% [vm-id]", "Terminate a VM."),

      Array("cloud sshkey/list", "List SSH Keys."),
      Array("cloud sshkey/set <keyname>", "Set default SSH Key."),
      Array("cloud sshkey/+ <keyname>", "Add a new SSH Key."),
      Array("cloud sshkey/- <keyname>", "Remove a SSH Key."),

      Array("cloud secgrp/list", "List Security Groups."),
      Array("cloud secgrp/set <group>", "Set default Security Group."),
      Array("cloud secgrp/+ <group>", "Add a new Security Group."),
      Array("cloud secgrp/- <group>", "Remove a Security Group."),

      Array("cloud fwall/+ <group@rule>", "Add a new Firewall rule."),
      Array("cloud fwall/- <group@rule>", "Remove a Firewall rule."),
      Array(":e.g. xyz@tcp#0.0.0.0/0#1#10", "From port 1 to port 10."),
      Array(":e.g. xyz@tcp#0.0.0.0/0#22", "Port 22.")
    )
    drawHelpLines("> %-35s\' %s\n", a)
    println(">")
    println("> help - show standard commands")
    println("> help cloud - show commands related to cloud operations")
    println(mkString('=',78))
  }

  private def drawHelpLines(fmt:String, a:Array[Array[String]]) {
    a.foreach { (ss) =>
      if (ss!=null) {
        print ( fmt.format( ss(0), ss(1)) )
      } else {
        println("")
      }
    }
  }

  private def usage() {
    println(mkString('=',78))
    println("> bedrock <commands & options>")
    println("> standard commands")
    println("> -----------------")
    val a= Array(

      Array("app create/web <app-name>",  "e.g. create helloworld as a webapp."),
      Array("app create <app-name>",  "e.g. create helloworld"),

      Array("app ide/eclipse", "Generate eclipse project files."),
      Array("app compile", "Compile sources."),
      Array("app test", "Run test cases."),

//    "app invoke[/bg] <runnable>", "Invoke a Java Runnable object."),
      Array("app debug <port>", "Start & debug the application."),
      Array("app start[/bg]", "Start the application."),
      //      Array("app run[/bg] <script-file>", "Run a Groovy script."),

      Array("app bundle <output-dir>", "Package application."),

//      Array("device configure <device-type>", "Configure a device."),
//      Array("device add <new-type>", "Add a new  device-type."),

      Array("crypto generate/serverkey", "Create self-signed server key (pkcs12)."),
      Array("crypto generate/password", "Generate a random password."),
      Array("crypto generate/csr", "Create a Certificate Signing Request."),
      Array("crypto encrypt <some-text>", "e.g. encrypt SomeSecretData"),
//      Array("crypto testjce", "Check JCE  Policy Files."),

      Array("demo samples", "Generate a set of samples."),
      Array("version", "Show version info.")
    )
    drawHelpLines("> %-35s\' %s\n", a)
    println(">")
    println("> help - show standard commands")
    println("> help cloud - show commands related to cloud operations")
    println(mkString('=',78))
  }

  /**
   * @return
   */
  def bundle() = _RCB

  private def inizBundle() = {
    val ss= System.getProperty("bedrock.locale", "en_US").split("_")
    val lang= ss(0)
    val loc = if (ss.length > 1) {
      new Locale(lang, ss(1))
    }else {
      new Locale(lang)
    }
    //println("Locale= " + loc.toString());
    i18nBundle("com/zotoh/bedrock/i18n/AppRunner", loc)
  }

  private def parseArgs(args:Array[String]):Boolean = {

    if (args.length < 2) { false } else {
      inizBundle()
      val home=stripTail(niceFPath(new File(args(0))), "/")
      val hc= (new File(home), cwd())
      val x=Array[Cmdline](
        new CmdSamples(hc._1,hc._2), new CmdCrypto(hc._1,hc._2),
        new CmdCloud(hc._1,hc._2), // new CmdDevice(hc._1,hc._2),
        new CmdAppOps(hc._1,hc._2), new CmdMiscOps(hc._1,hc._2)
      ).find { (c) =>
        c.cmds().contains(args(1))
      }
      x match {
        case Some(c) =>
          c.eval( args.takeRight(args.length-1))
          true
        case _ =>
          false
      }
    }
  }

}


sealed class AppRunner {}
