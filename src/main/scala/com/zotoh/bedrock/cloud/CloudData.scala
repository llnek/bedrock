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
 * g
 * SEE THE LICENSE FOR THE SPECIFIC LANGUAGE GOVERNING PERMISSIONS
 * AND LIMITATIONS UNDER THE LICENSE.
 *
 * You should have received a copy of the Apache License
 * along with this distribution; if not, you may obtain a copy of the
 * License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 ??*/

package com.zotoh.bedrock.cloud

import scala.collection.mutable.ArrayBuffer
import com.zotoh.fwk.io.IOUte._
import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.util.{CoreImplicits,JSONUte}
import com.zotoh.fwk.util.JSONUte._
import com.zotoh.fwk.util.StrUte._
import com.zotoh.cloudapi.core.Vars
import com.zotoh.fwk.crypto.PwdFactory

import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream

import _root_.org.json.JSONException
import _root_.org.json.{JSONObject=>JSNO}



/**
 * @author kenl
 *
 */
object CloudData extends  Vars with CoreImplicits {

  private var _cfg= new JSNO()
  private var _pathToFile=""

  /**
   * @param f
   * @return
   * @throws Exception
   */
  def setDataPath(f:File) { _pathToFile= niceFPath(f) }

  /**
   * @throws JSONException
   * @throws IOException
   */
  def save() {
    if (!isEmpty(_pathToFile) ) {
      writeFile( new File(_pathToFile), JSONUte.asString(_cfg))
    }
  }

  /**
   * @throws FileNotFoundException
   * @throws JSONException
   */
  def load() {
    using(open(new File(_pathToFile))) { (inp) =>
      _cfg= JSONUte.read(inp)
    }
  }

  /**
   * @return
   * @throws JSONException
   */
  def credential() = getAndSetObject(_cfg,P_CRED)

  /**
   * @param user
   * @param pwd
   * @param keyfile
   * @throws Exception
   */
  def setSSHInfo(user:String, pwd:String, keyfile:String) {
    val obj= SSHInfo(); obj.put(P_USER, nsb(user))
    var p= if (isEmpty(pwd)) "" else PwdFactory.mk(pwd).encoded()
    obj.put(P_PWD, p)
    p= if (isEmpty(keyfile)) "" else
      asFileUrl( niceFPath( new File(keyfile)) )
    obj.put(P_KEY, p)
  }

  /**
   * @param vendor
   * @param acct
   * @param id
   * @param pwd
   * @throws JSONException
   */
  def setCAuth(vendor:String, acct:String, id:String, pwd:String) {
    tstArg(vendor.eqic("aws") || vendor.eqic("amazon"), "Invalid cloud-vendor: " + vendor)
    tstEStrArg("cloud-account", acct)
//    tstEStrArg("cloud-id", id)
//    tstEStrArg("cloud-pwd", pwd)
    val p = if (isEmpty(pwd)) "" else PwdFactory.mk(pwd).encoded()
    val obj= credential()
    obj.put(P_VENDOR, "amazon")
    obj.put(P_ACCT, acct)
    obj.put(P_ID, nsb(id))
    obj.put(P_PWD, p)
    save()
  }

  /**
   * @return
   * @throws JSONException
   */
  def vendor() = nsb( credential().optString( P_VENDOR ))

  /**
   * @return
   * @throws JSONException
   */
  def isAWS() = {
    vendor().lc match {
      case "aws"|"amazon" => true
      case _ => false
    }
  }

  /**
   * @return
   * @throws JSONException
   */
  def SSHKeys() = getAndSetObject(_cfg,P_KEYS)

  /**
   * @return
   * @throws JSONException
   */
  def regions() = getAndSetObject(_cfg,P_REGIONS)

  /**
   * @param rgs
   * @throws JSONException
   */
  def setRegions(rgs:JSNO) {
    if (_cfg.has(P_REGIONS)) { _cfg.remove(P_REGIONS) }
    _cfg.put(P_REGIONS, rgs)
  }

  /**
   * @return
   * @throws JSONException
   */
  def listRegions() = {
    val rc= ArrayBuffer[String]()
    val it = regions().keys()
    while (it.hasNext() ) {
      rc += nsb( it.next() )
    }
    rc.toArray
  }

  /**
   * @param region
   * @return
   * @throws JSONException
   */
  def listZones(region:String) = {
    val zs=regions().optJSONObject(region)
    val rc= ArrayBuffer[String]()
    val it = if (zs == null) null else zs.keys()
    if (it != null) while (it.hasNext()) {
      rc +=  nsb(it.next())
    }
    rc.toArray
  }

  /**
   * @return
   * @throws JSONException
   */
  def firewalls() = getAndSetObject(_cfg,P_FWALLS)

  /**
   * @return
   * @throws JSONException
   */
  def ipAddrs() = getAndSetObject(_cfg,P_IPS)

  /**
   * @return
   * @throws JSONException
   */
  def servers() = getAndSetObject(_cfg,P_VMS)

  /**
   * @return
   * @throws JSONException
   */
  def images() = getAndSetObject(_cfg, P_IMAGES)

  /**
   * @return
   * @throws JSONException
   */
  def SSHInfo() = getAndSetObject(_cfg,P_SSHINFO)

  /**
   * @return
   * @throws JSONException
   */
  def dftRegion() = nsb ( defaults().optString(P_REGION) )

  /**
   * @param region
   * @throws JSONException
   */
  def setDftRegion(region:String) {
    defaults().put(P_REGION, trim(region))
  }

  /**
   * @return
   * @throws JSONException
   */
  def dftServer() = nsb ( defaults().optString(P_VM) )

  /**
   * @param vm
   * @throws JSONException
   */
  def setDftServer(vm:String) {
    defaults().put(P_VM, trim(vm))
  }

  /**
   * @return
   * @throws JSONException
   */
  def dftImage() = nsb ( defaults().optString(P_IMAGE) )

  /**
   * @param image
   * @throws JSONException
   */
  def setDftImage(image:String) {
    defaults().put(P_IMAGE, trim(image))
  }

  /**
   * @return
   * @throws JSONException
   */
  def dftKey() = nsb ( defaults().optString(P_KEY) )

  /**
   * @param key
   * @throws JSONException
   */
  def setDftKey(key:String) {
    defaults().put(P_KEY, trim(key))
  }

  /**
   * @return
   * @throws JSONException
   */
  def dftFirewall() = nsb ( defaults().optString(P_FWALL) )

  /**
   * @param fw
   * @throws JSONException
   */
  def setDftFirewall(fw:String) {
    defaults().put(P_FWALL, trim(fw))
  }

  /**
   * @return
   * @throws JSONException
   */
  def dftZone() = nsb ( defaults().optString(P_ZONE) )

  /**
   * @param z
   * @throws JSONException
   */
  def setDftZone(z:String) {
    defaults().put(P_ZONE, trim(z))
  }

  /**
   * @return
   * @throws JSONException
   */
  def dftProduct(arch:String) = {
    val obj= defaults().optJSONObject(P_PRODUCT)
    if( arch==null || obj==null) "" else nsb ( obj.optString(arch) )
  }

  /**
   * @param type
   * @throws JSONException
   */
  def setDftProduct(arch:String, ptype:String) {
    tstEStrArg("architecture-key", arch)
    tstEStrArg("architecture", ptype)
    getAndSetObject( defaults() ,P_PRODUCT).put(arch, ptype)
  }

  /**
   * @return
   * @throws JSONException
   */
  def customQuirks() = {
    getAndSetObject(_cfg,P_CUSTOM).
      optJSONObject( nsb ( credential().optString(P_VENDOR) ))
  }

  private def defaults() = getAndSetObject(_cfg,P_DFTS)

}

sealed class CloudData {}

