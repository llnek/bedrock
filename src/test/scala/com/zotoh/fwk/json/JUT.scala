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

package com.zotoh.fwk.json


import org.json.JSONArray
import org.json.JSONObject

import com.zotoh.fwk.util.JSONUte

import com.zotoh.fwk.util.Consts

import org.scalatest.Assertions._
import org.scalatest._


class FwkJsonJUT  extends FunSuite with BeforeAndAfterEach with BeforeAndAfterAll with Consts  {

  override def beforeAll(configMap: Map[String, Any]) {
  }

  override def afterAll(configMap: Map[String, Any]) {
  }

  override def beforeEach() { }

  override def afterEach() { }

  test("testFromString") {
    val top= JSONUte.read( jsonStr())
    assert(top != null)
    expect("hello")( top.getString("a"))
    expect("world")( top.getString("b"))
    var a= top.getJSONArray("c")
    assert(a != null)
    expect(a.length())(2)
    expect(a.get(0))(true)
    expect(a.get(1))(false)
    var obj= top.getJSONObject("d")
    assert(obj != null)
  }

  test("testToString") {
    val top= JSONUte.read( jsonStr())
    var s= JSONUte.asString(top)
    assert(s != null && s.length() > 0)
  }

  private def jsonStr()  = {
    "{" +
    "a : \"hello\"," +
    "b : \"world\"," +
    "c : [true,false]," +
    "d : {} " +
    "}"
  }
}

