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

import org.apache.commons.dbcp.PoolableConnection
import java.sql.{Connection,SQLException}

import com.zotoh.fwk.util.LoggerFactory._
import com.zotoh.fwk.util.Logger
import com.zotoh.fwk.db.DBUte._


/**
 * Wrapper class on top of a pooled jdbc connection.
 *
 * @author kenl
 *
 */
sealed class JConnection protected[db] (private val _pool:JDBCPool, private val _pc:PoolableConnection)  {

  private def ilog() { _log= getLogger(classOf[JConnection]) }
  @transient private var _log:Logger=null
  def tlog() = { if (_log==null) ilog(); _log  }

  private var _dead=false

  /**
   * @return
   */
  def connection() = _pc.getDelegate()

  /**
   * @return
   */
  def vendor() = _pool.vendor()

  /**
   * Start a DB transaction.
   */
  protected[db] def begin() {
    tlog().debug("JConnection: Starting db transaction...")
    reset()
    connection().setAutoCommit(false)
  }

  /**
   * Commit changes.
   *
   */
  protected[db] def flush() { connection().commit() }

  /**
   * Rollback changes, ignore any errors.
   */
  protected[db] def cancelQuietly() {
    try {
      cancel(false)
    }
    catch {
      case e => tlog().warnX("", Some(e))
    }
  }

  /**
   * Rollback changes.
   *
   */
  protected[db] def cancel() { cancel(true) }

  /**
   * Tests if this connection should not be used anymore.
   *
   * @return
   */
  def isDead() = _dead

  /**
   * Render this connection un-unsable, closes the underlying connection.
   */
  def die() {
    safeClose(connection())
    _dead=true
  }

  /**
   * @return
   */
  protected[db] def getInternal() = _pc

  private def cancel(wantError:Boolean) {
    try {
      connection().rollback()
    }
    catch {
      case e =>
        if (wantError)  { throw e }
        else {
          tlog().warnX("", Some(e))
        }
    }
  }

  private def reset() {}

}

