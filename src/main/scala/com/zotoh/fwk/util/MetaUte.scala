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

import java.lang.reflect.{Field,InvocationTargetException,Method,Modifier}
import com.zotoh.fwk.util.CoreUte._

import scala.collection.mutable.{ArrayBuffer,HashMap}

/**
 * Utility functions for class related or reflection related operations.
 *
 * @author kenl
 *
 */
object MetaUte {

  /**
   * @param z
   * @return
   */
  def forName(z:String) = Class.forName(z)

  /**
   * Get the classloader used by this object.
   *
   * @return
   */
  def getCZldr(cl:Option[ClassLoader]=None) = {
    cl match {
      case Some(c) => c
      case _ => Thread.currentThread().getContextClassLoader()
    }
  }

  /**
   *
   * @param clazz
   * @param ld
   * @return
   */
  def loadClass(clazz:String, cl:Option[ClassLoader]=None) = {
    getCZldr(cl).loadClass(clazz)
  }

  /**
   * Create an object of this class, calling the default constructor.
   *
   * @param clazz
   * @param ldr optional.
   * @return
   */
  def create(clazz:String, cl:Option[ClassLoader]=None):Any = {
    create( loadClass(clazz, cl))
  }

  /**
   * Create an object of this class, calling the default constructor.
   *
   * @param c
   * @return
   */
  def create(c:Class[_]):Any = {
    c.getDeclaredConstructor(null).newInstance(null)
  }

  /**
   * @param c
   * @return
   */
  def listParents(c:Class[_]) = {
    // since we always add the original class
    var a= collPars(c, Nil) match {
      case x :: tail if tail.length > 0 => tail
      case lst => lst
    }
    a.toSeq
  }

  /**
   * @param c
   * @return
   */
  def listMethods(c:Class[_]) = {
    collMtds(c, 0, Map[String,Method]()).values.toSeq
  }

  /**
   * @param c
   * @return
   */
  def listFields(c:Class[_]) = {
    collFlds(c, 0, Map[String,Field]() ).values.toSeq
  }

  private def collPars(c:Class[_], bin:List[Class[_]]):List[Class[_]] = {
    val par = c.getSuperclass()
    var rc= if (par != null) {
      collPars(par, bin)
    } else {
      bin
    }
    c :: rc
  }

  private def collFlds(c:Class[_], level:Int,
          bin:Map[String,Field]):Map[String,Field] = {

    val flds= c.getDeclaredFields()
    val par = c.getSuperclass()
    var m= if (par != null) { collFlds(par, level +1, bin) } else { bin }

    // we only want the inherited fields from parents
    (m /: flds) { (rc, f) =>
      val x= f.getModifiers()
      if (level > 0 &&
         (Modifier.isStatic(x) || Modifier.isPrivate(x)) ) rc  else {
         rc + (f.getName() -> f)
      }
    }

  }

  private def collMtds(c:Class[_], level:Int, bin:Map[String,Method]):Map[String,Method] = {
    val mtds= c.getDeclaredMethods()
    val par = c.getSuperclass()
    var mp = if (par != null) { collMtds(par, level +1, bin) } else { bin }

    // we only want the inherited methods from parents
    (mp /: mtds) { (rc,m) =>
      val x= m.getModifiers()
      if (level > 0 &&
        ( Modifier.isStatic(x) || Modifier.isPrivate(x))) rc else {
        rc + (m.getName() -> m)
      }
    }
  }

}

