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

import com.zotoh.fwk.util.JSONUte._
import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.util.StrUte._
import com.zotoh.fwk.util.FileUte._
import com.zotoh.fwk.util.LoggerFactory._
import com.zotoh.fwk.util.Logger
import com.zotoh.fwk.util.JSONUte

import com.zotoh.bedrock.util.MiscUte.maybeSetKey

import java.io.{File,IOException}
import java.util.ResourceBundle
import org.json.{JSONObject=>JSNO}

import com.zotoh.bedrock.core.AppDirError
import com.zotoh.bedrock.core.Vars

/**
 * (Internal use only).
 *
 * @author kenl
 */
abstract class Cmdline(private val _mhome:File,
  private val _cwd:File) extends Vars {

  private def ilog() { _log=getLogger(classOf[Cmdline]) }
  @transient private var _log:Logger=null
  def tlog() = { if(_log==null) ilog(); _log }

  protected val _rcb= AppRunner.bundle()

  /**
   * @param args
   * @throws Exception
   */
  def evalArgs(args:Seq[String]) {
    eval(args); println("")
  }

  /**
   * @param args
   * @throws Exception
   */
  def eval(args:Seq[String]):Unit

  /**
   * @return
   */
  def cmds():Seq[String]

  /**
   * @return
   */
  protected def rcb() = _rcb

  /**
   * @param target
   * @throws Exception
   */
  protected def _runTargetService(target:String, svc:String) {
    org.apache.tools.ant.Main.main(Array(
        "-Dservicepoint="+svc,
        "-buildfile",
        buildFilePath(),
//        "-quiet",
        target
    ))
  }

  /**
   * @param target
   * @param prop
   * @param value
   * @throws Exception
   */
  protected def runTargetExtra(target:String, prop:String, value:String) {
    org.apache.tools.ant.Main.main(Array(
      "-D"+prop+"="+value,
      "-buildfile",
      buildFilePath(),
//        "-quiet",
      target
    ))
  }

  /**
   * @param target
   * @throws Exception
   */
  protected def runTargetInProc(target:String) {
    new AntMainXXX().startAnt( Array(
        "-buildfile",
        buildFilePath(),
//        "-quiet",
        target
    ))
  }


  /**
   * @param target
   * @throws Exception
   */
  protected def runTarget(target:String) {
    org.apache.tools.ant.Main.main(Array(
        "-buildfile",
        buildFilePath(),
//        "-quiet",
        target
    ))
  }

  /**
   * @return
   * @throws Exception
   */
  protected def maedrDir() = _mhome

  /**
   * @return
   * @throws Exception
   */
  protected def cwd() = _cwd

  /**
   * @throws AppDirError
   * @throws IOException
   */
  protected def assertAppDir() {
    val bin=new File(cwd(), BIN)
    val cfg=new File(cwd(), CFG)
    var cf=new File(cfg, APPPROPS)

    //tlog().debug("AppRunner: cwd = " + c)

    val ok= isDirWRX(bin) &&
    isDirWRX(cfg) &&
    isFileWRX(cf)

    if (!ok) {
      throw new AppDirError()
    }

    cf= new File(new File(cwd(), REALM), KEYFILE)
    maybeSetKey(cf)
  }

  private def buildFilePath() = {
    niceFPath(new File(new File(cwd(), CFG), "ant.xml"))
  }

  /**
   * @return
   * @throws Exception
   */
  protected def isWebApp() = {
    nsb(appMeta().optString("apptype")) == APPTYPE_WEB
  }

  /**
   * @return
   * @throws Exception
   */
  protected def appLang() = {
    nsb(appMeta().optString("lang"))
  }

  /**
   * @return
   * @throws Exception
   */
  protected def appMeta() = {
    val root= JSONUte.read(new File(new File(cwd(),CFG), APP_META))
    if (root==null) newJSON() else root
  }

}

sealed class AntMainXXX extends org.apache.tools.ant.Main {
  override def exit(exitCode:Int) { }

  def startAnt(args:Array[String]) = {
    super.startAnt(args, null, null)
  }
}
