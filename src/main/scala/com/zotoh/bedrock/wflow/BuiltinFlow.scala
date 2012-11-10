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

import com.zotoh.fwk.net.HTTPStatus.{OK,FORBIDDEN}
import com.zotoh.fwk.util.StrUte._
import com.zotoh.fwk.util.StrArr

import com.zotoh.bedrock.device.{HttpEvent,HttpEventResult}
import com.zotoh.bedrock.core.Job

/**
 * Handles internal system events.
 * (Internal use only)
 *
 * @author kenl
 */
class BuiltinFlow(j:Job) extends Workflow(j) {

  override def onStart() = {
    val me=this
    val t3= PTask().withWork(new Work() {
      override def eval(j:Job) {
        me.eval_shutdown(j)
      }
    })
    val t2= Delay(3000L)
    val t1= PTask().withWork(new Work() {
      override def eval(j:Job) {
        me.do_shutdown(j)
      }
    })
    val t= new BoolExpr(){
      def eval(j:Job) = {
        j.event() match {
          case Some(e) => SHUTDOWN_DEVID == e.device().id()
          case _ => false
        }
        
    }}

    If(t, t3.chain(t2).chain(t1))
  }

  private def do_shutdown(j:Job) { j.engine().shutdown() }

  private def eval_shutdown(j:Job) {
    val ev = j.event() match {
      case Some(e) => e match { case x:HttpEvent => x ; case _ => null }
      case _ => null
    }
    val res= HttpEventResult()
    val a= ev.param("pwd") match {
      case Some(x) => x
      case _ =>
        ev.param("password") match { case Some(x) => x; case _ => null }
    }
    val w = if (a == null) "" else nsb(a.first())
    if (! j.engine().verifyShutdown(ev.uri(), w)) {
      tlog().warn("ShutdownTask: wrong password or uri, ignore shutdown request")
      res.setStatus(FORBIDDEN)
    } else {
      res.setStatus(OK)
    }
    ev.setResult(res)
  }
  
  
}

