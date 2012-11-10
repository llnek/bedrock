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

import scala.collection.mutable.HashMap
import scala.actors.Actor



import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.util.StrUte._
import com.zotoh.fwk.util.LoggerFactory._
import com.zotoh.fwk.util.{Logger}

import java.io.IOException
import java.util.{Properties=>JPS,ResourceBundle}

import org.json.{JSONObject=>JSNO}

import com.zotoh.fwk.util.{CmdLineMust,CmdLineQ,CmdLineSeq}
import com.zotoh.bedrock.core.{Job,Pipeline,Vars}

object Device {
//  type EventHandler[+Event] = (Event => Unit)
  type EventHandler = (Job => Unit)
}

/**
 * A Device is a software component which produces events.  Each device must have a unique name (id).
 *
 * The set of basic properties:
 *
 * <b>id</b>
 * The name of this device, e.g. dev-1
 * <b>processor</b>
 * The processor class to handle events from this device.  If not defined, then runtime will ask the application delegate for
 * a processor.
 *
 * @author kenl
 */
abstract class Device(private val _devMgr:DeviceMgr) extends Actor with Vars {

  private def ilog() { _log=getLogger(classOf[Device]) }
  @transient private var _log:Logger=null
  def tlog() = { if(_log==null) ilog(); _log }


  private val _backlog=HashMap[Any,WaitEvent]()
  private var _enabled= true
  private var _status=false
  private var _proc=""
  private var _id= uid()
  private var _handler:Device.EventHandler = null

  def act = loop {
    react {
      case "unload" => onUnloadEvent()
      case "load" => onLoadEvent()
      case "start" => onStartEvent()
      case "stop" => onStopEvent()
    }
  }

  /**
   * @return
   */
  def deviceMgr() = _devMgr

  /**
   * @param pps
   * @throws Exception
   */
  def configure(pps:JSNO) {
    inizCommon(pps)
    inizWithQuirks(pps)
  }

  /*
   * Returns true if this device supports configuration via the console - text based menu.
   * */
  def supportsConfigMenu() = false

  /**
   * @param rcb The bundle from which messages are read.
   * @param out This is where the captured values are placed.
   * @return false means ignore this operation, such as the user decided to cancel during input.
   * @throws IOException
   */
  def showConfigMenu(rcb:ResourceBundle, out:JPS) = {
    val props=new JPS()
    var ok=false
    if (supportsConfigMenu()) getCmdSeq(rcb, props) match {
      case Some(s) =>
        if ( ! s.start(props).isCanceled()) {
          out.putAll(props)
          ok=true
        }
      case _ =>
    }
    ok
  }

  /**
   * @param rcb
   * @param pps
   * @return
   * @throws Exception
   */
  protected def getCmdSeq(rcb:ResourceBundle, pps:JPS):Option[CmdLineSeq] = {

    val p= new CmdLineQ( "proc", bundleStr(rcb, "cmd.dev.proc") ) {
      def onRespSetOut(a:String, p:JPS) = {
        p.put(DEV_PROC, a)
        ""
      }}
    val q0= new CmdLineMust("dev", bundleStr(rcb, "cmd.dev.id")) {
      def onRespSetOut(a:String, p:JPS) = {
        p.put(DEVID, a)
        "proc"
      }}

    Some(new CmdLineSeq(Array(q0, p)) {
      def onStart() =  q0.label()
    })
  }

  /**
   * @return if false, this device will not be *started*.
   */
  def isEnabled() = _enabled

  /**
   * Mark this device as *non start-able*.  If the device is currently active, it will be stopped.
   */
  def disable() {
    if ( isActive()) { onStopEvent() }
    _enabled=false
  }

  /**
   * Mark this device as *start-able*.
   */
  def enable() { _enabled=true  }

  /**
   * @return true if this device is currently running - has been started.
   */
  def isActive() = _status

  /**
   * @return the identity of this device, should be unique within the set of devices managed by the device-manager.
   */
  def id() = _id

  /**
   * Pushes this event downstream to the application space.
   *
   * @param ev
   */
  def dispatch(ev:Event) {
    _devMgr.engine().jobCtr().create(ev)
  }

  def onUnloadEvent() {
    tlog().info( "Device: unloading type= {}" , this.getClass().getName() )
    block { () => this.exit() }
    _devMgr.remove(this)
  }

  def onLoadEvent() {
    if (isEnabled()) {
      tlog().info( "Device: loading type= {}" , this.getClass().getName() )
    }
  }

  /**
   * Activate this device.
   *
   * @throws Exception
   */
  def onStartEvent() {
    if (isEnabled()) {
      onStart()
      _status=true
    }
  }

  /**
   * Deactivate this device.
   */
  def onStopEvent() {
    if (isActive()) try {
      tlog().debug("Device: about to stop {}, id= {}", getClass().getName(), id() )
      onStop()
    } finally {
      _status=false
    }
  }

  /**
   * If this event is currently on hold, release it as processing can be resume on this event, most likely
   * due to the application indicating that a result is ready.
   *
   * @param w
   */
  def releaseEvent(w:WaitEvent) {
    if (w != null) { _backlog.remove(w.id()) }
  }

  /**
   * Hold on to this event for now (meaning queue it but ignore it for now), the downstream application will
   * process it and until a result is ready, no processing is needed for this event.
   *
   * @param w
   */
  def holdEvent(w:WaitEvent) {
    if (w != null) { _backlog += Tuple2(w.id(), w) }
  }

  /**
   * Internal use only.
   *
   * @param j
   * @return
   */
  def pipeline(j:Job):Option[Pipeline] =  {

    if (_handler != null) {
      _handler(j)
      j.engine().module().newPipeline()
    }
    else
    if (isEmpty(_proc)) {
      None
    }
    else {
      j.engine().module().newPipeline(_proc, j)
    }

  }

  /**
   * Initialize this device with a set of properties.
   *
   * @param pps
   * @throws Exception
   */
  protected def inizWithQuirks(pps:JSNO):Unit

  /**
   * Do something to start this device.
   *
   * @throws Exception
   */
  protected def onStart():Unit

  /**
   * Do something to stop this device.
   */
  protected def onStop():Unit

  def bindHandler(f: Device.EventHandler) {
    _handler=f
  }

  /**
   * @param pps
   */
  protected def inizCommon(pps:JSNO) {
    val str= trim( pps.optString( DEVID ))
    this._id= str
    var b= pps.optBoolean( DEV_STATUS)
    if (pps.has(DEV_STATUS) && b==false) {
      // device explicitly turned off
      disable()
    }
    _proc= trim( pps.optString( DEV_PROC ))
  }



}

