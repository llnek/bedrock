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

import scala.collection.JavaConversions._
import scala.collection.mutable.{HashSet,ArrayBuffer}

import com.zotoh.fwk.io.IOUte._
import com.zotoh.fwk.util.CoreUte._
import com.zotoh.fwk.util.LoggerFactory._
import com.zotoh.fwk.util.{CoreImplicits,Logger}
import com.zotoh.fwk.util.ProcessUte._
import com.zotoh.fwk.util.StrUte._
import com.zotoh.fwk.util.WWID._

import java.io.File
import java.util.{ResourceBundle,Properties=>JPS}

import org.dasein.cloud.{Tag,CloudProvider}
import org.dasein.cloud.compute.MachineImage
import org.dasein.cloud.compute.VirtualMachine
import org.dasein.cloud.compute.VirtualMachineSupport
import org.dasein.cloud.compute.VmState
import org.dasein.cloud.dc.DataCenter
import org.dasein.cloud.dc.DataCenterServices
import org.dasein.cloud.dc.Region
import org.dasein.cloud.identity.ShellKeySupport
import org.dasein.cloud.identity.SSHKeypair
import org.dasein.cloud.network.AddressType
import org.dasein.cloud.network.Firewall
import org.dasein.cloud.network.FirewallSupport
import org.dasein.cloud.network.IpAddress
import org.dasein.cloud.network.IpAddressSupport
import org.dasein.cloud.network.Protocol

import org.json.{JSONObject=>JSNO,JSONArray=>JSNA}

import com.zotoh.cloudapi.aws.{AWSInitContext,AWSCloud}
import com.zotoh.fwk.crypto.PwdFactory

import com.zotoh.bedrock.cloud.CloudData._
import com.zotoh.bedrock.cloud.SSHUte._

/**
 *  Deals with all the cloud related setup and invocations.
 *  (Internal use only)
 *
 * @author kenl
 */
object Cloudr extends com.zotoh.bedrock.core.Vars with com.zotoh.cloudapi.core.Vars with CoreImplicits {

  private def ilog() { _log=getLogger(classOf[Cloudr]) }
  @transient private var _log:Logger=null
  def tlog() = { if(_log==null) ilog(); _log }

  private var _rcb:ResourceBundle=null
  private var _prov:AWSCloud=null
  private var _appDir:File=null
  private var _cfgDir:File=null

  /**
   * @param appDir
   * @param rc
   * @throws Exception
   */
  def configure(appDir:File, rcb:ResourceBundle) {

    tlog().debug("Cloudr: cofigure()")

    tstObjArg("app-dir", appDir)

    _cfgDir=new File(appDir, CFG)
    _rcb=rcb
    _appDir= appDir

    setDataPath( new File( _cfgDir, CLOUDDATA))
    load()

    nsb( credential().optString(P_VENDOR)).lc match {
      case "amazon"| "aws" => inizAWS()
      case v:String =>
        throw new Exception("Unknown cloud provider: " + v)
    }
  }

  /**
   * @param vendor
   * @param acct
   * @param id
   * @param pwd
   * @throws Exception
   */
  def setConfig(vendor:String, acct:String, id:String, pwd:String) {
    setCAuth(vendor, acct, id, pwd)
    configure(_appDir,_rcb)
  }

  /**
   * @param user
   * @param pwd
   * @param keyfile
   * @throws Exception
   */
  def setSSHInfo(user:String, pwd:String, keyfile:String) {
    CloudData.setSSHInfo(user, pwd, keyfile)
    doSave()
  }

  /**
   *
   */
  def finz() {
    tlog().debug("Cloudr: finz()")
    _prov=null
  }

  private def inizAWS() {
    var c= customQuirks()
    val props= new JPS()
    var it= if (c == null) null else c.keys()
    if (it!=null) while(it.hasNext() ) {
      val p= nsb( it.next())
      props.put(p, nsb( c.opt(p)) )
    }
    props.add(P_REGION, nsb ( dftRegion() )).
    add(P_ACCT, nsb( c.optString(P_ACCT)) ).
    add(P_ID, nsb( c.optString(P_ID)) ).
    add(P_PWD, nsb( c.optString(P_PWD)) )

    _prov= AWSInitContext.configure(props)
  }

  /**
   * @return
   */
  def provider():AWSCloud = _prov

  /**
   * @param arch
   * @return
   * @throws Exception
   */
  def getProduct(arch:String) = dftProduct(arch)

  /**
   * @param props
   * @param version
   * @param target
   * @throws Exception
   */
  def install(props:JPS, version:String, target:String) {

    tstEStrArg("target host@folder", target)
    tstEStrArg("version", version)
    tstObjArg("sshinfo", props)

    var script= rc2Str("com/zotoh/bedrock/util/remote_install.txt", "utf-8")
    val ss= target.split(":")
    if (ss==null || ss.length != 2) {
      errBadArg("target host:folder > " + target)
    }

    var keyfile= trim( props.gets("key"))
    val user= trim( props.gets("user"))
    val pwd= trim( props.gets("pwd"))
    val host= trim(ss(0))
    val dir= trim(ss(1))
    val rfile=newWWID()

    tlog().debug("Cloud.install() version = {}", version)
    tlog().debug("Cloud.install() host = {}", host)
    tlog().debug("Cloud.install() dir = {}", dir)

    tlog().debug("Cloud.install() user = {}", user)
    tlog().debug("Cloud.install() pwd = {}",
       if(isEmpty(pwd)) "null" else "****")
    tlog().debug("Cloud.install() key = {}", keyfile)

    script=strstr(script, "${BEDROCK_VERSION}", version)
    script=strstr(script, "${TARGET_DIR}", dir)

    if ( !isEmpty(keyfile)) {
      val f= new File(keyfile)
      if ( !f.exists() || !f.canRead()) {
        errBadArg("key file does not exist or is not readable")
      }
      keyfile=niceFPath(f)
    }

//    tlog().debug("{}" , script)

    scp( host, 22, user, pwd, keyfile, asBytes(script), rfile, "/tmp", "0700")

    if ( rexec( true, host, user, pwd, keyfile, rfile, "/tmp", "Installed OK.") ) {
      //System.out.format("Bedrock installed OK.\n")
      println("")
    } else {
      println("Failed to install \"Bedrock\".")
    }

  }

  /**
   * @param run
   * @param props
   * @param target
   * @throws Exception
   */
  def deploy(run:Boolean, props:JPS, target:String) {

    tstEStrArg("target host@folder", target)
    tstObjArg("sshinfo", props)

    var script= rc2Str("com/zotoh/bedrock/util/remote_deploy.txt", "utf-8")
    val ss= target.split(":")
    if (ss==null || ss.length != 2) {
      errBadArg("target host:folder > " + target)
    }

    var keyfile= trim( props.gets("key"))
    val user= trim( props.gets("user"))
    val pwd= trim( props.gets("pwd"))
    var pd= ""
    val localFile= trim( props.gets("bundle"))
    tstEStrArg("application bundle", localFile)

    if (run) {
      pd= trim( props.gets("bedrock"))
      tstEStrArg("remote Bedrock home", pd)
    }

    val host= trim(ss(0))
    val dir= trim(ss(1))

    val fp= new File( localFile)
    val rfile=newWWID()
    var action=""
    val fn= fp.getName()
    var rdir=strstr(fn, ".tar.gz", "")
    rdir=strstr(rdir, ".zip", "")

    tlog().debug("Cloud.install() host = {}", host)
    tlog().debug("Cloud.install() dir = {}", dir)

    tlog().debug("Cloud.install() user = {}", user)
    tlog().debug("Cloud.install() pwd = {}", if(isEmpty(pwd)) "null" else "****")
    tlog().debug("Cloud.install() key = {}", keyfile)

    if (!isEmpty(keyfile)) {
      val f= new File(keyfile)
      if ( !f.exists() || !f.canRead()) {
        errBadArg("key file does not exist or is not readable")
      }
      keyfile=niceFPath(f)
    }

    if (fn.endsWith(".tar.gz")) { action="tar xvfz " }
    else
    if (fn.endsWith(".zip")) { action="unzip " }
    else {
      errBadArg("bundle file must be tarzipped or ziped")
    }

    script=strstr(script, "${TARGET_FILE}",  "/tmp/" + fn)
    script=strstr(script, "${TARGET_DIR}", dir)
    script=strstr(script, "${TARGET_FILE_DIR}", rdir )
    script=strstr(script, "${BEDROCK_HOME}", pd )
    script=strstr(script, "${UNPACK_ACTION}", action )

    scp(host, 22, user, pwd, keyfile, "/tmp", fp, "0644")
    scp(host, 22, user, pwd, keyfile, asBytes(script), rfile, "/tmp", "0700")

    if ( rexec( true, host, user, pwd, keyfile, rfile, "/tmp", "Deployed OK.") ) {
      println("")
    } else {
      println("Failed to deploy application.")
    }
  }

  /**
   * @return
   */
  def imageId() = dftImage()

  /**
   * @throws Exception
   */
  def syncRegions() {

    println("%s".format( bundleStr(_rcb, "cmd.cloud.req.preamble")) )

    val lst=_prov.getDataCenterServices().listRegions()
    val ps= new JPS()
    var ok=false

    val dft=dftRegion()
    val rgs=regions()
    var s=""

    s=bundleStr(_rcb, "cmd.available.regions") +":"
    println("%s\n%s".format( s, underline(s.length())) )

    lst.foreach { (r) =>
      s=r.getName()
      ps.put(s, r.getProviderRegionId())
      if (!rgs.has(s)) {
        rgs.put(s, new JSNO())
      }
      if (s==dft) { ok=true }
      println("%s".format(s) )
    }

    if (!ok) { setDftRegion("") }
    preSave()
    doSave()
  }

  /**
   * @throws Exception
   */
  def syncDatacenters() {

    println("%s".format( bundleStr(_rcb, "cmd.cloud.req.preamble")) )

    val svc=_prov.getDataCenterServices()
    val rgs=regions()
    var s=""

    s=bundleStr(_rcb, "cmd.available.zones")+":"
    println("%s\n%s".format( s, underline(s.length())) )

    val it= rgs.keys()
    while (it.hasNext() ) {
      s= nsb( it.next())
      val j= rgs.optJSONObject(s)
      if (j!=null) svc.listDataCenters(s).foreach { (c) =>
        val z=c.getProviderDataCenterId()
        j.put(z, new JSNO())
        println("%s) %s".format( s, z) )
      }
    }

    preSave()
    doSave()

  }

  /**
   * @param image
   */
  def setImage(image:String) {

    if (!isEmpty(image)) try {

      println("%s".format( bundleStr(_rcb, "cmd.cloud.req.preamble")) )
      println("%s".format( bundleStr(_rcb, "cmd.cloud.image.info")) )

      val mi=_prov.getComputeServices().getImageSupport().getMachineImage(image)
      if (mi != null) {
        val obj= CloudData.images()
        val j=new JSNO()
        setDftImage(image)

        j.put(P_PLATFORM, if(mi.getPlatform().isLinux()) PT_LINUX else PT_WINDOWS)
        j.put(P_ARCH, mi.getArchitecture().name())
        obj.put(image, j)

        println("%s: %s\n%s: %s".format(
            bundleStr(_rcb, "cmd.image.platform"), j.optString(P_PLATFORM),
            bundleStr(_rcb, "cmd.image.arch"),
            bundleStr(_rcb, if(I32==mi.getArchitecture()) "cmd.32bit" else  "cmd.64bit")
        ) )
      }
    }
    catch {
      case e => error(e); tlog().warnX("",Some(e))
    }

    setDftImage( image )
    preSave()
    doSave()
  }

  /**
   * @return
   */
  def server() = dftServer()

  /**
   * @param vm
   */
  def setServer(vm:String) {
    setDftServer( vm)
    doSave()
  }

  /**
   * @return
   */
  def SSHKey() = dftKey()

  /**
   * @param key
   */
  def setSSHKey(key:String) {
    setDftKey(key)
    doSave()
  }

  /**
   * @return
   * @throws Exception
   */
  def dataCenter() = dftZone()

  /**
   * @param dc
   * @throws Exception
   */
  def setDataCenter(dc:String) {
    setDftZone( dc )
    doSave()
  }

  /**
   * @return
   */
  def region() = dftRegion()

  /**
   * @param region
   */
  def setRegion(region:String) {
    setDftRegion( region)
    doSave()
  }

  /**
   * @return
   */
  def secGrp() = dftFirewall()

  /**
   * @param grp
   */
  def setSecGrp(grp:String) {
    setDftFirewall( grp )
    doSave()
  }

  /**
   * @param image
   * @param ptype
   * @param key
   * @param groups
   * @param zone
   * @throws Exception
   */
  def launchImage(img:String, ptype:String, key:String,
      groups:Seq[String], region:String, zone:String) {

    val ami= if (isEmpty(img)) imageId() else img

    tstEStrArg("product or instance type", ptype)
    tstEStrArg("image-id", ami)
    tstEStrArg("ssh key name", key)
    tstEStrArg("region", region)
    tstObjArg("groups", groups)

    tlog().debug("LaunchImage: groups {} " ,  join(groups, "|"))
    tlog().debug("LaunchImage: region {} " , region)
    tlog().debug("LaunchImage: zone {} " , nsn( zone))

    tlog().debug("LaunchImage: product {} " ,  ptype)
    tlog().debug("LaunchImage: image {} " ,  ami)
    tlog().debug("LaunchImage: key {} " , key)

    println("%s".format(
        bundleStr(_rcb, "cmd.cloud.req.preamble")) )

    val vs= _prov.getComputeServices().getVirtualMachineSupport()
    val vm= vs.launch(ami, vs.getProduct(ptype),
        region+"|" + nsb(zone), "", "", key, "", false, false, groups.toList.toArray)

    if (vm != null) {
      testVmReady(vm, vm.getProviderVirtualMachineId())
    }

  }

  private def testVmReady(vma:VirtualMachine, vmid:String) {

    val vs= _prov.getComputeServices().getVirtualMachineSupport()
    val s1=bundleStr(_rcb,"cmd.vm.lbl")+": "
    val s2=bundleStr(_rcb,"cmd.state")+": "
    val s3=bundleStr(_rcb, "cmd.check.wait", "8")
    var vm = if (vma==null) vs.getVirtualMachine(vmid) else vma
    var st=VmState.PENDING
    var ip=""
    var name=""

    while (vm != null) {

      name= vm.getProviderVirtualMachineId()
      st=vm.getCurrentState()

      println("%-32s%-30s".format( s1 + name,  s2 + st.name() ) )

      if ( VmState.PENDING == st) {
        println("%s".format(  s3) )
        safeThreadWait(8000)
        vm=vs.getVirtualMachine(name)
      }
    }

    if (vm != null) {
      ip=vm.getPublicDnsAddress()
      st=vm.getCurrentState()
      println("%-32s%-30s%-24s".format( s1+name, s2+st.name(),
          bundleStr(_rcb,"cmd.public.dns") +": " + ip) )
    }

    if (!isEmpty(ip)) {
      val obj= servers()
      val j= new JSNO()
      j.put(P_PUBDNS, ip)
      obj.put(name, j)
      doSave()
    }

  }

  /**
   * @param vmid
   * @throws Exception
   */
  def startServer(vm:String) {

    val vmid = if (isEmpty(vm)) server() else vm
    tstEStrArg("server-hostvm-id", vmid)

    println("%s".format( bundleStr(_rcb, "cmd.cloud.req.preamble")) )

    _prov.getComputeServices().getVirtualMachineSupport().boot(vmid)
    testVmReady(null, vmid)
  }

  /**
   * @param vmid
   * @throws Exception
   */
  def terminateServer(vm:String) {

    val vmid = if (isEmpty(vm)) server() else vm
    tstEStrArg("server-hostvm-id", vmid)

    println("%s".format( bundleStr(_rcb, "cmd.cloud.req.preamble")) )

    _prov.getComputeServices().getVirtualMachineSupport().terminate(vmid)
    CloudData.servers().remove(vmid)

    preSave()
    doSave()
  }

  /**
   * @param vmid
   * @throws Exception
   */
  def stopServer(vm:String) {

    val vmid = if (isEmpty(vm)) server() else vm
    tstEStrArg("vm-server-id", vmid)

    println("%s".format( bundleStr(_rcb, "cmd.cloud.req.preamble")) )

    val vs= _prov.getComputeServices().getVirtualMachineSupport()
    val vma= vs.getVirtualMachine(vmid)
    if (vma != null) {
      if ( !vma.isPausable()) {
        println("%s".format( bundleStr(_rcb, "cmd.vm.not.pausable")) )
      } else {
        vs.pause(vmid)
        preSave()
        doSave()
      }
    }

  }

  /**
   * @param vmid
   * @throws Exception
   */
  def descServer(vm:String) {

    var vmid= if (isEmpty(vm)) server() else vm
    tstEStrArg("vm-server-id", vmid)

    println("%s".format( bundleStr(_rcb, "cmd.cloud.req.preamble")) )

    val vma= _prov.getComputeServices().getVirtualMachineSupport().getVirtualMachine(vmid)
    if (vma==null) { return }

    vmid=vma.getProviderVirtualMachineId()
    val st=vma.getCurrentState().name()
    val obj= servers()
    val dns=nsb( vma.getPublicDnsAddress())

    drawTable("\n%-16s%-8s%-12s%-16s%-20s\n",
        bundleStr(_rcb, "cmd.vmid"),
        bundleStr(_rcb, "cmd.state"),
        bundleStr(_rcb, "cmd.region"),
        bundleStr(_rcb, "cmd.zone"),
        bundleStr(_rcb, "cmd.public.ip"))
    println("%s".format( underline(78)) )

    println("%-16s%-8s%-12s%-16s%-20s".format( vmid,   st.charAt(0).toString(),
        vma.getProviderRegionId(), vma.getProviderDataCenterId(),
        join(vma.getPublicIpAddresses(),"|")) )
    println("%s-> %s".format( bundleStr(_rcb, "cmd.public.dns"), dns) )

    obj.remove(vmid)
    if ("terminated" != st.toLowerCase()) {
      val j=new JSNO()
      obj.put(vmid,  j)
      j.put(P_PUBDNS, dns)
      j.put(P_REGION, nsb(vma.getProviderRegionId()) )
      j.put(P_ZONE, nsb(vma.getProviderDataCenterId()))
    }

    preSave()
    doSave()
  }

  /**
   * @throws Exception
   */
  def listServers() {

    println("%s".format( bundleStr(_rcb, "cmd.cloud.req.preamble")) )

    val lst= _prov.getComputeServices().getVirtualMachineSupport().listVirtualMachines()
    val obj= servers()
    val ok= lst.iterator().hasNext()
    var s=bundleStr(_rcb, "cmd.available.vms")+":"
    println("%s\n%s\n%s".format(s,
          underline(s.length()), if(ok) "" else  "none") )

    if (ok) {
      drawTable("%-16s%-8s%-12s%-16s%-20s\n",
          bundleStr(_rcb, "cmd.vmid"),
          bundleStr(_rcb, "cmd.state"),
          bundleStr(_rcb, "cmd.region"),
          bundleStr(_rcb, "cmd.zone"),
          bundleStr(_rcb, "cmd.public.ip"))
      println("%s".format( underline(78)) )
    }

    val bin= HashSet[String]()
    val it= lst.iterator()
    var pos=0
    var vmid=""
    while (it.hasNext()) {
      if (pos > 0) { println("") }

      val vm=it.next()
      val st=vm.getCurrentState().name().lc
      val dns=nsb( vm.getPublicDnsAddress())
      vmid=vm.getProviderVirtualMachineId()

      println("%-16s%-8s%-12s%-16s%-20s".format( vmid,  st.charAt(0).toString(),
          vm.getProviderRegionId(), vm.getProviderDataCenterId(),
          join(vm.getPublicIpAddresses(),"|")) )
      println("%s-> %s".format( bundleStr(_rcb, "cmd.public.dns"), dns) )

      obj.remove(vmid)
      if ("terminated"!=st) {
        val j=new JSNO()
        obj.put(vmid, j)
        j.put(P_REGION, vm.getProviderRegionId())
        j.put(P_ZONE, vm.getProviderDataCenterId())
        j.put(P_PUBDNS, dns)
        bin += vmid // keep track of the good ones
      }

      pos += 1
    }

    // get rid of ones that don't exist anymore
    val arr= obj.names()
    val len= if(arr==null) 0 else arr.length()
    for ( i <- 0 until len) {
      vmid= nsb( arr.get(i))
      if (! bin.contains(vmid)) {
        obj.remove(vmid)
      }
    }

    preSave()
    doSave()
  }

  /**
   * @param key
   * @throws Exception
   */
  def removeSSHKey(key:String) {

    if (isEmpty(key)) { } else {
      println("%s".format( bundleStr(_rcb, "cmd.cloud.req.preamble")) )
      val ssh=_prov.getIdentityServices().getShellKeySupport()
      ssh.deleteKeypair(key)
      if (key == dftKey()) { setDftKey("") }
      val obj= SSHKeys()
      if (obj.has(key)) { obj.remove(key) }
      println("%s".format( bundleStr(_rcb, "cmd.deleted.key")) )
      preSave()
      doSave()
    }
  }

  /**
   * @throws Exception
   */
  def listSSHKeys() {

    println("%s".format( bundleStr(_rcb, "cmd.cloud.req.preamble")) )
    val keys= _prov.getIdentityServices().getShellKeySupport().list()
    val obj= SSHKeys()
    val dft=dftKey()
    var ok=false
    var s=""
    val ks = (ArrayBuffer[String]() /: keys) { (b, k) =>
      s=k.getName()
      b += s
      // check is current default is ok
      if ( !obj.has(s))  { obj.put(s, new JSNO()) }
      if (s==dft) { ok=true }
      b
    }

    if (!ok) { setDftKey("") }

    s=bundleStr(_rcb, "cmd.available.keys")+":"
    println("%s\n%s\n%s".format(
        s,
        underline(s.length()),
        if(keys.isEmpty()) "none" else   join(ks, "\n")) )

    preSave()
    doSave()

  }

  /**
   * @param key
   * @param path
   * @throws Exception
   */
  def addSSHKey(key:String, path:String) {

    println("%s".format( bundleStr(_rcb, "cmd.cloud.req.preamble")) )

    val ssh=_prov.getIdentityServices().getShellKeySupport()
    val out=new File(path)
    out.getParentFile().mkdirs()

    val kpair= ssh.createKeypair(key)
    var pem= asString(kpair.getPrivateKey())
    writeFile( out, pem, "utf-8")

    println("%s".format( bundleStr(_rcb, "cmd.keyfile.saved", niceFPath(out))) )

    val obj= SSHKeys()
    val empty= !obj.keys().hasNext()
    val j=new JSNO()
    obj.put(key, j)
    pem= PwdFactory.mk(pem).encoded()
    j.put(P_PEM, pem)

    if (empty) { setDftKey(key) }

    preSave()
    doSave()
  }

  /**
   * @throws Exception
   */
  def listFwalls() {

    println("%s".format( bundleStr(_rcb, "cmd.cloud.req.preamble")) )

    val walls= _prov.getNetworkServices().getFirewallSupport().list()
    val dft= dftFirewall()
    val obj= firewalls()
    var ok=false
    var s=""
    val b = (new StringBuilder /: walls) { (b, w) =>
      // check is current default is ok
      val gn=w.getName()
      addAndDelim(b, "\n", gn)
      if (gn==dft) { ok=true }
      if ( !obj.has(gn))  { obj.put(gn, new JSNO()) }
      b
    }
    if (!ok) { setDftFirewall("") }

    s=bundleStr(_rcb, "cmd.available.groups")+":"
    println("%s\n%s\n%s".format(
        s,
        underline(s.length()),
        if(b.length()==0) "none" else b) )

    preSave()
    doSave()
  }

  /**
   * @param fw
   * @throws Exception
   */
  def removeFwall(fw:String) {

    if (isEmpty(fw)) { } else {
      println("%s".format( bundleStr(_rcb, "cmd.cloud.req.preamble")) )
      val fs= _prov.getNetworkServices().getFirewallSupport()
      fs.delete(fw)
      if (fw == dftFirewall() ) { setDftFirewall("") }
      val obj= firewalls()
      if (obj.has(fw)) { obj.remove(fw) }
      println("%s".format( bundleStr(_rcb, "cmd.deleted.group")) )
      preSave()
      doSave()
    }
  }

  /**
   * @param fw
   * @param desc
   * @throws Exception
   */
  def addFwall(fw:String, desc:String) {

    tstEStrArg("firewall-securitygroup", fw)

    println("%s".format( bundleStr(_rcb, "cmd.cloud.req.preamble")) )
    _prov.getNetworkServices().getFirewallSupport().create(fw, desc)
    preSave()
    doSave()
  }

  /**
   * @param rule
   * @throws Exception
   */
  def addCidr(rule:String) {

    println("%s".format( bundleStr(_rcb, "cmd.cloud.req.preamble")) )

    val t= splitRule(rule)
    _prov.getNetworkServices().getFirewallSupport().
      authorize( nsb(t._1), nsb(t._3), t._2, t._4, t._5)
    preSave()
    doSave()
  }

  /**
   * @param rule
   * @throws Exception
   */
  def revokeCidr(rule:String) {

    println("%s".format( bundleStr(_rcb, "cmd.cloud.req.preamble")) )

    val t= splitRule(rule)
    _prov.getNetworkServices().getFirewallSupport().revoke( nsb(t._1), nsb(t._3), t._2, t._4, t._5)
    preSave()
    doSave()
  }

  /**
   * @throws Exception
   */
  def listEIPs() {

    println("%s".format( bundleStr(_rcb, "cmd.cloud.req.preamble")) )

    val lst= _prov.getNetworkServices().getIpAddressSupport().listPublicIpPool(false)
    val ok=lst.iterator().hasNext()
    var s=bundleStr(_rcb, "cmd.available.ips")+":"
    println("%s\n%s\n%s".format(
          s,
          underline(s.length()),
          if(ok) "" else  "none") )

    if (ok) {
      drawTable("%-20s%-16s%-16s\n",
              bundleStr(_rcb, "cmd.public.ip"),
              bundleStr(_rcb, "cmd.region"),
          bundleStr(_rcb, "cmd.vmid"))
      println("%s".format( underline(78)) )
    }

    val obj=ipAddrs()
    val bin = (HashSet[String]() /: lst) { (b, ip) =>
      val vmid=nsb(ip.getServerId())
      val addr=ip.getAddress()
      val rg=ip.getRegionId()
      println("%-20s%-16s%-16s".format( addr, rg, vmid ) )
      b += addr
      obj.remove(addr)
      val j=new JSNO()
      obj.put(addr,j)
      j.put(P_REGION, rg)
      j.put(P_VM, vmid)
      b
    }

    // get rid of ones that don't exist anymore
    val arr= obj.names()
    val len= if(arr==null) 0 else arr.length()
    for ( i <- 0 until len) {
      s= nsb( arr.get(i))
      if (! bin.contains(s)) {
        obj.remove(s)
      }
    }

    preSave()
    doSave()
  }

  def removeEIP(ip:String) {

    println("%s".format( bundleStr(_rcb, "cmd.cloud.req.preamble")) )

    _prov.getNetworkServices().getIpAddressSupport().releaseFromPool(ip)
    ipAddrs().remove(ip)
    preSave()
    doSave()
  }

  /**
   * @param region
   * @throws Exception
   */
  def addEIP(region:String) {

    println("%s".format( bundleStr(_rcb, "cmd.cloud.req.preamble")) )

    if (!isEmpty(region)) {
      _prov.getContext().setRegionId(region)
    }

    val addr= _prov.getNetworkServices().getIpAddressSupport().request(AddressType.PUBLIC)
    println("%s".format( addr) )

    val obj=ipAddrs()
    val j=new JSNO()
    obj.put(addr, j)
    j.put(P_REGION, _prov.getContext().getRegionId())
    j.put(P_VM, "")

    preSave()
    doSave()
  }

  /**
   * @param ip
   * @param vmid
   * @throws Exception
   */
  def setEIP(ip:String, vmid:String) {

    println("%s".format( bundleStr(_rcb, "cmd.cloud.req.preamble")) )

    val svc=_prov.getNetworkServices().getIpAddressSupport()
    if ("0" == vmid) {
      svc.releaseFromServer(ip)
    } else {
      svc.assign(ip, vmid)
    }

    safeThreadWait(3000)
    listEIPs()
  }

  private def splitRule(rs:String) = {
    tstEStrArg("firewall-rule", rs)
    var rule= rs
    var pos= rule.indexOf('@')
    if (pos < 0) { errBadArg("Malformed rule: " + rule) }
    var g= rule.substring(0, pos)
    if (isEmpty(g)) { g= secGrp() }
    tstEStrArg("firewall-securitygroup", g)
    rule= rule.substring(pos+1)
    val ss= rule.split("#")
    if (isNilSeq(ss) || ss.length < 3) {errBadArg("Malformed rule: " + rule) }
    val pc= if("tcp"==ss(0)) Protocol.TCP else if("udp"==ss(0)) Protocol.UDP else null
    tstObjArg("firewall-protocol", pc)
    val cidr=ss(1)
    val p1= asInt(ss(2), 0)
    val p2= if(ss.length > 3) asInt(ss(3), 0) else p1
    tstPosIntArg("from-port", p1)
    tstPosIntArg("to-port", p2)
    (g, pc, cidr, p1, p2)
  }

  private def preSave() {
    println("")
  }

  private def doSave() {
    CloudData.save()
    //System.out.format("%s\n", getResourceStr(_rcb, "cmd.cloud.success"))
  }

  private def error(e:Throwable) {
    println("\n%s\n".format(
        bundleStr(_rcb, "cmd.cloud.error",   e.getMessage())) )
  }

  private def underline(len:Int)  = mkString('-',len)

  private def drawTable(fmt:String, strs:Object* ) {
    print(fmt.format( strs) )
  }

}

sealed class Cloudr {}
