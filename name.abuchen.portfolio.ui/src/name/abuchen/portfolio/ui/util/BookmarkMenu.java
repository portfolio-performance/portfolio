package name.abuchen.portfolio.ui.util;

import java.util.Collections;
import java.util.List;

import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;

import name.abuchen.portfolio.model.Bookmark;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientSettings;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.editor.PortfolioPart;
import name.abuchen.portfolio.ui.views.settings.SettingsView;

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
                add(new SimpleAction(bookmark.getLabel(),
                                a -> DesktopAPI.browse(bookmark.constructURL(client, security))));
            }
        }

        // check for urls in notes of security
        add(new Separator());
        security.getCustomBookmarks()
                        .forEach(bm -> add(new SimpleAction(bm.getLabel(), a -> DesktopAPI.browse(bm.getPattern()))));

        add(new Separator());

        MenuManager templatesMenu = new MenuManager(Messages.LabelTaxonomyTemplates);
        add(templatesMenu);

        List<Bookmark> templates = ClientSettings.getDefaultBookmarks();
        Collections.sort(templates, (r, l) -> r.getLabel().compareTo(l.getLabel()));
        templates.forEach(bookmark -> templatesMenu.add(new SimpleAction(bookmark.getLabel(),
                        a -> DesktopAPI.browse(bookmark.constructURL(client, security)))));

        add(new Separator());
        add(new SimpleAction(Messages.BookmarkMenu_EditBookmarks,
                        a -> editor.activateView(SettingsView.class, Integer.valueOf(0))));
    }

}
