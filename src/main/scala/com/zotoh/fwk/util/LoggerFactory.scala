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

package com.zotoh.fwk.util

import scala.collection.JavaConversions._
import java.lang.reflect.Method

/**
 * @author kenl
 */
object LoggerFactory {

  private var NOTSKIPCHECK = true
  private var SLFJ:Class[_] = null
  
  init()
  
  private def init() = {
    if (SLFJ==null && NOTSKIPCHECK) try {
      SLFJ= Class.forName("org.slf4j.LoggerFactory")
    }
    catch {
      case _ => NOTSKIPCHECK=false
    }
  }

  def getLogger(z:Class[_]) = {
    init() 
    if (SLFJ==null || z==null) Logger.Dummy else {
        Logger( Some( SLFJ.getDeclaredMethod("getLogger",  z.getClass() ).
            invoke(null, z).asInstanceOf[ org.slf4j.Logger ] ))      
    }
  }

}

