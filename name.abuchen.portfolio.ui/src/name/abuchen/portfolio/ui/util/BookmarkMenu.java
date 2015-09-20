package name.abuchen.portfolio.ui.util;

import name.abuchen.portfolio.model.Bookmark;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPart;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;

public class BookmarkMenu extends MenuManager
{
    private final Security security;
    private final Client client;
    private final PortfolioPart editor;

    public BookmarkMenu(PortfolioPart editor, Security security)
    {
        super(Messages.MenuOpenSecurityOnSite);

        this.security = security;
        this.editor = editor;
        this.client = editor.getClient();

        addDefaultPages();
    }

    private void addDefaultPages()
    {
        for (Bookmark bookmark : client.getSettings().getBookmarks())
        {
            if (bookmark.isSeparator())
            {
                add(new Separator());
            }
            else
            {
                add(new Action(bookmark.getLabel())
                {
                    @Override
                    public void run()
                    {
                        DesktopAPI.browse(bookmark.constructURL(security));
                    }
                });
            }
        }
        add(new Separator());
        add(new Action(Messages.BookmarkMenu_EditBookmarks)
        {
            @Override
            public void run()
            {
                editor.activateView("settings.Settings", Integer.valueOf(0)); //$NON-NLS-1$
            }
        });
    }
}
