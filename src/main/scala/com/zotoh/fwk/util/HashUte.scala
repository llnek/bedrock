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


/**
 * A rough clone of some Hash code in public domain.
 *
 * @author kenl
 *
 */
object HashUte  {

  private val fODD_PRIME_NUMBER = 83
  private val SEED = 71

  /**
   * @param b
   * @return
   */
  def hash(b:Boolean):Int = firstTerm( SEED ) +  ( if (b) 1 else 0 )

  /**
   * @param c
   * @return
   */
  def hash(c:Char):Int = firstTerm( SEED ) + c.toInt

  /**
   * @param n
   * @return
   */
  def hash(n:Int):Int = hash(SEED, n)

  /**
   * @param n
   * @return
   */
  def hash( n:Long):Int = firstTerm(SEED)  + ( n ^ (n >>> 32) ).toInt

  /**
   * @param f
   * @return
   */
  def hash( f:Float):Int = hash( SEED, java.lang.Float.floatToIntBits(f) )

  /**
   * @param d
   * @return
   */
  def hash( d:Double ):Int = hash( SEED, java.lang.Double.doubleToLongBits(d) )

  /**
   * @param obj
   * @return
   */
  def hash( obj:AnyRef):Int = hash(SEED, obj)

  private def hash( seed:Int, obj:Any):Int = {
    obj match {
      case a:Seq[_] => (seed /: a) { (res, e) => hash(res,e) }
      case null => hash(seed, 0)
      case _ => hash(seed, obj.hashCode())
    }
  }

  private def hash(seed:Int, n:Int):Int = firstTerm( seed ) + n

  private def firstTerm( aSeed:Int ) = fODD_PRIME_NUMBER * aSeed

}

