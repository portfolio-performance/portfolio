package name.abuchen.portfolio.ui.views;

import java.util.function.Consumer;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
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
        EmbeddedBrowser browser = make(EmbeddedBrowser.class);
        browser.setHtmlpage("/META-INF/html/test.html"); //$NON-NLS-1$
        return browser.createControl(parent, (Consumer<Browser>[]) null);
    }

}
