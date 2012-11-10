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
import com.zotoh.fwk.io.IOUte._
import com.zotoh.fwk.util.FileUte._

import java.io.File
import java.io.FilenameFilter
import java.io.InputStream
import java.net.URL

import org.scalatest.Assertions._
import org.scalatest._

import org.w3c.dom.Document



class FwkXmlJUT  extends FunSuite with BeforeAndAfterEach with BeforeAndAfterAll with XmlVars  {

  override def beforeAll(configMap: Map[String, Any]) {
  }

  override def afterAll(configMap: Map[String, Any]) {
  }

  override def beforeEach() { }

  override def afterEach() { }

  test("testToDOM") {
    using(rc2Stream("com/zotoh/fwk/xml/simple.xml")) { (inp) =>
      assert(XmlUte.toDOM(inp) != null)
    }
  }

  test("testWriteDOM") {
    using(rc2Stream("com/zotoh/fwk/xml/simple.xml")) { (inp) =>
      val doc= XmlUte.toDOM(inp)
      val s= DOMWriter.writeOneDoc(doc)
      assert(s != null && s.length() > 0)
    }

  }

  test("testXmlScanner") {
    var doc= rc2Url("com/zotoh/fwk/xml/malformed.xml")
    assert( ! new XmlScanner().scan(doc))

    doc= rc2Url("com/zotoh/fwk/xml/simple.xml")
    assert(new XmlScanner().scan(doc))
  }

  test("testDTDValidator") {

    var dtd= rc2Url("com/zotoh/fwk/xml/3a4.dtd",
        Some(this.getClass().getClassLoader()))

    using(rc2Stream("com/zotoh/fwk/xml/bad.dtd.xml")) { (inp) =>
      assert(new DTDValidator().scanForErrors(inp, dtd))
    }

    using(rc2Stream("com/zotoh/fwk/xml/good.dtd.xml")) { (inp) =>
      assert(! new DTDValidator().scanForErrors(inp, dtd))
    }

  }

  test("testXSDValidator") {
    var dtd= rc2Url("com/zotoh/fwk/xml/3a4.xsd")

    using(rc2Stream("com/zotoh/fwk/xml/bad.xsd.xml")) { (inp) =>
      assert( ! new XSDValidator().scan(inp, dtd))
    }

    using(rc2Stream("com/zotoh/fwk/xml/good.xsd.xml")) { (inp) =>
      assert(new XSDValidator().scan(inp, dtd))
    }

  }

  private class FF extends FilenameFilter {
    def accept(dir:File, fname:String) = fname.endsWith(".xml")
  }



}

