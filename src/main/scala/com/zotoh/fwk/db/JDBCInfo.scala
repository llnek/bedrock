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

import java.sql.Connection

import com.zotoh.fwk.util.LoggerFactory._
import com.zotoh.fwk.util.Logger
import com.zotoh.fwk.util.StrUte._

object JDBCInfo {}

/**
 * @author kenl
 *
 */
case class JDBCInfo( private var _url:String, private var _user:String, private var _pwd:String)  {
  private def ilog() { _log = getLogger(classOf[JDBCInfo]) }
  private val serialVersionUID = 6871654777100857463L
  @transient private var _log:Logger=null
  def tlog() = { if(_log==null) ilog(); _log  }

  private var _isolation= Connection.TRANSACTION_READ_COMMITTED
  private var _driver=""
    
  _user=nsb(_user)
  _url=nsb(_url)
  _pwd=nsb(_pwd)

  /**
   * @param driver
   * @param url
   * @param user
   * @param pwd
   */
  def this(driver:String, url:String, user:String, pwd:String) {
//    tlog().debug( "JDBC: driver = {}, url = {}, user = {}, pwd=****", driver, url, user ) ;
    this(url,user,pwd)
    setDriver(driver)
  }

  def this() {
    this("","","")
  }
  
  /**
   *
   * @return
   */
  def isolation() = _isolation

  /**
   * @param n
   */
  def setIsolation(n:Int) { _isolation= n  }

  /**
   * @param driver
   */
  def setDriver( driver:String ) { _driver= nsb(driver) ; this }

  /**
   * @return
   */
  def dbDriver() = _driver

  /**
   * @param url
   */
  def setUrl( url:String) { _url= nsb(url); this }

  /**
   * @param user
   */
  def setUser( user:String) { _user= nsb(user); this }

  /**
   * @param pwd
   */
  def setPwd( pwd:String) { _pwd= nsb(pwd); this }

  /**
   * @return
   */
  def dbUrl() = _url

  /**
   * @return
   */
  def dbUser() = _user

  /**
   * @return
   */
  def dbPwd() = _pwd

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  override def toString() = {
    "Driver: " + dbDriver() + ", Url: " + dbUrl() + ", User: " + dbUser() + "Pwd: ****"
  }

}

