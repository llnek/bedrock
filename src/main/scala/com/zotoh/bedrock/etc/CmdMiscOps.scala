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

import java.io.File
import com.zotoh.fwk.util.StrUte._
import com.zotoh.fwk.io.IOUte._
import com.zotoh.bedrock.core.CmdHelpError

/**
 * @author kenl
 *
 */
class CmdMiscOps(home:File,cwd:File) extends Cmdline(home,cwd) {

  override def cmds() = Array("package-app","version","deploy-app")

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.etc.Cmdline#eval(java.lang.String[])
   */
  override def eval(args:Seq[String]) {

    if (args==null || args.length < 1) {
      throw new CmdHelpError()
    }
    args(0) match {
      case "package-app" => runTarget("packzip-app")
      case "version" => showVersion()
      case _ => throw new CmdHelpError()
    }
  }

  private def showVersion() {
    val f=new File(maedrDir(), "VERSION")
    if ( f.canRead()) {
      val s= readText(f, "utf-8")
      println( if (isEmpty(s)) "???" else s )
    }
  }





}
