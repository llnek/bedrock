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

package com.zotoh.fwk.io

import scala.math._

import com.zotoh.fwk.util.LoggerFactory._
import com.zotoh.fwk.util.{Logger}
import com.zotoh.fwk.util.ByteUte._
import com.zotoh.fwk.util.StrUte._
import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.io.XData._

import java.io.{ByteArrayInputStream=>BAIS,ByteArrayOutputStream=>BAOS,DataInputStream}
import java.io.{File,FileInputStream,FileOutputStream,CharArrayWriter,OutputStreamWriter}
import java.io.{InputStream,InputStreamReader,OutputStream,Reader,Writer}
import java.util.zip.{GZIPInputStream, GZIPOutputStream}

import org.apache.commons.codec.binary.Base64
import org.xml.sax.InputSource

import java.nio.charset.Charset

/**
 * Util functions related to stream/io.
 *
 * @author kenl
 *
 */
object IOUte {

  private var READ_STREAM_LIMIT=1024*1024*8 // if > 8M switch to file
  
  private val _log= getLogger(classOf[IOUte])
  def tlog() = _log

  def setStreamLimit(n:Int) {
    READ_STREAM_LIMIT=n
  }
  
  /**
   * @param fn
   * @return
   */
  def read(fn:File) = {
    using(new FileInputStream(fn)) { (inp) => bytes(inp) }
  }

  /**
   * @param s
   * @param enc
   * @return
   */
  def gzip(s:String, enc:String="utf-8"):Array[Byte] = {
    if (s==null) null else gzip( s.getBytes(enc))
  }

  /**
   * Calls InputStream.reset().
   *
   * @param inp
   */
  def safeReset(inp:InputStream) {
    try { if (inp != null) inp.reset() } catch {case _ =>}
  }

  /**
   * @param bits
   * @return
   */
  def gzip(bits:Array[Byte]):Array[Byte] = {
    using(new BAOS(4096)) { (baos) =>
      if (bits!=null) using(new GZIPOutputStream(baos)) { (g) =>
          g.write(bits, 0, bits.length)
      }
      baos.toByteArray()
    }
  }

  /**
   * @param bits
   * @return
   */
  def gunzip(bits:Array[Byte]) = {
    if ( bits==null) null
            else bytes(new GZIPInputStream( asStream(bits)))
  }

  /**
   * @param bits
   * @return
   */
  def asStream(bits:Array[Byte]):InputStream = {
    if (bits==null) null else new BAIS(bits)
  }

  /**
   * @param ins
   * @return
   */
  def gunzip(ins:InputStream):InputStream = {
    if (ins==null) null else new GZIPInputStream(ins)
  }

  /**
   * @param ins
   * @return
   */
  def bytes(ins:InputStream) = {
    using( new BAOS(4096)) { (baos) =>
      val cb= new Array[Byte](4096)
      var n=0
      do {
        n = ins.read(cb)
        if (n>0) { baos.write(cb, 0, n) }
      } while (n>0)
      baos.toByteArray()
    }
  }

  /**
   * @param fp
   * @return
   */
  def open(fp:File):InputStream = {
    if ( fp==null ) null else XStream(fp)
  }

  /**
   * @param gzb64
   * @return
   */
  def fromGZipedB64(gzb64:String) = {
    if (gzb64==null) null else gunzip(Base64.decodeBase64(gzb64))
  }

  /**
   * @param bits
   * @return
   */
  def toGZipedB64(bits:Array[Byte])= {
    if (bits == null) null else Base64.encodeBase64String( gzip(bits))
  }

  /**
   * @param inp
   * @return
   */
  def available(inp:InputStream) = {
    if (inp==null) 0 else inp.available()
  }

  /**
   * @param inp
   * @param useFile if true always use file-backed data.
   * @return
   */
  def readBytes(inp:InputStream, useFile:Boolean=false) = {
    var lmt= if (useFile) 1 else READ_STREAM_LIMIT
    val bits= new Array[Byte](4096)
    val baos= new BAOS(10000)
    var os:OutputStream= baos
    val rc= XData()
    var cnt=0
    var c=0

    try {
      do {
        c= inp.read(bits)
        if (c > 0) {
          os.write(bits, 0, c)
          cnt += c
          if ( lmt > 0 && cnt > lmt) {
            os=swap(baos, rc)
            lmt= -1
          }
        }
      } while (c>0)

      if (!rc.isDiskFile() && cnt > 0) {
        rc.resetMsgContent(baos)
      }

    } finally {
      close(os)
    }

    rc
  }

  /**
   * @param rdr
   * @param useFile if true always use file-backed data.
   * @return
   */
  def readChars(rdr:Reader, useFile:Boolean=false) = {
    var lmt = if (useFile) 1  else READ_STREAM_LIMIT
    val wtr= new CharArrayWriter(10000)
    val bits= new Array[Char](4096)
    var w:Writer=wtr
    val rc= XData()
    var cnt=0
    var c=0

    try {
      do {
        c=rdr.read(bits)
        if (c > 0) {
          w.write(bits, 0, c)
          cnt += c
          if ( lmt > 0 && cnt > lmt) {
            w=swap(wtr, rc)
            lmt= -1
          }
        }
      } while( c>0)

      if (!rc.isDiskFile() && cnt > 0) {
        rc.resetMsgContent(wtr.toString())
      }
    }
    finally {
      close(w)
    }

    rc
  }

  /**
   * @param out
   * @param s
   * @param enc
   */
  def writeFile(out:File, s:String, enc:String="utf-8") {
    if (s != null) { writeFile( out, s.getBytes( enc)) }
  }

  /**
   * @param out
   * @param bits
   */
  def writeFile(out:File, bits:Array[Byte]) {
    if (bits != null) {
      using(new FileOutputStream(out)) { (os) =>
        os.write(bits)
      }
    }
  }

  /**
   * @param src
   * @param out
   */
  def copy(src:InputStream, out:OutputStream) {
    val bits = new Array[Byte](4096)
    var c=0
    do {
      c=src.read(bits)
      if (c > 0) { out.write(bits, 0, c); out.flush() }
    } while (c>0)
  }

  /**
   * @param src
   * @return
   */
  def copy(src:InputStream):File = {
    var t=newTempFile(true)
    using(t._2) { (os) =>
      copy(src, os)
    }
    t._1
  }

  /**
   * @param src
   * @param out
   * @param bytesToCopy
   */
  def copy(src:InputStream, out:OutputStream, bytesToCopy:Long) {
    val dis = new DataInputStream(src)
    val buff = new Array[Byte](4096)
    var cl= bytesToCopy
    var c=0
    while (cl > 0L) {
      c=dis.read(buff,0, min(4096L, cl).toInt )
      if (c > 0) {
        cl -= c
        out.write(buff,0,c)
      } else if (c < 0) {
        throw new RuntimeException("Premature EOF, expecting more bytes to be copied.")
      }
    }
    out.flush()
  }

  /**
   * @param iso
   */
  def resetInputSource(iso:InputSource) {
    if (iso != null) {
      val rdr= iso.getCharacterStream()
      val ism = iso.getByteStream()
      try { if (ism != null) ism.reset() } catch {case _ =>}
      try { if (rdr != null) rdr.reset() } catch {case _ =>}
    }
  }

  /**
   * @param pfx
   * @param sux
   * @return
   */
  def mkTempFile(pfx:String="", sux:String="") = {
    File.createTempFile(
      if (isEmpty(pfx)) "temp-" else pfx,
      if (isEmpty(sux)) ".dat" else sux,
      workDir())
  }

  /**
   * @return
   */
  def mkFSData() = XData(mkTempFile()).setDeleteFile(true)

  /**
   * @param open
   * @return
   */
  def newTempFile(open:Boolean=false) = {
    val f= mkTempFile()
    (f, if (open) new FileOutputStream(f) else null)
  }

  /**
   * @param r
   */
  def close(r:Reader) {
    using(r) { (r) => }
  }

  /**
   * @param o
   */
  def close(o:OutputStream) {
    using(o) { (o) => }
  }

  /**
   * @param w
   */
  def close(w:Writer) {
    using(w) { (w) => }
  }

  /**
   * @param i
   */
  def close(i:InputStream) {
    using(i) { (i) => }
  }

  /**
   * @param b Array[Byte] to be converted.
   * @param count Number of Array[Byte] to read.
   * @param cs Charset.
   * @return Converted char array.
   */
  def bytesToChars(b:Array[Byte], count:Int, cs:Charset) = {
    val bb= if (count != b.length) {
      b.slice(0, min(b.length, count))
    } else { b }
    convertBytesToChars(bb,cs)
    
//    (1 to min(b.length, count)).foreach { (i) =>
//      val b1 = b(i-1)
//      ch(i-1) = (if (b1 < 0) { 256 + b1 } else b1 ).asInstanceOf[Char]
//    }
//    ch
  }
  
  /**
   * Tests if both streams are the same or different at byte level.
   *
   * @param s1
   * @param s2
   * @return
   */
  def different(s1:InputStream, s2:InputStream):Boolean = {
    if (s1 != null && s2 != null && s2.available() == s1.available()) {
      var b2= new Array[Byte](4096)
      var b1= new Array[Byte](4096)
      var loop=true
      var c2= 0
      var c1= 0
      while (loop) {
        c2= s2.read(b2)
        c1= s1.read(b1)
        loop=false
        if (c2==c1) {
          if (c1 <= 0) { return false }
          loop = b2.sameElements(b1)
        }
      }
    }
    if (s1==null && s2==null) false else true
  }

  def readText(fn:File, enc:String="utf-8") = {
    val sb= new StringBuilder(4096)
    val cs= new Array[Char](4096)
    var n=0
    using(new InputStreamReader(open(fn), enc)) { (rdr) =>
      do {
        n=rdr.read(cs)
        if (n > 0) sb.appendAll(cs, 0, n)
      } while (n>0)
    }

    sb.toString
  }

  private def swap(baos:BAOS, data:XData):OutputStream = {
    val bits=baos.toByteArray()
    val t= newTempFile(true)
    if ( !isNilSeq(bits)) {
      t._2.write(bits)
      t._2.flush()
    }
    baos.close()
    data.resetMsgContent(t._1)
    t._2
  }

  private def swap(wtr:CharArrayWriter, data:XData) = {
    val t= newTempFile(true)
    val w= new OutputStreamWriter(t._2)
    data.resetMsgContent(t._1)
    w.write( wtr.toCharArray())
    w.flush()
    w
  }

}

sealed class IOUte {}

