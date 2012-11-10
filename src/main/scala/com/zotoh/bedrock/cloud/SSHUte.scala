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

package com.zotoh.bedrock.cloud

import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.util.StrUte._
import com.zotoh.fwk.util.{CoreImplicits}

import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader

import ch.ethz.ssh2.Connection
import ch.ethz.ssh2.SCPClient
import ch.ethz.ssh2.Session
import ch.ethz.ssh2.StreamGobbler

/**
 * @author kenl
 *
 */
object SSHUte extends CoreImplicits {

  /**
   * @param host
   * @param port std port = 22
   * @param user
   * @param pwd
   * @param key
   * @param data
   * @param remoteFile
   * @param remoteDir
   * @param mode
   * @throws Exception
   */
  def scp(host:String, port:Int, user:String, pwd:String, key:String,
      data:Array[Byte], remoteFile:String, remoteDir:String, mode:String ) {
    using(new Connection(host, port)) { (conn) =>
      conn.connect()
      val ok = if (! isEmpty(key)) {
        conn.authenticateWithPublicKey(user, new File(key), pwd)
      } else {
        conn.authenticateWithPassword(user, pwd)
      }
      if (!ok) {
        throw new Exception("SSH: Cant authenticate with host: " + host)
      }
      new SCPClient(conn).put(data, remoteFile, remoteDir, mode)
    }
  }

  /**
   * @param host
   * @param port std port = 22.
   * @param user
   * @param pwd
   * @param key
   * @param remoteDir
   * @param localFile
   * @param mode
   * @throws Exception
   */
  def scp(host:String, port:Int, user:String, pwd:String, key:String,
          remoteDir:String, localFile:File, mode:String) {
    using(new Connection(host, port )) { (conn) =>
      conn.connect()

      val ok = if (! isEmpty(key)) {
        conn.authenticateWithPublicKey(user, new File(key), pwd)
      } else {
        conn.authenticateWithPassword(user, pwd)
      }

      if (!ok) {
        throw new Exception("SSH: Cant authenticate with host: " + host)
      }
      new SCPClient(conn).put(niceFPath(localFile), remoteDir, mode)
    }
  }

  /**
   * @param delete
   * @param host
   * @param user
   * @param pwd
   * @param key
   * @param remoteFile
   * @param remoteDir
   * @return
   * @throws Exception
   */
  def rexec(delete:Boolean, host:String, user:String, pwd:String, key:String,
      remoteFile:String, remoteDir:String, testString:String) = {

    var success=false
    using(new Connection(host)) { (conn) =>
      conn.connect()
      val ok = if (! isEmpty(key)) {
        conn.authenticateWithPublicKey(user, new File(key), pwd)
      } else {
        conn.authenticateWithPassword(user, pwd)
      }
      if (!ok) {
        throw new Exception("SSH: Cant authenticate with host: " + host)
      }
      using(conn.openSession()) { (sess) =>
        sess.execCommand("cd " + remoteDir + " && ./" + remoteFile )
        val stdout = new StreamGobbler(sess.getStdout())
        val br = new BufferedReader(new InputStreamReader(stdout))
        var line=""
        do {
          line = br.readLine()
          if (line != null) {
            System.out.println(trim(line))
            if (line.has(testString)) { success=true }
          }
        } while (line != null)
      }
      using( conn.openSession()) { (sess) =>
        sess.execCommand("rm -f  " + remoteDir + "/" + remoteFile )
      }
    }
    success
  }


  /**
   * @param host
   * @param user
   * @param pwd
   * @param key
   * @param remoteFile
   * @param remoteDir
   * @return
   * @throws Exception
   */
  def rdelete(host:String, user:String, pwd:String, key:String,
          remoteFile:String, remoteDir:String) = {

    var success=false
    using(new Connection(host)) { (conn) =>
      conn.connect()
      val ok = if (! isEmpty(key)) {
        conn.authenticateWithPublicKey(user, new File(key), pwd)
      } else {
        conn.authenticateWithPassword(user, pwd)
      }
      if (!ok) {
        throw new Exception("SSH: Cant authenticate with host: " + host)
      }
      using(conn.openSession()) { (sess) =>
        sess.execCommand("rm -f " + remoteDir + " /" + remoteFile )
        val stdout = new StreamGobbler(sess.getStdout())
        val br = new BufferedReader(new InputStreamReader(stdout))
        var line=""
        do {
          line = br.readLine()
          if (line != null) {
            System.out.println(trim(line))
          }
        } while (line != null)
      }
    }
    success
  }


}

