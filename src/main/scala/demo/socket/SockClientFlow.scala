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

package demo.socket

import java.io.OutputStream
import java.net.Socket

import com.zotoh.bedrock.device.TcpIO
import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.util.ByteUte
import com.zotoh.bedrock.core.Job
import com.zotoh.bedrock.wflow._
import com.zotoh.fwk.net.NetUte


/**
 * @author kenl
 *
 */
class SockClientFlow(job:Job) extends Workflow(job) {

    val task1= new Work() {
        def eval(job:Job) {
            // opens a socket and write something back to parent process
            val tcp= engine().deviceMgr().device("server").get.asInstanceOf[TcpIO]
            val host=tcp.host()
            val port= tcp.port()
            val msg= "Hello World!"
            val bits= asBytes(msg)
            println("TCP Client: about to send message" + msg )

            using(new Socket( NetUte.hostByName(host), port))
            { (soc) =>
                val os= soc.getOutputStream()
                os.write(ByteUte.readAsBytes(bits.length))
                os.write(bits)
                os.flush()
            }
        }
    }

    override def onStart() = {
        new Delay(3000).chain(new PTask(task1))
    }


}

