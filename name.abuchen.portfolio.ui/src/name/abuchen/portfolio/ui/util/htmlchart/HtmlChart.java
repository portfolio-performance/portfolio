package name.abuchen.portfolio.ui.util.htmlchart;

import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.JavaFXBrowser;
import name.abuchen.portfolio.ui.util.JavaFXBrowser.BrowserFunction;
import netscape.javascript.JSObject;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

public class HtmlChart
{

    private JavaFXBrowser browser;
    private HtmlChartConfig args;
    private HtmlChartContextMenu contextMenu;

    public HtmlChart(HtmlChartConfig args)
    {
        this.args = args;
    }

    public Control createControl(Composite container)
    {
        browser = new JavaFXBrowser(container);
        browser.registerBrowserFunction("loadData", new LoadDataFunction()); //$NON-NLS-1$
        browser.load(args.getHtmlPageUri());
        return browser;
    }

    public Control getBrowserControl()
    {
        return browser;
    }

    public HtmlChartConfig getChartConfig()
    {
        return args;
    }

    public void refreshChart()
    {
        browser.load(args.getHtmlPageUri());
    }

    public final class LoadDataFunction implements BrowserFunction
    {
        @Override
        public Object function(JSObject arguments)
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
        Image image = new Image(display, browser.getBounds().width, browser.getBounds().height);
        ImageLoader loader = new ImageLoader();

        GC gc = new GC(image);
        browser.print(gc);
        gc.dispose();

        loader.data = new ImageData[] { image.getImageData() };
        loader.save(filename, format);
    }

}
