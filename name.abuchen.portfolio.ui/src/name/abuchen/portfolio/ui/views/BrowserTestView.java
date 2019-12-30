package name.abuchen.portfolio.ui.views;

import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.util.EmbeddedBrowser;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

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
