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

import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.util.JSONUte._
import com.zotoh.fwk.util.StrUte._
import com.zotoh.bedrock.util.MiscUte._

import java.util.{Properties=>JPS}
import java.io.File
import org.json.{JSONObject=>JSNO}

import com.zotoh.fwk.util.{CmdLineQ,CmdLineSeq,CoreImplicits}
import com.zotoh.fwk.util.MetaUte
import com.zotoh.bedrock.core.CmdHelpError
import com.zotoh.bedrock.device.Device
import com.zotoh.bedrock.device.DeviceMgr
import com.zotoh.bedrock.mock._
import com.zotoh.bedrock.impl.DefaultDeviceFactory._

/**
 * (Internal use only).
 *
 * @author kenl
 */
class CmdDevice(home:File,cwd:File) extends Cmdline(home,cwd) with CoreImplicits {

  private val _dummyDevMgr=new MockDeviceMgr()

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.etc.Cmdline#getCmds()
   */
  override def cmds() = Array("device")

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.etc.Cmdline#eval(java.lang.String[])
   */
  override def eval(args:Seq[String]) {

    if (args==null || args.length < 3) {
      throw new CmdHelpError()
    }

    if ("?"==args(2) && "configure"==args(1)) {
      listDevs()
    } else {
      assertAppDir()
      args(1) match {
        case "configure" => cfgDev(args(2))
        case "add" => addDev(args(2))
        case _ => throw new CmdHelpError()
      }
    }
  }

  private def listDevs() {
    devCZMap().foreach{ (t) =>
      println("%s".format( t._1) )
    }
  }

  private def cfgDev(dev:String) {

    val top= loadConf( cwd())
    if (! existsDevice(top, dev)) {
      throw new Exception("Unknown device type: " + dev)
    }
    val z= if ( listDefaultTypes().contains(dev)) {
      devCZ(dev)
    } else {
      userDevCZ(top, dev)
    }

    val d= z match {
      case Some(x) =>
        x.getConstructor(classOf[DeviceMgr]).
          newInstance(_dummyDevMgr).asInstanceOf[Device]
      case _ =>
        throw new ClassNotFoundException("Class not found for device: " + dev)
    }

    val props= new JPS()
    val ok = if (d != null && d.supportsConfigMenu()) {
      d.showConfigMenu(rcb(),props)
    }else {
      false
    }

    if (!ok) { return }

    val id= nsb( props.remove("_id"))
    tstEStrArg("device id", id)
    props.put("type", dev)

    val obj= newJSON(props)
    val g= devs(top)

    if (g.has(id)) {
      throw new Exception("Another device with name \"" + id + "\" is defined already")
    } else {
      g.put(id, obj)
    }

    saveConf( cwd(), top)
  }

  private def addDev(dev:String) {

    val top=loadConf( cwd())

    if ( existsDevice(top, dev)) {
      throw new Exception("Device type: " + dev + " already exists")
    }

    val q1= new CmdLineQ("dev", bundleStr(rcb(), "cmd.devimpl.class") ) {
      def onRespSetOut(a:String, p:JPS) = {
        p.put("device", a)
        ""
      }}
    val q0= new CmdLineQ("type", bundleStr(rcb(),"cmd.dev.type") ) {
      def onRespSetOut(a:String, p:JPS) = {
        p.put("type", a)
        "dev"
      }}
    val s= new CmdLineSeq(Array(q0,q1)) {
      def onStart() = q1.label()
    }

    val props= new JPS().add("type", dev)
    if (s.start(props).isCanceled()) { return }

    val fac=props.gets("device")
    val t=props.gets("type")
    tstEStrArg("device-class", fac)
    tstEStrArg("type", t)

    devFacs(top).put(t, fac)
    saveConf( cwd(), top)
  }


}


