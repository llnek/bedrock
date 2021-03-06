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

import com.zotoh.fwk.db.DBUte._
import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.util.LoggerFactory._
import com.zotoh.fwk.util.Logger
import com.zotoh.fwk.util.StrUte._

import java.io.File
import java.io.IOException
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.sql.Statement


/**
 * @author kenl
 *
 */
trait HxxDB extends DBVars {

  private var _log= getLogger(classOf[HxxDB])
  def tlog() = _log

  /**
   * Shut down this database by issuing a SHUTDOWN call through this
   * connection.
   *
   * @param c
   * @throws SQLException
   */
  def shutdown(c:Connection) {

    if (c != null) {
      c.setAutoCommit(true)
      using(c.createStatement()) { (stmt) =>
        stmt.execute("SHUTDOWN")
      }
    }
  }

  /**
   * Shutdown the database.
   *
   * @param dbUrl
   * @param user
   * @param pwd
   * @throws SQLException
   */
  def closeDB(dbUrl:String, user:String, pwd:String) {
    tstEStrArg("db-url", dbUrl )
    tstEStrArg("user", user )

    tlog().debug("Shutting down HxxDB: {}", dbUrl)

    using(DriverManager.getConnection(dbUrl, user, nsb(pwd))) { (c1) =>
      shutdown(c1)
    }

  }

  /**
   * @param param
   * @throws SQLException
   */
  def closeDB(jp:JDBCInfo ) {
    tstObjArg("jdbc-info", jp )
    closeDB(jp.dbUrl(), jp.dbUser(), jp.dbPwd())
  }

  /**
   * Create a database.
   *
   * @param user
   * @param pwd
   * @return
   * @throws SQLException
   * @throws IOException
   */
  def mkDB(dbid:String, user:String, pwd:String):String = mkDB( genTmpDir(), dbid, user, pwd)

  /**
   * Clean up all the relevant database files in this folder, effectively
   * removing the database.
   *
   * @param dbPath
   */
  def dropDB(dbPath:String) {
    onDropDB( trimLastPathSep(dbPath))
  }

  /**
   * Load a DDL from file and run it.
   *
   * @param url
   * @param user
   * @param pwd
   * @param sql
   * @throws IOException
   * @throws SQLException
   */
  def loadSQL( dbUrl:String, user:String, pwd:String, sql:File) {

    tstEStrArg("db-url", dbUrl )
    tstObjArg("file", sql )
    tstObjArg("user", user )

    tlog().debug("Loading SQL: {}", niceFPath(sql))
    tlog().debug("JDBC-URL: {}", dbUrl)

    DDLUte.loadDDL( JDBCInfo(dbUrl, user, pwd), sql)
  }

  /**
   * Create an in-memory database.
   *
   * @param dbid
   * @param user
   * @param pwd
   * @throws SQLException
   */
  def mkMemDB(dbid:String, user:String, pwd:String) = {

    tstEStrArg("db-id", dbid )
    tstEStrArg("user", user )

    val dbUrl= getMemPfx() + dbid + getMemSfx()

    using(DriverManager.getConnection( dbUrl, user, nsb(pwd))) { (c1) =>
      c1.setAutoCommit(true)
    }

    dbUrl
  }

  /**
   * @return
   */
  protected def getMemSfx() = ""

  /**
   * Create a database in the specified directory.
   *
   * @param fileDir
   * @param user
   * @param pwd
   * @return
   * @throws IOException
   * @throws SQLException
   */
  def mkDB(dbFileDir:File, dbid:String, user:String, pwd:String):String = {

    tstObjArg("file-dir", dbFileDir)
    tstEStrArg("db-id", dbid)
    tstEStrArg("user", user )

    var dbUrl= trimLastPathSep(niceFPath(dbFileDir)) + "/" + dbid
    dbFileDir.mkdirs()
    dropDB(dbUrl)
    dbUrl= getEmbeddedPfx() + dbUrl

    tlog().debug("Creating HxxDB: {}", dbUrl)

    using( DriverManager.getConnection(dbUrl, user, nsb(pwd)) ) { (c1) =>
      c1.setAutoCommit(true)
      using(c1.createStatement()) { (s) =>
        //s.execute("CREATE USER " + user+ " PASSWORD \"" + pwd + "\" ADMIN")
        onCreateDB(s)
      }
      using(c1.createStatement()) { (s) =>
        s.execute("SHUTDOWN")
      }
    }

    dbUrl
  }


  /**
   * Tests if there is a database.
   *
   * @param dbPath
   * @return
   */
  def existsDB(dbPath:String) = onTestDB( trimLastPathSep(dbPath) )

  /**
   * @param dbPath
   * @return
   */
  protected def onTestDB(dbPath:String):Boolean

  /**
   * @param dbPath
   */
  protected def onDropDB(dbPath:String):Boolean

  /**
   * @return
   */
  protected def getEmbeddedPfx():String

  /**
   * @return
   */
  protected def getMemPfx():String

  /**
   * @param s
   * @throws SQLException
   */
  protected def onCreateDB(s:Statement):Unit

}

