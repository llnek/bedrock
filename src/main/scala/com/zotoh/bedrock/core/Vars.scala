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

package com.zotoh.bedrock.core


/**
 * @author kenl
 *
 */
trait Vars {

  val APPPROPS= "app.properties"
  val CLOUDDATA= "cloud.json"
  val APPCONF= "app.conf"
  val LOG4J= "log4j.txt"
  val LOG4J_PROPS= "log4j.properties"

  val SHUTDOWN_PORT_PWD= "bedrock.shutdown.port.pwd"
  val SHUTDOWN_PORT= "bedrock.shutdown.port"
  val DELEGATE_CLASS= "bedrock.delegate.class"
  val MANIFEST_FILE= "bedrock.conf.file"
  val PIPLINE_MODULE= "bedrock.pipeline"
  val APP_DIR= "bedrock.app.dir"
  val SECRET_KEY= "bedrock.key"
  val USE_CLDR= "bedrock.use.classloader"
  val NIO_CHOICE= "bedrock.nio.choice"
  val ENG_PROPS= "bedrock.engine.props"
  val WEBSERVLET_PROC= "bedrock.servlet.processor"
  val FILE_ENCODING= "bedrock.file.encoding"

  val PP_IMAGEID="cloud.image"
  val PP_VMID="cloud.server"
  val PP_REGION="cloud.region"
  val PP_ZONE="cloud.zone"
  val PP_SSHKEY="cloud.sshkey"
  val PP_SECGRP="cloud.firewall"
  val PP_PRODUCT="cloud.product"

  val TDS_EVENTS= "bedrock.core.event.tds"
  val TDS_WORK= "bedrock.core.work.tds"
  val TDS_WAIT= "bedrock.core.wait.tds"

  val DT_ONESHOT= "oneshot-timer"
  val DT_WEB_SERVLET= "web-servlet"
  val DT_REPEAT= "repeat-timer"
  val DT_JETTY= "jetty"
  val DT_WEBSOC= "websocket"
  val DT_HTTPS= "https"
  val DT_HTTP= "http"
  val DT_TCP= "tcp"
  val DT_REST= "rest"
  val DT_JMS= "jms"
  val DT_FILE= "filepicker"
  val DT_POP3= "pop3"
  val DT_ATOM= "atom"
  val DT_MEMORY= "in-memory"

  val XML_ROOT= "bedrock"
  val STATE_TABLE= "BEDROCK_STATE_INFO"

  val SYS_DEVID_PFX= "system.####"
  val SYS_DEVID_SFX= "####"

  val SYS_DEVID_REGEX= SYS_DEVID_PFX+"[0-9A-Za-z_\\-\\.]+"+SYS_DEVID_SFX
  val SHUTDOWN_DEVID= SYS_DEVID_PFX+"kill_9"+SYS_DEVID_SFX
  val SHUTDOWN_URI="/kill9"

  val WEBSERVLET_DEVID= "____in_a_webservlet____"
  val INMEM_DEVID= "____in_memory____"
  val DEVID= "id"
  val DEV_STATUS= "enabled"
  val DEV_PROC= "processor"

  val WORK_DIR="bedrock.work.dir";
  val JDBC_POOLSIZE="bedrock.db.poolsize"
  val JDBC_DRIVER="bedrock.db.driver"
  val JDBC_URL="bedrock.db.url"
  val JDBC_USER="bedrock.db.user"
  val JDBC_PWD="bedrock.db.pwd"
  val JDBC_RESET="bedrock.db.reset"

  val CFGKEY_DEVHDLRS="devicehandlers"
  val CFGKEY_DEVICES="devices"
  val CFGKEY_CORES="cores"

  val CFGKEY_DEV_IMPL="device-impl-class"
  //val CFGKEY_FACTORY="factory"
  val CFGKEY_TYPE="type"
  val CFGKEY_THDS="threads"

  val CFGKEY_SOCTOUT="soctoutsecs"
  val CFGKEY_HOST="host"
  val CFGKEY_PORT="port"


  val DB_STATE_TBL="BEDROCK_STATE_INFO"
  val COL_BIN="BININFO"
  val COL_KEYID="KEYID"
  val COL_TRACKID="TRACKID"
  val COL_EXPIRY="EXPIRYTS"

  val SAMPLES= "samples"
  val REALM= ".vault"
  val KEYFILE= ".appkey"
  val APP_META= ".meta"
  val PROCID= "pid"

  val APPTYPE_WEB="webapp"
  val APPTYPE_SVR="server"

  val ANTOPT_SVCPOINT="bedrock.servicepoint"
  val ANTOPT_SCRIPTFILE="bedrock.scriptfile"
  val ANTOPT_OUTDIR="bedrock.useroutdir"

  val TESTSRC= "src/test"
  val SRC= "src/main"
  val CLSS= "classes"
  val DIST= "dist"
  val TPCL= "thirdparty"
  val BIN= "bin"
  val CFG= "cfg"
  val LOGS= "logs"
  val DB= "db"
  val LIB= "lib"
  val TMP= "tmp"
  val PATCH= "patch"
  val ECPPROJ="eclipse.projfiles"

  val APP_PACKAGE= "app.package"

}

object Vars  extends Vars {

}