package uk.ac.ic.doc.spe;

import java.util.Arrays;
import java.util.Collections;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import tech.tablesaw.aggregate.Summarizer;
import tech.tablesaw.analytic.AnalyticQuery;
import tech.tablesaw.api.DateColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvReadOptions;
import tech.tablesaw.api.ColumnType;

import java.time.LocalDate;

import static tech.tablesaw.aggregate.AggregateFunctions.max;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

@State(Scope.Thread)
public class MyBenchmark2 {
  private Table input;
  private Summarizer summarizer;
  private Table highestCaseloadByDay;
  @Param({"1", "25", "50", "75", "100"})
  public int percentage;

  public static void main(String[] args) throws RunnerException {
    System.out.println("Working Directory = " + System.getProperty("user.dir"));

    Options opt = new OptionsBuilder()
        .include(MyBenchmark2.class.getSimpleName())
        .forks(1)
        .build();

    new Runner(opt).run();
  }

  @Benchmark
  public void benchmark() {
    highestCaseloadByDay = summarizer.by("date");
  }

  @Setup
  public void setup() {
    try {
      ColumnType[] types = {
          ColumnType.STRING,     // 0     iso_code
          ColumnType.STRING,     // 1     continent
          ColumnType.STRING,     // 2     location
          ColumnType.LOCAL_DATE, // 3     date
          ColumnType.INTEGER,    // 4     total_cases
          ColumnType.INTEGER,    // 5     new_cases
          ColumnType.DOUBLE,     // 6     new_cases_smoothed
          ColumnType.INTEGER,    // 7     total_deaths
          ColumnType.INTEGER,    // 8     new_deaths
          ColumnType.DOUBLE,     // 9     new_deaths_smoothed
          ColumnType.DOUBLE,     // 10    total_cases_per_million
          ColumnType.DOUBLE,     // 11    new_cases_per_million
          ColumnType.DOUBLE,     // 12    new_cases_smoothed_per_million
          ColumnType.DOUBLE,     // 13    total_deaths_per_million
          ColumnType.DOUBLE,     // 14    new_deaths_per_million
          ColumnType.DOUBLE,     // 15    new_deaths_smoothed_per_million
          ColumnType.DOUBLE,     // 16    reproduction_rate
          ColumnType.INTEGER,    // 17    icu_patients
          ColumnType.DOUBLE,     // 18    icu_patients_per_million
          ColumnType.INTEGER,    // 19    hosp_patients
          ColumnType.DOUBLE,     // 20    hosp_patients_per_million
          ColumnType.INTEGER,    // 21    weekly_icu_admissions
          ColumnType.DOUBLE,     // 22    weekly_icu_admissions_per_million
          ColumnType.INTEGER,    // 23    weekly_hosp_admissions
          ColumnType.DOUBLE,     // 24    weekly_hosp_admissions_per_million
          ColumnType.LONG,    // 25    total_tests
          ColumnType.INTEGER,    // 26    new_tests
          ColumnType.DOUBLE,     // 27    total_tests_per_thousand
          ColumnType.DOUBLE,     // 28    new_tests_per_thousand
          ColumnType.INTEGER,    // 29    new_tests_smoothed
          ColumnType.DOUBLE,     // 30    new_tests_smoothed_per_thousand
          ColumnType.DOUBLE,     // 31    positive_rate
          ColumnType.DOUBLE,     // 32    tests_per_case
          ColumnType.STRING,     // 33    tests_units
          ColumnType.LONG,       // 34    total_vaccinations
          ColumnType.LONG,       // 35    people_vaccinated
          ColumnType.LONG,       // 36    people_fully_vaccinated
          ColumnType.LONG,    // 37    total_boosters
          ColumnType.INTEGER,    // 38    new_vaccinations
          ColumnType.INTEGER,    // 39    new_vaccinations_smoothed
          ColumnType.DOUBLE,     // 40    total_vaccinations_per_hundred
          ColumnType.DOUBLE,     // 41    people_vaccinated_per_hundred
          ColumnType.DOUBLE,     // 42    people_fully_vaccinated_per_hundred
          ColumnType.DOUBLE,     // 43    total_boosters_per_hundred
          ColumnType.INTEGER,    // 44    new_vaccinations_smoothed_per_million
          ColumnType.INTEGER,    // 45    new_people_vaccinated_smoothed
          ColumnType.DOUBLE,     // 46    new_people_vaccinated_smoothed_per_hundred
          ColumnType.DOUBLE,     // 47    stringency_index
          ColumnType.DOUBLE,     // 48    population_density
          ColumnType.DOUBLE,     // 49    median_age
          ColumnType.DOUBLE,     // 50    aged_65_older
          ColumnType.DOUBLE,     // 51    aged_70_older
          ColumnType.DOUBLE,     // 52    gdp_per_capita
          ColumnType.DOUBLE,     // 53    extreme_poverty
          ColumnType.DOUBLE,     // 54    cardiovasc_death_rate
          ColumnType.DOUBLE,     // 55    diabetes_prevalence
          ColumnType.DOUBLE,     // 56    female_smokers
          ColumnType.DOUBLE,     // 57    male_smokers
          ColumnType.DOUBLE,     // 58    handwashing_facilities
          ColumnType.DOUBLE,     // 59    hospital_beds_per_thousand
          ColumnType.DOUBLE,     // 60    life_expectancy
          ColumnType.DOUBLE,     // 61    human_development_index
          ColumnType.LONG,       // 62    population
          ColumnType.DOUBLE,     // 63    excess_mortality_cumulative_absolute
          ColumnType.DOUBLE,     // 64    excess_mortality_cumulative
          ColumnType.DOUBLE,     // 65    excess_mortality
          ColumnType.DOUBLE,     // 66    excess_mortality_cumulative_per_million
      };

      // make sure to download https://github.com/owid/covid-19-data/blob/master/public/data/owid-covid-data.csv?raw=true into the current directory
      input = Table.read()
          .usingOptions(CsvReadOptions
              .builder("owid-covid-data.csv")
              .columnTypes(types).build())
          .retainColumns("iso_code", "date", "new_cases_per_million");

      input = splitBins(input, percentage);

      AnalyticQuery
          .query()
          .from(input)
          .partitionBy("iso_code")
          .orderBy("date")
          .rowsBetween()
          .preceding(7)
          .andFollowing(8)
          .sum("new_cases_per_million")
          .as("smoothed")
          .executeInPlace();

      summarizer = input
          .where(input.column("smoothed").isNotMissing())
          .summarize("smoothed", max);
    } catch (Throwable e) {
      System.out.println("Error " + e.getMessage());
      e.printStackTrace();
    }
  }

  @TearDown
  public void teardown() {
    highestCaseloadByDay = highestCaseloadByDay
        .where(highestCaseloadByDay.numberColumn("Max [smoothed]").isGreaterThan(0));
    highestCaseloadByDay.column("Max [smoothed]").setName("smoothed");

    Table result = highestCaseloadByDay
        .joinOn("smoothed", "date")
        .inner(true, input)
        .retainColumns("date", "iso_code", "smoothed");
  }

  public Table splitBins(Table table, int percentage) {
    int n = table.rowCount();
    int k = (int) (n * ((float) percentage / 100));
    int binSize = n / k;
    int lastBinSize = binSize + (n - binSize * k);
    LocalDate[] dates = new LocalDate[n];
    LocalDate date = LocalDate.now();
    for (int i = 0; i < k; i++) {
      int m = binSize;
      if (i == k - 1) {
        m = lastBinSize;
      }
      for (int j = 0; j < m; j++) {
        dates[i * binSize + j] = date;
      }
      date = date.plusDays(1);
    }
    Collections.shuffle(Arrays.asList(dates));
    DateColumn column = DateColumn.create("date", dates);
    table = table.replaceColumn("date", column);
    return table;
  }
}
