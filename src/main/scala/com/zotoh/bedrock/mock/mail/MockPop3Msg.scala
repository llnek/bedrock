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


package com.zotoh.bedrock.mock.mail

import scala.collection.JavaConversions._

import java.io.IOException
import java.io.InputStream
import java.util.{Date=>JDate}
import java.util.Enumeration
import java.util.Random
import java.util.Vector

import javax.mail.Folder
import javax.mail.MessagingException
import javax.mail.internet.MimeMessage

import com.zotoh.fwk.util.DateUte._
import com.zotoh.fwk.io.IOUte._
import com.zotoh.fwk.util.CoreUte._

/**
 * @author kenl
 *
 */
class MockPop3Msg (f:Folder, m:Int) extends MimeMessage(f,m) {

  private var _msg= ""

  override def getAllHeaderLines() = {

    _msg = "The current time is: " + fmtDate(new JDate())

    val bits= asBytes(_msg)
    Array(
      "message-id: a-mock-pop3-msg-"+new Random().nextInt(1000) ,
      "from: mickey@koala.com" ,
      "to: anonymous@acme.com" ,
      "subject: hello world" ,
      "content-length: " + bits.length ).toIterator
  }

  override def getRawInputStream() = {
    try {
      asStream( asBytes(_msg))
    } catch {
      case _ => null
    }
  }

  override def getInputStream() = getRawInputStream()

}


