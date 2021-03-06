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

import java.io.{File,InputStream,IOException}
import java.util.{Properties=>JPS}
import org.json.{JSONObject=>JSNO,JSONArray=>JSNA,JSONTokener=>JSNTkr}
import com.zotoh.fwk.io.IOUte._
import com.zotoh.fwk.util.CoreUte._

/**
 * Utility functions related to JSON objects/strings.  The JSNO source code is from json.org
 *
 */
object JSONUte {

  /**
   * @param root
   * @return
   */
  def asString(root:JSNO) = if(root==null) "" else root.toString(2)

  /**
   * @param obj
   * @param key
   * @return
   */
  def optObject(obj:JSNO, key:String) = if(obj==null) null else obj.optJSONObject(key)

  /**
   * @param obj
   * @param key
   * @return
   */
  def optArray(obj:JSNO, key:String) = if (obj==null) null else obj.optJSONArray(key)

  /**
   * @param arr
   * @param pos
   * @return
   */
  def optObject(arr:JSNA, pos:Int) = if (arr==null) null else arr.optJSONObject(pos)

  /**
   * @param obj
   * @param key
   * @return
   */
  def optString(obj:JSNO, key:String) = if (obj==null) null else obj.optString(key)

  /**
   * @param obj
   * @param key
   * @return
   */
  def optBoolean(obj:JSNO, key:String) = if (obj==null) false else obj.optBoolean(key)

  /**
   * @param obj
   * @param key
   * @return
   */
  def optInt(obj:JSNO, key:String) = if (obj==null) 0 else obj.optInt(key)

  /**
   * @param j
   * @param fld
   * @param value
   */
  def addString(j:JSNO, fld:String, value:String) {
    if (j != null && fld != null && value != null) {
      j.put(fld, value)
    }
  }

  /**
   * @param j
   * @param fld
   * @param b
   */
  def addString(j:JSNO, fld:String, b:Boolean) {
    if (j != null && fld != null)     {
      j.put(fld, b.toString())
    }
  }

  /**
   * @param j
   * @param fld
   * @param n
   */
  def addString(j:JSNO, fld:String, n:Int) {
    if (j != null && fld != null)    {
      j.put(fld, n.toString())
    }
  }

  /**
   * @param j
   * @param fld
   * @return
   */
  def getAndSetArray(j:JSNO, fld:String) = {
    j.opt(fld) match {
      case x:JSNA =>
      case _ => j.remove(fld); j.put(fld, new JSNA)
    }
    j.optJSONArray(fld)
  }

  /**
   * @param j
   * @param fld
   * @return
   */
  def getAndSetObject(j:JSNO, fld:String) = {
    j.opt(fld) match {
      case x:JSNO =>
      case _ => j.remove(fld); j.put(fld, new JSNO)
    }
    j.optJSONObject(fld)
  }

  /**
   * @param j
   * @param fld
   * @param obj
   */
  def addObject(j:JSNO, fld:String, obj:JSNO ) {
    if (j != null && fld != null  && obj != null) {
      j.put(fld, obj)
    }
  }


  /**
   * @param r
   * @param obj
   */
  def addItem(r:JSNA, obj:JSNO) {
    if (r != null  && obj != null) { r.put(obj) }
  }

  /**
   * @param json
   * @return
   */
  def read(inp:InputStream) = new JSNO( new JSNTkr(inp))

  /**
   * @param json
   * @return
   */
  def read(f:File) = {
    using(open(f)) { (inp) =>
      new JSNO( new JSNTkr(inp))
    }
  }

  /**
   * @param json
   * @return
   * @throws JSONException
   */
  def read(js:String) = new JSNO( new JSNTkr(js))

  /**
   * @return
   */
  def newJSON() = new JSNO

  def newJSON(m:java.util.Map[_,_]) = new JSNO(m)

  def newJSA(c:java.util.Collection[_]) = new JSNA(c)

  def newJSA() = new JSNA

}

