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

package demo.async

import com.zotoh.bedrock.core.{AsyncCallback, Job}
import com.zotoh.bedrock.wflow._

/**
 * @author kenl
 *
 */
class Demo(job:Job) extends Workflow(job) {

  val task1= new Work() {
      override def eval(job:Job) {
          val t= new AsyncResumeToken( curStep() )

          println("/* Pretend to call a webservice which takes a long time (10secs),")
          println("- since the call is *async*, event loop is not blocked.")
          println("- When we get a *call-back*, then the normal processing will continue */")

          DemoAsyncWS.doLongAsyncCall(new AsyncCallback() {
              override def onSuccess(result:Option[Any]) {
                  println("CB: Got WS callback: onSuccess")
                  println("CB: Tell the scheduler to re-schedule the original process")
                  // use the token to tell framework to restart the idled process
                  t.resume(result)
              }
              override def onError(e:Exception) {
                  t.resume(Some(e))
              }
              override def onTimeout() {
                  onError( new Exception("time out"))
              }
          })

          println("\n\n")
          println("+ Just called the webservice, the process will be *idle* until")
          println("+ the websevice is done.")
          println("\n\n")

          setResult( new AsyncWait())
      }
  }

  val task2= new Work() {
      override def eval2(j:Job, arg:Option[Any])  {
          println("-> The result from WS is: " + arg)
          println("\nPRESS Ctrl-C anytime to end program.\n")
      }
      def eval(j:Job) {}
  }

  override def onStart() = PTask(task1).chain( PTask(task2))


}

class DemoPreamble(j:Job) extends Workflow(j) {
  override def onStart() = PTask( new Work() {
      override def eval(j:Job ) {
          println("Demo calling an async java-api & resuming.")
      }
  })
}

