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

import java.io.IOException

/**
 * Simple extension on top of <i>ByteArrayInputStream</i> to provide
 * users the abilities to access a range of byte[] from the
 * stream.
 *
 * @author kenl
 *
 */
class ByteFragmentInputStream(buf:Array[Byte], offset:Int, len:Int) extends java.io.ByteArrayInputStream(buf,offset,len) {

  /**
   * @param bp
   */
  def this(bp:ByteFragment)  {
    this(bp.buf(), bp.offset(), bp.length())
  }

  /**
   * @param buf
   */
  def this(buf:Array[Byte])  {
    this(buf,0,buf.length)
  }


  /**
   * @param roffset
   * @param len
   * @return
   * @throws IOException
   */
  def getFrag(roffset:Int, len:Int):ByteFragment = {
    if (len > (count - roffset)) {
      throw new IOException("Not enough data in buffer")
    }
    new ByteFragment(buf, roffset, len)
  }

  /**
   * @param len
   * @return
   * @throws IOException
   */
  def getFrag(len:Int):ByteFragment  = {
    if (len > (count - pos)) {
      throw new IOException("Not enough data in buffer")
    }
    new ByteFragment(buf, pos, len)
  }


  /**
   * @return
   */
  def getFrag():ByteFragment  = new ByteFragment(buf, pos, count - pos)

  /**
   * @return
   */
  def getPos() = pos

  /**
   * @return
   */
  def getBuf()  = buf

}

