#include "../Source/CppBenchmark.hpp"
#define CATCH_CONFIG_MAIN
#include <catch2/catch.hpp>
#include "../Source/Implementation.hpp"
using namespace std;

TEST_CASE("Check if all items in each slice.selection "
          "have the same entry in the relevant column") {
  Implementation impl;
  std::string name = "date";
  std::vector<Slice> slices = impl.split_on(name);
  rapidcsv::Document table = impl.get_table();
  for (Slice slice : slices) {
    std::string e;
    bool first = true;
    for (size_t ix : slice.get_selection()) {
      std::string e2 = table.GetCell<std::string>(name, ix);
      if (first) {
        e = e2;
        first = false;
      } else {
        REQUIRE(e == e2);
      }
    }
  }
}

TEST_CASE("Check if the sum of all indices in "
          "slices equals the number of rows in table") {
  Implementation impl;
  std::vector<Slice> slices = impl.split_on("date");
  rapidcsv::Document table = impl.get_table();
  int ctr = 0;
  for (Slice slice : slices) {
    ctr += slice.get_selection().size();
  }
  REQUIRE(ctr == table.GetRowCount());
}

TEST_CASE("Check if splitBins produces k - 1 bins of equal sizes and 1 bin, "
          "which is potentially a bit larger") {
  Implementation impl(75);
  rapidcsv::Document table = impl.get_table();
  std::vector<std::string> column = table.GetColumn<std::string>("date");
  std::map<std::string, size_t> freq;
  for (std::string date : column) {
    int count = freq.count(date);
    if (count == 0) {
      freq[date] = 1;
    } else {
      freq[date] += 1;
    }
  }
  std::set<size_t> sizes{};
  for (auto const& [_, f] : freq) {
    sizes.insert(f);
  }
  REQUIRE((sizes.size() == 1 || sizes.size() == 2));
}

