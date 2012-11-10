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

import scala.collection.mutable.ArrayBuffer
import scala.util.Sorting

import java.io.{IOException,CharArrayWriter,File,OutputStream,OutputStreamWriter,Reader,Writer}
import java.util.{Arrays,Collection,Iterator,StringTokenizer}

import com.zotoh.fwk.io.IOUte._

/**
 * @author kenl
 *
 */
object StrUte extends CoreImplicits {

  /**
   * @param str
   * @param stripChars
   * @return
   */
  def stripHead(str:String, stripChars:String) = {
    if (isEmpty(str) || isEmpty(stripChars))  str  else {
      val arr=stripChars.toCharArray()
      val len= str.length()
      var head=0
      while ((head < len) && arr.contains(str.charAt(head)) ) {
        head += 1
      }
      str.substring(head)
    }
  }

  /**
   * @param str
   * @param stripChars
   * @return
   */
  def stripTail(str:String, stripChars:String) = {
    if ( isEmpty(str) || isEmpty(stripChars))  str  else {
      val arr= stripChars.toCharArray()
      var tail= str.length()
      while ((tail > 0) && arr.contains(str.charAt(tail - 1))) {
        tail -= 1
      }
      str.substring(0, tail)
    }
  }

  /**
   * @param strs
   * @return
   */
  def trimAll(strs:String*) = {
    ( List[String]() /: strs) { (rc, s) =>
      trim(s) :: rc
    }.reverse.toSeq
  }

  /**
   * Trim head &amp; tail of the string.
   *
   * @param s
   * @param chars characters to be removed.
   * @return
   */
  def trim(s:String, chars:String) = stripTail( stripHead(s, chars), chars)

  /**
   * Safe trim.
   *
   * @param s
   * @return
   */
  def trim(s:String) = if (s==null) "" else s.trim()

  /**
   * Append to a string-builder, optionally inserting a delimiter if the buffer is not
   * empty.
   *
   * @param buf
   * @param delim
   * @param item
   * @return
   */
  def addAndDelim(buf:StringBuilder, delim:String, item:String) = {
    if (item != null) {
      if (buf.length() > 0 && delim != null) { buf.append(delim) }
      buf.append(item)
    }
    buf
  }

  def qsort(ids:Array[String]) = Sorting.quickSort(ids)

  /**
   * @param iter
   * @param sep
   * @return
   */
  def join(objs:Seq[Any], sep:String="") = {
    (new StringBuilder /: objs) { (rc,obj) =>
      addAndDelim(rc, sep, nsn(obj))
    }.toString
  }

  /**
   * Tests if the string contains any one of the given characters.
   *
   * @param s
   * @param chars
   * @return
   */
  def containsChar(s:String, chars:Seq[Char]) = {
    nsb(s).toCharArray().exists { (c) =>
      chars.contains(c)
    }
  }

  /**
   * Get the string without the stuff between head &amp; end.
   * e.g.
   *   src="this is a sample message", head="is", end="sample"
   *   = &gt; "this  message"
   *
   * @param src
   * @param head
   * @param end
   * @return
   */
  def chomp(src:String, head:String, end:String) = {
    if ( !isEmpty(src) && head != null && end != null) {
      val h= src.indexOf(head)
      var rc=src
      if (h >= 0) {
        var r= src.lastIndexOf(end)
        if (r >= 0 ) {          r += end.length()        }
        if (r >= (h + head.length())) {
          rc=src.substring(0,h) + src.substring(r)
        }
      }
      rc
    } else {
      src
    }
  }

  /**
   * Get the string between head &amp; end.
   * e.g.
   *   src="this is a sample message", head="is", end="message"
   *   = &gt; " a sample "
   *
   * @param src
   * @param head
   * @param end
   * @return
   */
  def mid(src:String, head:String, end:String) = {
    if ( !isEmpty(src) && head != null && end != null) {
      var h= src.indexOf(head)
      var rc=""
      if (h >= 0) {
        val r= src.lastIndexOf(end)
        h += head.length()
        if (r >= 0 && r > h) {
          rc= src.substring(h, r)
        }
      }
      rc
    } else {
      src
    }
  }


  /**
   * Like the c version of strstr().  Substitute all occurrences of a substring, and replace them with a different substring.
   *
   * @param original
   * @param delim
   * @param replaceStr
   * @return
   */
  def strstr(original:String, delim:String, replacer:String) = {
    val b = new StringBuilder(1024)
    var src=original
    if (src != null && delim != null && replacer != null) {
      var tail = src.indexOf(delim)
      val len = delim.length()
      while (tail >= 0) {
        b.append(src.substring(0, tail)).append(replacer)
        src = src.substring(tail + len)
        tail = src.indexOf(delim)
      }
      b.append(src).toString
    } else {
      src
    }
  }

  /**
   * Split a large string into chucks, each chunk having a specific length.
   *
   * @param src
   * @param chunkLength
   * @return
   */
  def splitIntoChunks(original:String, chunkLength:Int) = {
    val ret= ArrayBuffer[String]()
    var src=original
    if (src != null) {
      while (src.length() > chunkLength) {
        ret += src.substring(0, chunkLength)
        src = src.substring(chunkLength)
      }
      if (src.length() > 0) {   ret += src }
    }
    ret.toSeq
  }

  /**
   * Tests String.indexOf() against a list of possible args.
   *
   * @param src
   * @param strs
   * @return
   */
  def hasWithin(src:String, strs:Seq[String]) = {
    if (src == null) false else strs.exists { (s) => src.has(s) }
  }

  /**
   * Tests startWith(), looping through the list of possible prefixes.
   *
   * @param src
   * @param pfxs
   * @return
   */
  def startsWith(src:String, pfxs:Seq[String]) = {
    if(src==null) false else pfxs.exists { (s) => src.startsWith(s) }
  }

  /**
   * Tests String.equals() against a list of possible args. (ignoring case)
   *
   * @param original
   * @param args
   * @return
   */
  def equalsOneOfIC(src:String, args:Seq[String]) = {
    if (src==null) false else args.exists { (s) => src.eqic(s) }
  }

  /**
   * Tests String.equals() against a list of possible args.
   *
   * @param src
   * @param args
   * @return
   */
  def equalsOneOf(src:String, args:Seq[String]) = {
    if (src==null) false else args.exists { (s) => src==s }
  }

  /**
   * Tests String.indexOf() against a list of possible args. (ignoring case).
   *
   * @param src
   * @param strs
   * @return
   */
  def hasWithinIC( src:String, strs:Seq[String]) = {
    if (src == null) false else strs.exists { (s) => src.hasic(s) }
  }

  /**
   * @param str
   * @param len
   * @return
   */
  def right(str:String, len:Int) = {
    if (isEmpty(str) || len <= 0) "" else {
      val delta= str.length() - len
      if (delta <= 0) str else str.substring(delta)
    }
  }

  /**
   * @param str
   * @param len
   * @return
   */
  def left(str:String, len:Int) = {
    if ( isEmpty(str) || len <= 0) "" else {
      if (str.length() <= len) str else str.substring(0,len)
    }
  }

  /**
   * @param s
   * @return
   */
  def isBlank(s:String) = {
    ! nsb(s).toCharArray().exists { (c) =>
      !Character.isWhitespace(c)
    }
  }

  /**
   * @param s
   * @return
   */
  def isEmpty(s:String) = (s==null || s.length()==0)

  /**
   * @param text
   * @param search
   * @param replacer
   * @param max
   * @return
   */
  def replace(text:String, search:String, replacer:String, count:Int):String = {
    if (isEmpty(text) || isEmpty(search) || replacer == null || count <= 0) {
      text
    } else {
      val replLength = search.length()
      val buf = new StringBuilder
      var start = 0
      var c=count
      var end = text.indexOf(search, start)
      while (end >= 0 && c > 0) {
        buf.append(text.substring(start, end)).append(replacer)
        start = end + replLength
        c -= 1
        end = text.indexOf(search, start)
      }
      buf.append(text.substring(start)).toString
    }
  }

  /**
   * Tests startsWith (ignore-case).
   *
   * @param original source string.
   * @param pfxs list of prefixes to test with.
   * @return
   */
  def startsWithIC(src:String, pfxs:Seq[String]) = {
    if(src==null) false else pfxs.exists { (s) => src.swic(s) }
  }

  /**
   * Safely call toString().
   *
   * @param o
   * @return "" if null.
   */
  def nsb(o:Any) = {
    if ( o == null ) "" else o.toString()
  }

  /**
   * @param o
   * @return
   */
  def nsn(o:Any) = {
    if ( o == null ) "(null)" else o.toString()
  }

  /**
   * @param src
   * @param chars
   * @return
   */
  def indexOfAnyChar(src:String, chars:Seq[Char]) = {
    nsb(src).toCharArray().indexWhere { (c) =>
      chars.contains(c)
    }
  }

  /**
   * @param c
   * @param times
   * @return
   */
  def mkString(c:Char, times:Int) = {
    (new StringBuilder /: (1 to times) ) { (rc,i) => rc.append(c) } .toString
  }

}
