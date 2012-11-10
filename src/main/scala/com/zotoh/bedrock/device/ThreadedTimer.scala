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

package com.zotoh.bedrock.device

import com.zotoh.fwk.util.ProcessUte._

/**
 * Looping mechanism using a thread to implement a periodic timer.
 *
 * @author kenl
 */
abstract class ThreadedTimer(devMgr:DeviceMgr) extends RepeatingTimer(devMgr) {

  @volatile private  var _readyToLoop=false
  @volatile private  var _tictoc=false

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.Poller#onStart()
   */
  override def onStart() {
    _readyToLoop= true
    _tictoc=true
    preLoop()
    schedule()
  }

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.Poller#onStop()
   */
  override def onStop() {
    _readyToLoop= false
    _tictoc=false
    endLoop()
  }

  /* (non-Javadoc)
   * @see com.zotoh.bedrock.device.RepeatingTimer#schedule()
   */
  override def schedule() {
    val me=this
    asyncExec(new Runnable() {
      def run() {
        while ( me.loopy()) try {
            me.onOneLoop()
        }
        catch {
          case e => tlog().warn("",e)
        }
      }
    })
  }

  /**
   * @return
   */
  protected def readyToLoop() = _readyToLoop

  /**
   * @throws Exception
   */
  protected def preLoop() = {}

  /**
   *
   */
  protected  def endLoop() = {}

  /**
   * @throws Exception
   */
  protected def onOneLoop():Unit

  private def loopy() = {
    if (! _readyToLoop) false else {
      if (_tictoc) {
        _tictoc=false
        safeThreadWait( delayMillis() )
      } else {
        safeThreadWait( intervalMillis() )
      }
      _readyToLoop
    }
  }

}
