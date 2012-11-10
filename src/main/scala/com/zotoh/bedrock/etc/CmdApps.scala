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

import com.zotoh.fwk.util.JSONUte
import com.zotoh.fwk.io.IOUte._
import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.util.StrUte._
import com.zotoh.fwk.util.FileUte._
import com.zotoh.bedrock.core.Module

import java.util.{Properties=>JPS}
import java.io.File

import org.apache.commons.codec.binary.Base64
import org.json.{JSONObject=>JSNO}

import com.zotoh.fwk.crypto.PwdFactory
import com.zotoh.fwk.util.{CmdLineQ,CmdLineSeq,CoreImplicits}
import com.zotoh.fwk.util.JSONUte._
import com.zotoh.bedrock.core.CmdHelpError

/**
 * (Internal use only).
 *
 * @author kenl
 */
class CmdApps(home:File,cwd:File) extends Cmdline(home,cwd) with CoreImplicits {

  protected val DIRS= Array(
    PATCH, BIN, REALM, CFG, TPCL, LOGS, DB, LIB, CLSS, SRC, DIST, TMP )

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.etc.Cmdline#getCmds()
   */
  override def cmds() = Array("create")

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.etc.Cmdline#eval(java.lang.String[])
   */
  override def eval(args:Seq[String]) {

    if (args==null || args.length < 2 || "create" != args(0)) {
      throw new CmdHelpError()
    }

    val webapp= "/webapp"==( if(args.length > 2) args(2) else "")
    val app= args(1)

    var bOK=true
    val props= new JPS().add("name", app).add("storage", true)
    constructInput(app) match {
      case Some(s) => bOK = !s.start(props).isCanceled()
    }
    if (bOK) {
      props.add("delegate", "DelegateImpl").
      add("processor", "FlowImpl").
      put("apptype", if(webapp) APPTYPE_WEB else APPTYPE_SVR)
      evalMore(webapp, args,props)
    }
  }

  private def evalMore(webapp:Boolean, args:Seq[String], props:JPS) {

    println("\n\n")
    println("%-20s%s".format( "Application name:" , props.gets("name")) )
    println("%-20s%s".format( "Package name:" , props.gets("package")) )
    /*
    println("%-20s%s".format( "Database:" ,
      if(props.getb("storage")) "yes" else "no") )
    */
    println("%-20s%s".format( "Language Choice:" , props.gets("lang")) )
    println("\n")

    val appdir=create0( false,cwd(), props.gets("name"))
    create1(appdir, "", props)
    create2(appdir, "", props)
    create3(appdir, props)
    create4(appdir, props)
    create5(appdir, props)
    create6(appdir)

    if (webapp) { create7(appdir, props) }
    create8(appdir, props)

    println("\n")
    println("Application was created successfully.")
    println("\n")
  }

  /**
   * Make all the 1st level sub-dirs under app-dir.
   *
   * @param dir
   * @param app
   * @return
   * @throws Exception
   */
  protected def create0(isSamples:Boolean, dir:File, app:String) = {
    val appdir= new File(dir, app)
    if (!isSamples) { println("Creating application directory: " + app) }
    appdir.mkdir()
    DIRS.foreach { (d) =>
      if (!isSamples) { println("Creating sub-directory: " + d) }
      new File(appdir, d).mkdirs()
    }
    appdir
  }

  /**
   * Create the manifest file.
   *
   * @param appdir
   * @param mf
   * @param pps
   * @throws Exception
   */
  protected def create1(appdir:File, mf:String, pps:JPS) {
    var s=rc2Str("com/zotoh/bedrock/env/app.mf", "utf-8" )
    val cfg=new File(appdir, CFG)
    val out= new File( niceFPath(cfg) + "/app.conf")
    if (!isEmpty(mf)) { s= mf }
    val pv= fullClassNS(pps, "processor", "")
    s=strstr(s, "${PROCESSOR}", pv)
    writeFile(out, s, "utf-8")
    pps.put("mf", out)
  }

  /**
   * @param appdir
   * @param ps
   * @param pps
   * @throws Exception
   */
  protected def create2(appdir:File, ps:String, pps:JPS) {
    val realm=new File(appdir, REALM)
    val wdb= pps.getb("storage")
    val cfg=new File(appdir, CFG)
//    var w= new File(appdir, TMP)
    val db= new File(appdir, DB)
//    File mf= (File)props.get("mf")
    val pcfg= niceFPath(cfg)

    val dfcz= Module.pipelineModule() match {
      case Some(m) => m.dftDelegateClass()
      case _ => ""
    }
    var s=rc2Str("com/zotoh/bedrock/env/"+APPPROPS, "utf-8")
    if (!isEmpty(ps)) {
      s=ps
    }

    val pwd= PwdFactory.mkRandomText(24)
    writeFile( new File(realm, KEYFILE),
        "B64:"+Base64.encodeBase64URLSafeString(asBytes(pwd)), "utf-8")

    val dg= fullClassNS(pps, "delegate", dfcz)
    s= strstr(s, "${DELEGATE_CLASS}", dg)
    s= strstr(s, "${MANIFEST_FILE}", "") //niceFPath(mf))
    s= strstr(s, "${WORK_DIR}", "") //niceFPath(w))
    s= strstr(s, "${DB_URL}", niceFPath(db))
    s=strstr(s, "${DB_FLAG}", if(wdb) "" else "#")
    s=strstr(s, "${WANT_DB}", if(wdb) "true" else "false")

    var out= new File( pcfg + "/" + APPPROPS)
    writeFile(out, s, "utf-8")

    s=rc2Str("com/zotoh/bedrock/env/" + CLOUDDATA, "utf-8")
    out= new File( pcfg + "/" + CLOUDDATA)
    writeFile(out, s, "utf-8")

    // copy test server p12 file
    out= new File( pcfg + "/test.p12")
    writeFile(out, rc2Bytes("com/zotoh/bedrock/env/test.p12") )
  }

  /**
   * @param appdir
   * @param pps
   * @throws Exception
   */
  protected def create3(appdir:File, pps:JPS) {
    var cname= pps.gets("delegate")
    val pk= pps.gets("package")
    val lang= pps.gets("lang")
    val wdb= pps.getb("storage")
    val cfg= new File(appdir, CFG)
    val test= new File(appdir, TESTSRC+ "/" + lang + "/test")
    var f= new File(appdir, SRC+"/"+lang)
    test.mkdirs()
    f.mkdirs()
    f=new File(f, strstr(pk, ".", "/"))
    f.mkdirs()

    var out= new File(f, cname+"."+lang)
    // deal with delegate file
    var s= rc2Str("com/zotoh/bedrock/tpl/Delegate."+lang+".tpl", "utf-8")
    s=strstr(s, "${PACKAGE_ID}", pk)
    s=strstr(s, "${CLASS_NAME}", cname)
    // for scala ?
    s=strstr(s, "${STATEFUL_FLAG}", if(wdb) "true" else "false")
    s=strstr(s, "${PROC_CLASS_NAME}", pps.gets("processor"))
    writeFile(out, s, "utf-8")

    // deal with processor file
    cname=pps.gets("processor")
    out= new File(f, cname+"."+lang)
    s= rc2Str("com/zotoh/bedrock/tpl/" + (if(wdb) "StateP" else "NStateP") + "."+lang+".tpl", "utf-8")
    s=strstr(s, "${PACKAGE_ID}", pk)
    s=strstr(s, "${CLASS_NAME}", cname)
//    s=strstr(s, "${TASK_NAME}", pps.gets("task"))
    writeFile(out, s, "utf-8")

    // add a mock runner for local debugging purpose
    s= rc2Str("com/zotoh/bedrock/tpl/MockRunner" + "."+lang+".tpl", "utf-8")
    s=strstr(s, "${PACKAGE_ID}", pk)
    s=strstr(s, "${APP.DIR}", niceFPath(appdir))
    s=strstr(s, "${LOG4J.REF}", asFileUrl(new File(cfg, "log4j.properties")))
    s=strstr(s, "${MANIFEST.FILE}", niceFPath(new File( cfg, APPPROPS)))
    out= new File(f, "MockRunner."+lang)
    writeFile(out, s, "utf-8")

    // add a junit test class
    s= rc2Str("com/zotoh/bedrock/tpl/TestSuite" + "."+lang+".tpl", "utf-8")
    out= new File(test, "TestSuite."+lang)
    writeFile(out, s, "utf-8")
  }

  /**
   * Create the properties file.
   *
   * @param appdir
   * @param pps
   * @throws Exception
   */
  protected def create4(appdir:File, pps:JPS) {
    val cfg= new File(appdir, CFG)
    var s=rc2Str("com/zotoh/bedrock/env/ant.xml", "utf-8")
    s= strstr(s, "${env.BEDROCK_HOME}", niceFPath(maedrDir()))
    writeFile(new File( niceFPath(cfg) + "/ant.xml"),s,"utf-8")
  }

  /**
   * Create the log4j file.
   *
   * @param appdir
   * @param pps
   * @throws Exception
   */
  protected def create5(appdir:File, pps:JPS) {
//    File log= new File(appdir, LOGS)
    val cfg= new File(appdir, CFG)
    var s=rc2Str("com/zotoh/bedrock/env/"+LOG4J, "utf-8" )
//    s= strstr(s, "${LOGS_DIR}", niceFPath(log))
    s= strstr(s, "${LOGS_DIR}", "./logs")
    writeFile( new File( cfg, LOG4J_PROPS), s, "utf-8")
  }

  /**
   * @param appdir
   * @throws Exception
   */
  protected def create6(appdir:File) {
    if (isWindows()) {
      //TODO
    } else {
      var cmd= "chmod 700 " + niceFPath(  new File(appdir, REALM) )
      Runtime.getRuntime().exec(cmd)
      cmd= "chmod 600 " + niceFPath(  new File(new File(appdir, REALM),KEYFILE) )
      Runtime.getRuntime().exec(cmd)
    }
  }

  /**
   * @param appdir
   * @throws Exception
   */
  protected def create7(appdir:File, pps:JPS) {

    var s= rc2Str("com/zotoh/bedrock/env/webapp.conf", "utf-8")
    val pkg= pps.gets("package")
    val lang= pps.gets("lang")

    new File(appdir, "webapps/WEB-INF/classes").mkdirs()
    new File(appdir, "webapps/WEB-INF/lib").mkdirs()
    new File(appdir, "webapps/images").mkdirs()
    new File(appdir, "webapps/scripts").mkdirs()
    new File(appdir, "webapps/styles").mkdirs()

    s=strstr(s, "${RESBASE}", niceFPath(new File(appdir, "webapps")))
    s=strstr(s, "${PROCESSOR}", pkg+"." + "WEBProcessor")
    writeFile( new File( new File(appdir, CFG), APPCONF), s)

    var f= new File(appdir, SRC+"/"+lang)
    f.mkdirs()
    f=new File(f, strstr(pkg, ".", "/"))
    f.mkdirs()

    s= rc2Str("com/zotoh/bedrock/tpl/WEBProc."+ lang + ".tpl", "utf-8")
    s=strstr(s, "${PACKAGE_ID}", pkg)
    writeFile( new File(f, "WEBProcessor"+"."+lang), s, "utf-8")
  }

  protected def create8(appdir:File, pps:JPS) {
    val lang=pps.gets("lang")
    val obj= newJSON()
    obj.put("apptype", pps.gets("apptype"))
    obj.put("lang", lang)
    writeFile( new File(new File(appdir, CFG), APP_META), JSONUte.asString(obj), "utf-8" )
    new CmdAppOps(maedrDir(), appdir).genEclipseProj(lang)
  }

  private def fullClassNS(pps:JPS, key:String, df:String) = {
    val pkg= pps.gets("package")
    var pv=pps.gets(key)
    if (isEmpty(pv)) {
      df
    } else {
      pkg+"."+pv
    }
  }

  private def constructInput(appdir:String) = {
    val q4= new CmdLineQ("lang", bundleStr(rcb(),"cmd.which.lang"), "java/groovy/scala", "scala") {
      def onRespSetOut(a:String, p:JPS) = {
        p.put("lang", a)
        ""
      }}
    val q3= new CmdLineQ("db", bundleStr(rcb(),"cmd.use.db"), "y/n", "n") {
      def onRespSetOut(a:String,  p:JPS)= {
        p.put("storage", asJObj("Yy".has(a)))
        "lang"
      }}
    val q2= new CmdLineQ("pkg", bundleStr(rcb(),"cmd.package"), "", appdir) {
      def onRespSetOut(a:String, p:JPS) = {
        p.put("package", a)
        //"db"
        "lang"
      }}
    val q1= new CmdLineQ("app", bundleStr(rcb(),"cmd.app.name"), "",appdir) {
      def onRespSetOut(a:String, p:JPS) = {
        p.put("name", a)
        q2.setDftAnswer("com." + a.lc)
        "pkg"
      }}
     Some(new CmdLineSeq(Array(q1,q2,q3,q4)) {
      def onStart() = q2.label()
    })

  }



}


