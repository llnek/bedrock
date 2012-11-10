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
import com.zotoh.fwk.util.StrUte._

object INSERTStmt {
}

/**
 * Wrapper abstracting a SQL Insert statement.
 *
 * @author kenl
 *
 */
case class INSERTStmt(s:String, p:Seq[Any]) extends WritableStmt(s,p) {

  /**
   * Create an insert stsmt from this row.
   *
   * @param row
   */
  def this(row:DbRow) {
    this("", ZOBJS)
    dbgData(row)
    iniz(row)
  }

  private def iniz(row:DbRow) {
    val table = row.sqlTable()
    val m= row.values()
    var s=""
    val bf= new StringBuilder(1024)
    val b2= new StringBuilder(512)
    val b1= new StringBuilder(512)

    bf.append("INSERT INTO ").append(table).append(" (")
    m.foreach { (t) =>
      val s= if (isNichts(t._2)) {
        "NULL"
      } else {
        _values += t._2
        "?"
      }
      addAndDelim(b2, ",", s)
      addAndDelim(b1, ",", t._1)
      _cols.add(t._1)
    }

    bf.append(b1).append(") VALUES (").append(b2).append(")")
    setSQL(bf.toString )
  }

}

