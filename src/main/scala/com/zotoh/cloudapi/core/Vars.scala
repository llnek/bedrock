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

package com.zotoh.cloudapi.core


/**
 * @author kenl
 *
 */
trait Vars {

  val PT_WINDOWS="windows"
  val PT_LINUX="linux"

  val I64= "I64"
  val I32= "I32"

  val P_CUSTOM= "custom"

  val P_CRED= "credential"
  val P_KEYS= "sshkeys"
  val P_FWALLS= "firewalls"
  val P_IPS= "eips"
  val P_VMS= "vms"
  val P_SSHINFO= "sshinfo"

  val P_PUBDNS= "pubdns"

  val P_VENDOR= "provider"
  val P_PWD= "pwd"
  val P_ID= "id"
  val P_ACCT= "account"

  val P_IMAGES= "images"
  val P_ARCH= "arch"
  val P_PLATFORM= "platform"
  val P_PRODUCT= "product"

  val P_REGIONS= "regions"
  val P_DFTS= "defaults"

  val P_REGION= "region"
  val P_ZONE= "zone"
  val P_IMAGE= "image"
  val P_KEY= "key"
  val P_FWALL= "firewall"
  val P_VM= "vm"

  val P_PEM= "pem"
  val P_IP= "ip"

  val P_USER="user"

}
