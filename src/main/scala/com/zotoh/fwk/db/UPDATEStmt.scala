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

import com.zotoh.fwk.crypto.Password
import com.zotoh.fwk.util.CoreTypes._
import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.util.StrUte._

object UPDATEStmt {}

/**
 * Wrapper on top of a SQL update statement.
 *
 * @author kenl
 *
 */
case class UPDATEStmt(s:String, ps:Seq[Any]) extends WritableStmt(s,ps) {

  /**
   * @param row
   * @param where
   * @param vals
   */
  def this(row:DbRow, where:String, pms:Seq[Any]) {
    this("", ZOBJS)
    set(row, where, pms)
  }

  private def set(row:DbRow, where:String, pms:Seq[Any]) {
    dbgData(row)
    iniz(row, where, pms)
  }

  private def iniz(row:DbRow, where:String, pms:Seq[Any]) {
    val bf= new StringBuilder().append("UPDATE ").append(row.sqlTable()).append(" SET ")
    val b1= new StringBuilder(512)
    row.values().foreach { (t) =>
      addAndDelim(b1, " , ", t._1)
      val v= t._2 match {
        case pwd:Password => pwd.encoded()
        case a:Any => a
      }
      if (isNichts(v)) {
        b1.append("=NULL")
      } else {
        b1.append("=?")
        _values += v
        _cols.add(t._1)
      }
    }

    bf.append(b1)

    if (! isEmpty(where)) {
      bf.append(" WHERE ").append(where)
      // extra params for where clause
      addParams(pms)
    }

    setSQL(bf.toString())
  }

}
