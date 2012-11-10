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

package com.zotoh.bedrock.http

import scala.collection.mutable.ArrayBuffer

/**
 * Breaks up a http request uri into path fragments.
 *
 * @author kenl
 */
case class UriPathChain () {

  private val _chain= ArrayBuffer[UriPathElement]()

  /**
   * @param p
   */
  def add(p:UriPathElement) {
    if (p != null) {
      _chain += p
    }
  }

  /**
   * @return
   */
  def elements() = _chain.toArray

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  override def toString() = {
    var b= new StringBuilder(512)
    _chain.foldLeft(b) { (b,e) =>
      b.append(e)
    }
    b.toString
  }

}
