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

import scala.collection.mutable.HashSet

import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.util.StrUte._
import com.zotoh.fwk.util.FileUte._

import java.io.{File,FilenameFilter,IOException}
import java.util.{Properties=>JPS,ResourceBundle}

import org.apache.commons.io.filefilter._
import org.json.{JSONObject=>JSNO}

import com.zotoh.fwk.util.{CmdLineMust,CmdLineQ,CmdLineSeq}
import com.zotoh.fwk.util.FileUte
import scala.Some

object FilePicker {
  val PSTR_FMASK= "fmask"
  val PSTR_ROOTDIR= "rootdir"
  val PSTR_DESTDIR= "destdir"
  val PSTR_AUTOMOVE= "automove"

}

/**
 * The FilePicker device inspects  a directory for files periodically.  When it detects
 * a file is present, an event will be generated.
 *
 * The set of properties:
 *
 * <b>rootdir</b>
 * The full path of the directory to inspect.
 * <b>fmask</b>
 * Optional file mask to selectively look for certain files in regular expression format.
 * <b>automove</b>
 * If set to boolean value <i>true</i>, the file will be moved out of the directory to
 * avoid repeated scan.  If false (by default), it is up to the application to move or
 * delete the file.
 * <b>destdir</b>
 * The full path of a target directory to move files to, effective only iff automove is true.
 *
 * @see com.zotoh.bedrock.device.RepeatingTimer
 *
 * @author kenl
 *
 */
class FilePicker(devMgr:DeviceMgr) extends ThreadedTimer(devMgr) {

  private var _mask:Option[FilenameFilter]=None
  private val _dirs= HashSet[File]()
  private var _destMove:Option[File]=None
  private var _folder:Option[File]=None

  /**
   * @return
   */
  def destDir() = _destMove

  /**
   * @return
   */
  def srcDir() = _folder

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.RepeatingTimer#inizWithProperties(org.json.JSONObject)
   */
  override def inizWithQuirks(pps:JSNO) {
    super.inizWithQuirks(pps)
    val mask= trim(pps.optString(FilePicker.PSTR_FMASK) )
    val root= trim(pps.optString(FilePicker.PSTR_ROOTDIR) )
    val move= pps.optBoolean(FilePicker.PSTR_AUTOMOVE)
    val dest= trim(pps.optString(FilePicker.PSTR_DESTDIR) )

    tstEStrArg("file-root-folder", root)

    if (isEnabled()) {
      var fp=new File(root)
      _folder= Some(testDir(fp))
      _dirs += fp
      if (move) {
        tstEStrArg("file-automove-folder", dest)
        fp= new File(dest)
        _destMove= Some(testDir(fp))
      }
    }

    val ff = mask match {
      case s:String if s.startsWith("*.") => new SuffixFileFilter(s.substring(1))
      case s:String if s.endsWith("*") => new PrefixFileFilter(s.substring(0,s.length()-1))
      case s:String if s.length() > 0 => new WildcardFileFilter(mask)
      case _ => TrueFileFilter.TRUE
    }

    _mask= Some(ff)
  }

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.RepeatingTimer#supportsConfigMenu()
   */
  override def supportsConfigMenu() = true

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.ThreadedTimer#preLoop()
   */
  override def preLoop() {}

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.ThreadedTimer#endLoop()
   */
  override def endLoop() {}

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.ThreadedTimer#onOneLoop()
   */
  override def onOneLoop() {
    _dirs.foreach { (f) => scanOneDir(f) }
  }

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.RepeatingTimer#getCmdSeq(java.util.ResourceBundle, java.util.Properties)
   */
  override def getCmdSeq(rcb:ResourceBundle, pps:JPS) = {
    val q4= new CmdLineQ("fmask", bundleStr(rcb, "cmd.fp.mask")) {
      def onRespSetOut(a:String, p:JPS) = {
        p.put(FilePicker.PSTR_FMASK, a)
        ""
      }}
    val q3= new CmdLineQ("dest", bundleStr(rcb, "cmd.fp.dest")) {
      def onRespSetOut(a:String, p:JPS) = {
        p.put(FilePicker.PSTR_DESTDIR , a)
        "fmask"
      }}
    val q2= new CmdLineQ("move", bundleStr(rcb, "cmd.fp.move"), "y/n","n") {
      def onRespSetOut(a:String, p:JPS) = {
        val b="Yy".has(a)
        if (b) { q3.setMust(true) }
        p.put( FilePicker.PSTR_AUTOMOVE, asJObj(b))
        if (b) "dest" else "fmask"
      }}
    val q1= new CmdLineMust("root", bundleStr(rcb, "cmd.fp.root")) {
      def onRespSetOut(a:String, p:JPS) = {
        p.put( FilePicker.PSTR_ROOTDIR , a)
        "move"
      }}
    Some(new CmdLineSeq(super.getCmdSeq(rcb, pps),Array(q1,q2,q3,q4)) {
      def onStart() = q1.label()
    })
  }

  private def scanOneDir(dir:File ) {
    _mask match {
      case Some(m) => postPoll(dir.listFiles(m))
      case _ =>
    }
  }

  private def testDir(dir:File) = {
    if ( ! FileUte.isDirWRX(dir)) {
      throw new Exception("FilePicker: Folder: " +
          dir.getCanonicalPath() + " must be a valid directory with RW access")
    }
    dir
  }

  private def postPoll(files:Seq[File]) {
    val me=this
    files.foreach { (f) =>
      tlog().debug("FilePicker: new file : {}" , f)
      var error:Option[Exception]=None
      val fn= niceFPath(f)
      var cf=f
      _destMove match {
        case Some(dm) =>
          try {
            cf= FileUte.moveFileToDir(f, dm, false)
          } catch {
            case e:Exception => error=Some(e)
          }
        case _ =>
      }
      try {
        dispatch(new FileEvent(me, fn, cf, error))
      } catch {
        case e => tlog().warn("",e)
      }
    }
  }

}
