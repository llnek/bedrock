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

package com.zotoh.fwk.net

import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.net.InetAddress._
import java.net.{InetAddress,ServerSocket,Socket}

import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.RejectedExecutionHandler
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

import com.zotoh.fwk.util.LoggerFactory._
import com.zotoh.fwk.util.Logger
import com.zotoh.fwk.net.NetUte._

import com.zotoh.fwk.io.IOUte._
import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.util.StrUte._

/**
 * @author kenl
 *
 */
class SimpleFileServer {

  @transient private var _log=getLogger(classOf[SimpleFileServer])
  def tlog() = _log

  private var _ip:InetAddress = _
  private var _port= -1
  private var _engine:Td= _

  /**
   * @param ipAddr
   * @param ipv6
   * @param port
   * @throws UnknownHostException
   * @throws IOException
   */
  def this(ipAddr:String, ipv6:Boolean, port:Int) {
    this()

    tstEStrArg("ip-address", ipAddr)
    tstPosIntArg("port", port)

    _ip= if(isEmpty(ipAddr)) getLocalHost() else
        getByAddress( ipv4AsBytes(ipAddr))
    _port=port
  }


  /**
   * @param host
   * @param port
   * @throws UnknownHostException
   */
  def this(host:String, port:Int) {
    this()

    tstPosIntArg("port", port)
    //tstEStrArg("host", host)

    _ip= getByName(host)
    _port=port
  }


  /**
   * @param port
   * @throws UnknownHostException
   */
  def this(port:Int) {
    this("", port)
  }


  /**
   * @throws IOException
   */
  def start() {
    _engine= new Td(new ServerSocket(_port, 0, _ip) )
    _engine.start()
  }


  /**
   *
   */
  def stop() {
    _engine.halt()
  }



}

/**
 * @author kenl
 *
 */
sealed class Td(private  var _ssoc:ServerSocket) extends Thread  with RejectedExecutionHandler {

  @transient private var _log=getLogger(classOf[Td])
  def tlog() = _log

  setDaemon(true)

  /* (non-Javadoc)
   * @see java.util.concurrent.RejectedExecutionHandler#rejectedExecution(java.lang.Runnable, java.util.concurrent.ThreadPoolExecutor)
   */
  def rejectedExecution(r:Runnable, e:ThreadPoolExecutor)    {
    //TODO: better print out
    tlog().warn("Threadpool: rejectedExecution!")
  }

  /**
   *
   */
  protected[net] def halt() {
    NetUte.close(_ssoc)
    _ssoc= null
  }

  /* (non-Javadoc)
   * @see java.lang.Thread#run()
   */
  override def run()    {
    val pe= new ThreadPoolExecutor(4, 8, 5000, TimeUnit.MILLISECONDS,
        new LinkedBlockingQueue[Runnable](),
        this)
    var stopped=false

    while ( ! stopped) {
      try {
        pe.execute( new FileServerHandler( _ssoc, _ssoc.accept() ))
      } catch {
        case _ => NetUte.close(_ssoc); stopped=true
      }
    }

    tlog().debug("FileServer: halted")
    return
  }

}


/**
 * @author kenl
 *
 */
sealed class FileServerHandler(private val  _ssoc:ServerSocket, private val _soc:Socket) extends Runnable {

  @transient private var _log=getLogger(classOf[FileServerHandler])
  def tlog() = _log

  /* (non-Javadoc)
   * @see java.lang.Runnable#run()
   */
  def run()  {
    var halt=false
    try     {
      val rdr  = new BufferedReader(new InputStreamReader(_soc.getInputStream()))
      var stopped= false
      tlog().debug("FileServerHandler: run started")
      while ( ! stopped)  {
        val s=rdr.readLine()
        var cmd=""
        var p1=""
        if (s != null) {
          val pos= indexOfAnyChar(s, " \t".toCharArray())
          cmd = if (pos >= 0) s.substring(0,pos) else ""
          p1= if (pos >=0) s.substring(pos+1) else ""
          cmd=trim(cmd)
          p1=trim(p1)
        }

        tlog().debug("FileServer: cmd= {}, fp={}", cmd, p1)

        if ("rcp" == cmd) {
          // command expected: rcp <SPACE> filepath
          val fp = new File(p1)
          var len= if ( fp.exists() && fp.canRead()) fp.length() else 0L
          val out = new DataOutputStream(_soc.getOutputStream())
          using(new FileInputStream(fp)) { (fin) =>
            tlog().debug("FileServer: clen= {}", asJObj(len))
              out.writeLong(len)
              out.flush();
              copy(fin, out)
          }
          tlog().debug("FileServer: file served")
        }
        else
        if ("rrm" == cmd)  {
          val fp= new File(p1)
          if ( ! fp.exists()) {
            tlog().debug("File doesn't exist: {}", fp.getCanonicalPath())
          }
          else  {
            val ok= fp.delete()
            tlog().debug("File: {} {}", fp.getCanonicalPath() ,
                if(ok) "deleted" else "not deleted")
          }
        }
        else
        if ("stop" == cmd) {
          stopped=true
          halt=true
        }
        else {
          tlog().debug("FileServerHandler: stopped")
          stopped=true
        }
      }
    } catch {
      case _ =>
    }
    finally {
      NetUte.close(_soc)
    }

    if (halt) { NetUte.close(_ssoc) }


  }




}
