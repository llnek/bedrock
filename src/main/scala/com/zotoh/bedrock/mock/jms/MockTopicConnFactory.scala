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

package com.zotoh.bedrock.mock.jms

import javax.jms.Connection
import javax.jms.JMSException
import javax.jms.TopicConnection
import javax.jms.TopicConnectionFactory


/**
 * @author kenl
 *
 */
class MockTopicConnFactory extends TopicConnectionFactory {

  def createConnection() = createTopicConnection()

  def createConnection(user:String , pwd:String ) = createTopicConnection(user, pwd)

  def createTopicConnection() = new MockTopicConnection("","")

  def createTopicConnection(user:String , pwd:String ) = new MockTopicConnection(user, pwd)

}
