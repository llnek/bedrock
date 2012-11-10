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

import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.util.LoggerFactory._
import com.zotoh.fwk.util.Logger
import com.zotoh.fwk.io.XData
import com.zotoh.fwk.util.StrUte._
import org.jboss.netty.channel.Channels.pipeline

import java.io.File
import java.io.IOException
import java.net.InetSocketAddress
import java.net.URI
import java.nio.ByteBuffer
import java.util.concurrent.Executors

import javax.net.ssl.SSLEngine
import java.lang.System._

import org.jboss.netty.bootstrap.ClientBootstrap
import org.jboss.netty.buffer.ByteBufferBackedChannelBuffer
import org.jboss.netty.channel.Channel
import org.jboss.netty.channel.ChannelFuture
import org.jboss.netty.channel.ChannelFutureListener
import org.jboss.netty.channel.ChannelPipeline
import org.jboss.netty.channel.ChannelPipelineFactory
import org.jboss.netty.channel.group.ChannelGroup
import org.jboss.netty.channel.group.DefaultChannelGroup
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory
import org.jboss.netty.handler.codec.http.DefaultHttpRequest
import org.jboss.netty.handler.codec.http.HttpClientCodec
import org.jboss.netty.handler.codec.http.HttpHeaders
import org.jboss.netty.handler.codec.http.HttpMethod
import org.jboss.netty.handler.codec.http.HttpRequest
import org.jboss.netty.handler.codec.http.HttpVersion
import org.jboss.netty.handler.ssl.SslHandler
import org.jboss.netty.handler.stream.ChunkedStream
import org.jboss.netty.handler.stream.ChunkedWriteHandler

object HttpClientr {
  
  def main(args:Array[String]) {
    try {
      val data= new XData(new File("/tmp/play.zip"))
      data.setDeleteFile(false)
      val c= HttpClientr()
      c.connect(new URI("http://localhost:8080/p.zip"))
      c.post(new BasicHttpMsgIO(){
        def onOK(code:Int, reason:String, resOut:XData) {
          println("COOL")
          c.wake()
        }
      }, data)
      c.block()
      c.destroy()
      exit(0)
    } catch {
      case e => e.printStackTrace()
    }
  }

}

/**
 * @author kenl
 *
 */
case class HttpClientr() {

  @transient private  var _log=getLogger(classOf[HttpClientr])
  def tlog() = _log

  private var _curScope:(URI,ChannelFuture) = _
  private var _boot:ClientBootstrap = _
  private var _chs:ChannelGroup = _
  private val _lock= new Object()

  iniz()

  /**
   *
   */
  def block()  {
    _lock.synchronized {
      try {
        _lock.wait()
      } catch { case _ => }
    }
  }

  /**
   *
   */
  def wake() {
    _lock.synchronized {
      try {
        _lock.notify()
      } catch { case _ => }
    }
  }

  /* (non-Javadoc)
   * @see java.lang.Object#finalize()
   */
  override def finalize() {
    if (_boot != null) { _boot.releaseExternalResources() }
    super.finalize()
  }

  /**
   * @param remote
   * @throws Exception
   */
  def connect(remote:URI) {
    val ssl= "https" == remote.getScheme()
    val host= remote.getHost()
    var port= remote.getPort()
    if (port < 0) { port = if(ssl) 443 else 80 }

    tlog().debug("HttpClientr: connecting to host: {}, port: {}", host, asJObj(port))
    inizPipeline(ssl)

    val cf= _boot.connect(new InetSocketAddress(host, port))
    // wait until the connection attempt succeeds or fails.
    cf.awaitUninterruptibly()

    if (cf.isSuccess()) {
      _curScope= (remote, cf)
      _chs.add(cf.getChannel())
    } else {
      onError(cf.getCause())
    }

    tlog().debug("HttpClientr: connected OK to host: {}, port: {}", host, asJObj(port))
  }

  /**
   * @param cfg
   * @param data
   * @throws IOException
   */
  def post(cfg:HttpMsgIO, data:XData) {
    tstObjArg("scope-data", _curScope)
    tstObjArg("payload-data", data)

    send( create_request(HttpMethod.POST) , cfg, data)
  }

  /**
   * @param cfg
   * @throws IOException
   */
  def get(cfg:HttpMsgIO) {
    tstObjArg("scope-data", _curScope)
    send( create_request(HttpMethod.GET), cfg, null)
  }

  private def send(req:HttpRequest, io:HttpMsgIO, data:XData) {

    tlog().debug("HttpClientr: {} {}",
      (if(data==null) "GET" else "POST"), _curScope._1)

    val clen= if(data==null) 0L else data.size()
    val cf= _curScope._2
    val uri= _curScope._1

    var cfg:HttpMsgIO = if (io == null) {
      new BasicHttpMsgIO() {
        def onOK(code:Int, reason:String, res:XData) {}
        //def onError(code:Int, reason:String) {}
      }
    } else io

    req.setHeader(HttpHeaders.Names.CONNECTION,
      if(cfg.keepAlive()) HttpHeaders.Values.KEEP_ALIVE else HttpHeaders.Values.CLOSE)

    req.setHeader(HttpHeaders.Names.HOST, uri.getHost())
    cfg.configMsg(req)

    if (data!= null && isEmpty( req.getHeader("content-type"))) {
      req.setHeader("content-type", "application/octet-stream")
    }

    tlog().debug("HttpClientr: content has length: {}", asJObj(clen))
    req.setHeader("content-length", clen.toString())

    val cc= cf.getChannel()
    val h= cc.getPipeline().get("handler").asInstanceOf[HttpResponseHdlr]
    h.bind(cfg)

    tlog().debug("HttpClientr: about to flush out request (headers)")

    var f= cc.write(req)
    f.addListener(new ChannelFutureListener() {
      def operationComplete(fff:ChannelFuture) {
        tlog().debug("HttpClientr: req headers flushed")
      }
    })

    if (clen > 0L) {
      if (clen > HttpUte.dftThreshold() ) {
        f=cc.write(new ChunkedStream( data.stream()))
      } else {
        f=cc.write( new ByteBufferBackedChannelBuffer( ByteBuffer.wrap(data.bytes()) ))
      }
      f.addListener(new ChannelFutureListener() {
        def operationComplete(fff:ChannelFuture) {
          tlog().debug("HttpClientr: req payload flushed")
        }
      })
    }

  }

  /**
   *
   */
  def destroy() {
    tlog().debug("HttpClientr: destory()")
    close()
    try { _boot.releaseExternalResources() } finally { _boot=null }
  }

  /**
   *
   */
  def close() {
    tlog().debug("HttpClientr: close()")
    if (_curScope != null) try {
      _chs.close()
    } finally {
      _curScope=null
    }
  }

  private def create_request(m:HttpMethod) = {
    val uri= _curScope._1
    new DefaultHttpRequest( HttpVersion.HTTP_1_1, m, uri.toASCIIString())
  }

  private def inizPipeline(ssl:Boolean) {

    _boot.setPipelineFactory(new ChannelPipelineFactory() {
      def getPipeline() = {
        val pl= org.jboss.netty.channel.Channels.pipeline()
        if (ssl) {
          val engine = HttpUte.clientSSL().createSSLEngine()
          engine.setUseClientMode(true)
          pl.addLast("ssl", new SslHandler(engine))
        }
        pl.addLast("codec", new HttpClientCodec())
//        pipe.addLast("inflater", new HttpContentDecompressor())
        //pipeline.addLast("aggregator", new HttpChunkAggregator(1048576))
        pl.addLast("chunker", new ChunkedWriteHandler())
        pl.addLast("handler", new HttpResponseHdlr(_chs))
        pl
      }
    })

  }

  private def onError(t:Throwable) {

    t match {
      case e:Exception => throw e
      case e =>
        throw new Exception( if(e==null) "Failed to connect" else e.getMessage())
    }

  }

  private def iniz() {
    _boot = new ClientBootstrap( new NioClientSocketChannelFactory (
        Executors.newCachedThreadPool(),
        Executors.newCachedThreadPool()))
    _boot.setOption("tcpNoDelay" , true)
    _boot.setOption("keepAlive", true)
    _chs= new DefaultChannelGroup(uid())
  }

}
