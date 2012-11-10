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

package com.zotoh.bedrock.core

import java.lang.System._

import com.zotoh.fwk.util.MetaUte._
import com.zotoh.bedrock.device.Device

object Module extends Vars  {
  /**
   * @return
   * @throws Exception
   */
  def pipelineModule() = {
    loadClass( getProperty(PIPLINE_MODULE)).getConstructor().
    newInstance() match {
      case m:Module => Some(m)
      case _ => None
    }
  }

}

/**
 * @author kenl
 *
 */
trait Module {

  /**
   * @return
   */
  def dftDelegateClass():String

  /**
   * @return
   */
  def shortName():String

  /**
   * @return
   */
  def name():String

  def onCreate(v:Device, sys:Boolean, j:Job):Unit

  def newPipeline(sz:String, j:Job):Option[Pipeline]

  def newPipeline():Option[Pipeline]

  def xrefCore(w:Runnable):String

  def xrefPID(w:Runnable):Long

  def bind(eng:AppEngine):Unit

}
