package name.abuchen.portfolio.ui.util;

import java.net.MalformedURLException;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.WebLocation;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;

public class WebLocationMenu extends MenuManager
{
    private final Security security;

    public WebLocationMenu(Security security)
    {
        super(Messages.MenuOpenSecurityOnSite);

        this.security = security;

        addDefaultPages();
    }

    private void addDefaultPages()
    {
        WebLocation[] locations = new WebLocation[] {
                        new WebLocation("Yahoo Finance", //$NON-NLS-1$
                                        "http://de.finance.yahoo.com/q?s={tickerSymbol}"), //$NON-NLS-1$
                        new WebLocation("OnVista", //$NON-NLS-1$
                                        "http://www.onvista.de/suche.html?SEARCH_VALUE={isin}&SELECTED_TOOL=ALL_TOOLS"), //$NON-NLS-1$
                        new WebLocation("Finanzen.net", //$NON-NLS-1$
                                        "http://www.finanzen.net/suchergebnis.asp?frmAktiensucheTextfeld={isin}") }; //$NON-NLS-1$

        for (final WebLocation loc : locations)
        {
            add(new Action(loc.getLabel())
            {
                @Override
                public void run()
                {
                    try
                    {
                        final IWebBrowser browser = PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser();
                        browser.openURL(loc.constructURL(security));
                    }
                    catch (PartInitException e)
                    {
                        PortfolioPlugin.log(e);
                    }
                    catch (MalformedURLException e)
                    {
                        PortfolioPlugin.log(e);
                    }
                }
            });
        }
    }

}
