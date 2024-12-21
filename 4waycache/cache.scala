//following is the code for 4 way set associative cache 
package sodor.stage2

import chisel3._
import chisel3.util._
import sodor.common._

class Cache extends Module {
  val io = IO(new Bundle {
    val cpu_req    = Input(Bool())
    val cpu_addr   = Input(UInt(32.W))
    val cpu_wr     = Input(Bool())
    val cpu_wdata  = Input(UInt(32.W))
    val cpu_valid  = Output(Bool())
    val cpu_rdata  = Output(UInt(32.W))
    
    // Memory interface
    val mem_req    = Output(Bool())
    val mem_addr   = Output(UInt(32.W))
    val mem_wr     = Output(Bool())
    val mem_wdata  = Output(UInt(32.W))
    val mem_valid  = Input(Bool())
    val mem_rdata  = Input(UInt(32.W))
  })

  // Change the following parameters to change the respective cache properties
  val blockSize = 4  
  val cacheSize = 8192  
  val numLines = cacheSize / blockSize / 4  // 512 lines per set (4-way associative)

  val latency_cycles = 10
  val mem_access_cycles = RegInit(0.U(32.W))
  
  val tagWidth = 32 - log2Ceil(numLines) - log2Ceil(blockSize)
  val indexWidth = log2Ceil(numLines)
  
  val validBits = RegInit(VecInit(Seq.fill(4)(VecInit(Seq.fill(numLines)(false.B)))))
  val tags = Reg(Vec(4, Vec(numLines, UInt(tagWidth.W))))
  val data = Reg(Vec(4, Vec(numLines, UInt(32.W))))
  val lru = RegInit(VecInit(Seq.fill(numLines)(0.U(2.W)))) // 2-bit LRU for each set
  
  val addr_tag = io.cpu_addr(31, indexWidth + 2)
  val addr_index = io.cpu_addr(indexWidth + 1, 2)
  
  val line_valid = VecInit(validBits.map(_(addr_index)))
  val line_tag = VecInit(tags.map(_(addr_index)))
  val line_data = VecInit(data.map(_(addr_index)))
  
  val hit = line_valid.zip(line_tag).map { case (v, t) => v && (t === addr_tag) }
  val hit_index = PriorityEncoder(hit)
  val is_hit = hit.reduce(_ || _)
  
  val idle :: miss :: Nil = Enum(2)
  val state = RegInit(idle)
  
  io.cpu_valid := false.B
  io.cpu_rdata := 0.U
  io.mem_req := false.B
  io.mem_addr := 0.U
  io.mem_wr := false.B
  io.mem_wdata := 0.U

  switch(state) {
    is(idle) {
      when(io.cpu_req) {
        when(is_hit) {
          when(io.cpu_wr) {
            // Write hit
            data(hit_index)(addr_index) := io.cpu_wdata
            io.cpu_valid := true.B
          }.otherwise {
            // Read hit
            io.cpu_rdata := line_data(hit_index)
            io.cpu_valid := true.B
          }
          lru(addr_index) := hit_index
        }.otherwise {
          // Miss - transition to miss state
          state := miss
        }
      }
    }
    
    is(miss) {
      // Request data from memory
      io.mem_req := true.B
      io.mem_addr := io.cpu_addr
      io.mem_wr := false.B
      
      // Wait for memory to be valid
      when(mem_access_cycles < latency_cycles.U) {
        mem_access_cycles := mem_access_cycles + 1.U
        io.mem_req := true.B
      }.otherwise {
        when(io.mem_valid) {
          val replace_index = lru(addr_index)
          // Update cache line
          validBits(replace_index)(addr_index) := true.B
          tags(replace_index)(addr_index) := addr_tag
          data(replace_index)(addr_index) := io.mem_rdata
          
          // Handle write miss separately
          when(io.cpu_wr) {
            data(replace_index)(addr_index) := io.cpu_wdata
          }
          
          // Signal data is ready
          io.cpu_valid := true.B
          io.cpu_rdata := Mux(io.cpu_wr, io.cpu_wdata, io.mem_rdata)
          
          // Update LRU
          lru(addr_index) := (lru(addr_index) + 1.U) % 4.U
          
          // Reset state
          state := idle
        }
      }
    }
  }

  val total_accesses = RegInit(0.U(32.W))
  val cache_hits = RegInit(0.U(32.W))
  val cache_misses = RegInit(0.U(32.W))
  val memory_stall_cycles = RegInit(0.U(32.W))

  when(io.cpu_req) {
    total_accesses := total_accesses + 1.U
    
    when(is_hit) {
      cache_hits := cache_hits + 1.U
    }.otherwise {
      cache_misses := cache_misses + 1.U
      
      // Track memory stall cycles during miss
      when(state === miss) {
        memory_stall_cycles := memory_stall_cycles + 1.U
      }
    }
  }

  // Periodic or conditional logging
  when(total_accesses % 10.U === 0.U) {
    printf(
      "Performance Report:\n" +
      "Total Accesses: %d\n" +
      "Cache Hits: %d(%d%%)\n" +
      "Cache Misses: %d(%d%%)\n" +
      "Memory Stall Cycles: %d\n", 
      total_accesses, 
      cache_hits, 
      (cache_hits * 100.U) / total_accesses,
      cache_misses,
      (cache_misses * 100.U) / total_accesses,
      memory_stall_cycles
    )
  }
}
