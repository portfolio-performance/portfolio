package name.abuchen.portfolio.ui.util;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URISyntaxException;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.WebLocation;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;

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
                                        "http://www.finanzen.net/suchergebnis.asp?frmAktiensucheTextfeld={isin}"), //$NON-NLS-1$
                        new WebLocation("justETF", //$NON-NLS-1$
                                        "https://www.justetf.com/de/etf-profile.html?isin={isin}"), //$NON-NLS-1$
                        new WebLocation("fondsweb.de", //$NON-NLS-1$
                                        "http://www.fondsweb.de/{isin}"), //$NON-NLS-1$
                        new WebLocation("Morningstar.de", //$NON-NLS-1$
                                        "http://www.morningstar.de/de/funds/SecuritySearchResults.aspx?type=ALL&search={isin}"), //$NON-NLS-1$
                        new WebLocation(
                                        "maxblue Kauforder", //$NON-NLS-1$
                                        "https://meine.deutsche-bank.de/trxm/db/init.do" //$NON-NLS-1$
                                                        + "?style=mb&style=mb&login=br24order&action=PurchaseSecurity2And3Steps&wknOrIsin={isin}") }; //$NON-NLS-1$

        for (final WebLocation loc : locations)
        {
            add(new Action(loc.getLabel())
            {
                @Override
                public void run()
                {
                    try
                    {
                        if (Desktop.isDesktopSupported())
                        {
                            Desktop desktop = Desktop.getDesktop();
                            if (desktop.isSupported(Desktop.Action.BROWSE))
                            {
                                desktop.browse(loc.constructURL(security));
                            }
                        }
                    }
                    catch (IOException e)
                    {
                        PortfolioPlugin.log(e);
                    }
                    catch (URISyntaxException e)
                    {
                        PortfolioPlugin.log(e);
                    }
                }
            });
        }
    }

}
