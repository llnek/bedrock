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

package com.zotoh.fwk.db

import scala.collection.mutable.ArrayBuffer

import scala.math._


import com.zotoh.fwk.util.LoggerFactory._
import com.zotoh.fwk.util.{CoreImplicits, Logger}
import com.zotoh.fwk.util.StrUte._

/**
 * Abstract a SQL statement.
 *
 * @author kenl
 *
 */
abstract class SQLStmt protected(var sql:String="") extends CoreImplicits {
  protected val _values= ArrayBuffer[Any]()
  private var _skipCache=false
  private var _tbl=""
  private var _sql=""

  setSQL(sql)

  @transient private var _log= getLogger(classOf[SQLStmt])
  def tlog() = _log

  /**
   * @param b
   */
  def setDirect(b:Boolean) { _skipCache= b  }

  /**
   * @return
   */
  def isDirect() =  _skipCache

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  override def toString() =  _sql

  /**
   * @param pms
   */
  def setParams( pms:Seq[Any]) = {
    _values.clear()
    addParams(pms)
    this
  }

  /**
   * @return
   */
  def params() = _values.toArray

  /**
   * @param sql
   * @param params
   */
  protected def this(sql:StringBuilder, pms:Seq[Any]) {
    this(sql.toString())
    setParams(pms)
  }

  /**
   * @param sql
   * @param params
   */
  protected def this(sql:String, pms:Seq[Any]) {
    this(sql)
    setParams(pms)
  }

  /**
   * @param sql
   */
  protected def setSQL(sql:String) {
    _sql= nsb(sql)
    table()
  }

  /**
   * @param pms
   */
  def addParams(pms:Seq[Any]) = {
    pms.foreach{ (a) => _values += a }
    this
  }

  /**
   * @param row
   */
  protected def dbgData(row:DbRow) {
    if ( ! tlog().isDebugEnabled()) { } else {
      val msg=new StringBuilder().append("SQLStmt: DbRow: ###############################################\n")
      row.values().foreach { (t) =>
        msg.append("fld= ").append(t._1).append(",value= ").append(t._2).append("\n")
      }
      msg.append("###############################################")
      tlog().debug(msg.toString())
    }
  }

  /**
   * @param table
   */
  protected def setTable(table:String) { _tbl= nsb(table) }

  /**
   * @return
   */
  def table() = {
    if ( isEmpty(_tbl)) {
      val sql= toString()
      var s= sql.lc
      var pos= s.indexOf("from")
      if (pos > 0) {
        s= sql.substring(pos+4).trim()
        val b= s.indexOf('\t')
        val a= s.indexOf(' ')
        if (b < 0) { pos = a }
        else
        if (a < 0) { pos = b }
        else {
          pos= min(a, b)
        }
      }
      if (pos > 0) {
        _tbl= s.substring(0,pos)
      }
    }
    _tbl
  }

}

