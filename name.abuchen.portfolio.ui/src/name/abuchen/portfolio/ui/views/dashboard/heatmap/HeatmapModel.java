package name.abuchen.portfolio.ui.views.dashboard.heatmap;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import name.abuchen.portfolio.money.Values;

public class HeatmapModel<N extends Number>
{
    public static class Row<N>
    {
        private String label;
        private List<N> data = new ArrayList<>();

        public Row(String label)
        {
            this.label = label;
        }

        public String getLabel()
        {
            return label;
        }

        public void addData(N value)
        {
            this.data.add(value);
        }

        public void setData(int index, N value)
        {
            this.data.set(index, value);
        }

        public N getData(int index)
        {
            return data.get(index);
        }

        public Stream<N> getData()
        {
            return data.stream();
        }

    }

    private Values<N> formatter;
    private List<String> header = new ArrayList<>();
    private List<Row<N>> rows = new ArrayList<>();

    private Function<N, String> toolTipBuilder;

    public HeatmapModel(Values<N> formatter)
    {
        this.formatter = formatter;
    }

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

    public void addRow(Row<N> row)
    {
        rows.add(row);
    }

    public Row<N> getRow(int index)
    {
        return rows.get(index);
    }

    public Stream<Row<N>> getRows()
    {
        return rows.stream();
    }

    public List<N> getColumnValues(int index)
    {
        List<N> values = new ArrayList<>();

        for (Row<N> row : rows)
        {
            N v = row.getData(index);
            if (v != null)
                values.add(v);
        }

        return values;
    }

    public Function<N, String> getCellToolTip()
    {
        return toolTipBuilder;
    }

    public void setCellToolTip(Function<N, String> toolTipBuilder)
    {
        this.toolTipBuilder = toolTipBuilder;
    }

    public Values<N> getFormatter()
    {
        return formatter;
    }
}
