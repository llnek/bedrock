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

package com.zotoh.bedrock.device

import scala.collection.mutable.ArrayBuffer

import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.util.StrUte._

import java.util.{Properties=>JPS,ResourceBundle}
import java.net.{URI,URL}
import org.json.{JSONArray=>JSNA,JSONObject=>JSNO}

import com.sun.syndication.io.SyndFeedInput
import com.sun.syndication.io.XmlReader
import com.zotoh.fwk.util.{CmdLineMust,CmdLineQ,CmdLineSeq}

object FeedIO {
  val PSTR_CHECK="validate"
  val PSTR_URLS="urls"
}

/*
 * A device which reads data from a set of RSS Atom feeds.
 *
 * The set of properties:
 *
 * <b>urls</b>
 * The set of remote RSS Feed URLs.
 *
 * @see com.zotoh.bedrock.device.RepeatingTimer
 *
 * @author kenl
 */
class FeedIO(devMgr:DeviceMgr) extends ThreadedTimer(devMgr) {

  private val _urls= ArrayBuffer[URI]()
  private var _validate=false

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.RepeatingTimer#inizWithProperties(org.json.JSONObject)
   */
  override def inizWithQuirks(pps:JSNO) {
    super.inizWithQuirks(pps)
    val check=pps.optBoolean(FeedIO.PSTR_CHECK)
    val a=pps.optJSONArray(FeedIO.PSTR_URLS)
    if (a!=null) for ( i <- 0 until a.length()) {
      val s=trim(a.optString(i))
      if ( ! isEmpty(s)) {
        _urls += new URI(s)
      }
    }
    _validate=check
  }

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.ThreadedTimer#preLoop()
   */
  override def preLoop() {}

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.ThreadedTimer#endLoop()
   */
  override def endLoop() {}

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.ThreadedTimer#onOneLoop()
   */
  override def onOneLoop() {
    val input = new SyndFeedInput(_validate)
    val me=this
    _urls.foreach { (u) =>
      dispatch( new FeedEvent(me,
          u.toASCIIString(),
          input.build(new XmlReader(u.toURL()))))
    }
  }

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.RepeatingTimer#supportsConfigMenu()
   */
  override def supportsConfigMenu() = true

  override def getCmdSeq(rcb:ResourceBundle, pps:JPS) = {
    pps.put( FeedIO.PSTR_URLS , ArrayBuffer[String]())
    val q1= new CmdLineMust("url", bundleStr(rcb, "cmd.feed.url")) {
      def onRespSetOut(a:String, p:JPS) = {
        if (isEmpty(a)) "" else {
          p.get( FeedIO.PSTR_URLS ).asInstanceOf[ArrayBuffer[String]] += a
          label()
      }}}
    Some(new CmdLineSeq(  super.getCmdSeq(rcb, pps), Array(q1)) {
      def onStart() = q1.label()
    })

  }

}
