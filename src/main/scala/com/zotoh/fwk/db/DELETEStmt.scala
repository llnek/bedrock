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

import com.zotoh.fwk.util.CoreTypes._
import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.util.CoreImplicits
import com.zotoh.fwk.util.StrUte._

object DELETEStmt  {

  /**
   * Create a simple delete stmt such as "delete from XYZ".
   *
   * @param table
   * @return
   */
  def simpleDelete(table:String) =
    new DELETEStmt("DELETE FROM " + nsb(table) , ZOBJS )  

}

/**
 * Simple wrapper abstracting a delete SQL statement.
 *
 * @author kenl
 *
 */
class DELETEStmt(s:String, pms:Seq[Any]) extends SQLStmt(s,pms) with CoreImplicits {

  /**
   * Create a delete stmt based on the SQL provided.
   *
   * @param sql   e,g.  "delete from XYZ where name=?"
   * @param params  e.g. [ "john" ]
   */

  /**
   * @param sql
   */
  def this(sql:String ) {
    this(sql , ZOBJS )
  }

  /**
   * Create a delete stmt and construct the sql inside based on the parameters
   * provided.
   *
   * @param table e.g. XYZ
   * @param where e.g. name=? and age=?
   * @param params e.g. [ 'john' , 21 ]
   */
  def this(table:String, where:String, pms:Seq[Any]) {
    this("", ZOBJS)
    setWhere( table, nsb(where))
    addParams(pms)
  }

  private def setWhere(tbl:String, where:String) {
    val bd= new StringBuilder(512).append("DELETE FROM ").append( tbl )
    if (! isEmpty(where)) {
      bd.append(" WHERE ").append(where)
    }
    setSQL(bd.toString )
  }

}

