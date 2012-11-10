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

import com.zotoh.fwk.util.LoggerFactory._
import com.zotoh.fwk.io.XData
import com.zotoh.fwk.util.StrUte._
import com.zotoh.fwk.xml.XmlUte._
import com.zotoh.fwk.util.Logger

import java.util.{Stack=>JSTK}

import org.xml.sax.Attributes
import org.xml.sax.ErrorHandler
import org.xml.sax.Locator
import org.xml.sax.SAXException
import org.xml.sax.SAXParseException
import org.xml.sax.XMLReader
import org.xml.sax.helpers.DefaultHandler


/**
 * @author kenl
 *
 */
class SaxHandler extends DefaultHandler with ErrorHandler  {

  private def ilog() {  _log=getLogger(classOf[XData]) }
  @transient private var _log:Logger=null
  def tlog() = { if(_log==null) ilog(); _log  }

  protected var _errors:JSTK[SAXException] = _
  protected var _warns:JSTK[SAXException] = _

  protected var _elemData:StringBuilder = _
  protected var _prevHdlr:SaxHandler = _

  protected var _trimCDATA= true
  protected var _docType=""
  protected var _sysID=""

  @transient protected var _locator:Locator = _
  protected var _rdr:XMLReader = _

  iniz()

  /**
   * Constructor.
   *
   * Create a new handler that is chained to the previous one.  This is a way we handle
   * a nested element which has it's own sax handling code.
   *
   * @param previousHandler
   */
  def this(prev:SaxHandler) {
    this()
    attachHandler(prev)
  }

  /**
   * @param trim
   */
  def setTrimCDATA(trim:Boolean) = {
    _trimCDATA= trim
    this
  }


  /**
   * @return
   */
  def isCDATATrimmed() = _trimCDATA

  /**
   * @return
   */
  def docType()  = _docType


  /**
   * @return
   */
  def systemID() =  _sysID


  /* (non-Javadoc)
   * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
   */
  override def startElement(uri:String, lname:String,
    qname:String, atts:Attributes) {
    onStartElement(uri, lname, qname, attrsToQNMap(atts))
  }

  /* (non-Javadoc)
   * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
   */
  override def endElement(uri:String, lname:String, qname:String) {
    onEndElement(uri,lname,qname, lastElementCDATA())
  }

  /**
   * Pop current handler, revert back to parent handler.
   *
   * @throws SAXException error while xml processing.
   */
  def resetDocumentHandler() {
    if ( _rdr != null &&  _prevHdlr != null) {
       _rdr.setContentHandler( _prevHdlr)
    }
  }

  /**
   * @return
   */
  def warningsAsString() = exceptionsAsString(_warns)

  /**
   * @return
   */
  def errorsAsString() = exceptionsAsString( _errors)


  /* (non-Javadoc)
   * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
   */
  override def characters(chs:Array[Char], start:Int, n:Int) {
    _elemData.appendAll(chs, start, n)
  }


  /**
   * @return
   */
  protected def lastElementCDATA() = {
    val v=  _elemData.toString()
    try {
      if (isEmpty(v)) null else ( if(_trimCDATA) v.trim() else v )
    }
    finally {
      _elemData.setLength(0)
    }
  }

  /* (non-Javadoc)
   * @see org.xml.sax.helpers.DefaultHandler#warning(org.xml.sax.SAXParseException)
   */
  override def warning(e:SAXParseException) {
    _warns.push(e)
  }

  /* (non-Javadoc)
   * @see org.xml.sax.helpers.DefaultHandler#error(org.xml.sax.SAXParseException)
   */
  override def error(e:SAXParseException) {
    _errors.push(e)
  }

  /* (non-Javadoc)
   * @see org.xml.sax.helpers.DefaultHandler#fatalError(org.xml.sax.SAXParseException)
   */
  override def fatalError(e:SAXParseException) {
    _errors.push(e)
  }

  /**
   * @param e
   */
  protected def push(e:SAXException) {
    _errors.push(e)
  }

  /**
   * @param uri
   * @param lname
   * @param qname
   * @param atts
   * @throws SAXException
   */
  protected def onStartElement(uri:String, lname:String,
    qname:String, atts:Map[String,String] ) {
    // subclass do the work
  }

  /**
   * @param uri
   * @param lname
   * @param qname
   * @param cdata
   * @throws SAXException
   */
  protected def onEndElement(uri:String, lname:String, qname:String, cdata:String) {
    // subclass do the work
  }

  /**
   * @param curr
   */
  protected def attachHandler(curr:SaxHandler) {
    if (curr != null) {
      _elemData= curr._elemData
      _prevHdlr= curr
      _sysID= curr._sysID
      _docType= curr._docType
      _locator= curr._locator
      _errors= curr._errors
      _warns= curr._warns
      _rdr= curr._rdr
    }
  }

  /**
   * @return
   */
  protected def hasErrors() = _errors.size() > 0

  /**
   *
   */
  protected def iniz()  {
    _errors= new JSTK[SAXException]()
    _warns= new JSTK[SAXException]()
    _locator= null
    _sysID= null
    _docType= null
    _elemData = new StringBuilder(256)
  }

  private def exceptionsAsString(stk:JSTK[SAXException]) = {
    val ret=new StringBuilder(1024)
    for (i <- 0 until stk.size) {
      addAndDelim(ret, "\n", eToString(stk.get(i)))
    }
    ret.toString
  }

  private def eToString(e:SAXException) = {
    val e2 = e.getException()
    val err= e.getMessage()
    val line = e match {
      case pe:SAXParseException => pe.getLineNumber()
      case _ => -1
    }

    "SAX error near line " + line + " : " +
        ( if (e2 == null) err else ( err + ": " + e2.getMessage()) )
  }

}

