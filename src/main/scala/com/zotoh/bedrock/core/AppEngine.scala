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

package com.zotoh.bedrock.core

import scala.collection.mutable.HashMap

import java.lang.System._

import java.lang.management.ManagementFactory
import java.io.{File,IOException}
import java.lang.reflect.{Constructor=>JCtor}
import java.sql.SQLException
import java.util.{Properties=>JPS}

import com.zotoh.fwk.io.XData._
import com.zotoh.fwk.io.XData
import com.zotoh.fwk.io.IOUte._
import com.zotoh.fwk.util.MetaUte._
import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.util.StrUte._
import com.zotoh.fwk.util.ProcessUte._
import com.zotoh.fwk.util.FileUte._
import com.zotoh.fwk.util.LoggerFactory._
import com.zotoh.fwk.util.{Logger,FileUte,CoreImplicits}
import com.zotoh.fwk.util.ProcessUte._
import com.zotoh.fwk.db.{DBUte,DBVendor,DDLUte,JDBC,JDBCInfo,JDBCPool,JDBCPoolMgr}
import com.zotoh.fwk.crypto.PwdFactory

import com.zotoh.bedrock.device.DeviceMgr
import java.util
import com.zotoh.bedrock.svc.DeviceService


object AppEngine {
}


/**
 * The AppEngine is the runtime which *hosts* the user application.  Essentially, a user application is a collection of
 * processors provided by the application to handle jobs, generated  by IO devices.
 *
 * @author kenl
 */
class AppEngine(private val _module:Module) extends Vars with CoreImplicits {

  _module.bind(this)

  private def ilog() { _log=getLogger(classOf[AppEngine]) }
  @transient private var _log:Logger = null
  def tlog() = {  if (_log==null) ilog(); _log  }

  private var _externalContainer=false
  private var _active = false

  private val _freeze= new Object()
  private val _props= new JPS()


  private var _pidFile:File= _
  private var _appDir:File = _

  private val _jobCreator = new JobCreator(this)
  private val _scheduler= new Scheduler(this)
  private val _devMgr = new DeviceMgr(this)
  // config depends on device-manager,
  // so create it after dev-mgr
  private val _cfg= new Config(this)

  // db stuff
  private val _poolMgr= new JDBCPoolMgr()
  private var _pool:JDBCPool= _

  // app should replace with their own delegate
  private var _app:AppDelegate= new AppDelegate(this) {
    def newProcess(j: Job): Pipeline = null
  }

  def putQuirk(p:Object, v:Object) = _props.put(p,v)
  
  def quirk(p:String) = _props.gets(p)

  /**
   * @return the engine properties.
   */
  def quirks() = _props

  def appDir() = _appDir

  def deviceMgr() = _devMgr

  def delegate() = _app

  def config() = _cfg

  def jobCtr() = _jobCreator

  def scheduler() = _scheduler

  def module() = _module
  
  /**
   * @param props
   */
  def load(props:JPS) = { _props.putAll(props); this }

  /**
   * @param props
   */
  def startViaContainer(root:File, props:JPS) {
    tlog().info("AppEngine: startViaContainer() called")
    _externalContainer=true
    _appDir=root
    start(false, props)
  }

  /**
   * Starts the application.
   * @param props
   */
  def start( props:JPS) {
    start(true, props)
  }

  def scanForDeviceDefns() {
    var z:Class[_] = null
    val obj= null //z.newInstance()

    block { () =>
      z= loadClass( _props.gets(APP_PACKAGE) + ".DeviceManifest")
    }

    if (z != null)
    try {
      val m= z.getDeclaredMethod("extensions").invoke(obj).asInstanceOf[ Map[String,String] ]
      m.foreach { (t) =>
        config().cusDevFactory().add(t._1,t._2)
      }
    } catch {
      case e => //tlog().errorX("",Some(e))
    }

    if (z != null)
    try {
      val a= z.getDeclaredMethod("devices").invoke(obj).asInstanceOf[ Array[DeviceService] ]
      a.foreach { (d) =>
        d.reify(deviceMgr())
      }
    } catch {
      case e => //tlog().errorX("",Some(e))
    }

  }

  def shutdown() {
    if (_active) {
      tlog().info("AppEngine: shutdown() called")
      stop()
      safeThreadWait(1500)
      _freeze.synchronized {
        block { () => _freeze.notify() }
      }
    }
  }

  def newJdbc() = {
    if (_pool == null) null else JDBC(_pool)
  }

  /**
   * @param uri
   * @param pwd
   * @return
   */
  def verifyShutdown(uri:String, pwd:String) = {
    tlog().debug("AppEngine: uri=> {} pwd=> {}", uri, "***")
    if (SHUTDOWN_URI != nsb(uri)) true else {
      var s= _props.gets(SHUTDOWN_PORT_PWD)
      try {
        s=nsb( PwdFactory.mk(s).text())
      } catch {
        case e =>  { tlog().warnX("",Some(e)); s="" }
      }
      if (isEmpty(s)) true else s==pwd
    }
  }

  def bindSvcs( f: DeviceMgr => Unit )  {
    f(_devMgr)
  }

  /**
   * @return
   */
  def isEmbedded() = _externalContainer

  /**
   * @return
   */
  def db() = _pool

  private def start( block:Boolean, props:JPS) {
    _props.putAll(props)
    start(block)
  }

  private def start(block:Boolean) {
    tlog().info("AppEngine: start() called")
    preBoot_0()
    preBoot_1()
    boot()
    if (block) {    blockAndWait()  }
  }

  private def preBoot_0() {
    var v=""
    val cwd = if (_appDir==null) {
      v= _props.gets(APP_DIR)
      if(isEmpty(v)) FileUte.cwd() else new File(v)
    } else {
      _appDir
    }

    // config system resources
    scheduler().iniz()

    v= trim( _props.gets(WORK_DIR))
    if (isEmpty(v)) {
      v= niceFPath( new File(cwd, TMP))
    }
    setWorkDir(new File(v))

    v= trim(_props.gets(FILE_ENCODING))
    if (! isEmpty(v)) {
      setProperty("file.encoding", v)
    }

    getProperties().put(APP_DIR, niceFPath(cwd))
    _props.put(APP_DIR, niceFPath(cwd))

    _appDir= cwd
  }

  private def preBoot_1() {
    var str= trim( _props.gets(DELEGATE_CLASS))
    // deal with delegate creation...
    if (isEmpty(str)) {
      str= module().dftDelegateClass()
    }
    //tlog().warn("AppEngine: no delegate class provided, the built-in delegate will be used")
    // find the package name
    if (!isEmpty(str)) {
      val pos= str.lastIndexOf('.')
      if (pos >0) {
        _props.put(APP_PACKAGE, str.substring(0,pos) )
      }
    }

    val z= loadClass(str); tstArgIsType("Delegate-Class",
      z, classOf[AppDelegate])

    val cs= z.getConstructors()
    val cwd= appDir()
    var i=0
    var ctor:JCtor[_]= null

    if (cs != null)
    do {
      val zz=cs(i).getParameterTypes()
      if (!isNilSeq(zz) &&
        classOf[AppEngine].isAssignableFrom( zz(0))) {
        ctor= cs(i)
      }
      i += 1
    } while (ctor==null && i < cs.length)

    if (ctor == null) {
      throw new InstantiationException("Class: " + str + " is missing ctor(AppEngine)")
    }

    _app= ctor.newInstance(this).asInstanceOf[AppDelegate]

    // parse manifest file which has all the sources defined
    /*
    str= _props.gets(MANIFEST_FILE)
    if (isEmpty(str)) {
      str= niceFPath( new File( new File(cwd, CFG), APPCONF))
    }
    */
//    tstEStrArg("conf-file-path", str);
    //tlog().debug("AppEngine: about to load conf file: {}" , str)
    //_cfg.parse( new File( str).toURI().toURL() )
  }

  private def boot() {
    tlog().debug("AppEngine: delegate class: {}" , _app.getClass().getName() )

    _cfg.onSys()
    inizDB()
    loadDevices()

    if ( !isEmbedded()) block { () =>
      hookShutdown()
    }

    maybeUpdateProcID()

    _devMgr.start()
    _active=true
    
    tlog().info("AppEngine: ready")
  }

  private def maybeUpdateProcID() {
    val cwd= appDir()
    val p= pid() 
    tlog().info("AppEngine: process-id {}", asJObj(p))
    if ( cwd.canWrite()) {
      _pidFile =new File(cwd, PROCID)
      writeFile(_pidFile, p)
    }
  }

  private def hookShutdown() {
    tlog().debug("AppEngine: adding shutdown hook...")
    Runtime.getRuntime().addShutdownHook(new Thread() {
      override def run() {
        block { () => shutdown() }
      }
    })
  }

  private def blockAndWait() {
    _freeze.synchronized {
      block { () =>   _freeze.wait()      }
    }
    tlog().info("AppEngine: stopped")
  }

  private def stop() {
    println("")
    FileUte.delete(_pidFile)
    finzDB()
    unloadDevices()
    block { () => _app.onShutdown() }
    _active=false
    _pidFile=null
  }

  private def unloadDevices() {
    tlog().debug("AppEngine: unloading event devices...")
    _devMgr.unload()
  }

  private def loadDevices() {
    var str= _props.gets(MANIFEST_FILE)
    if (isEmpty(str)) {
      str= niceFPath( new File( new File(appDir(), CFG), APPCONF))
    }
    tlog().debug("AppEngine: about to load conf file: {}" , str)

    _cfg.parse( new File( str).toURI().toURL() )
    scanForDeviceDefns()

    tlog().info("AppEngine: loading devices...")
    _devMgr.load()
    tlog().info("AppEngine: devices loaded")
  }

  private def inizDB() {
    val reset= "true".eqic( trim(_props.gets(JDBC_RESET)))
    val url = trim(_props.gets(JDBC_URL))
    if ( ! isEmpty(url)) {
      tlog().info("AppEngine: initializing state datastore")
      val p= new JDBCInfo(
          trim(_props.gets(JDBC_DRIVER)),
          url,
          trim(_props.gets(JDBC_USER)),
          trim(_props.gets(JDBC_PWD)))
      maybeConfigDB(p, reset)
      _pool= _poolMgr.mkPool(p)
    }
  }

  private def finzDB() {
    block { () => _poolMgr.finz() }
  }

  private def maybeConfigDB(p:JDBCInfo, reset:Boolean) {
    if (reset || ! DBUte.tableExists(p, STATE_TABLE)) {
      inizStateTables(p)
    }
  }

  private def inizStateTables(jp:JDBCInfo) {
    val v= DBUte.vendor(jp)
    if (DBVendor.NOIDEA == v) { throw new SQLException("Unknown DB: " + jp.dbUrl()) }
    val bd= "com/zotoh/bedrock/db/" + v + ".sql"
    val ddl= rc2Str(bd, "utf-8")
    if (isEmpty(ddl)) { throw new SQLException("Unsupported DB: " + v) }
    DDLUte.loadDDL(jp, ddl)
  }


}

