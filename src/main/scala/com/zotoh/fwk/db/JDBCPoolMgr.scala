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

import scala.collection.mutable.HashMap

import java.sql.{Driver,DriverManager,SQLException,Connection=>JConn}
import java.util.{Properties=>JPS}

import org.apache.commons.dbcp.{DriverConnectionFactory, PoolableConnectionFactory}
import org.apache.commons.pool.ObjectPool
import org.apache.commons.pool.impl.GenericObjectPool
import org.apache.commons.dbcp.{PoolableConnection=>PConn}

import com.zotoh.fwk.util.LoggerFactory._
import com.zotoh.fwk.util.{Logger,CoreImplicits}
import com.zotoh.fwk.util.WWID._
import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.util.StrUte._
import com.zotoh.fwk.db.DBUte._


/**
 * @author kenl
 *
 */
sealed class JDBCPoolMgr extends DBVars with CoreImplicits {

  @transient private val _log = getLogger(classOf[JDBCPoolMgr])
  def tlog() = _log

  private var _ps= HashMap[String,JDBCPool]()

  /**
   * @param pl
   * @param pms
   * @param pps
   * @return
   */
  def mkPool(pl:String, pms:JDBCInfo, pps:JPS) = create(pl, pms, pps)

  /**
   * @param pms
   * @param pps
   * @return
   */
  def mkPool(pms:JDBCInfo, pps:JPS) = create( uid(), pms, pps)

  /**
   * @param param
   * @return
   */
  def mkPool(pms:JDBCInfo):JDBCPool = mkPool( uid(), pms)

  /**
   * @param pl
   * @param pms
   * @return
   */
  def mkPool(pl:String, pms:JDBCInfo) = {
    create(pl, pms,
      new JPS().add("username", pms.dbUser()).
      add("user", pms.dbUser()).
      add("password", pms.dbPwd()) )
  }

  private def create(pl:String, pms:JDBCInfo, pps:JPS) = synchronized {
    if (existsPool(pl)) {
      throw new SQLException("Jdbc Pool already exists: " + pl)
    }
    tlog().debug("JDBCPoolMgr: Driver : {}" , pms.dbDriver())
    tlog().debug("JDBCPoolMgr: URL : {}" ,  pms.dbUrl())

//    Ute.loadDriver(param.dbDriver());
    val d= DriverManager.getDriver(pms.dbUrl())
    val dcf= new DriverConnectionFactory(d, pms.dbUrl(), pps)
    val gop= new GenericObjectPool[PConn]()
    gop.setMaxActive(asInt(pps.gets("max-conns"), 10))
    gop.setTestOnBorrow(true)
    gop.setMaxIdle(gop.getMaxActive())
    gop.setMinIdle(asInt(pps.gets("min-conns"), 2))
    gop.setMaxWait(asLong(pps.gets("max-wait4-conn-millis"), 1500L))
    gop.setMinEvictableIdleTimeMillis(asLong(pps.gets("evict-conn-ifidle-millis"), 300000L))
    gop.setTimeBetweenEvictionRunsMillis(asLong(pps.gets("check-evict-every-millis"), 60000L))
    val pcf=new PoolableConnectionFactory(dcf, gop, null, null, true, false)
    pcf.setDefaultReadOnly(false)

    tlog().debug("JDBCPoolMgr: Added db pool: {}, info= {}", pl, pms)

    val j= JDBCPool(vendor(pms), pms, pcf.getPool().asInstanceOf[ObjectPool[PConn]])
    _ps.put(pl, j)
    j
  }

  /**
   *
   */
  def finz() {
    this.synchronized {
      _ps.foreach ( (t)=> t._2.finz() )
      _ps.clear()
    }
  }

  /**
   * @param n
   * @return
   */
  def existsPool(n:String) = _ps.isDefinedAt(nsb(n))

  /**
   * @param n
   * @return
   */
  def getPool(n:String) = _ps.get(nsb(n))

}
