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


class SplitJoinJUT () extends TestBase {

  test("test Split Join") {
    val ps=create_props("com.zotoh.bedrock.wflow.TestSplitJoin")
    ps.put("_", this)
    _eng.start(ps)
    val s= _testOut.asInstanceOf[String]
    assert( s.indexOf("hello") >=0 && s.indexOf("world") >= 0)
  }
}


class TestSplitJoin(j:Job) extends Workflow(j) {
  val e= new PTask(new Work(){
    def eval(j:Job) {
      curStep().workflow().engine().quirks().get("_") match {
          case u:SplitJoinJUT =>
            u._testOut= nsb( j.slot("s1").get ) +  nsb( j.slot("s2").get )
            u._eng.shutdown()
          case _ =>
      }
    }
  })


  val s1= new PTask(new Work(){
    def eval(j:Job) {
      j.setSlot("s1","hello") ; Thread.sleep(1000)
    }
  })
  val s2= new PTask(new Work(){
    def eval(j:Job) {
      j.setSlot("s2","world"); Thread.sleep(2000)
    }
  })

  def onStart() = {
    new Split().addSplit(s1).addSplit(s2).withJoin(new  And()).chain(e) 
  }

}


