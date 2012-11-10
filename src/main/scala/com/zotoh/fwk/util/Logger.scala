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

package com.zotoh.fwk.util

import scala.collection.JavaConversions._

/**
 * @author kenl
 */
object Logger {
  val Dummy = Logger()
}

/**
 * @author kenl
 */
sealed case class Logger( private val _logr:Option[org.slf4j.Logger]=None ) {

  /**
   * @return
   */
  def isDebugEnabled() = _logr match {
    case Some(x) => x.isDebugEnabled()
    case _ => false
  }

  /**
   * @return
   */
  def isWarnEnabled() = _logr match {
    case Some(x) => x.isWarnEnabled()
    case _ => false
  }

  /**
   * @return
   */
  def isInfoEnabled() = _logr match {
    case Some(x) => x.isInfoEnabled()
    case _ => false
  }

  /**
   * @return
   */
  def isErrorEnabled() = _logr match {
    case Some(x) => x.isErrorEnabled()
    case _ => false
  }

  /**
   * @param msg
   * @param args
   */
   def error(msg: => String, args: Object* ) {
    _logr match {
      case Some(x) if x.isErrorEnabled() => x.error(msg,args.toArray)
      case _ =>
    }
  }

  /**
   * @param msg
   * @param t
   */
  def errorX(msg: => String, t:Option[Throwable] = None) {
    _logr match {
      case Some(x) if x.isErrorEnabled() =>
        t match {
          case Some(e) =>x.error(msg,e)
          case _ => x.error(msg)
        }
      case _ =>
    }
  }

  /**
   * @param msg
   * @param args
   */
  def debug(msg: => String , args: Object*) {
    _logr match {
      case Some(x) if x.isDebugEnabled() => x.debug(msg,args.toArray)
      case _ =>
    }
  }

  /**
   * @param msg
   * @param t
   */
  def debugX(msg: => String, t:Option[Throwable]=None) {
    _logr match {
      case Some(x) if x.isDebugEnabled() =>
        t match {
          case Some(e) => x.debug(msg,e)
          case _ => x.debug(msg)
        }
      case _ =>
    }
  }

  /**
   * @param msg
   * @param args
   */
  def info(msg: => String, args: Object* ) {
    _logr match {
      case Some(x) if x.isInfoEnabled() => x.info(msg, args.toArray)
      case _ =>
    }
  }

  /**
   * @param msg
   * @param t
   */
  def infoX(msg: => String, t:Option[Throwable] = None) {
    _logr match {
      case Some(x) if x.isInfoEnabled() =>
        t match {
          case Some(e) => x.info(msg,e)
          case _ => x.info(msg)
        }
      case _ =>

    }
  }

  /**
   * @param msg
   * @param args
   */
  def warn(msg: => String, args: Object* ) {
    _logr match {
      case Some(x) if x.isWarnEnabled() => x.warn(msg,args.toArray)
      case _ =>
    }
  }

  /**
   * @param msg
   * @param t
   */
  def warnX(msg: => String, t:Option[Throwable]=None) {
    _logr match {
      case Some(x) if x.isWarnEnabled() =>
        t match {
          case Some(e) => x.warn(msg,e)
          case _ => x.warn(msg)
        }
      case _ =>
    }
  }


}

