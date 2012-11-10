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


package com.zotoh.bedrock.device.netty

import scala.collection.mutable.HashMap
import scala.collection.JavaConversions._

import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.util.MetaUte._
import com.zotoh.fwk.util.StrUte._

import java.lang.reflect.{Constructor=>Ctor}
import java.util.{Properties=>JPS,ResourceBundle,LinkedHashMap=>JLHM}

import org.jboss.netty.channel.SimpleChannelHandler

import org.json.{JSONObject=>JSNO,JSONArray=>JSNA}

import com.zotoh.fwk.util.{CmdLineMust,CmdLineQ,CmdLineSeq}
import com.zotoh.bedrock.core.Job
import com.zotoh.bedrock.core.Pipeline
import com.zotoh.bedrock.device.DeviceMgr
import com.zotoh.bedrock.device.HttpEvent
import com.zotoh.bedrock.device.RESTEvent

/**
 * A Http IO device but specific to handling RESTful events.
 *
 * The set of properties:
 *
 * <b>contextpath</b>
 * The application context path, default is /.
 * <b>resources</b>
 * A map of resources as path components.  Each resource is a map of name value pairs.
 * -----> <b>rpath</b> - resource path (regular expression)
 * -----> <b>processor</b> - the class name of the processor responsible for this resource.
 *
 * @see com.zotoh.bedrock.device.NettyIOTrait
 *
 * @author kenl
 *
 */
/**
 * @author kenl
 *
 */
class RestIO(devMgr:DeviceMgr) extends NettpIO(devMgr) {

  private val _resmap= new JLHM[String,String]()
  private val _pmap= HashMap[String,Ctor[_]]()
  private var _context=""

  /**
   * @return
   */
  def context() = _context

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.NettyIOTrait#onStop()
   */
  override def onStop() {
    try { super.onStop() } finally {  _pmap.clear() }
  }

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.BaseHttpIO#inizWithProperties(org.json.JSONObject)
   */
  override def inizWithQuirks(pps:JSNO) {
    super.inizWithQuirks(pps)
    val a=pps.optJSONArray("resources")
    if (a != null) for ( i <- 0 until a.length) {
      val obj=a.optJSONObject(i)
      if (obj!=null) {
        val h=trim(obj.optString("processor"))
        val p=trim(obj.optString("path"))
        //    tstEStrArg("resource-processor", h)
        tstEStrArg("resource-path", p)
        _resmap += Tuple2(p,h)
      }
    }
    _context= trim( pps.optString("contextpath"))
  }

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.NettpIO#getHandler()
   */
  def handler() = new NettpReqHdlr(this)

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.NettpIO#createEvent()
   */
  override def createEvent() = new RESTEvent(this)

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.Device#getPipeline(com.zotoh.bedrock.core.Job)
   */
  override def pipeline(j:Job) = {
    val v= j.event().asInstanceOf[RESTEvent]
    var error=false
    var u=v.uri()

    if (!isEmpty(_context)) {
      if ( !u.startsWith(_context)) {
        error=true
      } else {
        u= u.substring(_context.length())
      }
    }

    if (error) None else {

      // exact match ?
      if ( _resmap.isDefinedAt(u)) {
        newProc( _resmap(u), j)
      } else {
        // search for the 1st matching pattern
        _resmap.find { (t) => u.matches(t._1) } match {
          case Some(x) => newProc( x._2,j)
          case _ =>
            super.pipeline(j)
        }
      }
    }

  }

  private def newProc(c:String, j:Job):Option[Pipeline] = {

    if (isEmpty(c)) None else {
      try {
        Some(maybeFindClass(c).
          newInstance(j).
          asInstanceOf[Pipeline] )
      } catch {
        case e => tlog().warnX("",Some(e)); None
      }
    }
  }

  private def maybeFindClass(c:String) = {
    _pmap(c) match {
      case x:Ctor[_] =>  x
      case _ =>
        val z= loadClass(c).getConstructor(classOf[Job])
        _pmap += Tuple2(c,z)
        z
    }
  }

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.HttpIOTrait#supportsConfigMenu()
   */
  override def supportsConfigMenu() = true

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.HttpIOTrait#getCmdSeq(java.util.ResourceBundle, java.util.Properties)
   */
  override def getCmdSeq(rcb:ResourceBundle, pps:JPS) = {
    pps.put("resources", HashMap[String,String]())

    val q3= new CmdLineMust("resproc", bundleStr(rcb, "cmd.rest.resproc")) {
      def onRespSetOut(a:String, p:JPS) = {
        val m = p.get("resources").asInstanceOf[HashMap[String,String]]
        val uri= p.remove("resuri").asInstanceOf[String]
        m.put(uri, a)
        "resptr"
      }}
    val q2= new CmdLineMust("resptr", bundleStr(rcb,"cmd.rest.resptr")) {
      def onRespSetOut(a:String, p:JPS) = {
        if (isEmpty(a)) { "" } else {
          p.put("resuri", a); "resproc"
        }
      }}
    val q1= new CmdLineQ("ctx", bundleStr(rcb, "cmd.http.ctx")) {
      def onRespSetOut(a:String, p:JPS) = {
        p.put("contextpath", a)
        "resptr"
      }}
    Some(new CmdLineSeq(super.getCmdSeq(rcb, pps), Array(q1,q2,q3)) {
      def onStart() = q1.label()
    })
  }

}
