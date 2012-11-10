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

package com.zotoh.fwk.io

import java.io.{ByteArrayOutputStream=>BAOS,File,InputStream}

import com.zotoh.fwk.util.{Logger,FileUte}
import com.zotoh.fwk.util.LoggerFactory._
import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.util.StrUte._
import com.zotoh.fwk.io.IOUte._

/**
 * Wrapper structure to abstract a piece of data which can be a file
 * or a memory byte[].  If the data is byte[], it will also be
 * compressed if a certain threshold is exceeded.
 *
 * @author kenl
 *
 */

object XData {

  private var _wd = tmpDir()

  /**
   * Get the current shared working directory.
   *
   * @return working dir.
   */
  def workDir() = _wd

  /**
   * Working directory is where all the temporary/sharable files are created.
   *
   * @param fpDir a directory.
   */
  def setWorkDir(fpDir:File) {
    if (fpDir != null) try {
      if (fpDir.isDirectory() && fpDir.canRead() && fpDir.canWrite()) {
        _wd=fpDir
      }
    }
    catch { case _ => }
  }
}

case class XData(ctObj:AnyRef = null) {

  private def ilog() { _log= getLogger(classOf[XData]) }
  @transient private var _log:Logger=null
  def tlog() = { if (_log==null) ilog(); _log }

  private val serialVersionUID = -8637175588593032279L
  private val CMPZ_THREADHOLD=1024*1024*4 // 4meg

  private var _cmpz=false
  private var _cls=true

  private var _encoding="UTF-8"
  private var _fp=""

  private var _bin:Array[Byte]= _
  private var _binSize= 0L

  if (ctObj!=null) { resetMsgContent(ctObj) }

  /**
   * @param enc
   */
  def setEncoding(enc:String) = { _encoding=enc ; this }

  /**
   * @return
   */
  def encoding() = _encoding

  def isZiped() =  _cmpz

  /**
   * Control the internal file.
   *
   * @param del true to delete, false ignore.
   */
  def setDeleteFile(del:Boolean) = { _cls= del ; this }

  /**
   * Tests if the file is to be deleted.
   *
   * @return
   */
  def isDeleteFile() = _cls

  /**
   * Clean up.
   */
  def destroy() = {
    if (! isEmpty(_fp) && isDeleteFile()) {
      FileUte.delete(_fp)
    }
    reset()
    this
  }

  /**
   * Tests if the internal data is a file.
   *
   * @return
   */
  def isDiskFile() =  ! isEmpty( _fp)


  /**
   * @param obj
   */
  def resetMsgContent(obj:Any):XData = resetMsgContent(obj, true)

  /**
   * @param obj
   * @param delIfFile
   */
  def resetMsgContent(obj:Any, delIfFile:Boolean):XData = {
    destroy()
    obj match {
      case baos: BAOS => maybeCmpz( baos.toByteArray())
      case bits: Array[Byte] => maybeCmpz( bits)
      case f:File => _fp= niceFPath( f)
      case fa:Array[File] => if (fa.length > 0) _fp= niceFPath( fa(0))
      case o:Any => maybeCmpz( o.toString().getBytes(_encoding))
    }
    setDeleteFile(delIfFile)
  }

  /**
   * Get the internal data.
   *
   * @return
   */
  def content():Any = {
    if (isDiskFile())  {
      new File( _fp)
    }
    else if ( _bin==null) {
      null
    }
    else if (! _cmpz) {
      _bin
    } else {
      gunzip( _bin)
    }
  }

  /**
   * @return
   */
  def hasContent() = {
    if (isDiskFile()) true else if ( _bin !=null) true else false
  }

  override def toString() = nsb( asString(bytes()) )

  /**
   * Get the data as bytes.  If it's a file-ref, the entire file content will be read
   * in as byte[].
   *
   * @return
   * @throws IOException
   */
  def bytes() = {
    if (isDiskFile()) read(new File( _fp)) else binData()
  }

  /**
   * Get the file path if it is a file-ref.
   *
   * @return the file-path, or null.
   */
  def fileRef() =  if ( isEmpty(_fp)) None else Some(new File( _fp))

  /**
   * Get the file path if it is a file-ref.
   *
   * @return the file-path, or null.
   */
  def filePath() =  if ( isEmpty(_fp)) null else _fp

  /**
   * Get the internal data if it is in memory byte[].
   *
   * @return
   */
  def binData()  = {
    if (_bin==null) null else if (!_cmpz) _bin else gunzip( _bin)
  }

  /**
   * Get the size of the internal data (no. of bytes).
   *
   * @return
   */
  def size() = {
    if ( isDiskFile()) new File(_fp).length() else _binSize
  }

  /* (non-Javadoc)
   * @see java.lang.Object#finalize()
   */
  override def finalize() {
    destroy()
  }

  def stream():InputStream = {
    if (isDiskFile()) { XStream(new File( _fp)) }
    else if ( _bin==null) { null }
    else {
      asStream( if (_cmpz) gunzip(_bin) else _bin )
    }
  }

  private def maybeCmpz(bits:Array[Byte]) {
    _binSize= if (bits==null) 0L else bits.length
    _cmpz=false
    _bin=bits
    if (_binSize > CMPZ_THREADHOLD) {
      _cmpz=true
      _bin= gzip(bits)
    }
  }

  private def reset() {
    _cmpz=false
    _cls=true
    _bin=null
    _fp=null
    _binSize=0L
  }

}

