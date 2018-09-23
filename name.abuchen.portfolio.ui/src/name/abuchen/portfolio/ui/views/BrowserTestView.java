package name.abuchen.portfolio.ui.views;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import name.abuchen.portfolio.ui.AbstractFinanceView;
import name.abuchen.portfolio.ui.util.EmbeddedBrowser;

public class BrowserTestView extends AbstractFinanceView
{

    @Override
    protected String getDefaultTitle()
    {
        return "Browser Test"; //$NON-NLS-1$
    }

    @Override
    protected Control createBody(Composite parent)
    {
        return new EmbeddedBrowser("/META-INF/html/test.html").createControl(parent, null); //$NON-NLS-1$
    }

}
