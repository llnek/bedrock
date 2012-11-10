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


package com.zotoh.bedrock.svc

import com.zotoh.bedrock.core.Vars.DT_FILE
import com.zotoh.bedrock.device._

/**
 * Created with IntelliJ IDEA.
 * User: kenl
 * Date: 8/11/12
 * Time: 7:44 PM
 * To change this template use File | Settings | File Templates.
 */

class FilePickerService(name:String) extends XXXTimerService(name, DT_FILE) {

  def withFileMask(mask:String) = {
    super.withAttrString(FilePicker.PSTR_FMASK ,mask)
    this
  }

  def withRootDir(dir:String) = {
    super.withAttrString(FilePicker.PSTR_ROOTDIR, dir)
    this
  }

  def withAutoMove(destDir:String) = {
    super.withAttrString(FilePicker.PSTR_DESTDIR, destDir)
    super.withAttrBool(FilePicker.PSTR_AUTOMOVE, true)
    this
  }

  def withDelay(secs:Int) = {
    super.setDelay(secs)
    this
  }

  def withWhen(when:String) = {
    super.setWhen(when)
    this
  }

  def withInterval(secs:Int) = {
    super.setInterval(secs)
    this
  }

  def withProcessor(cz:String) = {
    super.setProc(cz)
    this
  }


  def withHandler( h: Device.EventHandler ) = {
    super.setHandler(h)
    this
  }

  override def end = {
    super.end
  }


}
