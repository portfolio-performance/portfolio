package name.abuchen.portfolio.ui.util.htmlchart;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.EmbeddedBrowser;

public class HtmlChart
{

    private EmbeddedBrowser browser;
    private Control browserControl;
    private HtmlChartConfig args;

    public HtmlChart(HtmlChartConfig args)
    {
        this.args = args;
    }

    public Control createControl(Composite container)
    {
        browser = new EmbeddedBrowser(args.getHtmlPageUri()); // $NON-NLS-1$
        browserControl = browser.createControl(container, b -> new LoadDataFunction(b, "loadData")); //$NON-NLS-1$
        return browserControl;
    }

    public Control getBrowserControl()
    {
        return browserControl;
    }

    public HtmlChartConfig getChartConfig()
    {
        return args;
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
                return args.getJsonString();
            }
            catch (Exception e)
            {
                PortfolioPlugin.log(e);
                return "{}"; //$NON-NLS-1$
            }
        }

    }
}
