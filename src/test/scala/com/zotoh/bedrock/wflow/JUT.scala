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
//import org.junit.runner.RunWith
//import org.scalatest.junit.JUnitRunner

//@RunWith(classOf[JUnitRunner])
class FlowJUT () extends FunSuite
with BeforeAndAfterEach with BeforeAndAfterAll with Vars {

  private var _p12File:File = null
  private var _appDir:File= null
  var _eng:AppEngine=null
  var _testOut:Any=null

  private val _lock=new Object()

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

  test("wwww") {
    val ps=create_props("demo.pop3.POP3ServerFlow")
    ps.put("_", this)
    _eng.start(ps)
  }

  test("test1") {
    /*
    val ps=create_props("com.zotoh.bedrock.wflow.Test1")
    ps.put("_", this)
    _eng.start(ps)
    //safeThreadWait(3000)
    assert(_testOut == "helloworld")
    */
  }






  def create_props(fw:String) = {
    val t= new File(new File(_appDir,CFG), "app.conf")
    val s= deviceBlock(fw)
    IOUte.writeFile(t,s,"utf-8")
    val props= new JPS().
    add("bedrock.manifest.file", niceFPath(t)).
    //add("bedrock.shutdown.port.password","stopengine").
    //add("bedrock.shutdown.port","7051").
    //add("bedrock.delegate.class", "com.zotoh.bedrock.wflow.FlowDelegate").
    add(APP_DIR, niceFPath(_appDir))
    props
  }

  def deviceBlock(fw:String) = {
      "{ devices : {" +
        "topic : {" +
        "provider: \"com.zotoh.bedrock.mock.mail.MockPop3Store\"," +
      "processor:\"" + fw + "\"," +
    "type: \"pop3\"," +
    "host: \"lt-mintos\"," +
    "port: 7110," +
    "ssl: false," +
    "user: \"test1\"," +
    "pwd: \"secret\"," +
    "intervalsecs:5}"  +
      "}}"
  }
  def ___deviceBlock(fw:String) = {
    "{ devices : {" +
      "h1 : {" +
      "processor:\"" + "demo.socket.SockClientFlow" + "\"," +
      "type:\"oneshot-timer\"," +
      "intervalsecs:5},"  +
      "server : {" +
      "processor:\"" + "demo.socket.SockServerFlow" + "\"," +
      "type:\"tcp\"," +
      "port:5558 }" +
      "}}"
  }

  def __deviceBlock(fw:String) = {
    "{ devices : {" +
      "h1 : {" +
      "processor:\"" + fw + "\"," +
      "type:\"oneshot-timer\"," +
      "delaysecs:1}"  +
      "h1 : {" +
      "processor:\"" + fw + "\"," +
      "type:\"oneshot-timer\"," +
      "delaysecs:1 }" +
      "}}"
  }

  def pause() {
    _lock.synchronized {
      block { () => _lock.wait() }
    }
  }

  def wake() {
    _lock.synchronized {
      block { () => _lock.notify() }
    }
  }


}


class Test1(j:Job) extends Workflow(j) {

  private val w1= new Work() {
    def eval(j:Job) {
      j.setSlot("lhs", "hello")
    }
  }

  private val w2= new Work() {
      def eval(j:Job) {
        j.setSlot("rhs", "world")
      }
  }

  private val end= new Work() {
      def eval(j:Job) {
        val s= nsb(j.slot("lhs").get) + nsb( j.slot("rhs").get )
        curStep().workflow().engine().quirks().get("_") match {
          case u:FlowJUT =>
            u._testOut=s
            u._eng.shutdown()
          case _ =>
        }
      }
  }

  def onStart() = new Block(new PTask(w1)).
        chain( new PTask(w2)).chain(new PTask(end))

}

