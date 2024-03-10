package name.abuchen.portfolio.ui.views.dataseries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.swtchart.LineStyle;

import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.views.dataseries.DataSeries.ClientDataSeries;
import name.abuchen.portfolio.util.ColorConversion;

/**
 * Convert a list of DataSeries object to a string and re-create again from a
 * string.
 */
public class DataSeriesSerializer
{
    public List<DataSeries> fromString(DataSeriesSet set, String config)
    {
        List<DataSeries> series = new ArrayList<>();

        if (config != null && config.trim().length() > 0)
            load(set, config, series);

        if (series.isEmpty())
            set.getAvailableSeries().stream().filter(
                            s -> s.getType() == DataSeries.Type.CLIENT && (s.getInstance() == ClientDataSeries.TOTALS
                                            || s.getInstance() == ClientDataSeries.TRANSFERALS))
                            .forEach(series::add);

        return series;
    }

    private void load(DataSeriesSet set, String config, List<DataSeries> series)
    {
        Map<String, DataSeries> uuid2series = set.getAvailableSeries().stream()
                        .collect(Collectors.toMap(DataSeries::getUUID, s -> s));

        String[] items = config.split(","); //$NON-NLS-1$
        for (String item : items)
        {
            String[] data = item.split(";"); //$NON-NLS-1$

            String uuid = data[0];
            DataSeries s = uuid2series.get(uuid);
            if (s != null)
            {
                series.add(s);

                if (data.length >= 4)
                {
                    s.setColor(ColorConversion.hex2RGB(data[1]));
                    s.setLineStyle(LineStyle.valueOf(data[2]));
                    s.setShowArea(Boolean.parseBoolean(data[3]));
                }
                if (data.length >= 5)
                {
                    s.setLineWidth(Integer.parseInt(data[4]));
                }
                if (data.length >= 6)
                {
                    s.setVisible(Boolean.parseBoolean(data[5]));
                }
            }
        }
    }

    public String toString(List<DataSeries> series)
    {
        StringBuilder buf = new StringBuilder();
        for (DataSeries s : series)
        {
            if (buf.length() > 0)
                buf.append(',');
            buf.append(s.getUUID()).append(';');
            buf.append(Colors.toHex(s.getColor())).append(';');
            buf.append(s.getLineStyle().name()).append(';');
            buf.append(s.isShowArea()).append(';');
            buf.append(s.getLineWidth()).append(';');
            buf.append(s.isVisible());
        }
        return buf.toString();
    }

}
