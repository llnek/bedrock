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

package com.zotoh.bedrock.wflow

import java.util.{Properties=>JPS}
import java.io.File

import org.apache.commons.io.FileUtils

import com.zotoh.fwk.util.ProcessUte._
import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.util.StrUte._
import com.zotoh.fwk.io.IOUte
import com.zotoh.bedrock.core._
import com.zotoh.bedrock.wflow._

import org.scalatest.Assertions._
import org.scalatest._


abstract class TestBase () extends FunSuite
with BeforeAndAfterEach with BeforeAndAfterAll with Vars {

  private var _p12File:File = null
  private var _appDir:File= null
  var _eng:AppEngine=null
  var _testOut:Any = null

  override def beforeAll(configMap: Map[String, Any]) {
    System.setProperty(PIPLINE_MODULE, "com.zotoh.bedrock.wflow.FlowModule")
    _appDir=genTmpDir()
    new File(_appDir, LOGS).mkdirs()
    new File(_appDir, CFG).mkdirs()
  }

  override def afterAll(configMap: Map[String, Any]) {
    if ( _p12File != null) _p12File.delete()
    if (_appDir != null) block { () =>
      FileUtils.cleanDirectory(_appDir)
      FileUtils.deleteQuietly(_appDir)
    }
  }

  override def beforeEach() {
    _eng= Module.pipelineModule() match {
      case Some(m) => new AppEngine(m)
      case _ => null
    }
  }

  override def afterEach() {
    _eng.shutdown()
  }

  def create_props(fw:String) = {
    val t= new File(new File(_appDir,CFG), "app.conf")
    val s= deviceBlock(fw)
    IOUte.writeFile(t,s,"utf-8")
    val props= new JPS().
    add("bedrock.manifest.file", niceFPath(t)).
//    add("bedrock.shutdown.port.password","stopengine").
//    add("bedrock.shutdown.port","7051").
    add(APP_DIR, niceFPath(_appDir))
    props
  }

  def deviceBlock(fw:String) = {
      "{ devices : {" +
                      "h1 : {" +
                      "processor:\"" + fw + "\"," +
                      "type:\"oneshot-timer\"," +
                      "delaysecs:1" +
      "}}}"
  }

}



