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

import scala.collection.mutable.HashMap

import com.zotoh.fwk.util.{Logger,StrUte,CoreImplicits}
import com.zotoh.fwk.util.LoggerFactory._
import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.util.StrUte._

object DbRow {}

/**
 * Wrapper for a row of SQL table data.
 *
 * @author kenl
 *
 */
case class DbRow(private var _tbl:String="") extends DBVars with CoreImplicits {

  private val serialVersionUID = -1112175967176488069L
  private val _map=HashMap[String,Any]()

  private def ilog() = { _log= getLogger(classOf[DbRow]) }
  @transient private var _log:Logger=null
  def tlog() = { if (_log==null) ilog(); _log }

  _tbl= nsb(_tbl)

  /**
   * Add col & data to the row.
   *
   * @param  nameVals
   */
  def add(colVals:Map[String,Any]):DbRow = {
    colVals.foreach { (t) => add( t._1, t._2) }
    this
  }

  /**
   * Add a column & value.
   *
   * @param col
   * @param value
   */
  def add(col:String, value:Any):DbRow = {
    if (! StrUte.isEmpty(col)) {
      _map += Tuple2(col.uc, nilToNichts(value))
    }
    this
  }

  /**
   * Add a column with NULL value.
   *
   * @param col
   */
  def add(col:String):DbRow =  add(col, null) 

  /**
   * @return
   */
  def isEmpty() = _map.size == 0

  /**
   *
   * @param col
   * @return
   */
  def remove(col:String) = {
    if (col==null) None else _map.remove(col.uc)
  }

  /**
   * @param col
   * @return
   */
  def exists(col:String) = {
    if (col==null) false else _map.isDefinedAt( col.uc)
  }

  /**
   * @return Table name.
   */
  def sqlTable() = _tbl

  /**
   *
   */
  def clear() = { _map.clear() ; this }

  /**
   * @return immutable map
   */
  def values() = _map.toMap

  /**
   * Get value of column, if column is DB-NULL, returns null.
   *
   * @param col
   * @return
   */
  def get(col:String) = {
    if (col==null) None else _map.get(col.uc)
  }

  def dbg() {
    if ( ! tlog().isDebugEnabled())  { return }
    val bf = new StringBuilder(1024)
    _map.foreach { (t) =>
      if (bf.length() > 0) { bf.append("\n") }
      bf.append( t._1 ).
      append("=\"").
      append(nsb( t._2 )).
      append( "\"" )
    }
    tlog().debug(bf.toString )
  }

  /**
   * Constructor.
   *
   * @param bagOfNameValues columns & values.
   */
  def this(bagOfNameValues:Map[String,Any]) {
    this()
    add(bagOfNameValues)
  }

  /**
   * @return
   */
  def size() = _map.size

}

