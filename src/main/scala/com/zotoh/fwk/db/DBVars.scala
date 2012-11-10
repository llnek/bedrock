/*??
 * COPYRIGHT (C) 2012 CHERIMOIA LLC. ALL RIGHTS RESERVED.
 *
 * THIS IS FREE SOFTWARE YOU CAN REDISTRIBUTE IT AND/OR
 * MODIFY IT UNDER THE TERMS OF THE APACHE LICENSE,
 * VERSION 2.0 (THE "LICENSE").
 *
 * THIS LIBRARY IS DISTRIBUTED IN THE HOPE THAT IT WILL BE USEFUL,
 * BUT WITHOUT ANY WARRANTY WITHOUT EVEN THE IMPLIED WARRANTY OF
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.
 *
 * SEE THE LICENSE FOR THE SPECIFIC LANGUAGE GOVERNING PERMISSIONS
 * AND LIMITATIONS UNDER THE LICENSE.
 *
 * You should have received a copy of the Apache License
 * along with this distribution if not, you may obtain a copy of the
 * License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 ??*/

package com.zotoh.fwk.db

/**
 * DB Constants.
 *
 * @author kenl
 *
 */
trait DBVars {

  val POSTGRESQL_DRIVER= "org.postgresql.Driver"
  val MYSQL_DRIVER= "com.mysql.jdbc.Driver"
  val H2_DRIVER= "org.h2.Driver"
  val HSQLDB_DRIVER= "org.hsqldb.jdbcDriver"
  val HSQLDB_DRIVER_2= "org.hsqldb.jdbc.JDBCDriver"

  val DERBY_E_DRIVER= "org.apache.derby.jdbc.EmbeddedDriver"
  val DERBY_C_DRIVER= "org.apache.derby.jdbc.ClientDriver"

  val SQL_PARAM= " = ? "
  val VARCHAR_WIDTH= 255

  val ROWLOCK="rowlock"
  val NOLOCK="nolock"

  val HSQLDB_MEM_URL="jdbc:hsqldb:mem:"
  val HSQLDB_FILE_URL="jdbc:hsqldb:file:"

  //val H2_SERVER_URL = "jdbc:h2:tcp://host/path/db"
  val H2_FILE_URL = "jdbc:h2:"
  val H2_MEM_URL = "jdbc:h2:mem:"

  val S_POSTGRESQL= "postgresql"
  val S_ORACLE= "oracle"
  val S_MSSQL= "mssql"
  val S_MYSQL= "mysql"
  val S_H2= "h2"
  val S_HSQLDB= "hsql"
  val S_HYPERSQL= "hypersql"
  val S_DERBY= "derby"
  val S_DB2= "db2"

//  val COL_VERCNT = "_VERCNT_"
//  val COL_OID= "_OBJOID_"
//  val COL_LASTCHG= "_LASTCHG_"

//  val COL_ASSOC= "_ASSOC_"
//  val COL_RHS= "_RHS_"
//  val COL_LHS= "_LHS_"
//
//  val TBL_SHARED= "EDB_SHARED"
  val CSEP= "?"
  val S_DDLSEP= "-- :"  // watch out there's a space before the colon

  val TS_CURRENT= "current_timestamp"

}
