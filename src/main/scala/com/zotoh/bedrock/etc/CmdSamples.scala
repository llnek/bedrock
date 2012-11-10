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
import com.zotoh.fwk.util.StrUte._
import com.zotoh.fwk.io.IOUte._

import java.util.{Properties=>JPS}
import java.io.File

import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.NotFileFilter
import org.apache.commons.io.filefilter.SuffixFileFilter

import com.zotoh.bedrock.core.CmdHelpError

/**
 * (Internal use only).
 *
 * @author kenl
 */
class CmdSamples(home:File,cwd:File) extends CmdApps(home,cwd) {

  private val SPLDIRS= Array(
    "delegate", "atom", "file", "jetty",
    //"web",
    "stockquote", "multistep",
    "async", "fork", "http", "jms", "ssl", "pop3",
    "stateful", "tcpip", "timer", "rest", "websock" )

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.etc.CmdApps#getCmds()
   */
  override def cmds() = Array("demo")

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.etc.CmdApps#eval(java.lang.String[])
   */
  override def eval(args:Seq[String]) {

    if (args==null || args.length < 1
        || "samples" != args(1)
        || "demo" != args(0) ) {
      throw new CmdHelpError()
    }

    val lang= if(args.length > 2) args(2) else "java"
    val sample= new File("sample_apps")
    sample.mkdirs()
    val demo_src=new File(maedrDir(), "samples")
    var appprops=rc2Str("com/zotoh/bedrock/env/"+APPPROPS, "utf-8")

    println("\n\nCreating samples pack...\n")

    SPLDIRS.foreach { (ptr) =>
      val ps= new JPS().
      add("apptype", APPTYPE_SVR).
      add("storage", asJObj(false))

      println("%-24s%s".format("Creating sample: ", ptr) )

      val a=create0(true, sample, ptr)
      var ms=readText(new File(new File(demo_src, ptr), "app.mf"), "utf-8")
      ms=strstr(ms, "${PREAMBLE}",
          if("scala"==lang) "Preamble" else "$Preamble")

      ptr match {
        case "stateful" => ps.put("storage", asJObj(true))
        case "ssl" =>
          ms=strstr(ms, "$KEY.P12", asFileUrl(
                new File(new File(a, CFG), "test.p12") ) )
        case "file" =>
          var tp= new File(new File(a, TMP), "infiles")
          tp.mkdirs()
          ms=strstr(ms, "$FILEPICK_SRC", niceFPath(tp))
          tp= new File(new File(a, TMP), "cache")
          tp.mkdirs()
          ms=strstr(ms, "$FILEPICK_DES", niceFPath(tp))
        case "websock" =>
          FileUtils.copyFileToDirectory(new File(new File(demo_src, ptr),"squarenum.html"), new File(a, CFG))
          FileUtils.copyFileToDirectory(new File(new File(demo_src, ptr),"jquery.js"), new File(a, CFG))
        case "jetty" | "web" =>
          ps.put("apptype", APPTYPE_WEB)
          val bs="assets"
          val ss=new File(a, "webapps/scripts");ss.mkdirs()
          val ii=new File(a, "webapps/images");ii.mkdirs()
          val cc=new File(a, "webapps/styles");cc.mkdirs()
          FileUtils.copyFileToDirectory(new File(new File(demo_src, bs),"favicon.ico"), ii)
          FileUtils.copyFileToDirectory(new File(new File(demo_src, bs),"test.js"), ss)
          FileUtils.copyFileToDirectory(new File(new File(demo_src, bs),"main.css"), cc)
          if ("web"==ptr) {
            val inf=new File(a, "webapps/WEB-INF");inf.mkdirs()
            new File(a, "webapps/WEB-INF/classes").mkdirs()
            FileUtils.copyFileToDirectory(new File(new File(demo_src, ptr),"web.xml"), inf)
          }
          ms=strstr(ms, "$RESBASE", new File(a,"webapps").toURI().toURL().toExternalForm())
        case "pop3" =>
          appprops= "bedrock.pop3.mockstore=com.zotoh.bedrock.mock.mail.MockPop3Store\n" + appprops
        case "delegate" =>
          ps.put("delegate", "SampleDelegate")
      }

      create1(a, ms, ps)

      ps.add("package", "demo."+ptr).add("delegate", "").add("lang", lang)

      create2(a, appprops, ps)
      create3s(a, demo_src, ptr, ps)
      create4(a, ps)
      create5(a, ps)
      create6(a)
      create8(a, ps)
    }

    println("\nAll samples created successfully.\n")
  }

  private def create3s(appdir:File, src:File, ptr:String, pps:JPS) {
    val t= new File(appdir, SRC)
    val lang=pps.gets("lang")
    val j= new File(t, lang +"/demo/"+ptr)
    j.mkdirs()
    FileUtils.copyDirectory(new File(src, ptr+"/"+lang), j,
        new NotFileFilter(new SuffixFileFilter(".mf")))
    val it=FileUtils.iterateFiles(j, new SuffixFileFilter("."+lang), null)
    while (it.hasNext()) {
      val f=it.next()
      var s=readText(f, "utf-8")
      s=strstr(s, "demo."+ptr+"."+ lang, "demo."+ptr)
      writeFile(f, s, "utf-8")
    }
  }

}


