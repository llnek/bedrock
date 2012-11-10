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

package com.zotoh.fwk.crypto

import scala.Serializable

import com.zotoh.fwk.util.LoggerFactory._
import com.zotoh.fwk.util.Logger
import com.zotoh.fwk.util.StrUte._

object Password {
  val PWD_PFX= "CRYPT:"
}

/**
 * A Password is a wrapper class which has the ability
 * to obfuscate a piece of clear text.
 *
 * @author kenl
 *
 */
sealed case class Password private() {

  private def ilog() { _log=getLogger(classOf[Password]) }
  @transient private var _log:Logger=null
  def tlog() = { if (_log==null) ilog(); _log }


  private val serialVersionUID = -6871562722221029618L
  private val PWD_PFX_SZ= Password.PWD_PFX.length()
  private var _pwd=""

  /**
   * Constructor from encrypted text.
   *
   * @param encoded
   * @param dummy
   * @throws Exception
   */
  protected[crypto] def this(encoded:String, dummy:Int) = {
    this()
    _pwd= encoded
    if ( !isEmpty(encoded)) {
      if (encoded.startsWith( Password.PWD_PFX)) {
        _pwd=encoded.substring( PWD_PFX_SZ)
      }
      _pwd= JavaOfuscator().unobfuscate(_pwd)
    }
  }

  /**
   * Construct from clear text.
   *
   * @param clearText
   * @throws Exception
   */
  protected[crypto] def this(clearText:String) = {
    this()
    _pwd= clearText
  }

  /**
   * Copy Constructor.
   *
   * @param p
   */
  protected[crypto] def this(p:Password) = {
    this()
    if (p==null || p._pwd==null) {
      _pwd= null
    } else {
      _pwd= new String(p._pwd)
    }
  }

  /**
   * Get the password as encrypted text.  The convention is to have a prefix
   * "CRYPT:" prepended to indicate that the text is ofuscated.
   *
   * @return
   */
  def encoded() = {
    var c= _pwd
    if ( ! isEmpty(_pwd)) try {
      c= Password.PWD_PFX + JavaOfuscator().obfuscate(_pwd)
    }
    catch {
      case e => tlog().warnX("", Some(e))
    }
    c
  }

  /**
   * Return the password as clear text.
   *
   * @return
   */
  def text() = _pwd

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  override def toString() = text()

}
