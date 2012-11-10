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

/**
 * Simple structure to store a view of a chunk of bytes.
 *
 * @author kenl
 *
 */
class ByteFragment(private var _buf:Array[Byte]) extends Serializable {

  private val serialVersionUID = -3157516993124229948L
  private var _offsetPtr = 0
  private var _len = _buf.length


  /**
   * @param bf
   * @param offset
   * @param length
   */
  def this(bf:Array[Byte], offset:Int, length:Int) {
    this(bf)
    _offsetPtr= offset
    _len=length
  }

  /**
   * @return
   */
  def buf() = _buf


  /**
   * @return
   */
  def offset() = _offsetPtr


  /**
   * @return
   */
  def length() = _len

}
