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

import com.zotoh.fwk.util.CoreUte._
import java.io.IOException
import java.io.InputStream

/**
 * Counts an exact number of bytes from the stream.
 *
 * @author kenl
 *
 */
class BoundedInputStream(inp:InputStream) extends java.io.FilterInputStream(inp) {

  private var _count:Int = 0

  /**
   * @param inp
   * @param bytesToRead
   */
  def this(inp:InputStream, bytesToRead:Int)  {
    this(inp)
    _count = bytesToRead
  }

  /* (non-Javadoc)
   * @see java.io.FilterInputStream#read()
   */
  override def read():Int = {

    if (_count <= 0) -1 else {

      _count -= 1

      val c= super.read()
      if (c < 0)  { throw new IOException("No more data to read") }

      c
    }
  }


  /* (non-Javadoc)
   * @see java.io.FilterInputStream#read(byte[])
   */
  override def read(b:Array[Byte]):Int = read(b, 0, if(b==null) 0 else b.length )

  /* (non-Javadoc)
   * @see java.io.FilterInputStream#read(byte[], int, int)
   */
  override
  def read(b:Array[Byte], off:Int, len:Int):Int = {

    tstObjArg("input-bytes", b)

    if (_count <= 0 || b==null) -1 else {

      //val wd = if (_count <  len)  _count  else len
      val wd = min(_count,len)
      val c= in.read(b, off, wd)
      if (c < 0)  { throw new IOException("No more data to read") }
      _count -= c
      c
    }
  }

  /**
   * @return
   */
  def hasMore() = _count > 0

}
