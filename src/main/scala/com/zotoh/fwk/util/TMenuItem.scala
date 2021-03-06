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

import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.util.StrUte._

object TMenuItem {
}

trait TMenuCB {

  /**
   * @param i
   */
  def command(i:TMenuItem)
}

/**
 * @author kenl
 *
 */
case class TMenuItem(private var _id:String, private var _desc:String) {

  private var _parent:TMenu= _
  private var _cb:TMenuCB= _
  private var _sub:TMenu= _

  _desc=trim(_desc)
  _id=trim(_id)

  /**
   * @param id
   * @param desc
   * @param m
   */
  def this(id:String, desc:String, m:TMenu) {
    this(id,desc)
    _sub=m
  }

  /**
   * @param id
   * @param desc
   * @param cb
   */
  def this(id:String, desc:String, cb:TMenuCB) {
    this(id,desc)
    _cb=cb
  }

  /**
   * @param m
   */
  def setParent(m:TMenu) = { _parent=m ; this }

  /**
   * @return
   */
  def parent() = _parent

  /**
   * @return
   */
  def desc() = _desc

  /**
   * @return
   */
  def id() = _id

  def onSelect() = {
    if ( _sub != null) { _sub.show(parent()) }
    if ( _cb != null) { _cb.command(this) }
    this
  }

}

