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

package com.zotoh.fwk.xml

import com.zotoh.fwk.util.CoreUte._
import java.io.IOException
import java.io.StringWriter
import java.io.Writer

import org.w3c.dom.Document

import com.zotoh.fwk.util.LoggerFactory._
import com.zotoh.fwk.util.{Consts,Logger}

import javax.xml.transform.Transformer
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

object DOMWriter {

  /**
   * Convert a DOM object to string, allowing for nice indentation if required.
   *
   * @param doc the DOM.
   * @param indent true if nice indentation is needed.
   * @return the string.
   * @throws IOException
   */
  def writeOneDoc(doc:Document) = {
    if(doc==null) null else new DOMWriter().write(doc).writer().toString
  }

}

/**
 * Simple class to output a DOM object to string or file.
 *
 * @author kenl
 *
 */
class DOMWriter( @transient private var _wtr:Writer) extends XmlVars  {

  @transient private var _log= getLogger(classOf[DOMWriter])
  def tlog() = _log

  /**
   *
   */
  def this()  {
    this(new StringWriter())
  }


  /**
   * @param writer
   * @return
   */
  def setWriter(w:Writer) =  {
    tstObjArg("writer", w)
    _wtr= w
    this
  }


  /**
   * @return
   */
  def writer() = _wtr


  /**
   * @param doc
   * @return
   * @throws IOException
   */
  def write(doc:Document) = {

    if (doc != null) {
      val t= TransformerFactory.newInstance().newTransformer()
      t.transform(new DOMSource(doc), new StreamResult(_wtr))
    }

    this
  }

}
