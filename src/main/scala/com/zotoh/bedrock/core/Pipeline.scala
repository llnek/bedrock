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

package com.zotoh.bedrock.core

import com.zotoh.fwk.util.LoggerFactory._
import com.zotoh.fwk.util.Logger

import java.util.{Properties=>JPS}
import java.sql.Timestamp
import org.json.{JSONObject=>JSNO}

import com.zotoh.fwk.db.{DbRow,DELETEStmt,JConnection,JDBC,SELECTStmt}
import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.util.StrUte._
import com.zotoh.fwk.util.Nichts._
import com.zotoh.fwk.util.JSONUte._
import com.zotoh.fwk.util.{JSONUte,CoreUte}
import com.zotoh.fwk.io.XData

/**
 * @author kenl
 *
 */
trait Pipeline  extends Vars {

  private def ilog() { _log=getLogger(classOf[Pipeline]) }
  private var _log:Logger=null
  def tlog() = { if(_log==null) ilog(); _log }

  /**
   * @return
   */
  def engineQuirks():JPS

  /**
   * @return
   */
  def pid():Long

  /**
   * @return
   */
  def engine():AppEngine

  /**
   * @return
   */
  def curState():Option[AppState]

  /**
   * @return
   */
  def job():Job

  /**
   * @return
   */
  def isActive():Boolean

  def start():Unit

  def stop():Unit

  // -------------------- state management

  protected def mkState():AppState

  /**
   * @param prevState
   */
  def reconcileState(prevState:Option[JSNO]) {
    prevState match {
      case Some(j) =>
        val cur= curState() match {
          case Some(cur) => cur
          case _ => mkState()
        }
        cur.setRoot(j)
      case _ =>
    }
  }

  /**
   * @param col
   * @param key
   * @throws Exception
   */
  def retrievePreviousState(col:ColPicker, key:Any) {
    retrieveStateViaXXX(col.toString(), key) match {
      case Some(j) => reconcileState(Some(j))
      case _ =>
    }
  }

  /**
   * @param col
   * @param key
   * @return
   * @throws Exception
   */
  protected def retrieveStateViaXXX(col:String, key:Any):Option[JSNO] = {
    val obj:Any = maybeGetStateRow(engine().newJdbc(), col, key) match {
      case Some(row) => row.get(COL_BIN)
      case _ => NICHTS
    }
    val bits = obj match {
      case co:XData => co.bytes()
      case b:Array[Byte] => b
      case _ => null
    }
    if ( !isNilSeq(bits)) {
      Some(JSONUte.read( CoreUte.asString(bits)))
    } else {
      None
    }
  }

  /**
   * @throws Exception
   */
  def persistState() {
    curState() match {
      case Some(s) if (s.hasKey()) =>
        val j= engine().newJdbc()
        val tx= j.beginTX()
        try {
          delete() match { case Some(sql) => j.deleteRows(tx, sql) ; case _ => }
          insert() match { case Some(r) => j.insertOneRow(tx, r) ; case _ => }
          j.commitTX(tx)
        } catch {
          case e => { tlog().errorX("", Some(e)); j.cancelTX(tx) }
        } finally {
          j.closeTX(tx)
        }
    case _ =>
    }
  }

  /**
   * @throws Exception
   */
  def removeState() {
    delete() match {
      case Some(sql) => engine().newJdbc().deleteRows(sql)
      case _ =>
    }
  }

  private def query(col:String, key:Any) = {
    val sql= "select * from " + DB_STATE_TBL + " where " + col + "=?"
    Some(new SELECTStmt(sql, Array(key)))
  }

  private def delete() = {
    curState() match {
      case Some(s) if (s.hasKey()) =>
        Some(new DELETEStmt(DB_STATE_TBL,
          " where " + COL_KEYID + " =?", Array(s.key() )))
      case _ => None
    }
  }

  private def insert() = {
    //Timestamp ts= new Timestamp(new Date().getTime());
    insertWithExpiry(None)
  }

  private def insertWithExpiry(exp:Option[Timestamp]) = {
    curState() match {
      case Some(s) if (s.hasKey()) =>
        val row= DbRow(DB_STATE_TBL)
        val key= s.key()
        val bits= asBytes(JSONUte.asString(s.root()))
        row.add(COL_TRACKID, s.tracker())
        row.add(COL_KEYID, key)
        row.add(COL_BIN, bits)
        row.add(COL_EXPIRY, exp match { case Some(ts) => ts; case _ => NICHTS} )
        Some(row)
      case _ => None
    }
  }

  /**
   * @param jdbc
   * @param col
   * @param key
   * @return
   * @throws Exception
   */
  protected def maybeGetStateRow(j:JDBC, col:String, key:Any) = {
    query(col, key) match {
      case Some(sql) => j.fetchOneRow(   sql)
      case _ => None
    }

  }

}
