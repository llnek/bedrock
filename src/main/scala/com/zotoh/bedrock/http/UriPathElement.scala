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

import scala.collection.mutable.HashMap

import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.util.StrUte._
import com.zotoh.fwk.util.StrArr

/**
 * A path within a HTTP request uri.
 *
 * @author kenl
 */
case class UriPathElement(private var _path:String) {

  private val _matrixParams= HashMap[String,StrArr]()
  private val _queryParams= HashMap[String,StrArr]()

  _path=trim(_path)
  if ( !_path.startsWith("/")) { _path= "/" + _path }

  /**
   * @param name
   * @return
   */
  def matrixParam(name:String) = {
    if ( name==null) None else _matrixParams.get(name)
  }

  /**
   * @return
   */
  def path() =  _path

  /**
   * @param name
   * @param value
   */
  def addMatrixParam(name:String, v:String) {
    if ( ! _matrixParams.isDefinedAt(name)) {
      _matrixParams += Tuple2(name, StrArr())
    }
    _matrixParams(name).add( nsb(v))
  }

  /**
   * @param name
   * @param value
   */
  def addQueryParam(name:String, v:String) {
    if ( ! _queryParams.isDefinedAt(name)) {
      _queryParams += Tuple2(name, StrArr())
    }
    _queryParams(name).add( nsb(v))
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  override def toString() = {
    val b1= new StringBuilder(512)
    _queryParams.foreach {(t) =>
      addAndDelim(b1, "&", t._1 + "=" + t._2.toString())
    }

    val b2= new StringBuilder(512)
    _matrixParams.foreach { (t) =>
      addAndDelim(b2, ";", t._1 + "=" + t._2.toString())
    }

    _path + (if(_queryParams.size > 0) "?" else "") + b1 +
        (if(_matrixParams.size > 0) ";" else "") + b2
  }

}
