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

import com.zotoh.fwk.io.IOUte._
import com.zotoh.fwk.util.LoggerFactory.getLogger
import com.zotoh.fwk.util.Logger
import com.zotoh.fwk.util.StrUte._
import com.zotoh.fwk.net.HttpUte._

import java.io.File
import java.net.MalformedURLException
import java.net.URI
import java.net.URISyntaxException

import com.zotoh.fwk.io.XData
import com.zotoh.fwk.util.CoreUte._

object SimpleHttpSender  {
  /**
   * @param args
   */
  def main(args:Array[String])  {
    try {
      new SimpleHttpSender().start(args)
    } catch {
      case e => e.printStackTrace()
    }
  }
}

/**
 * @author kenl
 *
 */
class SimpleHttpSender  {

  @transient private var _log=getLogger(classOf[SimpleHttpSender])
  def tlog() = _log

  private var _client:HttpClientr = _
  private var _doc=""
  private var _url=""

  private def start(args:Array[String]) {

    if ( !parseArgs(args)) usage() else {
      _client= send(new BasicHttpMsgIO() {
        def onOK(code:Int, r:String, res:XData) {
          try {
            println("Response Status Code: " +  code)
            println("Response Data: " + nsn(res))
          } catch {
            case _ =>
          }
        }
        override def onError(code:Int, r:String) {
          println("Error: code =" + code + ", reason=" + r)
        }
      })
      _client.block()
    }
  }

  private def send(cb:HttpMsgIO) = {
    val d:XData  = _doc match {
      case s:String => using(open(new File(s)))  { (inp) => readBytes(inp) }
      case _ => null
    }
    d match {
      case s:XData => simplePOST( new URI(_url), s, cb)
      case _ => simpleGET( new URI(_url), cb)
    }
  }

  private def usage()  {

    println("HttpSender  <URL> [ <docfile> ]")
    println("e.g.")
    println("HttpSender http://localhost:8080/SomeUri?x=y ")
    println("")
    println("")
    println("")

  }

  private def parseArgs(args:Array[String]) = {

    if (args.length < 1) false else {
      _url= args(0)
      if (args.length > 1) {
        _doc=args(1)
      }
      true
    }

  }

}
