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

import java.sql.{Connection=>JConn,Statement,SQLException}

import org.apache.commons.dbcp.{PoolableConnection=>PConn}
import org.apache.commons.pool.ObjectPool

import com.zotoh.fwk.util.LoggerFactory._
import com.zotoh.fwk.util.{CoreImplicits,Logger}
import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.util.StrUte._
import com.zotoh.fwk.db.DBVendor._
import com.zotoh.fwk.db.DBUte._

object JDBCPool {}

/**
 * Pool management of jdbc connections.
 *
 * @author kenl
 *
 */
sealed case class JDBCPool(
    private var _vendor:DBVendor, private var _info:JDBCInfo,
    private var _pool:ObjectPool[PConn]) extends DBVars with CoreImplicits {
  @transient private val _log= getLogger(classOf[JDBCPool])
  def tlog() = _log

  /**
   * @param v
   * @param p
   */
  def this(p:ObjectPool[PConn]) {
    this(NOIDEA, new JDBCInfo(), p)
  }


  /**
   * @return
   */
  def info() = _info

  /**
   * @return
   */
  def newJdbc() = new JDBC(this) 

  /**
   *
   */
  def finz() {
    this.synchronized {
      forceCloseAll()
      try { _pool.clear() } catch { case _ => }
      try { _pool.close() } catch { case _ => }
    }
  }

  /**
   *
   */
  def clear() {
    this.synchronized { forceCloseAll() }
  }

  /**
   * @return
   */
  def nextFree() = {
    val jc= next()
    tlog().debug("JDBCPool: Got a free jdbc connection from pool")
    jc
  }

  /**
   * @return
   */
  def varCharMaxWidth() = VARCHAR_WIDTH

  /**
   * @return
   */
  def retries() = 2

  /**
   * @return
   */
  def vendor() = _vendor

  /**
   * @param c
   */
  def returnUsed(c:JConnection) { returnUsed(c, true) }

  /**
   * @param c
   * @param reuse
   */
  def returnUsed( c:JConnection, reuse:Boolean) {
    if (c != null) { c.cancelQuietly() }
    var obj:PConn=null
    if (reuse && c != null && !c.isDead()) {
      obj = if (c==null) null else c.getInternal()
      if (obj != null) {
        tlog().debug("JDBCPool: Returning a used jdbc connection to pool")
        try { _pool.returnObject(obj) } catch { case _ => }
      }
    }
    else if (c != null) {
      c.die()
      obj = if (c==null) null else c.getInternal()
      if (obj != null) {
        tlog().debug("JDBCPool: Removing a bad jdbc connection from pool")
        try { _pool.invalidateObject(obj) } catch { case _ => }
      }
    }
  }

  /**
   * @param e
   * @return
   */
  def isBadConnection(e:Exception):Boolean = {

    val sqlState = e match {
      case x:SQLException => x.getSQLState()
      case _ => ""
    }

    sqlState match {
      case "08003" | "08S01" => true
      case _ =>
        // take a guess...
        equalsOneOfIC( nsb(e.getMessage()).lc,Array(
        "reset by peer",
        "aborted by peer",
        "not logged on",
        "socket write error",
        "communication error",
        "error creating connection",
        "connection refused",
        "connection refused",
        "broken pipe"
        ))
    }
  }

  /**
   * @param conn
   * @return
   */
  def isBadConnection(conn:JConn):Boolean = {
    var rc=false
    if (conn != null)
    try {
      val sql = vendor() match {
        case ORACLE  =>  "select count(*) from user_tables"
        case SQLSERVER =>  "select count(*) from sysusers"
        case DB2 => "select count(*) from sysibm.systables"
        case _ => ""
      }
      if (!isEmpty(sql)) using(conn.createStatement()) { (stmt) =>
        stmt.execute(sql)
      }
    }
    catch {
      case _ => rc=true
    }
    rc
  }

  private def forceCloseAll() {
    tlog().debug("JDBCPool: closing down all db connections...")
    var loop=true
    while (loop) {
      try {
        var jc= next()
        if (jc==null) { loop=false } else {
          using(jc.connection()) { (c) => }
        }
      }
      catch {
        case _ => loop=false
      }
    }

    tlog().debug("JDBCPool: database connections closed")
  }

  private def next() = {
    try {
      new JConnection(this, _pool.borrowObject().asInstanceOf[PConn])
    }
    catch {
      case _ => throw new SQLException("No free connection")
    }
  }

}
