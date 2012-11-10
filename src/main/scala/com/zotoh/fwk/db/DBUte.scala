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

import java.sql.{Connection,DatabaseMetaData=>DBMD,Driver,DriverManager,PreparedStatement=>PPS}
import java.sql.{ResultSet=>RSET,ResultSetMetaData=>RSMD,SQLException,Statement}

import java.util.{Properties=>JPS}

import com.zotoh.fwk.db.DBVendor._
import com.zotoh.fwk.util.Logger
import com.zotoh.fwk.util.LoggerFactory._
import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.util.StrUte._


/**
 * Helper db functions.
 *
 * @author kenl
 *
 */
object DBUte extends DBVars {
  private val _log= getLogger(classOf[DBUte])
  def tlog() = _log

  /**
   * @param c
   */
   def safeClose(c:Connection) { using(c) {(c) => } }

  /**
   * @param s
   */
   def safeCloseEx(s:Statement) { using(s) {(s) => } }

  /**
   * @param s
   */
   def safeClose(s:Statement) { using(s) { (s) => } }

  /**
   * @param s
   */
   def safeClose(r:RSET) { using(r) { (r) => } }

  /**
   * @param jp
   * @return
   */
  def createConnection(jp:JDBCInfo) = {
/*
    Class<?> c= loadDriver(jp.getDriver());
    if (c == null) {
      throw new SQLException("Failed to load jdbc-driver class: " + jp.getDriver() ) ;
    }
            */
    val con= if (isEmpty(jp.dbUser())) DriverManager.getConnection(jp.dbUrl()) else safeGetConn(jp)
    if (con == null) {
      throw new SQLException("Failed to create db connection: " + jp.dbUrl() )
    }
    con.setTransactionIsolation( jp.isolation() )
    con
  }

  /**
   * @param jp
   */
  def testConnection(jp:JDBCInfo) {
    safeClose(createConnection(jp))
  }

  /**
   * @param jp
   * @return
   */
  def vendor(jp:JDBCInfo) = {
    using(createConnection(jp)) { (con) =>
      val md= con.getMetaData()
      val name= md.getDatabaseProductName()
      val v= md.getDatabaseProductVersion()
      maybeGetVendor(name) match {
        case v@DBVendor.NOIDEA => v 
        case dbv =>
          dbv.setProductName(name)
          dbv.setProductVer(v)
          dbv.setCase(md.storesUpperCaseIdentifiers(),
            md.storesLowerCaseIdentifiers(),
            md.storesMixedCaseIdentifiers())
          dbv
      }
    }
  }

  /**
   * @param jp
   * @param table
   * @return
   */
  def tableExists(jp:JDBCInfo, table:String) = {
    var ok=false
    using(createConnection(jp)) { (con) =>
      try {
        val mt=con.getMetaData()
        val tbl= nsb(table) match {
          case s if (mt.storesUpperCaseIdentifiers()) => s.uc
          case s if (mt.storesLowerCaseIdentifiers()) => s.lc
          case s => s
        }
        using(mt.getColumns(null,null, tbl, null)) { (res) =>
          if (res != null && res.next()) { ok=true }
        }
      }
      catch {
        case e:SQLException => ok=false
        case e => throw e
      }
    }
    ok
  }

  /**
   * @param jp
   * @param table
   * @return
   */
  def rowExists(jp:JDBCInfo, table:String) = {
    var ok=false
    using(createConnection(jp)) { (con) =>
      try {
        val sql="SELECT COUNT(*) FROM " + table.uc
        using(con.createStatement()) { (stm) =>
        using(stm.executeQuery(sql)) { (res) =>
          if (res != null && res.next()) { ok = res.getInt(1) > 0 }
        }}
      }
      catch {
        case e:SQLException => ok=false
        case e => throw e
      }
    }
    ok
  }

  /**
   * @param jp
   * @param sql
   * @return
   */
  def firstRow(jp:JDBCInfo, sql:String) = {
    var row:DbRow = null
    using(createConnection(jp)) { (con) =>
      using(con.createStatement()) { (stm) =>
      using(stm.executeQuery(sql)) { (res) =>
        if (res != null && res.next()) {
          val md= res.getMetaData()
          row= DbRow()
          (1 to md.getColumnCount()).foreach { (pos) =>
            row.add(md.getColumnName(pos), res.getObject(pos))
          }
        }
      }}
    }
    if (row==null) None else Some(row)
  }

  def nocaseMatch(col:String, v:String) = {
    " UPPER(" + nsb(col) + ")" + " LIKE" + " UPPER('" + nsb(v) + "') "
  }

  def likeMatch(col:String, v:String) = {
    nsb(col) + " LIKE" + " '" + nsb(v) + "' "
  }

  def wildcardMatch(col:String, filter:String) = {
    " UPPER(" + nsb(col) + ")" +
    " LIKE" +
    " UPPER('" + nsb(strstr(  nsb(filter), "*", "%"))  + "') "
  }

  def loadDriver(s:String) = {
    try {
      Class.forName(s)
    }
    catch {
      case _ => throw new SQLException("Drive class not found: " + s)
    }
  }

  /**
   * Get a connection.
   *
   * @param z the driver class.
   * @param jp
   * @return
   */
  private def safeGetConn(jp:JDBCInfo) = {
    val props=new JPS()
    val j= jp.dbDriver()
    val p= jp.dbPwd()
    val u= jp.dbUrl()
    val n= jp.dbUser()

    var d:Driver = if (isEmpty(u)) null else {
      DriverManager.getDriver(u)
    }

    if (d==null) {
      throw new SQLException("Can't load Jdbc Url : " + u)
    }

    if ( ! isEmpty(j))  {
      var dz=d.getClass().getName()
      if ( j != dz) {
        tlog().warn("DBUte: Expected : " + j + " , loaded with driver : " + dz)
      }
    }

    if ( ! isEmpty(n)) {
      props.add("password", p).
      add("username", n).
      add("user", n)
      //setProps(u, props)
    }

    d.connect(u, props)
  }

  //private def setProps(url:String, props:Properties) :Unit = {}

  private def maybeGetVendor(product:String) = {
    nsb(product).lc match {
      case s if (s.has("microsoft")) => SQLSERVER
      case s if (s.has("hypersql")) => HSQLDB
      case s if (s.has("hsql")) => HSQLDB
      case s if (s.has("h2")) => H2
      case s if (s.has("oracle")) => ORACLE
      case s if (s.has("mysql")) => MYSQL
      case s if (s.has("derby")) => DERBY
      case s if (s.has("postgresql")) => POSTGRESQL
      case _ => DBVendor.NOIDEA
    }
  }

}

sealed class DBUte {}
