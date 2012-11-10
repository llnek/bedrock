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

package com.zotoh.cloudapi.core


import scala.collection.JavaConversions._
import com.zotoh.fwk.util.StrUte._
import com.zotoh.fwk.util.{CoreImplicits}

import java.util.{Properties=>JPS,Comparator,TreeMap=>JTMap}

import org.dasein.cloud.compute.{VirtualMachineProduct=>VMP}
import org.json.JSONException
import org.json.{JSONObject=>JSNO}

import com.zotoh.cloudapi.aws.AWSAPI



object CloudAPI extends CoreImplicits {

  /**
   * @param vendor
   * @return
   */
  def configure(vendor:String) = {
    nsb(vendor).lc match {
      case "aws"|"amazon" => null
      case _ => null
    }
  }

  def sort(cs:Seq[VMP]) = {
    val m= new JTMap[VMP, VMP]( new AAA())
    cs.foreach { (p) => m.put(p,p) }
    m.values.toSeq
  }

}

sealed class AAA extends Comparator[VMP] {
  override def compare(o1:VMP, o2:VMP):Int = {
    var c1= o1.getCpuCount()
    var c2= o2.getCpuCount()
//      return  c1 == c2 ? 0 : ( (c1 > c2) ? 1 : -1 )
    if (c1 > c2) 1 else -1
  }

}

/**
 * @author kenl
 *
 */
trait CloudAPI extends Vars {

  /**
   * @param hint
   * @param target
   * @param props
   */
  //def setQuirks(hint:String, target:JSNO, pps:JPS):Unit

  /**
   * @param regions
   * @throws JSONException
   */
  def setRegionsAndZones(region:JSNO):Unit

  /**
   * @return
   */
  def listDatacenters(region:String):Seq[String]

  /**
   * @return
   */
  def listRegions():Seq[String]

  /**
   * @param bits 32 or 64
   * @return
   */
  def listProducts(bits:Int):Seq[VMP]

  /**
   * @param bits 32 or 64
   * @return
   */
  def listProductIds(bits:Int):Seq[String]

  /**
   * @param pid
   * @return
   */
  def findProduct(pid:String):VMP


}
