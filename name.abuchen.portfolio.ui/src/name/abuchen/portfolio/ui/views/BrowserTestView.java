package name.abuchen.portfolio.ui.views;

import name.abuchen.portfolio.ui.AbstractFinanceView;
import name.abuchen.portfolio.ui.util.JavaFXBrowser;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class BrowserTestView extends AbstractFinanceView
{

    @Override
    protected String getTitle()
    {
        return "Browser Test"; //$NON-NLS-1$
    }

    @Override
    protected Control createBody(Composite parent)
    {
        JavaFXBrowser browser = new JavaFXBrowser(parent);
        browser.load("/META-INF/html/test.html"); //$NON-NLS-1$
        return browser;
    }

}
