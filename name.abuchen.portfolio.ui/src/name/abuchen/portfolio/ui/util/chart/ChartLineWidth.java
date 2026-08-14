package name.abuchen.portfolio.ui.util.chart;

import java.util.stream.IntStream;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.util.SimpleAction;

/**
 * The width (in pixel) of the lines painted by those charts that do not offer a
 * line width per data series. The value is configured per installation - not
 * per file - because it compensates for how the monitor scales the lines.
 * <p/>
 * Charts driven by configurable data series (Statement of Assets, Performance)
 * are not affected: they let the user pick a line width per series. Dashboard
 * widgets are affected if their data series do not offer a per-series line
 * width.
 */
public final class ChartLineWidth
{
    public static final int MIN_WIDTH = 1;
    public static final int MAX_WIDTH = 3;
    public static final int DEFAULT_WIDTH = 2;

    private static int current = DEFAULT_WIDTH;

    private ChartLineWidth()
    {
    }

    public static int get()
    {
        return current;
    }

    /**
     * Sets the current line width. Values outside the supported range are
     * silently replaced by the default, for example because the preference has
     * been edited manually.
     */
    public static void set(int width)
    {
        current = width < MIN_WIDTH || width > MAX_WIDTH ? DEFAULT_WIDTH : width;
    }

    /**
     * Adds the sub-menu to pick the line width. Picking a width updates the
     * preference which in turn updates all charts.
     */
    public static void addMenu(IMenuManager manager)
    {
        MenuManager lineWidth = new MenuManager(Messages.ChartSeriesPickerLineWidth);
        IntStream.rangeClosed(MIN_WIDTH, MAX_WIDTH).forEach(width -> {
            Action action = new SimpleAction(width + " px", a -> PortfolioPlugin.getDefault().getPreferenceStore() //$NON-NLS-1$
                            .setValue(UIConstants.Preferences.CHART_LINE_WIDTH, width));
            action.setChecked(width == get());
            lineWidth.add(action);
        });
        manager.add(lineWidth);
    }
}
