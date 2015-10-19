package name.abuchen.portfolio.ui.util.htmlchart;

import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.EmbeddedBrowser;
// import name.abuchen.portfolio.ui.util.chart.ChartContextMenu;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class HtmlChart
{

    private EmbeddedBrowser browser;
    private HtmlChartConfig args;

    /**
     * @param container
     * @param args
     * @return
     */
    public HtmlChart(HtmlChartConfig args)
    {
        this.args = args;
    }

    public Control createControl(Composite container)
    {
        browser = new EmbeddedBrowser(args.getHtmlPageUri()); // $NON-NLS-1$
        return browser.createControl(container, b -> new LoadDataFunction(b, "loadData")); //$NON-NLS-1$ ;
    }

    public void refreshChart()
    {
        browser.refresh();
    }

    private final class LoadDataFunction extends BrowserFunction
    {
        private LoadDataFunction(Browser browser, String name)
        {
            super(browser, name);
        }

        public Object function(Object[] arguments)
        {
            try
            {
                // String tmp = args.getJsonString();
                // PortfolioPlugin.log(tmp);
                // return tmp;
                return args.getJsonString();
            }
            catch (Throwable e)
            {
                PortfolioPlugin.log(e);
                return "{}"; //$NON-NLS-1$
            }
        }

    }

    /*
     * public void exportMenuAboutToShow(IMenuManager manager, String label) {
     * this.contextMenu.exportMenuAboutToShow(manager, label); }
     */
}
