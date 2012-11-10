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

package demo.fork

import com.zotoh.bedrock.core.Job
import com.zotoh.bedrock.wflow._

/**
 * @author kenl
 *
    parent(s1) --> split&nowait
                   |-------------> child(s1)----> split&wait --> grand-child
                   |                              |                    |
                   |                              |<-------------------+
                   |                              |---> child(s2) -------> end
                   |
                   |-------> parent(s2)----> end
 */
class Demo(job:Job) extends Workflow(job) {

  val parent1= new Work() {
      override def eval(job:Job) {
          println("Hi, I am the parent")
      }
  }

  val parent2= new Work() {
      override def eval(job:Job) {
        def fib(n:Int):Int = {
            if (n <3) 1 else { fib(n-2) + fib(n-1) }
        }
        println("Parent: after fork, continue to calculate fib(6)...")
        print("Parent: ")
        for (i <- 1 to 6) {
            print( fib(i) + " ")
        }
        println()
      }
  }

  val gchild= new Work() {
      override def eval(job:Job) {
        println("Grand-child: taking some time to do this task... ( ~ 6secs)")
        for (i <- 1 to 6) {
          Thread.sleep(1000)
          print("...")
        }
        println("")
        job.setSlot("result",
          job.slot("rhs").asInstanceOf[Int] *
          job.slot("lhs").asInstanceOf[Int]
        )
      }
  }

  val child=new Work() {
      override def eval(job:Job) {
          println("Child: I am a child, will create my own child (blocking)")
          job.setSlot("rhs", 60)
          job.setSlot("lhs", 5)
          val p2= PTask(new Work() {
              override def eval(job:Job) {
                  println("Child: the result for (5 * 60) according to my own child is = "  +
                          job.slot("result"))
                  println("\nPRESS Ctrl-C anytime to end program.\n")
              }
          })
          val a= new Split(
                  // split & wait
                  new And().withBody(p2)
          )
          .addSplit(new PTask(gchild))

          setResult(a)
      }
    }

    // split but no wait
    // parent continues;
    override def onStart() = PTask(parent1).chain(
        new Split().addSplit( PTask(child) )).chain( PTask(parent2) )

}

class DemoPreamble(j:Job) extends Workflow(j) {

    override def onStart() = PTask().withWork( new Work() {
            override def eval(j:Job ) {
                    println("Demo fork(split)/join of tasks..." )
            }
        })
}



