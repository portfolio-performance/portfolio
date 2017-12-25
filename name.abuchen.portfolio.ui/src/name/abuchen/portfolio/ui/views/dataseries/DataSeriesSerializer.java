package name.abuchen.portfolio.ui.views.dataseries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.swtchart.LineStyle;

import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.views.dataseries.DataSeries.ClientDataSeries;

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
            set.getAvailableSeries().stream()
                            .filter(s -> s.getType() == DataSeries.Type.CLIENT
                                            && (s.getInstance() == ClientDataSeries.TOTALS
                                                            || s.getInstance() == ClientDataSeries.TRANSFERALS)
                                            || s.getType() == DataSeries.Type.CONSUMER_PRICE_INDEX)
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

                if (data.length == 4)
                {
                    s.setColor(Colors.toRGB(data[1]));
                    s.setLineStyle(LineStyle.valueOf(data[2]));
                    s.setShowArea(Boolean.parseBoolean(data[3]));
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
            buf.append(s.isShowArea());
        }
        return buf.toString();
    }

}
