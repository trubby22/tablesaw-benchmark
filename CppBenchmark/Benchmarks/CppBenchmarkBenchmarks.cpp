#include "../Source/CppBenchmark.hpp"
#include <benchmark/benchmark.h>
#include <iostream>

using namespace std;

static void BasicBenchmark(benchmark::State& state) {
  Implementation impl;
  for(auto _ : state) {
    impl.split_on("date");
  }
}
BENCHMARK_WITH_UNIT(BasicBenchmark, benchmark::kMillisecond)
    ->Iterations(5); // NOLINT

static void HypothesisBenchmark(benchmark::State& state) {
  Implementation impl(state.range(0));
  for(auto _ : state) {
    impl.split_on("date");
  }
}
BENCHMARK_WITH_UNIT(HypothesisBenchmark, benchmark::kMillisecond)
    ->Arg(1)->Arg(25)->Arg(50)->Arg(75)->Arg(100)
    ->Iterations(5); // NOLINT

BENCHMARK_MAIN();

