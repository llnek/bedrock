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

package com.zotoh.bedrock.util

import com.zotoh.fwk.io.IOUte._
import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.util.MetaUte._
import com.zotoh.fwk.util.StrUte._

import java.io.{File,IOException}

import org.apache.commons.codec.binary.Base64
import org.json.JSONException
import org.json.{JSONObject=>JSNO}

import com.zotoh.fwk.crypto.BaseOfuscator
import com.zotoh.fwk.util.JSONUte
import com.zotoh.bedrock.core.Vars
import com.zotoh.bedrock.impl.DefaultDeviceFactory._

/**
 * Helper functions to read/write data from the device config file.
 *
 * @author kenl
 */
object MiscUte extends Vars {

  /**
   * @param top
   * @param dev
   * @return
   * @throws JSONException
   * @throws ClassNotFoundException
   */
  def userDevCZ(top:JSNO, dev:String) = {
    val cz= devFacs(top).optString(dev)
    if (!isEmpty(cz)) {
      Some(loadClass(cz))
    } else {
      None
    }
  }

  /**
   * @param appDir
   * @return
   * @throws IOException
   * @throws JSONException
   */
  def loadConf(appDir:File) = {
    val pf= new File(new File( appDir, CFG), APPCONF)
    JSONUte.read( readText(pf, "utf-8"))
  }

  /**
   * @param appDir
   * @param top
   * @throws IOException
   * @throws JSONException
   */
  def saveConf(appDir:File, top:JSNO) {
    val pf= new File(new File( appDir, CFG), APPCONF)
    writeFile(pf, JSONUte.asString(top), "utf-8")
  }

  /**
   * @param top
   * @return
   * @throws JSONException
   */
  def devFacs(top:JSNO) = {
    JSONUte.getAndSetObject(top, CFGKEY_DEVHDLRS)
  }

  /**
   * @param top
   * @return
   * @throws JSONException
   */
  def devs(top:JSNO) = {
    JSONUte.getAndSetObject(top, CFGKEY_DEVICES)
  }

  /**
   * @param top
   * @param dev
   * @return
   * @throws JSONException
   */
  def existsDevice(top:JSNO, dev:String) = {
    dev match {
      case DT_WEB_SERVLET => false
      case x:String if (listDefaultTypes().contains(x)) => true
      case x:String => existsUserDevice(top,x)
    }
  }

  /**
   * @param top
   * @param dev
   * @return
   * @throws JSONException
   */
  def existsUserDevice(top:JSNO, dev:String) = {
    val keys= devFacs(top).keys()
    var ok=false
    while (!ok && keys.hasNext()) {
      if (dev == trim( nsb(keys.next()))) {
        ok=true
      }
    }
    ok
  }

  /**
   * @param keyFile
   * @throws IOException
   */
  def maybeSetKey(keyFile:File) {
    maybeSetKey( if (keyFile.exists()) readText(keyFile) else "" )
  }

  /**
   * @param s
   */
  def maybeSetKey(original:String) {
//      CoreUte.tlog().debug("MiscUte: APP.KEY = {}", original)
    val s=trim(original)
    val bits = if ( !isEmpty(s)) {
      if (s.startsWith("B64:")) {
        Base64.decodeBase64(s.substring(4) )
      } else {
        asBytes(s)
      }
    } else { null }

    if (bits != null) {
      BaseOfuscator.setKey(bits)
    }
  }


}
