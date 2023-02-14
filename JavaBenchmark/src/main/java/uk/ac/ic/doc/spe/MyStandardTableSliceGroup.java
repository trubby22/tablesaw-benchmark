package uk.ac.ic.doc.spe;

import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import tech.tablesaw.api.CategoricalColumn;
import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;
import tech.tablesaw.selection.BitmapBackedSelection;
import tech.tablesaw.selection.Selection;
import tech.tablesaw.table.TableSlice;
import tech.tablesaw.table.TableSliceGroup;

import org.openjdk.jmh.annotations.Benchmark;

/** A group of tables formed by performing splitting operations on an original table */
public class MyStandardTableSliceGroup extends TableSliceGroup {

  /**
   * Constructs a TableSliceGroup made by subdividing the original table by the given columns. A
   * group subdividing on the two columns "Name" and "Place" will have a slice for every combination
   * of name and place in the table
   */
  private MyStandardTableSliceGroup(Table original, CategoricalColumn<?>... columns) {
    super(original, splitColumnNames(columns));
    setSourceTable(getSourceTable());
    splitOn(getSplitColumnNames());
  }

  private static String[] splitColumnNames(CategoricalColumn<?>... columns) {
    String[] splitColumnNames = new String[columns.length];
    for (int i = 0; i < columns.length; i++) {
      splitColumnNames[i] = columns[i].name();
    }
    return splitColumnNames;
  }

  /**
   * Returns a viewGroup splitting the original table on the given columns. The named columns must
   * be CategoricalColumns
   */
  public static MyStandardTableSliceGroup create(Table original, String... columnsNames) {
    List<CategoricalColumn<?>> columns = original.categoricalColumns(columnsNames);
    return new MyStandardTableSliceGroup(original, columns.toArray(new CategoricalColumn<?>[0]));
  }

  /**
   * Returns a viewGroup splitting the original table on the given columns. The named columns must
   * be CategoricalColumns
   */
  public static MyStandardTableSliceGroup create(Table original, CategoricalColumn<?>... columns) {
    return new MyStandardTableSliceGroup(original, columns);
  }

  /**
   * Splits the sourceTable table into sub-tables, grouping on the columns whose names are given in
   * splitColumnNames
   */
//  @Benchmark
  private void splitOn(String... splitColumnNames) {
    Map<MyStandardTableSliceGroup.ByteArray, Selection> selectionMap = new LinkedHashMap<>();
    Map<MyStandardTableSliceGroup.ByteArray, String> sliceNameMap = new HashMap<>();
    List<Column<?>> splitColumns = getSourceTable().columns(splitColumnNames);

    if (containsTextColumn(splitColumns)) {
      for (int i = 0; i < getSourceTable().rowCount(); i++) {
        ByteArrayList byteArrayList = new ByteArrayList();
        StringBuilder stringKey = new StringBuilder();
        int count = 0;
        for (Column<?> col : splitColumns) {
          stringKey.append(col.getString(i));
          if (count < splitColumns.size() - 1) {
            stringKey.append(SPLIT_STRING);
          }
          byteArrayList.addElements(byteArrayList.size(), col.asBytes(i));
          count++;
        }
        // Add to the matching selection.
        MyStandardTableSliceGroup.ByteArray
            byteArray = new MyStandardTableSliceGroup.ByteArray(byteArrayList.toByteArray());
        Selection selection = selectionMap.getOrDefault(byteArray, new BitmapBackedSelection());
        selection.add(i);
        selectionMap.put(byteArray, selection);
        sliceNameMap.put(byteArray, stringKey.toString());
      }
    } else { // handle the case where split is on non-text-columns
      int byteSize = getByteSize(splitColumns);
      for (int i = 0; i < getSourceTable().rowCount(); i++) {
        StringBuilder stringKey = new StringBuilder();
        ByteBuffer byteBuffer = ByteBuffer.allocate(byteSize);
        int count = 0;
        for (Column<?> col : splitColumns) {
          stringKey.append(col.getString(i));
          if (count < splitColumns.size() - 1) {
            stringKey.append(SPLIT_STRING);
          }
          byteBuffer.put(col.asBytes(i));
          count++;
        }
        // Add to the matching selection.
        MyStandardTableSliceGroup.ByteArray
            byteArray = new MyStandardTableSliceGroup.ByteArray(byteBuffer.array());
        Selection selection = selectionMap.getOrDefault(byteArray, new BitmapBackedSelection());
        selection.add(i);
        selectionMap.put(byteArray, selection);
        sliceNameMap.put(byteArray, stringKey.toString());
      }
    }

    // Construct slices for all the values in our maps
    for (Entry<MyStandardTableSliceGroup.ByteArray, Selection> entry : selectionMap.entrySet()) {
      TableSlice slice = new TableSlice(getSourceTable(), entry.getValue());
      slice.setName(sliceNameMap.get(entry.getKey()));
      addSlice(slice);
    }
  }

  private boolean containsTextColumn(List<Column<?>> splitColumns) {
    return splitColumns.stream().anyMatch(objects -> objects.type().equals(ColumnType.TEXT));
  }

  /** Wrapper class for a byte[] that implements equals and hashcode. */
  private static class ByteArray {
    final byte[] bytes;

    ByteArray(byte[] bytes) {
      this.bytes = bytes;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      MyStandardTableSliceGroup.ByteArray
          byteArray = (MyStandardTableSliceGroup.ByteArray) o;
      return Arrays.equals(bytes, byteArray.bytes);
    }

    @Override
    public int hashCode() {
      return Arrays.hashCode(bytes);
    }
  }
}
