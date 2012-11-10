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

import java.lang.System._

import java.io.File
import java.io.IOException
import java.sql.SQLException

/**
 * @author kenl
 *
 */
object H2DBSQL extends HxxDBSQL {

  val _app="h2db"
  val _db="h2"
  val _furl="jdbc:h2"

  /**
   * @param args
   */
  def main(args:Array[String] )  {
    exit( runMain(H2DBSQL, args))
  }

  /* (non-Javadoc)
   * @see com.zotoh.core.db.HxxDBSQL#xxCreateDB(java.io.File, java.lang.String, java.lang.String, java.lang.String)
   */
  def xxCreateDB(dbFileDir:File, dbid:String, user:String, pwd:String) = {
    H2DB.mkDB(dbFileDir, dbid, user, pwd)
  }

  /* (non-Javadoc)
   * @see com.zotoh.core.db.HxxDBSQL#xxLoadSQL(java.lang.String, java.lang.String, java.lang.String, java.io.File)
   */
  def xxLoadSQL(dbUrl:String, user:String, pwd:String, sql:File) {

    H2DB.loadSQL(dbUrl, user, pwd, sql)
  }

  /* (non-Javadoc)
   * @see com.zotoh.core.db.HxxDBSQL#xxCloseDB(java.lang.String, java.lang.String, java.lang.String)
   */
  def xxCloseDB(dbUrl:String, user:String, pwd:String) {

    H2DB.closeDB(dbUrl, user, pwd)
  }



}
