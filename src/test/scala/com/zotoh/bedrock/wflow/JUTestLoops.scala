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


class TestLoopsJUT () extends TestBase {

  test("test WHILE") {
    val ps=create_props("com.zotoh.bedrock.wflow.TestWhile")
    ps.put("_", this)
    _eng.start(ps)
    assert( _testOut == 5 )
  }

  test("test IF (t)") {
    val ps=create_props("com.zotoh.bedrock.wflow.TestIF")
    ps.put("_", this)
    _eng.start(ps)
    assert( nsb(_testOut)== "hello" )
  }
  
  test("test IF (f)") {
    val ps=create_props("com.zotoh.bedrock.wflow.TestIFNOT")
    ps.put("_", this)
    _eng.start(ps)
    assert( nsb(_testOut)== "" )
  }

  test("test FOR") {
    val ps=create_props("com.zotoh.bedrock.wflow.TestForLoop")
    ps.put("_", this)
    _eng.start(ps)
    assert(_testOut.asInstanceOf[StringBuilder].toString() == "aaaaa")
  }
  
}


class TestWhile(j:Job) extends Workflow(j) {
  val e= new PTask(new Work(){
    def eval(j:Job) {
      curStep().workflow().engine().quirks().get("_") match {
          case u:TestLoopsJUT =>
            val a= j.slot("count").get.asInstanceOf[ Array[Int] ]
            u._testOut = a(0)
            u._eng.shutdown()
          case _ =>
      }
    }
  })

  val t= new PTask(new Work(){
      def eval(j:Job) {
        val a= Array[Int](1)
        a(0)=0
        j.setSlot("count", a)
      }
  })
  val b= new PTask(new Work(){
      def eval(j:Job) {
        val a=j.slot("count").get.asInstanceOf[ Array[Int] ]
        a(0)= a(0) + 1
      }
  })

  def onStart() = {
    val w= new While().withBody(b)
    w.withExpr(new BoolExpr(){
      def eval(j:Job) = {
        val a= j.slot("count").get.asInstanceOf[ Array[Int] ]
        a(0) < 5
      }
    })
    t.chain(w).chain(e)
  }

}

class TestIF(j:Job) extends Workflow(j) {
  val e= new PTask(new Work(){
    def eval(j:Job) {
      curStep().workflow().engine().quirks().get("_") match {
          case u:TestLoopsJUT =>
            u._testOut = nsb( j.slot("result").get )
            u._eng.shutdown()
          case _ =>
      }
    }
  })

  val t=	 new PTask(new Work() {
					def eval(j:Job) {
						j.setSlot("result", "hello")
				}})
  
  def onStart() = {
    		new If(	new BoolExpr() {
					def eval(j:Job) = {
						true
					}},
					t ).chain(e)							
    
  }

}

class TestIFNOT(j:Job) extends Workflow(j) {
  val e= new PTask(new Work(){
    def eval(j:Job) {
      curStep().workflow().engine().quirks().get("_") match {
          case u:TestLoopsJUT =>
            u._testOut = if ( j.slot("result").isEmpty ) "" else "error!"
            u._eng.shutdown()
          case _ =>
      }
    }
  })

  val t=	 new PTask(new Work() {
					def eval(j:Job) {
						j.setSlot("result", "hello")
				}})
  
  def onStart() = {
    		new If(	new BoolExpr() {
					def eval(j:Job) = {
						false
					}},
					t ).chain(e)							
    
  }

}


class TestForLoop(j:Job) extends Workflow(j) {
  val e= new PTask(new Work(){
    def eval(j:Job) {
      curStep().workflow().engine().quirks().get("_") match {
          case u:TestLoopsJUT =>
            u._testOut = j.slot("result").get
            u._eng.shutdown()
          case _ =>
      }
    }
  })

  val t=	 new PTask(new Work() {
					def eval(j:Job) {
					  val a = j.slot("result").get.asInstanceOf[StringBuilder]
						a.append("a")
				}})
  val a=	 new PTask(new Work() {
					def eval(j:Job) {
					  j.setSlot("result", new StringBuilder)
				}})
  
  def onStart() = {
    		val f=new For(t)
    		f.withLoopCount( new ForLoopCountExpr() {
    			def eval(j:Job) = 5
    		})
    		a.chain(f).chain(e)
  }

}
