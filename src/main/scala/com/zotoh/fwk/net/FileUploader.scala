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

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

import com.zotoh.fwk.util.LoggerFactory._
import com.zotoh.fwk.util.Logger
import com.zotoh.fwk.util.WWID._
import com.zotoh.fwk.io.IOUte._
import com.zotoh.fwk.io.XData
import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.util.Logger
import com.zotoh.fwk.util.StrUte._

import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.URI

import org.jboss.netty.handler.codec.http.HttpMessage


/**
 * @author kenl
 *
 */
class FileUploader {

  @transient private var _log=getLogger(classOf[FileUploader])
  def tlog() = _log

  private val _clientFnames= HashMap[File,String]()
  private val _fields= HashMap[String,String]()
  private val _files= ArrayBuffer[File]()
  private val _atts= ArrayBuffer[File]()
  private var _url=""

  /**
   * @param args
   * @throws Exception
   */
  def start(args:Array[String]) {
    if ( parseArgs(args)) {
      upload(new BasicHttpMsgIO() {
        def onOK(code:Int, r:String, res:XData) {
          println("Done: status=" + code)
        }
      })
    }
  }

  /**
   * @param name
   * @param value
   */
  def addField(name:String, value:String) ={
    if ( name != null) {
      _fields.put(name, nsb(value))
    }
    this
  }

  /**
   * @param path
   * @param clientFname
   * @throws IOException
   */
  def addAtt(path:File, clientFname:String):FileUploader = {
    addOneAtt( path, nsb(clientFname))
  }


  /**
   * @param path
   * @throws IOException
   */
  def addAtt(path:File):FileUploader = addAtt(path, "")

  /**
   * @param path
   * @param clientFname
   * @throws IOException
   */
  def addFile(path:File, clientFname:String):FileUploader = {
    addOneFile( path, nsb(clientFname))
  }

  /**
   * @param path
   * @throws IOException
   */
  def addFile(path:File):FileUploader = addFile(path, "")

  /**
   * @param url
   */
  def setUrl(url:String) = { _url= nsb(url); this }

  /**
   * @throws IOException
   */
  def send(cb:HttpMsgIO) { upload(cb) }

  private def upload(cb:HttpMsgIO) {

    tlog().debug("FileUploader: posting to url: {}" , _url)

    val t= preload()
    HttpUte.simplePOST(new URI(_url), XData(t._1), new WrappedHttpMsgIO(cb) {
      override def configMsg(m:HttpMessage) {
        super.configMsg(m)
        m.setHeader("content-type", t._2)
      }
    })
  }

  private def preload() = {
    val t= newTempFile(true)
    using(t._2) { (out) =>
      ( t._1, fmt(out) )
    }
  }

  private def fmt(out:OutputStream) = {
    val boundary = newWWID()

    // fields
    _fields.foreach { (t) =>
      writeOneField(boundary, t._1, t._2, out)
    }

    // files
    (1 /: _files) { (cnt, f) =>
      writeOneFile(boundary, "file."+cnt, f, "binary", out)
      cnt +1
    }

    // atts
    (1 /: _atts) { (cnt, f) =>
      writeOneFile(boundary, "att."+cnt, f, "binary", out)
      cnt +1
    }

    out.write( asBytes("--" + boundary + "--\r\n") )
    out.flush()

    "multipart/form-data; boundary=" + boundary
  }

  private def writeOneField(boundary:String, field:String, value:String, out:OutputStream) {

    out.write( asBytes("--" + boundary + "\r\n" +
      "Content-Disposition: form-data; " +
      "name=\"" + field +
      "\"\r\n" +
      "\r\n" +
      value + "\r\n"))
    out.flush()
  }

  private def writeOneFile(boundary:String, field:String,
      path:File, cte:String,  out:OutputStream) {

    val cfn = _clientFnames.get(path) match {
      case Some(s) => s
      case _ => ""
    }
    val fname=if (!isEmpty(cfn)) cfn else path.getName()
    val clen= path.length()

    out.write(asBytes("--" + boundary + "\r\n" +
    "Content-Disposition: form-data; " +
    "name=\"" + field + "\"; filename=\"" +
    fname + "\"\r\n" +
    "Content-Type: application/octet-stream\r\n" +
    "Content-Transfer-Encoding: " + cte + "\r\n" +
    "Content-Length: " + clen.toString + "\r\n" +
    "\r\n") )
    out.flush()

    using(new FileInputStream(path)) { (inp) =>
      copy(inp, out, clen)
    }

    out.write(asBytes("\r\n"))
    out.flush()

  }

  private def usage() =  {
    println("FileUpload url -p:a=b -p:c=d -f:f1 -f:f2 -a:a1 -a:a2 ...")
    println("e.g.")
    println("FileUpload http://localhost:8003/HelloWorld -p:a=b -f:/temp/a.txt -a:/temp/b.att")
    println("")
    false
  }

  private def parseArgs(av:Seq[String]):Boolean = {

    if (av.length < 2)    {   return usage()    }
    _url=av(0)

    for (i <- 1 until av.length) {
      val s= av(i)
      if (s.startsWith("-p:")) {
        val ss=s.substring(3).split("=")
        addField(ss(0),ss(1))
      }
      else
      if (s.startsWith("-f:")) {
        addFile(new File(s.substring(3)))
      }
      else
      if (s.startsWith("-a:")) {
        addAtt( new File(s.substring(3)))
      }
    }

    true
  }

  private def addOneFile(path:File, clientFname:String)  = {
    tstObjArg("file-url", path)
    _clientFnames += path -> clientFname
    _files += path
    this
  }

  private def addOneAtt(path:File, clientFname:String)  = {
    tstObjArg("file-url", path)
    _clientFnames += path -> clientFname
    _atts += path
    this
  }

}
