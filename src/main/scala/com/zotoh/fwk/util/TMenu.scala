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

import scala.collection.mutable.{ArrayBuffer,HashMap,HashSet}

import java.io.{BufferedReader,Console,InputStreamReader}

import com.zotoh.fwk.util.LoggerFactory._
import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.util.StrUte._

object TMenu {
}

/**
 * @author kenl
 *
 */
case class TMenu(protected var _title:String) {

  protected val _choices=ArrayBuffer[TMenuItem]()
  protected val _ids=HashSet[String]()
  protected var _prev:TMenu=null

  private def ilog() { _log= getLogger(classOf[TMenu]) }
  @transient private var _log:Logger= null
  def tlog() = { if (_log==null) ilog(); _log }

  _title=trim(_title)

  /**
   *
   */
  def pop() = {
    if (_prev != null) { _prev.display() }
    this
  }

  /**
   * @param upper
   */
  def show(upper:TMenu) = {
    if (upper != null) { _prev=upper }
    display()
    this
  }

  /**
   *
   */
  protected def display() {
    val c= System.console()
    //  clsConsole()
    dispTitle(c)
    (1 /: _choices) { (cnt, ch) => dispMItem(c, cnt, ch.desc()); cnt+1 }
    dispMItem(c, 99, if (_prev==null) "Quit" else "^Back")

    asInt(trim( getInput(c)), 0) match {
      case 99 => pop()
      case n:Int if (n >= 1 && n <= _choices.length) => _choices(n-1).onSelect()
      case _ => display()
    }
  }

  /**
   * @param i
   */
  def add(i:TMenuItem) {
    if (_ids.contains(i.id())) {
      errBadArg("Item(id) already exists.")
    }
    i.setParent(this)
    _ids += i.id()
    _choices += i
  }

  /**
   * @param i
   */
  def remove(i:TMenuItem) = {
    if (i != null) {
      _ids -= i.id()
      _choices -= i
    }
    this
  }

  /**
   * @return
   */
  def getItems() = _choices.toSeq

  private def dispTitle(c:Console) {
    if (c == null) {
      print("****************************************\n")
      println("%s %s".format("Menu:",_title) )
      print("****************************************\n")
    }
    else {
      c.printf("****************************************\n")
      c.printf("%s %s", "Menu:",_title)
      c.printf("****************************************\n")
    }
  }

  private def dispMItem(c:Console, pos:Int, desc:String) {
    val i= asJObj(pos)
    if (c == null) {
      println("%2d)  %s".format(i, desc) )
    } else {
      c.printf("%2d)  %s\n", i, desc)
    }
  }

  private def getInput(c:Console) = {
    if (c != null) { c.readLine() } else {
      new BufferedReader(new InputStreamReader(System.in)).readLine()
    }
  }

}
