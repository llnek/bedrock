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

import scala.math._

import java.io.{File,FileInputStream,FileOutputStream,InputStream,OutputStream,IOException}
import org.apache.commons.io.FileUtils

import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.io.IOUte._
import com.zotoh.fwk.util.StrUte._

/**
 * @author kenl
 *
 */
object FileUte {

  /**
   * @param fp
   * @return
   */
  def isFileWRX(fp:File) = {
    fp != null && fp.exists() && fp.isFile() && fp.canRead() && fp.canWrite()
  }

  def isFileRX(fp:File) = {
    fp != null && fp.exists() && fp.isFile() && fp.canRead()
  }

  /**
   * @param dir
   * @return
   */
  def isDirWRX(dir:File, exec:Boolean=false) = {
    dir != null  && dir.exists() && dir.isDirectory() && 
        dir.canRead() && dir.canWrite() &&
        (if(exec) dir.canExecute() else true) 
  }

  def isDirRX(fp:File) = {
    fp != null && fp.exists() && fp.isDirectory() && fp.canRead()
  }

  /**
   * @param src
   * @param desDir
   * @param createDir
   * @return
   */
  def moveFileToDir(src:File, desDir:File, createDir:Boolean) = {
    if ( !desDir.exists() && createDir) {
      desDir.mkdirs()
    }
    if ( ! desDir.exists() || ! desDir.isDirectory() ) {
      throw new IOException("\"" + desDir + "\" does not exist, or not a directory")
    }
    moveFile(src, new File(desDir, src.getName()) )
  }

  /**
   * @param src
   * @param des
   * @return
   */
  def moveFile(src:File, des:File) = {
    if ( ! src.exists() || ! src.isFile()) {
      throw new IOException("\"" + src + "\" does not exist or not a valid file")
    }
    if ( des.exists() ) {
      throw new IOException("\"" + des + "\" already exists")
    }
    if ( ! src.renameTo(des)) {
      copyFile( src, des)
      if (!src.delete()) {
        FileUte.delete(des)
        throw new IOException("Failed to delete original file \"" + src+ "\"")
      }
    }
    des
  }

  /**
   * @return Current working directory.
   */
  def cwd() =  new File(sysQuirk("user.dir"))

  /**
   * @param src
   * @param des
   */
  def copyFile(src:File, des:File) {
    if (src==des || src.getCanonicalPath() == des.getCanonicalPath()) {
    } else if ( !isFileRX(src)) {
      throw new IOException("\"" + src + "\" does not exist or not a valid file")
    } else if ( ! new File( des.getParent()).mkdirs() ) {
      throw new IOException("Failed to create directory for \"" + des+ "\"")
    } else {
      copyOneFile(src, des)
    }
  }

  /**
   * @param dir
   */
  def purgeDir(dir:File) {
    if ( dir != null) {
      FileUtils.deleteDirectory(dir)
    }
  }

  /**
   * @param dir
   * @return
   */
  def purgeDirFiles(dir:File) {
    if ( dir != null) {
      FileUtils.cleanDirectory(dir)
    }
  }

  /**
   * @param path
   * @return
   */
  def parentPath(path:String) = {
    if ( ! isEmpty(path)) {
      new File(path).getParent()
    } else {
      path
    }
  }

  /**
   * @param path
   * @return
   */
  def baseName(path:String) = {
    if ( ! isEmpty(path)) {
      trimExtension( new File(path).getName())
    } else {
      path
    }
  }

  /**
   * @param path
   * @return
   */
  def fileName(path:String) = {
    if ( ! isEmpty(path)) {
      new File(path).getName()
    } else {
      path
    }
  }

  private def trimExtension(path:String) = {
    val pos = posOfSuffix(path)
    if (pos >= 0) {
      path.substring(0, pos)
    } else {
      path
    }
  }

  /**
   * @param path
   */
  def delete(path:String)  { delete(  new File(path) ) }

  /**
   * @param f
   */
  def delete(f:File) {
    try {
      if (f.isDirectory()) { purgeDir(f) }
      else {
        f.delete()
      }
    }
    catch { case _ => }
  }

  private def maybeLastDirSepPos(path:String) = {
    if (path != null) {
      max( path.lastIndexOf("/"), path.lastIndexOf("\\"))
    } else {
      -1
    }
  }

  private def posOfSuffix(file:String) = {
    if (file != null) {
      val p2 = maybeLastDirSepPos(file)
      val p1 = file.lastIndexOf(".")
      if (p2 > p1) -1 else p1
    } else {
      -1
    }
  }

  private def copyOneFile(src:File, des:File) {
    if (des.exists()) {
      if (!des.isFile()) {
        throw new IOException("\"" + des+ "\" exists but is not a valid file")
      }
      if (!des.canWrite()) {
        throw new IOException("Cannot overwrite \"" + des + "\"")
      }
    }

    using( new FileOutputStream(des) ) { (out) =>
      using(new FileInputStream(src)) { (inp) =>
        copy(inp, out)
      }
    }

    if (src.length() != des.length()) {
      throw new IOException("Failed to copy full contents from '" +
          src + "' to '" + des + "'")
    }

    // preserve the file datetime
    des.setLastModified(src.lastModified())
  }

}

