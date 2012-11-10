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

import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.util.StrUte._

/**
 * Helper functions to parse a Uri path component.
 *
 * @author kenl
 */
object UriUte {

  /**
   * @param uri
   * @return
   */
  def toPathChain(original:String) = {
    val chain= UriPathChain()
    var uri=original
    var pos=0
    do {
      pos=uri.indexOf('/')
      if (pos >= 0) {
        val p= uri.substring(0,pos)
        if (!isEmpty(p)) {
          chain.add( toInnerPath(p))
        }
        uri=uri.substring(pos+1)
      }
    }
    while (pos >= 0)

    if (uri.length() > 0) {
      chain.add( toOnePath(uri))
    }

    chain
  }

  private def toOnePath(path:String) = {
    if (path.indexOf('?') > 0) {
      toQueryPath(path)
    } else {
      toInnerPath(path)
    }
  }

  private def toQueryPath(original:String) = {
    var pos=original.indexOf('?')
    val pe= UriPathElement(original.substring(0,pos))
    var path=original.substring(pos+1)
    val ps= path.split("&")
    if (ps != null) ps.foreach { (ss) =>
      val cs=ss.split(";")
      var p=cs(0)
      var pm=""
      var r=""
      var l=""
      pos= p.indexOf('=')
      if (pos > 0) {
        pm= p.substring(0,pos)
        r= p.substring(pos+1)
        pe.addQueryParam(pm, r)
      }
      if (cs != null) cs.foreach { (p) =>
        pos= p.indexOf('=')
        if (pos > 0) {
          l= p.substring(0,pos)
          r= p.substring(pos+1)
          pe.addMatrixParam(l, r)
        }
      }
    }

    pe
  }

  private def toInnerPath(path:String) = {
    val ps=path.split(";")
    val pe:UriPathElement = if (isNilSeq(ps)) null else {
      new UriPathElement(ps(0))
    }
    if (ps != null) ps.foreach { (p) =>
      var pos= p.indexOf('=')
      if (pos > 0) {
        val l= p.substring(0,pos)
        val r= p.substring(pos+1)
        pe.addMatrixParam(l, r)
      }
    }
    pe
  }

  private def main(args:Array[String]) = {
    try {
      println(
        toPathChain("/a;m=8/b;x=1;y=2;z=/?k=0;js=46456"))
    } catch {
      case t => t.printStackTrace()
    }
  }

}
