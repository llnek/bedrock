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

package com.zotoh.fwk.net

import com.zotoh.fwk.net.HttpUte._
import com.zotoh.fwk.net.NetUte._
import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.io.IOUte._

import java.io.{ByteArrayOutputStream=>BAOS,File}
import java.net.InetAddress
import java.net.Socket
import java.net.URI

import org.jboss.netty.handler.codec.http.HttpMessage

import com.zotoh.fwk.io.XData
import com.zotoh.fwk.util.{ByteUte,CoreImplicits,StrArr}

import org.scalatest.Assertions._
import org.scalatest._

class FwkNetJUT  extends FunSuite with BeforeAndAfterEach with BeforeAndAfterAll  {

  override def beforeAll(configMap: Map[String, Any]) {
  }

  override def afterAll(configMap: Map[String, Any]) {
  }

  override def beforeEach() { }

  override def afterEach() { }


  private val LPORT=9090
/*
  test("testGetEC2") {
    val url= "http://169.254.169.254/2008-09-01/meta-data/instance-id"
    val join= new Object()
    val t= simpleGET( new URI(url), new BasicHttpMsgIO(){
      def onOK(code:Int, r:String, out:XData) {
        println("Success: code="+code)
        println( out.toString())
        Thread.sleep(1500)
        join.synchronized { join.notify() }
      }
      override def onError(code:Int, r:String) {
        super.onError(code,r)
        Thread.sleep(1500)
        join.synchronized { join.notify() }
      }
    })
    join.synchronized { join.wait() }
  }
*/
  test("testUte") {

    var s= canonicalizeEmailAddress("DonaldDuck@ABC.cOm")
    expect("DonaldDuck@abc.com")(s)

    s= getHostPartUri("http://bing.com:80/search")
    expect("bing.com")(s)

    expect(80)(NetUte.getPort("http://bing.com:80/search"))

    val bits= ipv4AsBytes("192.168.220.250")
    val a= InetAddress.getByAddress(bits)
    s=a.getHostAddress()
    expect(s)( "192.168.220.250")

    val s1=System.getProperty("user.timezone")
    val s2="file://c:/abc/def/${user.timezone}/bbb/%user.timezone%/jjj"
    val s3="file://c:/abc/def/"+s1+"/bbb/"+s1+"/jjj";
    expect(NetUte.resolveAndExpandFileUrl(s2))( s3)
  }

  test("testFileUpload") {
    val tDir=niceFPath( tmpDir())
    val f1=mkTempFile()
    val f2=mkTempFile()
    val a1=mkTempFile()
    val a2=mkTempFile()

    writeFile(f1, "hello", "utf-8")
    writeFile(f2, "world", "utf-8")
    writeFile(a1, "goodbye", "utf-8")
    writeFile(a2, "joe", "utf-8")

    val ldr= new FileUploader()
    ldr.addField("fname", "Joe")
    ldr.addField("lname", "Bloggs")
    ldr.addFile(f1)
    ldr.addFile(f2, "f2")
    ldr.addAtt(a1)
    ldr.addAtt(a2, "a2")
    ldr.setUrl("http://localhost:"+LPORT+"/takethis")

    val m= new MemHttpServer(tDir, "localhost", LPORT)
    val baos= new BAOS()
    val join=new Object()
    m.bind(new BasicHttpMsgIO() {
      def onOK(code:Int, r:String, data:XData) {
        block { () =>
          Thread.sleep(1500)
          baos.write(data.bytes() )
          join.synchronized { join.notify() }
        }
      }
    }).start(false)

    Thread.sleep(5000) // allow memserver to stablize

    ldr.send(null)
    join.synchronized { join.wait() }
    m.stop()


    val data= asStr(baos)
    f1.delete()
    f2.delete()
    a1.delete()
    a2.delete()

    assert(data.has("hello") )
    assert(data.has("world") )
    assert(data.has("goodbye") )
    assert(data.has("joe") )
    assert(data.has("fname") )
    assert(data.has("lname") )

    Thread.sleep(5000)  // ensure port tear down
  }

  test("testFileServer") {
    val f= mkTempFile()
    writeFile(f, "hello world", "utf-8")

    val svr=new SimpleFileServer("localhost", LPORT)
    var s=""
    svr.start()
    Thread.sleep(5000)  // ensure server stablize

    val soc= new Socket("localhost", LPORT)
    soc.getOutputStream().write(  ("rcp " + f.getCanonicalPath() + "\r\n").getBytes("utf-8") )
    soc.getOutputStream().flush()
    Thread.sleep(1500)

    var bits=new Array[Byte](8)
    soc.getInputStream().read(bits)

    val clen=ByteUte.readAsLong(bits)
    if (clen > 0L) {
      bits= new Array[Byte]( clen.toInt )
      soc.getInputStream().read(bits)
      s= asString(bits)
    }

    soc.getOutputStream().write(  ("rrm " + f.getCanonicalPath() + "\r\n").getBytes("utf-8") )
    soc.getOutputStream().flush()
    Thread.sleep(1500)
    assert(!f.exists())

    soc.getOutputStream().write(  "stop\r\n".getBytes("utf-8") )
    soc.close()
    svr.stop()

    expect("hello world")(s)

    Thread.sleep(5000) // ensure port tear down
  }

  test("testHttpSend") {
    val tDir= niceFPath(tmpDir())
    val join= new Object()
    val baos= new BAOS()
    val m= new MemHttpServer(tDir, "localhost", LPORT)
    m.bind(new BasicHttpMsgIO(){
      override def onPreamble(mtd:String, uri:String, hds:Map[String,StrArr]) {
        block { () => baos.write(uri.getBytes()) }
      }
      def onOK(code:Int, r:String, data:XData) {
        block { () => baos.write(data.bytes()) }
      }
    }).start(false)

    Thread.sleep(5000)  // wait for server to stablize

    simpleGET(new URI("http://localhost:"+LPORT+"/dosomething?a=b&c=e"), new BasicHttpMsgIO() {
        def onOK(code:Int, r:String, res:XData) {
            Thread.sleep(1500)
            join.synchronized { join.notify() }
        }
        override def configMsg(m:HttpMessage) {
          m.setHeader("content-transfer-encoding", "base64")
        }
    })

    join.synchronized { join.wait() }

    var s= asStr(baos)
    //System.out.println("-->" + s)
    assert( s.has("a=b") )

    baos.reset()
    simpleGET(false, "localhost", LPORT, "/dosomething", "x=y&j=k",
      new BasicHttpMsgIO() {
        def onOK(code:Int, r:String, res:XData) {
            Thread.sleep(1500)
            join.synchronized { join.notify() }
        }
    })

    join.synchronized { join.wait() }

    assert( asStr(baos).has("x=y") )

    val in= new XData("hello world")
    baos.reset()

    simplePOST(new URI("http://localhost:"+LPORT+"/hereyougo"), in,
        new BasicHttpMsgIO(){
      override def configMsg(m:HttpMessage) { m.setHeader("content-type", "text/plain") }
      def onOK(code:Int, r:String, res:XData) {
        Thread.sleep(1500)
        join.synchronized { join.notify() }
      }
    })

    join.synchronized { join.wait() }

    assert( asStr(baos).has("hello") )
    baos.reset()

    simplePOST(false, "localhost", LPORT, "/hereyougo", in,
        new BasicHttpMsgIO(){
      override def configMsg(m:HttpMessage) { m.setHeader("content-type", "text/plain");}
      def onOK(code:Int, r:String, res:XData) {
        Thread.sleep(1500)
        join.synchronized { join.notify() }
      }
    })

    join.synchronized { join.wait() }
    assert( asStr(baos).has("hello") )

    baos.reset()
    simplePOST(false, "localhost", LPORT, "/hereyougo", in,
        new BasicHttpMsgIO(){
      override def configMsg(m:HttpMessage) { m.setHeader("content-type", "text/plain")
        m.setHeader("content-transfer-encoding", "binary")
      }
      def onOK(code:Int, r:String, res:XData) {
        Thread.sleep(1500)
        join.synchronized { join.notify() }
      }
    })
    join.synchronized { join.wait() }
    assert( asStr(baos).has("world") )

    m.stop()

    Thread.sleep(5000)  // wait for port tear down
  }

}
