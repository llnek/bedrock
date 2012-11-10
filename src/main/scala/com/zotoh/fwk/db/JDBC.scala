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

import scala.collection.mutable.{ArrayBuffer,HashMap}

import com.zotoh.fwk.util.LoggerFactory._
import com.zotoh.fwk.util.CoreTypes._
import com.zotoh.fwk.util.{CoreImplicits,Logger}
import com.zotoh.fwk.util.StrUte._
import com.zotoh.fwk.db.JDBCUte._
import com.zotoh.fwk.db.DBUte._
import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.io.IOUte

import java.io.{IOException,InputStream,Reader}
import java.sql.{Blob,Clob,Connection,PreparedStatement,ResultSet}
import java.sql.{ResultSetMetaData,SQLException}
import java.util.{Properties=>JPS}

object JDBC {}

/**
 * Higher level abstraction of a java jdbc object.
 *
 * @author kenl
 *
 */
sealed case class JDBC(private val _pool:JDBCPool) extends DBVars with CoreImplicits {

  @transient private val _log= getLogger(classOf[JDBC])
  def tlog() = _log

  /**
   * @param tbl Table.
   * @return
   */
  def getTableMetaData(table:String):Map[String,JPS] = {

    val tbl= _pool.vendor().assureTableCase(table)
    var bc:SQLException=null
    var jc:JConnection=null
    var tries= _pool.retries() + 1
    var done=false
    val ret=HashMap[String,JPS]()

    do {
      ret.clear()
      try {
        jc= _pool.nextFree()
        using( jc.connection().getMetaData().getColumns(null,null, tbl, null)) { (rset) =>
          if (rset!=null) while (rset.next()) {
            var i=rset.getInt("COLUMN_SIZE")
            val props= new JPS().add("COLUMN_SIZE",  asJObj(i))
            i=rset.getInt("DATA_TYPE")
            props.put("DATA_TYPE", asJObj(i))
            ret += rset.getString("COLUMN_NAME").uc -> props
          }
          done=true
        }
      }
      catch {
        case e:Exception => { bc= onError(jc, e) }
      }
      finally {
        drop(jc)
      }

      tries -= 1

    } while (!done && tries > 0)

    if (bc != null) { throw bc } else { ret.toMap }
  }

  /**
   * Do a "select count(*) from tbl where....".
   *
   * @param tbl
   * @param where
   * @param pms
   * @return
   */
  def existRows(tbl:String, where:String, pms:Seq[Any]) = countRows(tbl, where, pms ) > 0

  /**
   * Do a "select count(*) from tbl".
   *
   * @param tbl Table.
   * @return
   */
  def existRows(tbl:String) = countRows(tbl) > 0

  /**
   * Do a "select count(*) from tbl".
   *
   * @param tbl Table.
   * @return
   */
  def countRows(tbl:String):Int = countRows(tbl, "", ZOBJS )

  /**
   * Do a "select count(*) from tbl where....".
   *
   * @param tbl
   * @param where
   * @param params
   * @return
   */
  def countRows(tbl:String, where:String, pms:Seq[Any]):Int = {
    val sql= new SELECTStmt("COUNT(*)", tbl, where, pms)
    var tries= _pool.retries() + 1
    var done=false
    var rc=0
    var bc:SQLException=null
    var jc:JConnection=null

    do {
      try {
        jc= _pool.nextFree()
        using(jc.connection().prepareStatement(prepareSQL(sql))) { (stmt) =>
          (1 /: sql.params()) { (cnt,a) =>
            setStatement(stmt, cnt, a )
            cnt + 1
          }
          using(stmt.executeQuery()) { (rset) =>
            rc= if (rset != null && rset.next()) rset.getInt(1) else 0
            done=true
          }
        }
      }
      catch {
        case e:Exception => bc=onError(jc, e)
      }

      tries -= 1
    }
    while ( !done && tries > 0)

    if (bc != null) { throw bc } else { rc }
  }

  /**
   * Get & prepare a connection for a transaction.
   *
   * @return
   */
  def beginTX() = {
    val c=_pool.nextFree()
    c.begin()
    c
  }

  /**
   * Commit the transaction bound to this connection.
   *
   * @param c
   */
  def commitTX(c:JConnection) {
    if (c != null) {
      c.connection().commit()
    }
  }

  /**
   * Rollback the transaction bound to this connection.
   *
   * @param c
   */
  def cancelTX(c:JConnection) {
    if (c != null) {
      c.connection().rollback()
    }
  }

  /**
   * Close the transaction.  The connection SHOULD not be used afterwards.
   *
   * @param c
   */
  def closeTX(c:JConnection) { _pool.returnUsed(c) }

  /**
   * Do a "select * ...".
   *
   * @param sql
   * @return
   */
  def fetchOneRow(sql:SELECTStmt) = {
    val rows= fetchRows(sql)
    if (rows.length == 0) None else Some(rows(0))
  }

  /**
   * Do a "select * ...".
   *
   * @param sql
   * @return
   */
  def fetchRows(sql:SELECTStmt):Seq[DbRow] = {

    var tries= _pool.retries() + 1
    var bc:SQLException=null
    var done=false
    var rc:Seq[DbRow]=null
    var jc:JConnection=null

    do {
      try {
        jc = _pool.nextFree()
        rc= selectXXX(jc, sql)
        done=true
      }
      catch {
        case e:Exception => bc= onError(jc,e)
      }
      finally {
        drop(jc)
      }
      tries -= 1
    }
    while ( !done && tries > 0)

    if (bc != null) { throw bc } else { rc }
  }

  /**
   * Do a "delete from ...".
   *
   * @param jc
   * @param sql
   * @return
   */
  def deleteRows(jc:JConnection, sql:DELETEStmt) = delete(jc, sql)

  /**
   * Do a "delete from ...".
   *
   * @param sql
   * @return
   */
  def deleteRows(sql:DELETEStmt):Int = {

    var tries= _pool.retries() + 1
    var bc:SQLException=null
    var rc=0
    var done=false
    var jc:JConnection=null

    do {
       try {
         jc = _pool.nextFree()
         jc.begin()
         rc=delete(jc, sql)
         jc.flush()
         done=true
       }
       catch {
         case e:Exception => bc= onError(jc, e)
       }
       finally {
         drop(jc)
       }
       tries -= 1
    }
    while ( !done && tries > 0)

    if (bc != null) { throw bc } else { rc }
  }

  /**
   * Do a "insert into ...".
   *
   * @param jc
   * @param row
   */
  def insertOneRow(jc:JConnection, row:DbRow) { insert(jc, row) }

  /**
   * Do a "insert into ...".
   *
   * @param row
   * @return
   */
  def insertOneRow(row:DbRow):Int = {
    var tries = _pool.retries() + 1
    var bc:SQLException=null
    var done=false
    var rc=0
    var jc:JConnection=null

    do {
      try  {
        jc= _pool.nextFree()
        jc.begin()
        rc= insert(jc, row)
        jc.flush()
        done=true
      }
      catch {
        case e:Exception => bc=onError(jc,e)
      }
      finally {
        drop(jc)
      }
      tries -= 1
    }
    while (!done && tries >= 0)

    if (bc != null) { throw bc } else { rc }
  }

  /**
   * Do a "update set...".
   *
   * @param jc
   * @param row
   * @param where
   * @param pms
   * @return
   */
  def updateOneRow(jc:JConnection, row:DbRow, where:String, pms:Seq[Any]) = {
    update(jc, row, where, pms)
  }

  /**
   * Do a "update set...".
   *
   * @param row
   * @param where
   * @param pms
   * @return
   */
  def updateOneRow(row:DbRow, where:String, pms:Seq[Any]):Int = {
    var tries = _pool.retries() + 1
    var bc:SQLException=null
    var rc=0
    var done=false
    var jc:JConnection=null

    do {
      try {
        jc= _pool.nextFree()
        jc.begin()
        rc= update(jc, row, where, pms)
        jc.flush()
        done=true
      }
      catch {
        case e:Exception => bc= onError(jc,e)
      }
      finally {
        drop(jc)
      }
      tries -= 1
    }
    while ( !done && tries > 0)

    if (bc != null) { throw bc } else { rc }
  }

  private def update(jc:JConnection, row:DbRow, where:String, pms:Seq[Any]):Int = {
    val sql= new UPDATEStmt(row, where, pms)
    try {
      using(jc.connection().prepareStatement( prepareSQL(sql)) ) { (stmt) =>
        (1 /: sql.params()) { (pos, a) =>
          setStatement( stmt, pos, a)
          pos + 1
        }
        stmt.executeUpdate()
      }
    }
    catch {
      case e:Exception => throw onError(jc, e)
    }
  }

  private def buildRows(tbl:String, rset:ResultSet) = {
    val lst=ArrayBuffer[DbRow]()
    if (rset != null)
      while ( rset.next())  {
        lst += buildOneRow(tbl, rset)
      }
    tlog().debug("Fetched from table: \"{}\" : rows= {}" , tbl, asJObj(lst.size) )
    lst.toSeq
  }

  private def buildOneRow(tbl:String, rset:ResultSet) = {
    val meta= rset.getMetaData()
    val row= DbRow(tbl)

    (1 to meta.getColumnCount() ).foreach { (i) =>
      var obj=rset.getObject(i)
      var inp = obj match {
        case bb:Blob => bb.getBinaryStream()
        case s:InputStream => s
        case _ => null
      }
      if (inp != null) using(inp) { (inp) =>
        obj= IOUte.readBytes(inp)
      }
      var rdr = obj match {
        case cc:Clob => cc.getCharacterStream()
        case r:Reader => r
        case _ => null
      }
      if (rdr != null) using(rdr) { (rdr) =>
        obj= IOUte.readChars( rdr)
      }

      row.add( meta.getColumnName(i).uc, obj)
    }

    row
  }

  private def prepareSQL(stmt:SQLStmt) = {
    val sql= trim(stmt.toString())
    val v= _pool.vendor()
    val rc= sql.lc match {
      case s if (s.startsWith("select")) => v.tweakSELECT(sql)
      case s if (s.startsWith("update")) => v.tweakUPDATE(sql)
      case s if (s.startsWith("delete")) => v.tweakDELETE(sql)
      case _ => sql
    }
    tlog().debug(rc)
    rc
  }

  private def maybeDealWithBadConn( conn:Connection, e:Exception) = {
    _pool.isBadConnection(e) || _pool.isBadConnection(conn)
  }

  private def drop(jc:JConnection) {
    _pool.returnUsed(jc, if (jc.isDead()) false else true)
  }

  private def onError( jc:JConnection, con:Connection, e:Exception) = {
    if (maybeDealWithBadConn( con, e)) {
      jc.die()
      new DBBadConnError(e)
    }
    else e match {
      case x:SQLException => throw e
      case _ => throw new SQLException(e)
    }
  }

  private def selectXXX(jc:JConnection, sql:SELECTStmt):Seq[DbRow] = {
    try {
      using(jc.connection().prepareStatement(prepareSQL(sql))) { (stmt) =>
        (1 /: sql.params()) { (pos,a) =>
          setStatement( stmt, pos, a )
          pos +1
        }
        using(stmt.executeQuery()) { (rset) =>
          buildRows(sql.table(), rset)
        }
      }
    }
    catch {
      case e:Exception => throw onError(jc, e)
    }
  }

  private def delete(jc:JConnection, sql:DELETEStmt):Int = {
    try  {
      using(jc.connection().prepareStatement(prepareSQL(sql))) { (stmt) =>
        (1 /: sql.params()) { (pos, a) =>
          setStatement( stmt, pos, a)
          pos + 1
        }
        stmt.executeUpdate()
      }
    }
    catch {
      case e:Exception => throw onError(jc, e)
    }
  }

  private def insert(jc:JConnection, row:DbRow):Int = {
    val sql= new INSERTStmt(row)
    try {
      using(jc.connection().prepareStatement(prepareSQL(sql))) { (stmt) =>
        ( 1 /: sql.params()) { (pos, a) =>
          setStatement( stmt, pos, a)
          pos + 1
        }
        stmt.executeUpdate()
      }
    }
    catch {
      case e:Exception => throw onError(jc, e)
    }
  }

  private def onError(jc:JConnection, err:Exception):SQLException = {
    var bc:SQLException= null
    err match {
      case e:DBBadConnError => bc= e
      case _ => bc= onError(jc, jc.connection(), err)
    }
    jc.cancelQuietly()
    bc
  }

}


