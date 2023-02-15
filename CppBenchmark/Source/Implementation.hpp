#include <iostream>
#include <iomanip>
#include <cstdlib>
#include <vector>
#include "Libraries/rapidcsv.h"
//#include <roaring.hh>
#include <string>
#include <ctime>
#include <algorithm>
#include <random>

class Slice {
public:
  Slice(rapidcsv::Document *t, std::vector<size_t> s) : table(t), selection(s) {}

  void set_name(std::string n) {
    name = n;
  }

  std::vector<size_t> get_selection() {
    return selection;
  }
private:
  rapidcsv::Document *table;
  std::vector<size_t> selection;
  std::string name;
};

class Implementation {
public:
  Implementation(int percentage) : Implementation() {
    table = split_bins(table, percentage);
  }

  Implementation() {
//    std::cout << std::filesystem::current_path() << std::endl;
    table = rapidcsv::Document("../owid-covid-data-interm.csv");
  }

  std::vector<Slice> split_on(std::string columnName) {
    std::vector<Slice> res{};
    std::map<std::string, std::vector<size_t>> selection_map{};
    std::map<std::string, std::string> slice_name_map{};
    std::vector<std::string> column = table.GetColumn<std::string>(columnName);

    for (size_t i = 0; i < table.GetRowCount(); i++) {
      std::string byte_array_list = "";
      std::string string_key = "";
      string_key += column[i];
      byte_array_list += std::to_string(byte_array_list.size()) + column[i];
      std::vector<size_t> *selection = &selection_map[byte_array_list];
      selection->push_back(i);
      selection_map[byte_array_list] = *selection;
      slice_name_map[byte_array_list] = string_key;
    }

    for (auto const& [row, selection] : selection_map) {
      Slice slice(&table, selection);
      slice.set_name(slice_name_map[row]);
      res.push_back(slice);
    }
    return res;
  }

  rapidcsv::Document get_table() {
    return table;
  }

private:
  rapidcsv::Document table;

  void plus_days(struct tm* date, int days)
  {
    const time_t ONE_DAY = 24 * 60 * 60;
    time_t date_seconds = mktime( date ) + (days * ONE_DAY);
    *date = *localtime( &date_seconds );
  }

  rapidcsv::Document split_bins(rapidcsv::Document t, int percentage) {
    size_t n = t.GetRowCount();
    int k = (int) (n * (float) percentage / 100);
    int bin_size = n / k;
    int last_bin_size = bin_size + (n - bin_size * k);
    std::vector<std::string> dates(n);
    time_t now = time(0);
    struct tm *date;
    date = localtime(&now);
    for (int i = 0; i < k; i++) {
      int m = bin_size;
      if (i == k - 1) {
        m = last_bin_size;
      }
      std::stringstream ss;
      ss << date->tm_year << "-" << date->tm_mon << "-" << date->tm_yday;
      std::string date_str = ss.str();
      for (int j = 0; j < m; j++) {
        dates[i * bin_size + j] = date_str;
      }
      plus_days(date, 1);
    }
    auto rng = std::default_random_engine{};
    std::shuffle(std::begin(dates), std::end(dates), rng);
    t.RemoveColumn("date");
    t.InsertColumn(0, dates, "date");
    return t;
  }
};

//int main() {
//  Implementation impl;
//  impl.split_on("date");
//  return 0;
//}

