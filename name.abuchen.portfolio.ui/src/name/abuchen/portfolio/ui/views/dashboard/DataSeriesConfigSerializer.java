package name.abuchen.portfolio.ui.views.dashboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import name.abuchen.portfolio.ui.views.dataseries.DataSeriesSerializer;
import name.abuchen.portfolio.ui.views.dataseries.DataSeriesSet;

/**
 * Convert a list of DataSeriesConfig object to a string and re-create again
 * from a string.
 */
public class DataSeriesConfigSerializer
{
    public List<DataSeriesConfig> fromString(WidgetDelegate<?> delegate, String config)
    {
        DataSeriesSerializer ds = new DataSeriesSerializer();
        List<DataSeriesConfig> configs = new ArrayList<>();
        DataSeriesSet set = delegate.getDashboardData().getDataSeriesSet();

        if (config != null && config.trim().length() > 0)
        {
            String[] items = config.split("\\$"); //$NON-NLS-1$
            for (String item : items)
            {
                String[] data = item.split("\\|"); //$NON-NLS-1$
                boolean benchmark = Boolean.parseBoolean(data[0]);
                String uuid = ds.fromString(set, data[1]).get(0).getUUID();
                configs.add(DataSeriesConfig.create(delegate).withDataSeries(uuid)
                                .withBenchmarkDataSeries(benchmark).build());
            }
        }

        if (configs.isEmpty())
            configs.add(DataSeriesConfig.create(delegate).build());

        return configs;
    }

    public String toString(List<DataSeriesConfig> configs)
    {
        DataSeriesSerializer ds = new DataSeriesSerializer();
        StringBuilder buf = new StringBuilder();
        for (DataSeriesConfig c : configs)
        {
            if (buf.length() > 0)
                buf.append('$');
            buf.append(Boolean.toString(c.getSupportsBenchmarkDataSeries())).append('|');
            buf.append(ds.toString(Collections.singletonList(c.getDataSeries())));
        }
        return buf.toString();
    }
}
