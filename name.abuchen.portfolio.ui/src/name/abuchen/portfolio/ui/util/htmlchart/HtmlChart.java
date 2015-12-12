package name.abuchen.portfolio.ui.util.htmlchart;

import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.EmbeddedBrowser;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

public class HtmlChart
{

    private EmbeddedBrowser browser;
    private Control browserControl;
    private HtmlChartConfig args;
    private HtmlChartContextMenu contextMenu;

    public HtmlChart(HtmlChartConfig args)
    {
        this.args = args;
    }

    public Control createControl(Composite container)
    {
        browser = new EmbeddedBrowser(args.getHtmlPageUri()); // $NON-NLS-1$
        browserControl = browser.createControl(container, b -> new LoadDataFunction(b, "loadData")); //$NON-NLS-1$
        this.contextMenu = new HtmlChartContextMenu(this);
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

    public void exportMenuAboutToShow(IMenuManager manager, String label)
    {
        this.contextMenu.exportMenuAboutToShow(manager, label);
    }

    public void save(String filename, int format) throws Exception
    {
        // TODO: Implement Save Chart as Image
        // throw new Exception("Not implemented yet!");
        Display display = new Display();
        Image image = new Image(display, browserControl.getBounds().width, browserControl.getBounds().height);
        ImageLoader loader = new ImageLoader();

        GC gc = new GC(image);
        browserControl.print(gc);
        gc.dispose();

        loader.data = new ImageData[] { image.getImageData() };
        loader.save(filename, format);
    }

}
