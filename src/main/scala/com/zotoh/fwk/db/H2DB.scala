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

import com.zotoh.fwk.util.FileUte.delete

import java.io.File
import java.sql.SQLException
import java.sql.Statement

/**
 * Utility functions relating to the management of a H2 instance.
 *
 * @author kenl
 *
 */
object H2DB extends HxxDB {

  /* (non-Javadoc)
   * @see com.zotoh.core.db.HxxDB#getEmbeddedPfx()
   */
  def getEmbeddedPfx() = H2_FILE_URL

  /* (non-Javadoc)
   * @see com.zotoh.core.db.HxxDB#getMemPfx()
   */
  def getMemPfx() = H2_MEM_URL

  /* (non-Javadoc)
   * @see com.zotoh.core.db.HxxDB#getMemSfx()
   */
  override def getMemSfx() = ";DB_CLOSE_DELAY=-1"

  /* (non-Javadoc)
   * @see com.zotoh.core.db.HxxDB#onTestDB(java.lang.String)
   */
  def onTestDB(p:String) =
    if(p==null) false else new File(p + ".h2.db").exists()


  /* (non-Javadoc)
   * @see com.zotoh.core.db.HxxDB#onDropDB(java.lang.String)
   */
  def onDropDB(p:String)  = {
    val f= new File(""+p+".h2.db")
    val rc=f.exists()
    delete(""+p+".h2.lock")
    delete(f)
    rc
  }

  /* (non-Javadoc)
   * @see com.zotoh.core.db.HxxDB#onCreateDB(java.sql.Statement)
   */
  def onCreateDB(s:Statement) {
    if (s != null) { s.execute("SET DEFAULT_TABLE_TYPE CACHED") }
  }

}
