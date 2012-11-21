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

import com.zotoh.fwk.util.DateUte._
import com.zotoh.fwk.util.ByteUte._
import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.util.CoreUte.asString
import com.zotoh.fwk.util.StrUte.addAndDelim
import com.zotoh.fwk.util.StrUte.chomp
import com.zotoh.fwk.util.StrUte.containsChar
import com.zotoh.fwk.util.StrUte.equalsOneOf
import com.zotoh.fwk.util.StrUte.equalsOneOfIC
import com.zotoh.fwk.util.StrUte.hasWithin
import com.zotoh.fwk.util.StrUte.join
import com.zotoh.fwk.util.StrUte.splitIntoChunks
import com.zotoh.fwk.util.StrUte.startsWith
import com.zotoh.fwk.util.StrUte.startsWithIC
import com.zotoh.fwk.util.StrUte.strstr
import com.zotoh.fwk.util.StrUte.trim
import java.sql.{Timestamp=>JTSTMP}
import java.util.Calendar
import java.util.{Date=>JDate,Properties=>JPS}
import java.util.GregorianCalendar
import java.nio.charset.Charset
import java.nio._
import java.io.File
import scala.Serializable

import org.scalatest.Assertions._
import org.scalatest._


class FwkUtilJUT  extends FunSuite with BeforeAndAfterEach with BeforeAndAfterAll with Consts  {

  override def beforeAll(configMap: Map[String, Any]) {
  }

  override def afterAll(configMap: Map[String, Any]) {
  }

  override def beforeEach() { }

  override def afterEach() { }

  test("testByteUte") {
    val cs= Charset.forName("utf-8")
    val s= "hello"
    val bb= convertCharsToBytes(s.toCharArray(), cs)
    val ss=asBytes(s)
    assert(ss.sameElements( bb ))
    val z= new String(bb, cs)
    assert(z == s)
    expect(911L)(readAsLong( readAsBytes(911L)))
    expect(911)(readAsInt( readAsBytes(911)))
    try {
      readAsLong(readAsBytes(911L).slice(4,8))
      assert(false,"expect to fail but succeeded!!!")
    } catch {
      case _ => assert(true) // not enough bytes to read
    }
  }

  test("testDateUte") {
    var gc= new GregorianCalendar(2050, 5, 20)
    var base= gc.getTime()
    var dt= DateUte.addYears(base, -5)
    var g= new GregorianCalendar()
    g.setTime(dt)

    expect(g.get(Calendar.YEAR))( 2045)
    dt= DateUte.addYears(base, 5)
    g= new GregorianCalendar(); g.setTime(dt)
    expect(g.get(Calendar.YEAR))( 2055)
    dt= DateUte.addMonths(base, -2)
    g= new GregorianCalendar(); g.setTime(dt)
    expect(g.get(Calendar.MONTH))( 3)
    dt= DateUte.addMonths(base, 2)
    g= new GregorianCalendar(); g.setTime(dt)
    expect(g.get(Calendar.MONTH))( 7)
    dt= DateUte.addDays(base, -10)
    g= new GregorianCalendar(); g.setTime(dt)
    expect(g.get(Calendar.DAY_OF_MONTH))( 10)
    dt= DateUte.addDays(base, 10)
    g= new GregorianCalendar(); g.setTime(dt)
    expect(g.get(Calendar.DAY_OF_MONTH))( 30)
  }

  test("testCardinality") {
    var c= new Cardinality(null)
    expect(c.getMaxOccurs())( c.getMinOccurs())
    expect(c.getMaxOccurs())(0)
    assert(!c.isRequired())
    c= new Cardinality("")
    expect(c.getMaxOccurs())( c.getMinOccurs())
    expect(c.getMaxOccurs())(0)
    assert(!c.isRequired())
    c= new Cardinality("1,10")
    expect(c.getMaxOccurs() )(10)
    expect(c.getMinOccurs())(1)
    assert(c.isRequired())
    c= new Cardinality("0,n")
    expect(c.getMaxOccurs() )(Integer.MAX_VALUE)
    expect(c.getMinOccurs())(0)
    assert(!c.isRequired())
    c= new Cardinality("-9,20")
    expect(c.getMaxOccurs() )(20)
    expect(c.getMinOccurs())(0)
    assert( !c.isRequired())
  }

  test("testMiscStr") {
    val s= CoreUte.normalize("hello$")
    expect("hello_0x24")(s)
    expect("hello" )( asString(asBytes("hello")))
  }

  test("testZip") {
    var sa= "hello".getBytes("utf-8")
    sa=CoreUte.deflate(sa)
    sa=CoreUte.inflate(sa)
    expect("hello" )( new String(sa))
  }

  test("testTrim") {
    var s=trim("<hello>", "<>")
    expect("hello")(s)

    s= trim("   hello     ")
    expect("hello")(s)
  }

  test("testContainsChar") {
    var ok= containsChar("this is amazing !!!", "^%!$*")
    assert(ok)
    ok= containsChar("this is amazing !!!", "^%$*")
    assert(!ok)
  }

  test("testSplitChunks") {
    var ss= splitIntoChunks("1234567890", 5)
    assert(ss != null && ss.length==2)
    expect("12345" )( ss(0))
    expect("67890" )( ss(1))
  }

  test("testStrstr") {
    val s= strstr("this is a message to joe : hello joe", "joe", "bobby")
    expect("this is a message to bobby : hello bobby")(s)
  }

  test("testFmtDate") {
    val now= new GregorianCalendar(2000, 9, 2, 12, 13, 14)
    val n= now.getTime()
    var s= fmtDate(n, DT_FMT)
    assert(s != null && s.length() > 0)
    val a= parseDate(s, DT_FMT)
    expect(a.getOrElse(null))( n)
  }

  test("testFmtTS") {
    val now= new GregorianCalendar(2000, 9, 2, 12, 13, 14)
    val n= new JTSTMP(now.getTime().getTime())
    val s= n.toString()
    assert(s != null && s.length() > 0)
    val a= parseTimestamp(s)
    expect(a.getOrElse(null))(n)
  }

  test("testParseDate") {
    var d= parseDate("8764395345")
    assert(d==None)

    d= parseDate("2000-03-04 16:17:18")
    assert(d.getOrElse(null) != null)

    assert( !hasTZPart("2000-03-04 16:17:18"))
    assert( !hasTZPart("2000-03-04"))
    assert( !hasTZPart("2000-03-04 16:17:18.999"))
    assert( hasTZPart("2000-03-04 16:17:18 -099"))
    assert( hasTZPart("2000-03-04 16:17:18 +099"))
    assert( hasTZPart("2000-03-04 16:17:18 PDT"))
  }

  test("testParseTS") {
    var ts= parseTimestamp("43654kjljlfk")
    assert(ts==None)

    ts= parseTimestamp("2010-09-02 13:14:15")
    assert(ts.getOrElse(null) != null)
  }

  test("testUpcaseFirstChar") {
    val s= "joe".capitalize
    expect("Joe")(s)
  }

  test("testArrToStr") {
    var s= join(Array[String]("hello", "joe"), ",")
    expect("hello,joe")(s)
    s= join(Array[String]("hello", "joe"), null)
    expect("hellojoe")(s)
    s= join(Array[String]("hello", "joe") )
    expect("hellojoe")(s)
  }

  test("testChomp") {
    var s= chomp("this is a long string, to be chomped by joe",
        " a long", "chomped by")
    expect("this is joe")(s)
  }

  test("testHasWithin") {

    var ok=hasWithin("hello joe, how are you?", Array("are"))
    assert(ok)

    ok=hasWithin("hello joe, how are you?", Array("hello"))
    assert(ok)

    ok=hasWithin("hello joe, how are you?", Array("jack"))
    assert(!ok)
  }

  test("testAddAndDelim") {
    val bf= new StringBuilder(256)
    addAndDelim(bf, ";", "hello")
    expect("hello")(bf.toString())
    addAndDelim(bf, ";", "joe")
    expect("hello;joe")( bf.toString())
  }

  test("testEqualsOneOf") {

    var ok=equalsOneOf("jim", Array[String]("Jack", "joe", "jim"))
    assert(ok)

    ok=equalsOneOf("Jim", Array[String]("Jack", "joe", "jim"))
    assert(!ok)

    ok=equalsOneOfIC("Jim", Array[String]("Jack", "joe", "jim") )
    assert(ok)
  }

  test("testStartsWith") {

    var ok=startsWith("hello joe", Array[String]("joe", "hell" ))
    assert(ok)

    ok=startsWith("hello joe", Array[String]("joe", "HeLlo" ))
    assert(!ok)

    ok=startsWithIC("hello joe", Array[String]("joe", "HeLlo" ))
    assert(ok)

  }

  test("testZeroInteger") {
    val n= 0
    val m= ByteUte.readAsInt( ByteUte.readAsBytes(n))
    expect(m)(n)
  }

  test("testOneInteger") {
    val n= 1
    val m= ByteUte.readAsInt( ByteUte.readAsBytes(n))
    expect(m)(n)
  }

  test("testSmallInteger") {
    val n= 100
    val m= ByteUte.readAsInt( ByteUte.readAsBytes(n))
    expect(m)(n)
  }

  test("testLargeInteger") {
    val n= Integer.MAX_VALUE
    val m= ByteUte.readAsInt( ByteUte.readAsBytes(n))
    expect(m)(n)
  }

  test("testNegOneInteger") {
    val n= -1
    val m= ByteUte.readAsInt( ByteUte.readAsBytes(n))
    expect(m)(n)
  }

  test("testNegSmallInteger") {
    val n= -100
    val m= ByteUte.readAsInt( ByteUte.readAsBytes(n))
    expect(m)(n)
  }

  test("testNegLargeInteger") {
    val n= Integer.MIN_VALUE
    val m= ByteUte.readAsInt( ByteUte.readAsBytes(n))
    expect(m)(n)
  }

  test("testZeroLong") {
    val n= 0L
    val m= ByteUte.readAsLong( ByteUte.readAsBytes(n))
    expect(m)(n)
  }

  test("testOneLong") {
    val n= 1L
    val m= ByteUte.readAsLong( ByteUte.readAsBytes(n))
    expect(m)(n)
  }

  test("testSmallLong") {
    val n= 100L
    val m= ByteUte.readAsLong( ByteUte.readAsBytes(n))
    expect(m)(n)
  }

  test("testLargeLong") {
    val n= java.lang.Long.MAX_VALUE
    val m= ByteUte.readAsLong( ByteUte.readAsBytes(n))
    expect(m)(n)
  }

  test("testNegOneLong") {
    val n= -1L;
    val m= ByteUte.readAsLong( ByteUte.readAsBytes(n))
    expect(m)(n)
  }

  test("testNegSmallLong") {
    val n= -100L
    val m= ByteUte.readAsLong( ByteUte.readAsBytes(n))
    expect(m)(n)
  }

  test("testNegLargeLong") {
    val n= java.lang.Long.MIN_VALUE
    val m= ByteUte.readAsLong( ByteUte.readAsBytes(n))
    expect(m)(n)
  }

  /*
  test("testMenu") {
    var m1= new TMenu("M1")
    var m2= new TMenu("M2")
    var cb=new TMenuCB() {
      def command(i:TMenuItem) { }}
    m1.add(new TMenuItem("m.i1", "New", cb))
    m1.add(new TMenuItem("m.i2", "Open", m2))
    m1.add(new TMenuItem("m.i3", "Close", cb))
    m1.show(null)
    m1=null
  }
  */

  test("testNiceFilePath") {
    expect(niceFPath("/c:\\windows\\temp"))( "/c:/windows/temp")
  }

  test("testNiceFilePath2") {
    expect(niceFPath(new File("/c:\\windows\\temp")))( "/c:/windows/temp")
  }

  test("serializeObj") {
    val d=new Dumb("joe")
    val n=deserialize(serialize(d)) match {
      case x:Some[Dumb] => x.get.name
      case _ => ""
    }
    expect("joe")( n)
  }

  test("asSomeNumbers") {
    expect(asInt("911",-1))( 911)
    expect(asLong("911", 0L))( 911)
    expect(asDouble("911", 3.3).toInt)( 911)
    expect(asFloat("911", 6.toFloat).toInt)( 911)
    expect(asInt("xxx",-1))( -1)
    expect(asLong("xxx",-1L))( -1L)
    assert(asBool("yes",false))
    assert(! asBool("xxx",false))
  }

  test("testMatchChar") {
    assert( !matchChar('B', Array('A', 'X','Z')))
    assert( matchChar('X', Array('A', 'X','Z')))
  }

  test("testZipJPS") {
    val p= new JPS()
    p.put("a", "hi")
    expect(asQuirks(asBytes(p)).getProperty("a"))("hi")
  }

  test("testFileUrl") {
    val p=new File("/tmp/abc.txt").getCanonicalPath()
    val s=asFileUrl(new File(p))
    if (isWindows())
      expect(s)( "file:/"+p)
    else
      expect(s)( "file:" + p)
  }


  /*
  test("testCmdLineSeq") {
    val q3= new CmdLineQ("bad", "oh dear, too bad") {
      def onRespSetOut(a:String, props:JPS) = {
        ""
      }}

    val q2= new CmdLineQ("ok", "great, bye") {
      def onRespSetOut( a:String , props:JPS) = {
        ""
      }}
    val q1= new CmdLineQ("a", "hello, how are you?", "ok/bad", "ok") {
      def onRespSetOut(a:String, props:JPS) = {
        if ("ok"==a) {
          props.put("state", asJObj(true))
          "ok"
        } else {
          props.put("state", asJObj(false))
          "bad"
        }
      }
    }

    val seq= new CmdLineSeq(Array(q1,q2,q3)) {
      def onStart() = "a"
    }

    val props= new JPS()
    seq.start(props)
    if (seq.isCanceled()) {
      assert(true)
    }
    else {
      assert(props.containsKey("state"))
    }

  }
  */

}

class Dumb(val name:String) extends Serializable {
  private val serialVersionUID= 911L
}
