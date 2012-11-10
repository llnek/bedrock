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

import scala.collection.mutable.{HashMap}

import java.io.{IOException,FileReader,LineNumberReader,PrintStream}

import com.zotoh.fwk.util.LoggerFactory._

import com.zotoh.fwk.util.StrUte._

object Win32Ini {  
}


/**
 * Config class that can parse a MS-Windows style .INI file.
 *
 * @author kenl
 *
 */
case class Win32Ini(private val _iniFile:String) extends CoreImplicits {

  private def ilog() { _log= getLogger(classOf[Win32Ini]) }
  @transient private var _log:Logger=null
  def tlog() = { if (_log==null) ilog(); _log  }

  private val serialVersionUID= -873895734543L
  private var _secs:Map[String, Section] = _

  parse(_iniFile)
  
  /**
   * @param section
   * @return
   */
  def section(sn:String): Option[ Section ] = {
    if (sn==null) None else ncFind(_secs,sn )
  }

  /**
   * @return
   */
  def sections() = _secs.keySet.toSeq

  /**
   * @param section
   * @param key
   * @return
   */
  def valueAsString(sn:String, key:String) = {
    section(sn) match {
      case Some(m) => m.get(key)
      case _ => None
    }
  }

  /**
   * @param section
   * @param key
   * @return
   */
  def valueAsInt(section:String, key:String) = {
    valueAsString(section, key) match {
      case Some(s) => Some( s.toInt )
      case _ => None
    }
  }

  /**
   * @param ps
   */
  def dbgShow(ps:PrintStream) {
    _secs.foreach { (t) =>
      ps.println("[" + t._1 + "]")
      t._2.foreach { (a) => ps.println(a._1 + "=" + a._2) }
    }
  }

  /**
   * @param iniFilePath
   */
  protected def parse(iniFilePath:String) {
    val rdr= new LineNumberReader(new FileReader(iniFilePath))
    val secs=HashMap[String,Section]()
    var kvs:Section= null
    val ex= () =>
      throw new IOException("Bad INI line: " + rdr.getLineNumber())
    var line=""
    var s=""
    do {
      line = rdr.readLine()
      if (line != null) {
        line.trim() match {
          case ln if isEmpty(ln) || ln.startsWith("#") =>
          case ln if (ln.matches("^\\[\\]$")) =>
            s = trim( trim(ln, "[]"))
            if (isEmpty(s)) { ex }
            ncFind(secs.toMap,s) match {
              case Some(x) => kvs=x
              case None =>
                kvs= Section()
                secs += Tuple2(s,kvs)
            }
          case ln if (kvs != null) =>
            var pos=ln.indexOf('=')
            if (pos>0) {
              s= ln.substring(0, pos).trim()
              if (isEmpty(s)) { ex}
              kvs += Tuple2(s, ln.substring(pos + 1).trim() )
            } else {
              ex
            }
        }
      }
    } while (line != null)

    secs.foreach { (t) =>
      _secs = _secs + Tuple2(t._1, t._2)
    }

  }

  private def ncFind(m:Map[String,Section], key:String) = {
    m.find((t) => key.eqic(t._1)) match {
      case Some(t) => Some(t._2)
      case _ => None
    }
  }

}

sealed case class Section() extends HashMap[String,String] {}

