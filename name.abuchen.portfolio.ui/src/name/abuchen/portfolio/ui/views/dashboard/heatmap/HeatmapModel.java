package name.abuchen.portfolio.ui.views.dashboard.heatmap;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class HeatmapModel
{
    public static class Row
    {
        private String label;
        private List<Double> data = new ArrayList<>();

        public Row(String label)
        {
            this.label = label;
        }

        public String getLabel()
        {
            return label;
        }

        public void addData(Double value)
        {
            this.data.add(value);
        }

        public Double getData(int index)
        {
            return data.get(index);
        }

        public Stream<Double> getData()
        {
            return data.stream();
        }

    }

    private List<String> header = new ArrayList<>();
    private List<Row> rows = new ArrayList<>();

    private String cellToolTip;

    public void addHeader(String label)
    {
        header.add(label);
    }

    public Stream<String> getHeader()
    {
        return header.stream();
    }

    public int getHeaderSize()
    {
        return header.size();
    }

    public void addRow(Row row)
    {
        rows.add(row);
    }

    public Stream<Row> getRows()
    {
        return rows.stream();
    }

    public List<Double> getColumnValues(int index)
    {
        List<Double> values = new ArrayList<>();

        for (Row row : rows)
        {
            Double v = row.getData(index);
            if (v != null)
                values.add(v);
        }

        return values;
    }

    public String getCellToolTip()
    {
        return cellToolTip;
    }

    public void setCellToolTip(String cellToolTip)
    {
        this.cellToolTip = cellToolTip;
    }
}
