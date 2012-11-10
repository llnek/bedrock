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

import java.io.{InputStream,File,FilenameFilter}
import org.apache.commons.io.FileUtils
import org.json.JSONArray
import org.json.JSONObject

import com.zotoh.fwk.util.JSONUte
import com.zotoh.fwk.util.JSONUte._
import com.zotoh.bedrock.core.CmdHelpError

/**
 * (Internal use only).
 *
 * @author kenl
 */
class CmdAppOps(home:File,cwd:File) extends Cmdline(home,cwd) {

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.etc.Cmdline#getCmds()
   */
  override def cmds() = Array("app")

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.etc.Cmdline#eval(java.lang.String[])
   */
  override def eval(args:Seq[String]) {
    if (args==null || args.length < 2) {
      throw new CmdHelpError()
    }
    val s2= if(args.length > 2) args(2) else ""
    args(1) match {
      case "compile" => compile()
      case "test" => testCode()
      case "ide/eclipse" => genEclipse()
      case "bundle" => pack(s2)
      case "debug" => dbg(s2)
      case x:String if (x.swic("create")) => create("create/web"==x,s2)
      case x:String if (x.swic("start")) => launch("start/bg"==x)
      case _ => throw new CmdHelpError()
    }
  }

  private def dbg(port:String) {
    runTarget( if(isWindows()) "run-dbg-app-w32" else "run-dbg-app-nix")
  }

  private def create(web:Boolean, app:String) {
    if (isEmpty(app)) { throw new CmdHelpError() }
    new CmdApps( maedrDir(), cwd()).eval(Array( "create", app, if(web) "/webapp" else ""))
  }

  private def pack(out:String) {
    if (isEmpty(out)) { throw new CmdHelpError() }
    assertAppDir()
    // make a copy of original ant.xml in the tmp dir
    // coz that's the one which gets packaged.
    var fo= new File( new File( cwd() , TMP ) , "/ant.orig.xml")
    val s=rc2Str("com/zotoh/bedrock/env/ant.xml", "utf-8")
    val web=isWebApp()
    writeFile(fo, s, "utf-8")

    fo= new File(out)
    fo.mkdirs()

    val os=niceFPath(fo)
    if (isWindows()) {
      runTargetExtra(
        if (web) bundleWebApp("bundle-webapp-w32", os) else "bundle-w32", ANTOPT_OUTDIR, os)
    } else {
      runTargetExtra( if(web) bundleWebApp("bundle-webapp-nix",os) else "bundle-nix", ANTOPT_OUTDIR, os)
    }

  }

  private def bundleWebApp(target:String, outdir:String) = {
    val cfg= new File( cwd(), CFG)
    val (root,devs) = using(open(new File(cfg, APPCONF))) { (inp) =>
      val r=JSONUte.read(inp)
      (r,r.optJSONObject(CFGKEY_DEVICES) )
    }

    val it=devs.keys()
    var jetty=""
    var proc=""
    var cnt=0

    while (it.hasNext()) {
      val key=nsb( it.next())
      val dev= devs.optJSONObject(key)
      if(dev!=null) dev.optString(CFGKEY_TYPE) match {
        case "jetty"  if  ! (dev.has(DEV_STATUS) && !dev.optBoolean(DEV_STATUS)) =>
          proc= dev.optString("processor")
          jetty=key
          cnt += 1
      }
    }

    if (cnt > 1) { throw new Exception("Too many Jetty device(s)") }
    if (cnt==0) { throw new Exception("No Jetty device defined") }

    var dev= devs.remove(jetty).asInstanceOf[JSONObject]
    val xml= toWebXML(dev)

    dev= newJSON()
    dev.put(CFGKEY_TYPE, DT_WEB_SERVLET)
    dev.put("port", "0")
    dev.put("host", "")
    if (!isEmpty(proc)) {  dev.put("processor", proc)  }
    devs.put(WEBSERVLET_DEVID, dev)

    val json= JSONUte.asString(root)
    val fo= new File(outdir, "webapps")
    fo.mkdirs()

    val t= new File(fo, TMP)
    t.mkdirs()

    new File(fo, REALM).mkdirs()
    new File(fo, DB).mkdirs()

    val c=new File(fo, CFG)
    c.mkdirs()

    new File(fo, LOGS).mkdirs()

    FileUtils.copyFileToDirectory(new File(cfg, APPPROPS), c)
    writeFile(new File(c, APPCONF), json, "utf-8")
    writeFile(new File(t, "web.xml"), xml, "utf-8")

    target
  }

  private def testCode() {
    assertAppDir()
    runTarget("test-code")
  }

  private def compile() {
    assertAppDir()
    runTarget("compile-code")
  }

  private def launch(bg:Boolean) {
    assertAppDir()
    if (bg) {
      runTarget( if(isWindows()) "run-app-bg-w32" else "run-app-bg-nix")
    }else {
      runTarget("run-app")
    }
  }

  private def toWebXML(jetty:JSONObject) = {
    val xml= rc2Str("com/zotoh/bedrock/env/web.xml", "utf-8")
    val r= toServletFrag(jetty) + toFilterDefs(jetty)
    strstr(xml, "<!-- INSERT CONTENT HERE -->", r)
  }

  private def toServletFrag(jetty:JSONObject) = {
    val sn= "BEDROCK Servlet"
    val s= "<servlet>\n\t<servlet-name>" + sn+ "</servlet-name>\n\t" +
    "<servlet-class>com.zotoh.bedrock.device.WEBServlet</servlet-class>\n" +
    "\t<load-on-startup>1</load-on-startup>\n" +
    "</servlet>"
    s + "\n" + toServletMappings(jetty, sn)
  }

  private def toServletMappings(jetty:JSONObject, sn:String) = {
    val urls= jetty.optJSONArray("urlpatterns")
    val b= new StringBuilder(512)
    if ( urls != null) for (i <- 0 until urls.length()) {
      b.append ("<servlet-mapping>\n\t<servlet-name>" + sn + "</servlet-name>\n\t" +
               "<url-pattern>" + urls.optString(i) + "</url-pattern>\n</servlet-mapping>\n"
          )
    }
    b.toString
  }

  private def toFilterDefs(jetty:JSONObject) = {
    val fils= jetty.optJSONArray("filters")
    val b= new StringBuilder(512)
    if (fils != null) for ( i <- 0 until fils.length()) {
      val fn= "filter" + (i+1).toString
      val f= fils.optJSONObject(i)
      b.append( "<filter>\n\t<filter-name>" + fn + "</filter-name>\n\t<filter-class>" +
          f.optString("class") + "</filter-class>" +
      toFilterParams( f.optJSONObject("params"))+"</filter>\n" +
      "<filter-mapping>\n\t<filter-name>" + fn+"</filter-name>\n\t" +
      "<url-pattern>"+ f.optString("urlpattern") + "</url-pattern>\n" +
      "</filter-mapping>\n" )
    }
    b.toString
  }

  private def toFilterParams(obj:JSONObject) = {
    val it = if (obj != null) obj.keys() else null
    val b= new StringBuilder(512)
    if (it != null) while (it.hasNext()) {
      val key=nsb( it.next())
      b.append(toInitParam(key, obj.optString(key)))
    }
    b.toString
  }

  private def toInitParam(pn:String, pv:String ) = {
    "<init-param>\n\t<param-name>" +
    pn + "</param-name>\n\t<param-value>" +
    pv + "</param-value>\n</init-param>\n"
  }

  protected def genEclipse() {
    assertAppDir()
    val lang =appLang()
    lang match {
      case "groovy" | "scala" | "java" => genEclipseProj(lang)
      case _ =>
        throw new Exception("Failed to generate eclipse project, language = " + lang)
    }
  }

  protected[etc] def genEclipseProj(lang:String) {
    val ec= new File(cwd(), ECPPROJ)
    val home=maedrDir()
    ec.mkdirs()
    FileUtils.cleanDirectory(ec)
    val app=cwd().getName()

    var str=rc2Str("com/zotoh/bedrock/eclipse/"+lang+"/project.txt", "utf-8")
    str=strstr(str, "${APP.NAME}", app)
    str=strstr(str, "${" + lang.uc +".SRC}", niceFPath(new File(cwd(), "src/main/"+lang)))
    str=strstr(str, "${TEST.SRC}", niceFPath(new File(cwd(), "src/main/test")))
    writeFile( new File(ec, ".project"), str, "utf-8")
    str=rc2Str("com/zotoh/bedrock/eclipse/"+lang+"/classpath.txt", "utf-8")

    val sb= new StringBuilder(512)
    scanJars(new File(home, "lib"), sb)
    scanJars(new File(home, "thirdparty"), sb)
    scanJars(new File(home, "dist"), sb)
    scanJars(new File(cwd, "lib"), sb)
    scanJars(new File(cwd, "thirdparty"), sb)
    scanJars(new File(cwd, "dist"), sb)
    str=strstr(str, "${CLASS.PATH.ENTRIES}", sb.toString())
    writeFile( new File(ec, ".classpath"), str, "utf-8")
  }

  private def scanJars(dir:File, out:StringBuilder) {
    val fs= dir.listFiles(new FilenameFilter() {
      override def accept(f:File, name:String) = name.endsWith(".jar")
    })
    if (fs != null) fs.foreach { (f) =>
      var p=niceFPath(f)
      p="<classpathentry kind=\"lib\" path=\"" + p + "\"/>\n"
      out.append(p)
    }
  }

}


