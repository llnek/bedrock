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
import scala.collection.JavaConversions._

import java.io.{File,FileOutputStream,InputStream,OutputStream,IOException}
import java.sql.{Connection,SQLException,Statement,DatabaseMetaData}

import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.util.StrUte._
import com.zotoh.fwk.db.DBUte._
import com.zotoh.fwk.io.IOUte._
import com.zotoh.fwk.io.IOUte
import com.zotoh.fwk.util.LoggerFactory._
import com.zotoh.fwk.util.{CoreImplicits,Logger}


/**
 * Utility functions related to DDL creation/execution via JDBC.
 *
 * @author kenl
 *
 */
object DDLUte extends DBVars with CoreImplicits {

  private val _log= getLogger(classOf[DDLUte])
  def tlog() = _log

  /**
   * Write the ddl resource to output stream.
   *
   * @param resourcePath e.g. "com/acme/ddl.sql"
   * @param out
   * @param cl optional.
   */
  def ddlToStream(out:OutputStream, resourcePath:String, cl:Option[ClassLoader]=None) {
    if (out != null) {
      out.write( asBytes(rc2Str(resourcePath, "utf-8", cl)))
      out.flush()
    }
  }

  /**
   * Write ddl resource to file.
   *
   * @param fpOut
   * @param resourcePath
   * @param cl optional.
   */
  def ddlToFile(fpOut:File, resourcePath:String, cl:Option[ClassLoader]=None) {
    if (fpOut != null) {
      using(new FileOutputStream(fpOut)) { (out) =>
        ddlToStream(out, resourcePath, cl)
      }
    }
  }

  /**
   * Load a ddl from file and run that against a database.
   *
   * @param jp
   * @param fp
   */
  def loadDDL(jp:JDBCInfo, fp:File) {
    if (fp != null)
      using( open(fp)) { (inp) =>
        loadDDL(jp, inp)
      }
  }

  /**
   * Load ddl from stream and run that against a database.
   *
   * @param jp
   * @param inp
   */
  def loadDDL( jp:JDBCInfo, inp:InputStream) {    
    loadDDL(jp, asString( bytes(inp)) )
  }

  /**
   * Load ddl from string and run that against a database.
   *
   * @param jp
   * @param ddl
   */
  def loadDDL(jp:JDBCInfo, ddl:String) {
    if ( !isEmpty(ddl)) {
      tlog().debug(ddl)
      using(createConnection(jp)) { (con) =>
        loadDDL(con, ddl)
      }
    }
  }

  private def loadDDL(con:Connection, ddl:String) {
    val oldc= con.getAutoCommit()
    var ee:Throwable=null
    val lines= splitLines(ddl)
    con.setAutoCommit(true)
    try {
      lines.foreach { (line) =>

        trim( trim(line), ";") match {
          case ln if (!isEmpty(ln) && !ln.eqic("go") ) =>
            try {
              using(con.createStatement()) { (stmt) =>
                stmt.executeUpdate(ln)
              }
            } catch {
              case e:SQLException =>
                maybeOK(con.getMetaData().getDatabaseProductName(), e)
              case e => throw e
            }
        }
      }
    } catch {
      case e => tlog().errorX("", Some(e)); ee=e; throw e
    } finally {
      try { if (ee != null) con.rollback() } catch { case _ => }
      con.setAutoCommit(oldc)
    }
  }

  private def splitLines(lines:String) = {
    var pos = nsb(lines).indexOf(S_DDLSEP)
    var ddl=lines
    var rc=ArrayBuffer[String]()
    val w= S_DDLSEP.length()
    while (pos >= 0) {
      rc += trim(ddl.substring(0,pos))
      ddl= ddl.substring(pos+w)
      pos= ddl.indexOf(S_DDLSEP)
    }
    trim(ddl) match {
      case s:String if s.length() > 0 => rc += s
      case _ =>
    }
    rc.toArray
  }

  private def maybeOK(dbn:String, e:SQLException) = {

    val db= nsb(dbn).lc
    val oracle=db.has("oracle")
    val db2=db.has("db2")
    val derby=db.has("derby")

    if ( ! (oracle || db2 || derby)) { throw e }

    val ee=e.getCause() match {
      case x:SQLException => x
      case _ => e
    }
    ee.getErrorCode() match {
      case ec if (oracle && (942==ec || 1418==ec || 2289==ec || 0==ec)) => true
      case ec if (db2 && (-204==ec)) => true
      case ec if (derby && (30000==ec)) => true
      case _ => throw e
    }
  }

}

sealed class DDLUte {}

