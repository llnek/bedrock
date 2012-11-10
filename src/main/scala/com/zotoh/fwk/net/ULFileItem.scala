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

package com.zotoh.fwk.net

import com.zotoh.fwk.io.{IOUte,XData}
import com.zotoh.fwk.io.IOUte._
import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.util.LoggerFactory._
import com.zotoh.fwk.util.Logger
import com.zotoh.fwk.util.StrUte._

import java.io.{ByteArrayOutputStream=>BAOS}
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

import org.apache.commons.fileupload.FileItem


/**
 * @author kenl
 *
 */
class ULFileItem extends FileItem {

  private def ilog() { _log=getLogger(classOf[ULFileItem]) }
  @transient private var _log:Logger=null
  def tlog() = { if (_log==null) ilog(); _log }

  private val serialVersionUID = 2214937997601489203L
  @transient private var _os:OutputStream = _

  private var _filename = ""
  private var _field=""
  private var _ctype=""
  private var _ds:XData= _
  private var _fieldBits:Array[Byte] = _
  private var _ff = false

  /**
   * @param field
   * @param contentType
   * @param isFormField
   * @param fileName
   */
  def this(field:String, contentType:String,
    isFormField:Boolean, fileName:String)   {
    this()
    tstEStrArg("file-name", fileName)
    tstEStrArg("field-name", field)

    _ctype= nsb(contentType)
    _field= field
    _ff= isFormField
    _filename= fileName
  }

  /* (non-Javadoc)
   * @see org.apache.commons.fileupload.FileItem#delete()
   */
  override def delete()  {
    IOUte.close(_os)
    if (_ds!=null) {
      _ds.setDeleteFile(true)
      _ds.destroy()
    }
    _ds=null
  }

  /* (non-Javadoc)
   * @see org.apache.commons.fileupload.FileItem#get()
   */
  override def get() = null

  /* (non-Javadoc)
   * @see org.apache.commons.fileupload.FileItem#getContentType()
   */
  override def getContentType() = _ctype


  /* (non-Javadoc)
   * @see org.apache.commons.fileupload.FileItem#getFieldName()
   */
  override def getFieldName()  = _field

  /* (non-Javadoc)
   * @see org.apache.commons.fileupload.FileItem#getInputStream()
   */
  override def getInputStream() = {
    throw new IOException("not implemented")
  }

  /* (non-Javadoc)
   * @see org.apache.commons.fileupload.FileItem#getName()
   */
  override def getName() = _filename

  /* (non-Javadoc)
   * @see org.apache.commons.fileupload.FileItem#getOutputStream()
   */
  override def getOutputStream() = {
    if(_os==null) iniz() else _os
  }

  /**
   * @return
   */
  def fileData() = _ds

  /* (non-Javadoc)
   * @see org.apache.commons.fileupload.FileItem#getSize()
   */
  override def getSize() = 0L

  /* (non-Javadoc)
   * @see org.apache.commons.fileupload.FileItem#getString()
   */
  override def getString() = getString("UTF-8")

  /* (non-Javadoc)
   * @see org.apache.commons.fileupload.FileItem#getString(java.lang.String)
   */
  override def getString(charset:String) = {
    if (maybeGetBits() == null) null else new String(_fieldBits, charset)
  }

  /* (non-Javadoc)
   * @see org.apache.commons.fileupload.FileItem#isFormField()
   */
  override def isFormField() = _ff

  /* (non-Javadoc)
   * @see org.apache.commons.fileupload.FileItem#isInMemory()
   */
  override def isInMemory() = false


  /* (non-Javadoc)
   * @see org.apache.commons.fileupload.FileItem#setFieldName(java.lang.String)
   */
  override def setFieldName(s:String) {
    _field=s
  }

  /* (non-Javadoc)
   * @see org.apache.commons.fileupload.FileItem#setFormField(boolean)
   */
  override def setFormField(b:Boolean)  {
    _ff= b
  }

  /* (non-Javadoc)
   * @see org.apache.commons.fileupload.FileItem#write(java.io.File)
   */
  override def write(fp:File) {
  }

  /**
   *
   */
  def cleanup()  {
    if (_fieldBits == null) {  maybeGetBits() }
    IOUte.close(_os)
    _os=null
  }

  /* (non-Javadoc)
   * @see java.lang.Object#finalize()
   */
  override def finalize() {
    IOUte.close(_os);  super.finalize()
  }

  private def maybeGetBits() = {
    _os match {
      case baos:BAOS => _fieldBits= baos.toByteArray()
    }
    _fieldBits
  }

  private def iniz() = {

    if (_ff) {
      _os= new BAOS(1024)
    } else {
      _ds= XData()
      try {
        val t= newTempFile(true)
        _ds.resetMsgContent( t._1)
        _os = t._2
      } catch {
        case e => tlog().error("", e)
      }
    }

    _os
  }

}
