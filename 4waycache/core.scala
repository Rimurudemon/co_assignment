package sodor.stage2

import chisel3._
import chisel3.util._
import sodor.common._
import org.chipsalliance.cde.config.Parameters
import freechips.rocketchip.rocket.CoreInterrupts

// Core I/O Bundle
class CoreIo(implicit val p: Parameters, val conf: SodorCoreParams) extends Bundle {
  val imem = new MemPortIo(conf.xprlen)
  val dmem = new MemPortIo(conf.xprlen)
  val ddpath = Flipped(new DebugDPath())
  val dcpath = Flipped(new DebugCPath())
  val interrupt = Input(new CoreInterrupts(false))
  val hartid = Input(UInt())
  val reset_vector = Input(UInt())
}

class Core(implicit val p: Parameters, val conf: SodorCoreParams) extends AbstractCore with MemoryOpConstants {
  val io = IO(new CoreIo())
  val c = Module(new CtlPath())
  val d = Module(new DatPath())


// CONNECTING THE CACHE MODULE TO THE CORE -------------------------------------------------------
  val cache = Module(new Cache())

  cache.io.cpu_req := c.io.dmem.req.valid                
  cache.io.cpu_addr := c.io.dmem.req.bits.addr            
  cache.io.cpu_wr := (c.io.dmem.req.bits.fcn === M_XWR)   
  cache.io.cpu_wdata := d.io.dmem.req.bits.data           

  // Connect Cache output to DatPath
  d.io.dmem.resp.valid := cache.io.cpu_valid             
  d.io.dmem.resp.bits.data := cache.io.cpu_rdata         
  io.dmem.req.valid := cache.io.mem_req                  // Cache memory request valid goes to dmem valid
  io.dmem.req.bits.addr := cache.io.mem_addr              // Cache memory address
  io.dmem.req.bits.data := cache.io.mem_wdata            // Cache memory write data
  io.dmem.req.bits.fcn := Mux(cache.io.mem_wr, M_XWR, M_XRD) // Memory operation type: write or read

  cache.io.mem_valid := io.dmem.resp.valid               // Cache memory response valid connected to dmem response valid
  cache.io.mem_rdata := io.dmem.resp.bits.data           // Cache memory read data from dmem

  c.io.dmem.req.ready := cache.io.cpu_req                // Ready if cache is ready to accept requests

  // Default initialization for control path signals
  c.io.dmem.resp.valid := false.B
  c.io.dmem.resp.bits.data := 0.U
  when(cache.io.cpu_valid) {
    c.io.dmem.resp.valid := true.B
    c.io.dmem.resp.bits.data := cache.io.cpu_rdata
  }

  // Default initialization for the data path's ready signal
  d.io.dmem.req.ready := false.B
  when(cache.io.mem_req) {
    d.io.dmem.req.ready := true.B
  }

  c.io.ctl  <> d.io.ctl
  c.io.dat  <> d.io.dat

  io.imem <> c.io.imem
  io.imem <> d.io.imem

  io.dmem <> c.io.dmem
  io.dmem <> d.io.dmem
  io.dmem.req.valid := c.io.dmem.req.valid
  io.dmem.req.bits.typ := c.io.dmem.req.bits.typ
  io.dmem.req.bits.fcn := c.io.dmem.req.bits.fcn

  d.io.ddpath <> io.ddpath
  c.io.dcpath <> io.dcpath

  d.io.interrupt := io.interrupt
  d.io.hartid := io.hartid
  d.io.reset_vector := io.reset_vector

  val mem_ports = List(io.dmem, io.imem)
  val interrupt = io.interrupt
  val hartid = io.hartid
  val reset_vector = io.reset_vector
}
