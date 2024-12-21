# Modified RISC-V Sodor Processor

This repository contains modifications to the [RISC-V Sodor Processor](https://github.com/ucb-bar/riscv-sodor) originally developed by UC Berkeley. The enhancements focus on improving cache performance and pipeline efficiency.

## Modifications

### 1. 4-Way Set Associative Cache (2-Stage Processor)
- Implemented in the `4way_cache` directory
- Enhances the memory subsystem of the 2-stage processor
- Features:
  - 4-way set associative organization
  - Improved hit rate compared to direct-mapped cache
  - LRU (Least Recently Used) replacement policy
  - Configurable cache size and block size

### 2. Enhanced Branch Prediction (5-Stage Pipeline)
- Located in the `rv32_5stage` directory
- Moved branch prediction logic from Execute to Instruction Decode stage
- Benefits:
  - Reduced branch penalty
  - Earlier branch resolution
  - Improved pipeline efficiency

## Installation

1. Clone the original RISC-V Sodor repository:
```bash
git clone https://github.com/ucb-bar/riscv-sodor.git
cd riscv-sodor
```

2. Copy the modification directories into the Sodor source code:
```bash
# Copy 4-way cache modifications
cp -r /path/to/4way_cache/* src/

# Copy 5-stage pipeline modifications
cp -r /path/to/rv32_5stage/* src/
```

3. Follow the standard Sodor build process to compile the modified processor

## Usage

The modifications are integrated into the existing Sodor infrastructure. You can use the standard build and simulation commands:

```bash
make clean
make
```

### Testing the 4-Way Cache
- Use memory-intensive test programs to observe cache behavior
- Cache statistics are available through the standard debugging interface
- Monitor hit rates and replacement patterns using the provided debugging tools

### Evaluating Branch Prediction
- Run branch-heavy programs to test the improved prediction mechanism
- Compare pipeline stall rates with the original implementation
- Use performance counters to measure branch prediction accuracy

## Contributing

Feel free to submit issues and enhancement requests. To contribute:

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## Dependencies

- All original RISC-V Sodor dependencies
- Chisel3
- FIRRTL
- Verilator (for simulation)

## License

This project is licensed under the same terms as the original RISC-V Sodor processor.

## Acknowledgments

- UC Berkeley for the original RISC-V Sodor processor
- RISC-V community for continued support and development
