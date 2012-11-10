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

import scala.math._

import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.util.StrUte._

import java.util.StringTokenizer

/**
 * Cardinality definition.  Accepted syntaxes are:
 * {n,m} or (n,m) or {n} or (n)
 *
 * @author kenl
 *
 */
class Cardinality(s:String) extends CoreImplicits {

  private val serialVersionUID= -7593300202211134L
  private var _max= -1
  private var _min= -1

  parse(s)

  /**
   * @return
   */
  def isRequired() = _min > 0

  /**
   * @return
   */
  def getMaxOccurs() = _max


  /**
   * @return
   */
  def getMinOccurs() = _min


  /**
   * @param c
   */
  private def parse(c:String) {
    val tkz= new StringTokenizer( nsb(c), "{}(), \t\n\b\r\f")
    var s0= ""
    var s=""
    val N= "N"

    tkz.countTokens() match {
      case 1 =>
        s= tkz.nextToken()
        _max= if (N.eqic(s)) Integer.MAX_VALUE else asInt(s, -1)
        _min= _max
      case 2 =>
        s0= tkz.nextToken()
        s= tkz.nextToken()
        _max= if (N.eqic(s)) Integer.MAX_VALUE else asInt(s, -1)
        _min= asInt(s0, -1)
      case _ =>
    }

    _max= max(_max, 0)
    _min= max(_min, 0)

  }


  /**
   * @param min
   * @param max
   */
  def this(min:Int, max:Int) {
    this("{" + min + "," + max + "}")
  }


}
