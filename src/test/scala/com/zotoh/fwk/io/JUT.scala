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

import com.zotoh.fwk.io.IOUte._
import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.util.StrUte._

import java.io.{ByteArrayInputStream=>BAIS,ByteArrayOutputStream=>BAOS}
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.{Properties=>JPS}

import com.zotoh.fwk.util.FileUte

import org.scalatest.Assertions._
import org.scalatest._


class FwkIOJUT  extends FunSuite with BeforeAndAfterEach with BeforeAndAfterAll  {

  override def beforeAll(configMap: Map[String, Any]) {
  }

  override def afterAll(configMap: Map[String, Any]) {
  }

  override def beforeEach() { }

  override def afterEach() { }

  test("testUte") {
    var f=mkTempFile()
    writeFile(f, "hello", "utf-8")

    var b=read(f)
    expect(b.length)(5)

    var s = readText(f, "utf-8")
    expect(s.length())(5)

    assert(gzip("hello","utf-8").sameElements( gzip("hello".getBytes("utf-8"))) )
    assert(gunzip(gzip("hello","utf-8")).sameElements( "hello".getBytes("utf-8")) )

    var inp=asStream("hello".getBytes())
    assert(inp != null)
    assert( bytes(inp).sameElements( "hello".getBytes()) )

    inp=asStream("hello".getBytes())
    var dd=readBytes(inp)
    assert(dd != null)
    assert(dd.fileRef() == None)
    assert(dd.bytes().sameElements( "hello".getBytes()) )

    using(IOUte.open(f)) { (inp) =>
      assert(inp.isInstanceOf[XStream])
      assert(bytes(inp).sameElements( "hello".getBytes()) )
    }
    assert(f.exists())

    assert(read(f).sameElements( "hello".getBytes("utf-8")) )

    s=toGZipedB64("hello".getBytes())
    assert(fromGZipedB64(s).sameElements( "hello".getBytes()) )

    assert(read(f).sameElements( "hello".getBytes()) )
    expect( 5)(available(asStream("hello".getBytes())))

    var out= new BAOS()
    copy( asStream("hello".getBytes()), out)
    assert(out.toByteArray().sameElements( "hello".getBytes()) )

    f=copy(asStream("hello".getBytes()) )
    expect(5)(f.length())
    f.delete()

    b=new Array[Byte](10000)
    var baos= new BAOS()
    copy(asStream(b), baos, 9492)
    expect(baos.toByteArray().length)(9492)

    assert(!different(asStream("abc".getBytes()), asStream("abc".getBytes())))
    assert(different(asStream("abc".getBytes()), asStream("ABC".getBytes())))

    IOUte.setStreamLimit(6)
    dd=readBytes(asStream("hello world".getBytes()))
    assert(dd.fileRef() != None)
  }

  test("testByteFrags") {

    var bits= new Array[Byte](20)
    bits(3)='a'
    bits(13)='z'

    var is= new ByteFragmentInputStream(bits, 3, 15)
    var r=is.getFrag()
    assert(r != null)
    assert(r.buf() eq bits)
    expect(3)(is.getPos())

    r=is.getFrag(5)
    assert(r != null)

    r= is.getFrag(7, 5)
    assert(r != null)

    var b=new Array[Byte](11)
    is.read(b)
    assert(b(0)=='a' && b(10)=='z')
    expect(is.getPos())(14)
  }

  test("testEmptyStreamData") {
    val s= XData()
    expect(s.size())(0L)
    assert(s.fileRef() == None)
    assert(s.content() == null)
    assert(s.stream() == null)
    assert(s.filePath() == null)
    assert(s.binData() == null)
    assert(s.bytes() == null)
  }

  test("testBytesStreamData") {
    var s= XData()
    var data= "hello world"
    s.resetMsgContent(data)
    assert(s.content()==s.binData())
    assert(s.bytes()==s.binData())
    expect(s.size())( asBytes(data).length)
    assert(!s.isZiped())
    assert(s.binData().sameElements( asBytes(data)) )
  }

  test("testLargeBytesStreamData") {
    var s= XData()
    var b= mkString('x', 5000000)
    var bits=b.getBytes("UTF-8")
    s.resetMsgContent(b)
    assert(s.content() != s.binData())
    assert(s.bytes() != s.binData())
    expect(s.size())( bits.length)
    assert(s.isZiped())
    assert(s.bytes().sameElements( bits) )
  }

  test("testStreamStreamData") {
    var s= XData()
    var data= "hello world"
    s.resetMsgContent(data)
    assert( s.stream().isInstanceOf[BAIS] )
    assert(bytes(s.stream()).sameElements( asBytes(data)) )
  }

  test("testFileStreamData") {
    var s= IOUte.mkFSData()
    var fout= s.fileRef().get
    var os= new FileOutputStream(fout)
    var data= "hello world"
    os.write(asBytes(data))
    os.close()
    assert(s.stream().isInstanceOf[XStream])
    assert(bytes(s.stream()).sameElements( asBytes(data)) )
    assert(s.bytes().sameElements( asBytes(data)) )
    assert(s.binData()==null)
    expect(s.size())(data.length())
    s.destroy()
    assert( !fout.exists())
  }

  test("testFileRefStreamData") {
    var s= IOUte.mkFSData()
    var fout= s.fileRef().get
    var os= new FileOutputStream(fout)
    var data= "hello world"
    os.write(asBytes(data))
    os.close()
    s.destroy()
    assert( !fout.exists())
  }

  test("testFileRefStreamData2") {
    var s= IOUte.mkFSData()
    var fout= s.fileRef().get
    var os= new FileOutputStream(fout)
    var data= "hello world"
    os.write(asBytes(data))
    os.close()
    try {
      s.setDeleteFile(false)
      s.destroy()
      assert(fout.exists())
    }
    finally {
      FileUte.delete(fout)
    }
  }

  test("testSmartFileIS") {
    var t= IOUte.newTempFile( true)
    var fout= t._1
    var os= t._2
    var data= "hello world"
    os.write(asBytes(data))
    os.close()
    try {
      var inp= XStream(fout)
      var bits= new Array[Byte](1024)
      var c=inp.read(bits)
      expect(c)(11)
      inp.close()
      c=inp.read(bits)
      expect(c)(11)
      inp.delete()
      assert(fout.exists())
    }
    finally {
      FileUte.delete(fout)
    }
  }

  test("testSmartFileIS2") {
    var t= IOUte.newTempFile(true)
    var fout= t._1
    var os= t._2
    var data= "hello world"
    os.write(asBytes(data))
    os.close()
    try {
      var inp= new XStream(fout, true)
      var bits= new Array[Char](1024)
      var c=inp.readChars(chSet("utf-8"), 1024)
      expect(c._2)(11)
      inp.close()
      inp.delete()
      assert(! fout.exists())
    }
    finally {
    }
  }

  test("testRangeStream") {
    var t= IOUte.newTempFile(true)
    var fout= t._1
    var os= t._2
    var data= "hello world"
    os.write(asBytes(data))
    os.close()
    try {
      var inp= XStream(fout)
      var cip= new BoundedInputStream(inp, fout.length().toInt )
      expect(cip.available())(fout.length())
      assert(cip.hasMore())
      var cnt= fout.length()
      while (cip.hasMore()) {
        cip.read()
        cnt -= 1
      }
      expect(cnt)(0L)
    }
    finally {
      FileUte.delete(fout)
    }
  }


}

