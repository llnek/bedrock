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

import java.io.{File,FileInputStream,IOException,InputStream}
import java.nio.charset.Charset
import com.zotoh.fwk.io.IOUte._
import com.zotoh.fwk.util.CoreTypes._
import com.zotoh.fwk.util.CoreUte._

object XStream {
  //def apply(f:File,del:Boolean=false) = new XStream(f,del)
}

/**
 * Wrapper on top of a File input stream such that it can
 * delete itself from the file system when necessary.
 *
 * @author kenl
 *
 */
case class XStream(protected var _fn:File, protected var _deleteFile:Boolean=false) extends InputStream {

  @transient private var _inp:InputStream = _
  protected var _closed = true
  private var pos = 0L

  /* (non-Javadoc)
   * @see java.io.InputStream#available()
   */
  override def available() = {
    pre()
    _inp.available()
  }

  /* (non-Javadoc)
   * @see java.io.InputStream#read()
   */
  override def read() = {
    pre()
    val r = _inp.read()
    pos += 1
    r
  }

  /* (non-Javadoc)
   * @see java.io.InputStream#read(byte[])
   */
  override def read(b:Array[Byte]) = {
    if (b==null) -1 else read(b, 0, b.length)
  }

  /* (non-Javadoc)
   * @see java.io.InputStream#read(byte[], int, int)
   */
  override def read(b:Array[Byte], offset:Int, len:Int) = {
    if (b==null) { -1 } else {
      pre()
      val r = _inp.read(b, offset, len)
      pos = if (r== -1 ) -1 else { pos + r }
      r
    }
  }

  /**
   * @param ch
   * @return
   */
  def readChars(cs:Charset, len:Int):(Array[Char], Int) = {
    val rc= if (len > 0) {
      val b = new Array[Byte](len)
      val c = read(b, 0, len)
      if (c>0) bytesToChars(b, c, cs) else ZCHAS
    } else {
      ZCHAS
    }
    (rc, rc.length)      
  }

  /* (non-Javadoc)
   * @see java.io.InputStream#skip(long)
   */
  override def skip(n:Long) = {
    if (n < 0L) { -1L } else {
      pre()
      val r= _inp.skip(n)
      if (r > 0L) { pos +=  r }
      r
    }
  }

  /* (non-Javadoc)
   * @see java.io.InputStream#close()
   */
  override def close() {
    IOUte.close(_inp)
    _inp= null
    _closed= true
  }

  /* (non-Javadoc)
   * @see java.io.InputStream#mark(int)
   */
  override def mark(readLimit:Int) {
    if (_inp != null) {
      _inp.mark(readLimit)
    }
  }

  /* (non-Javadoc)
   * @see java.io.InputStream#reset()
   */
  override def reset() {
    close()
    _inp= new FileInputStream(_fn)
    _closed=false
    pos=0
  }

  /* (non-Javadoc)
   * @see java.io.InputStream#markSupported()
   */
  override def markSupported() = true

  /**
   * @param dfile
   */
  def setDelete(dfile:Boolean) { _deleteFile = dfile  }

  /**
   *
   */
  def delete() = {
    close()
    if (_deleteFile && _fn != null) {
      _fn.delete()
    }
    this
  }

  /**
   * @return
   */
  def filename():Option[String] = {
    if (_fn != null) Some(niceFPath(_fn)) else None
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  override def toString() = {
    filename() match {
      case Some(s) => s
      case _ => ""
    }
  }

  /**
   * @return
   */
  def getPosition() =  pos

  /**/
  /* (non-Javadoc)
   * @see java.lang.Object#finalize()
   */
  override def finalize() {
    delete()
  }

  private def pre() {
    if (_closed) { ready() }
  }

  private def ready() {
    reset()
  }

}

