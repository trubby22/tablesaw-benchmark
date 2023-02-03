#include "../Source/CppBenchmark.hpp"
#include <benchmark/benchmark.h>
#include <iostream>
using namespace std;

static void DummyBenchmark(benchmark::State& state) {
  auto dummy = 0;
  for(auto _ : state) {
    for(auto i = 0U; i < state.range(0); i++) { // NOLINT
      dummy++;
    }
    benchmark::DoNotOptimize(dummy);
  }
}
BENCHMARK(DummyBenchmark)->Range(0, 1024); // NOLINT

BENCHMARK_MAIN();
