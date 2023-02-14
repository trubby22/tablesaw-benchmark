package uk.ac.ic.doc.spe;

import com.google.common.collect.ArrayListMultimap;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;
import tech.tablesaw.aggregate.AggregateFunction;
import tech.tablesaw.aggregate.Summarizer;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;
import tech.tablesaw.table.TableSliceGroup;

public class MySummarizer extends Summarizer {

  public MySummarizer(Table sourceTable,
      Column<?> column,
      AggregateFunction<?, ?>... functions) {
    super(sourceTable, column, functions);
  }

  public MySummarizer(Table sourceTable, List<String> columnNames,
      AggregateFunction<?, ?>... functions) {
    super(sourceTable, columnNames, functions);
  }

  public MySummarizer(Table sourceTable, Column<?> column1, Column<?> column2,
      AggregateFunction<?, ?>... functions) {
    super(sourceTable, column1, column2, functions);
  }

  public MySummarizer(Table sourceTable, Column<?> column1, Column<?> column2,
      Column<?> column3, Column<?> column4,
      AggregateFunction<?, ?>... functions) {
    super(sourceTable, column1, column2, column3, column4, functions);
  }

  public MySummarizer(Table sourceTable, Column<?> column1, Column<?> column2,
      Column<?> column3, AggregateFunction<?, ?>... functions) {
    super(sourceTable, column1, column2, column3, functions);
  }

  @Override
  public Table by(String... columnNames) {
    Table res;
    try {
      Method tdnc = Summarizer.class.getDeclaredMethod(
          "tableDoesNotContain", String.class, Table.class);
      Method summarize = Summarizer.class.getDeclaredMethod("summarize",
          TableSliceGroup.class);
      Field tempF = Summarizer.class.getDeclaredField("temp");
      Field originalF = Summarizer.class.getDeclaredField("original");
      tdnc.setAccessible(true);
      tempF.setAccessible(true);
      originalF.setAccessible(true);

      Table tempV = (Table)tempF.get(this);
      Table originalV = (Table)originalF.get(this);

      for (String columnName : columnNames) {
        if ((boolean) tdnc.invoke(this, columnName, tempV)) {
          tempV.addColumns(originalV.column(columnName));
        }
      }
      TableSliceGroup group = MyStandardTableSliceGroup.create(tempV,
          columnNames);
//      res = (Table) summarize.invoke(this, group);
      res = mySummarize(group);
    } catch (NoSuchMethodException | NoSuchFieldException |
             IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
    return res;
  }

  private Table mySummarize(TableSliceGroup group) {
    Table res;
    try {
      List<Table> results = new ArrayList<>();

      Method gafm = Summarizer.class
          .getDeclaredMethod("getAggregateFunctionMultimap");
      Method ct = Summarizer.class
          .getDeclaredMethod("combineTables", List.class);

      gafm.setAccessible(true);
      ct.setAccessible(true);

      ArrayListMultimap<String, AggregateFunction<?, ?>> reductionMultimap =
          (ArrayListMultimap<String, AggregateFunction<?, ?>>) gafm.invoke(this);

      for (String name : reductionMultimap.keys()) {
        List<AggregateFunction<?, ?>> reductions = reductionMultimap.get(name);
        results.add(group.aggregate(name, reductions.toArray(new AggregateFunction<?, ?>[0])));
      }
      res = (Table) ct.invoke(this, results);
    } catch (NoSuchMethodException |
             IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
    return res;
  }
}
