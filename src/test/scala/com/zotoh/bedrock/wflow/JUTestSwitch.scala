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

package com.zotoh.bedrock.wflow

import java.util.{Properties=>JPS}
import java.io.File

import org.apache.commons.io.FileUtils

import com.zotoh.fwk.util.ProcessUte._
import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.util.StrUte._
import com.zotoh.fwk.io.IOUte
import com.zotoh.bedrock.core._
import com.zotoh.bedrock.wflow._

import org.scalatest.Assertions._
import org.scalatest._


class SwitchJUT () extends TestBase {

  test("test Switch (String)") {
    val ps=create_props("com.zotoh.bedrock.wflow.TestSwitchString")
    ps.put("_", this)
    _eng.start(ps)
    assert(_testOut == "hello")
  }
  
  test("test Switch (Int)") {
    val ps=create_props("com.zotoh.bedrock.wflow.TestSwitchInt")
    ps.put("_", this)
    _eng.start(ps)
    assert(_testOut == "world")
  }

  test("test Switch (Default)") {
    val ps=create_props("com.zotoh.bedrock.wflow.TestSwitchDft")
    ps.put("_", this)
    _eng.start(ps)
    assert(_testOut == "yoyoma")
  }
  
}


abstract class TestSwitchXXX(j:Job) extends Workflow(j) {
  val e= new PTask(new Work(){
    def eval(j:Job) {
      curStep().workflow().engine().quirks().get("_") match {
          case u:SwitchJUT =>
            u._testOut= j.slot("result").get
            u._eng.shutdown()
          case _ =>
      }
    }
  })

  val t1= new PTask(new Work(){
    def eval(j:Job) {
      j.setSlot("result","hello")
    }
  })

  val t2= new PTask(new Work(){
    def eval(j:Job) {
      j.setSlot("result","world")
    }
  })

  val t3= new PTask(new Work(){
    def eval(j:Job) {
      j.setSlot("result","yoyoma")
    }
  })

}

class TestSwitchString(j:Job) extends TestSwitchXXX(j) {
  val sw= new  Switch().
  withChoice("1", t1).
  withChoice("2", t2).
  withDef(t3).
  withExpr(new SwitchChoiceExpr() {
    def eval(j:Job) = {
      "1"
    }
  })

  def onStart() = sw.chain(e)
}

class TestSwitchInt(j:Job) extends TestSwitchXXX(j) {
  val sw= new  Switch().
  withChoice(1, t1).
  withChoice(2, t2).
  withDef(t3).
  withExpr(new SwitchChoiceExpr() {
    def eval(j:Job) = {
      2
    }
  })

  def onStart() = sw.chain(e)
}


class TestSwitchDft(j:Job) extends TestSwitchXXX(j) {
  
	val sw= new Switch()
			.withChoice("1", t1)
			.withChoice("2", t2)
			.withDef(t3)
	sw.withExpr(new SwitchChoiceExpr() {
			def eval(j:Job) = ""
	})
			
  def onStart() = sw.chain(e)
  
}

