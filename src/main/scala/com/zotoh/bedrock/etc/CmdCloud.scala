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
import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.util.StrUte._

import java.util.{Properties=>JPS}
import java.io.File
import org.json.{JSONObject=>JSNO}
import com.zotoh.fwk.util.{CmdLineMust,CmdLineQ,CmdLineSeq,CoreImplicits}
import com.zotoh.cloudapi.core.Vars
import com.zotoh.cloudapi.aws.AWSAPI

import com.zotoh.bedrock.cloud.CloudData._
import com.zotoh.bedrock.cloud.Cloudr._
import com.zotoh.bedrock.cloud.{Cloudr,CloudData}
import com.zotoh.bedrock.core.CmdHelpError
import org.dasein.cloud.dc.DataCenterServices

/**
 * (Internal use only).
 *
 * @author kenl
 */
class CmdCloud(home:File,cwd:File) extends Cmdline(home,cwd) with Vars with AWSAPI with CoreImplicits {

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.etc.Cmdline#getCmds()
   */
  override def cmds() = Array("cloud")

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.etc.Cmdline#eval(java.lang.String[])
   */
  override def eval(args:Seq[String]) {

    if (args==null || args.length < 2
        || "cloud" != args(0)) {
      throw new CmdHelpError()
    }

    assertAppDir()

    val a4= if(args.length > 3) args(3) else ""
    val a3= if(args.length > 2) args(2) else ""

    configure(cwd(), _rcb)
    println("")

    args(1) match {
      case "app/pack" => runTarget("packzip-app")
      case "configure" => config()
      case "sshinfo" => sshinfo()
      case "image/*" => launchImage()
      case "vm/?" => descServer(a3)
      case "vm/*" => startServer(a3)
      case "vm/!" => stopServer(a3)
      case "vm/%" => terminateServer(a3)
      case "ip/+" =>
        addIpInput() match {
          case Some(s) =>
            val pps=new JPS()
            if (!s.start(pps).isCanceled()) {
              addEIP( pps.gets("region"))
            }
        }
      case "ip/list" => listEIPs()
      case "vm/list" => listServers()
      case "sshkey/list" => listSSHKeys()
      case "secgrp/list" => listFwalls()
      case "fwall/-" => revokeCidr(a3)
      case "fwall/+" => addCidr(a3)
      case "install" if (!isEmpty(a3) && !isEmpty(a4)) =>
        install( sshinfo(), a3,a4)
      case a2@("a"|"b") if ( !isEmpty(a3)) =>
        val b= "app/run"==a2
        deploy( b, preRemote(b) , a3)
      case "sync" if ( !isEmpty(a3)) =>
        sync(a3)
      case "image/set" if(!isEmpty(a3)) =>
        setImage(a3)
      case "vm/set" if( !isEmpty(a3)) =>
        setServer(a3)
      case "sshkey/set" if (!isEmpty(a3)) =>
        setSSHKey(a3)
      case "sshkey/-"  if(!isEmpty(a3)) =>
        removeSSHKey(a3)
      case "sshkey/+"  if(!isEmpty(a3)) =>
        keyInput(a3) match {
          case Some(s) =>
            val pps=new JPS()
            if (!s.start(pps).isCanceled()) {
              addSSHKey(a3, pps.gets("fpath"))
            }
        }
      case "ip/bind" if(!isEmpty(a3) && !isEmpty(a4)) =>
        setEIP(a3, a4)
      case "ip/-"  if(!isEmpty(a3)) =>
        removeEIP(a3)
      case "secgrp/set" if(!isEmpty(a3)) =>
        setSecGrp(a3)
      case "secgrp/-" if(!isEmpty(a3)) =>
        removeFwall(a3)
      case "secgrp/+"  if(!isEmpty(a3)) =>
        grpInput(a3) match {
          case Some(s) =>
            val pps=new JPS()
            if (!s.start(pps).isCanceled()) {
              addFwall(a3, pps.gets("desc"))
            }
        }
      case _ =>
          throw new CmdHelpError()
    }
  }

  private def preRemote(b:Boolean) = {

    val s99= remoteInput()
    val props= new JPS()

    val q1= new CmdLineMust("home", bundleStr(rcb(),"cmd.remote.bedrock")) {
      def onRespSetOut(a:String, p:JPS) = {
        p.put("maedr", a)
        ""
    }}
    val q0= new CmdLineMust("bundle", bundleStr(rcb(),"cmd.bedrock.bundlefile")) {
      def onRespSetOut(a:String, p:JPS) = {
        p.put("bundle", a)
        if(b) "home" else ""
    }}
    val s= new CmdLineSeq(s99, Array(q0,q1)) {
      def onStart() = q0.label()
    }
    if (s.start(props).isCanceled()) props.cls() else props
  }

  private def sync(task:String) {
    task match {
      case "datacenters" => syncDatacenters()
      case "regions" =>
        syncRegions()
        println("")
        defRegInput() match {
          case Some(s) =>
            val props=new JPS()
            if (!s.start(props).isCanceled()) {
              setDftRegion( props.gets("region"))
              save()
            }
        }
      case _ =>
        throw new CmdHelpError()
    }
  }

  private def config() {
    val props=new JPS()
    cfgInput() match {
      case Some(s) =>
        if (! s.start(props).isCanceled()) Cloudr.setConfig(
          props.gets("vendor"),
          props.gets("acct"),
          props.gets("id"),
          props.gets("pwd"))
    }
  }

  private def sshinfo2() {
    val props=sshinfo()
    if (props != null) {
      Cloudr.setSSHInfo(
        props.gets("user"),
        props.gets("pwd"),
        props.gets("key"))
    }
  }

  private def sshinfo():JPS = {
    val props=new JPS()
    remoteInput() match {
      case Some(s) =>
        if (s.start(props).isCanceled()) props.cls() else props
      case _ => props
    }
  }

  private def launchImage() {
    val props=new JPS()
    var bOK=true
    launchInput() match {
      case Some(s) if (s.start(props).isCanceled()) => bOK=false
    }

    if (!bOK) { return }

    val groups= nsb(props.gets("group")).split("(,|;)")
    val image= props.gets("image")
    val ptype= props.gets("product")
    val key= props.gets("key")
    val region= props.gets("region")
    val zone= props.gets("zone")

    if (!isEmpty(zone)) {
      setDftZone(zone)
      save()
    }

    Cloudr.launchImage(image, ptype, key, groups, region, zone)
  }

  private def remoteInput() = {

    val q3= new CmdLineMust("key", bundleStr(rcb(),"cmd.ssh.keyfile")) {
      def onRespSetOut(a:String, p:JPS) = {
        p.put("key", a)
        ""
      }}
    val q2= new CmdLineMust("pwd", bundleStr(rcb(),"cmd.user.pwd")) {
      def onRespSetOut(a:String, p:JPS) = {
        p.put("pwd", a)
        "key"
      }}
    val q1= new CmdLineMust("user", bundleStr(rcb(),"cmd.user.id"), "",
        nsb(SSHInfo().optString(P_USER)) ) {
      def onRespSetOut(a:String, p:JPS) = {
        p.put("user", a)
        "pwd"
      }}
    Some(new CmdLineSeq(Array(q1,q2,q3)) {
      def onStart() = q1.label()
    })
  }

  private def addIpInput() = {

    val rgs= join( Cloudr.provider().getDataCenterServices().listRegions(), "\n")
    val q1= new CmdLineMust("region", bundleStr(rcb(),"cmd.region"),
        rgs, dftRegion()) {
      def onRespSetOut(a:String, p:JPS) = {
        p.put("region", a)
        ""
      }}
    Some(new CmdLineSeq(Array(q1)) {
      def onStart() = q1.label()
    })
  }

  private def defRegInput() = {
    val rgs= join( Cloudr.provider().getDataCenterServices().listRegions(), "\n")
    val q1= new CmdLineMust("region",
        bundleStr(rcb(),"cmd.setdef.region"), rgs, "") {
      def onRespSetOut(a:String, p:JPS) = {
        p.put("region", a)
        ""
      }}
    Some(new CmdLineSeq(Array(q1)) {
      def onStart() = q1.label()
    })
  }

  private def keyInput(keyname:String) = {
    val q1= new CmdLineMust("pem",
        bundleStr(rcb(),"cmd.save.file"), "", "cfg/"+keyname+".pem") {
      def onRespSetOut(a:String, p:JPS) = {
        p.put("fpath", a)
        ""
      }}
    Some(new CmdLineSeq(Array(q1)) {
      def onStart() = q1.label()
    })
  }

  private def grpInput(grp:String) = {
    val q1= new CmdLineMust("desc", bundleStr(rcb(),"cmd.brief.desc")) {
      def onRespSetOut(a:String, p:JPS) = {
        p.put("desc", a)
        ""
      }}
    Some(new CmdLineSeq(Array(q1)) {
      def onStart() = q1.label()
    })
  }

  private def cfgInput() = {
    val q4= new CmdLineMust("pwd", bundleStr(rcb(),"cmd.cloud.pwd")) {
      def onRespSetOut(a:String, p:JPS) = {
        p.put("pwd", a)
        ""
      }}
    val q3= new CmdLineMust("id", bundleStr(rcb(),"cmd.cloud.id")) {
      def onRespSetOut(a:String, p:JPS) = {
        p.put("id", a)
        "pwd"
      }}
    val q2= new CmdLineMust("acct", bundleStr(rcb(),"cmd.cloud.acct")) {
      def onRespSetOut(a:String, p:JPS) = {
        p.put("acct", a)
        "id"
      }}
    val q1= new CmdLineMust("vendor", bundleStr(rcb(),"cmd.cloud.vendor"),   "amazon", "amazon") {
      def onRespSetOut(a:String, p:JPS) = {
        p.put("vendor", a)
        "acct"
      }}
    Some(new CmdLineSeq( Array(q1,q2,q3,q4)) {
      def onStart() = q1.label()
    })
  }

  private def launchInput() = {

    val rgs= Cloudr.provider().getDataCenterServices().listRegions()
    val me=this
    val q6= new CmdLineQ("group", bundleStr(rcb(),"cmd.cloud.group"),
        join( CloudData.firewalls().keys().toSeq, "\n"), dftFirewall()) {
      def onRespSetOut(a:String, p:JPS) = {
        p.put("group", a)
        ""
      }}
    val q5= new CmdLineQ("zone", bundleStr(rcb(),"cmd.cloud.zone"), "", dftZone()) {
      def onRespSetOut(a:String, p:JPS) = {
        p.put("zone", a)
        "group"
      }}
    val q4= new CmdLineMust("region", bundleStr(rcb(),"cmd.cloud.region"), join(rgs, "\n"), dftRegion()) {
      def onRespSetOut(a:String, p:JPS) = {
        p.put("region", a)
        me.popZones(q5, a)
        "zone"
      }}
    val q3= new CmdLineMust("key", bundleStr(rcb(),"cmd.cloud.key"),
        join(SSHKeys().keys().toSeq, "\n"), dftKey()) {
      def onRespSetOut(a:String,  p:JPS) = {
        p.put("key", a)
        "region"
      }}
    val q2= new CmdLineMust("ptype", bundleStr(rcb(),"cmd.cloud.ptype")) {
      def onRespSetOut(a:String, p:JPS) = {
        p.put("product", a)
        "key"
      }}
    val q1= new CmdLineMust("img", bundleStr(rcb(),"cmd.cloud.image"), "", dftImage()) {
      def onRespSetOut(a:String, p:JPS) = {
        p.put("image", a)
        imageBits(q2, a)
        "ptype"
      }}
    Some(new CmdLineSeq(Array(q1,q2,q3,q4,q5,q6)) {
      def onStart() = q1.label()
    })

  }

  private def popZones(q:CmdLineQ, region:String) {
    val rc= listDatacenters(region)
    q.setChoices( join(rc, "\n"))
  }

  private def imageBits(q:CmdLineQ, image:String) {
    var arch=0
    try {
      val obj=images().optJSONObject(nsb( image))
      var s= if(obj==null) "" else obj.optString(P_ARCH)
      if (I32 == s) { arch= 32 }
      if (I64== s) { arch=64 }
      s=dftProduct(s)
      q.setDftAnswer(nsb(s))
    }
    catch { case _ => }

    q.setChoices( join( listProductIds(arch), "\n") )
  }


}


