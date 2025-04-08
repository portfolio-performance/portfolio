package name.abuchen.portfolio.ui.util.viewers;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.eclipse.swt.SWT;

import com.google.gson.Gson;

import name.abuchen.portfolio.ui.PortfolioPlugin;

/* package */ class ColumnConfiguration
{
    static class Item
    {
        private String id;
        private String label;
        private String option;
        private Integer sortDirection;
        private int width;

        private Item()
        {
        }

        public Item(String id)
        {
            this.id = id;
        }

        public String getId()
        {
            return id;
        }

        public String getLabel()
        {
            return label;
        }

        public void setLabel(String label)
        {
            this.label = label;
        }

        public String getOption()
        {
            return option;
        }

        public void setOption(String option)
        {
            this.option = option;
        }

        public Integer getSortDirection()
        {
            return sortDirection;
        }

        public void setSortDirection(Integer sortDirection)
        {
            this.sortDirection = sortDirection;
        }

        public int getWidth()
        {
            return width;
        }

        public void setWidth(int width)
        {
            this.width = width;
        }
    }

    private static final Pattern CONFIG_PATTERN = Pattern.compile("^([^=]*)=(?:([^\\|]*)\\|)?(?:(\\d*)\\$)?(\\d*)$"); //$NON-NLS-1$

    private List<Item> items = new ArrayList<>();

    public static final ColumnConfiguration read(String config)
    {
        if (config == null || config.isEmpty())
            return null;

        if (config.charAt(0) != '{')
            return readConcatenatedString(config);

        return new Gson().fromJson(config, ColumnConfiguration.class);
    }

    private static ColumnConfiguration readConcatenatedString(String config)
    {
        ColumnConfiguration configuration = new ColumnConfiguration();

        StringTokenizer tokens = new StringTokenizer(config, ";"); //$NON-NLS-1$
        while (tokens.hasMoreTokens())
        {
            try
            {
                Matcher matcher = CONFIG_PATTERN.matcher(tokens.nextToken());
                if (!matcher.matches())
                    continue;

                Item item = new Item();

                // index
                item.id = matcher.group(1);

                // option
                item.option = matcher.group(2);

                // direction
                String d = matcher.group(3);

                if (d != null)
                {
                    int sortDirection = Integer.parseInt(d);

                    switch (sortDirection)
                    {
                        case 1: // ascending
                            item.sortDirection = SWT.UP;
                            break;
                        case 2: // descending
                            item.sortDirection = SWT.DOWN;
                            break;
                        case 1 << 7: // legacy
                            item.sortDirection = SWT.DOWN;
                            break;
                        case 1 << 10: // legacy
                            item.sortDirection = SWT.UP;
                            break;
                        default:
                    }
                }

                // width
                item.width = Integer.parseInt(matcher.group(4));

                configuration.items.add(item);
            }
            catch (RuntimeException e)
            {
                PortfolioPlugin.log(e);
            }
        }

        return configuration.items.isEmpty() ? null : configuration;
    }

    public Stream<Item> getItems()
    {
        return items.stream();
    }

    public void addItem(Item item)
    {
        items.add(item);
    }

    public String serialize()
    {
        return new Gson().toJson(this);
    }
}
