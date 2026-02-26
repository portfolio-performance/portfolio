package name.abuchen.portfolio.ui.util;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;

import name.abuchen.portfolio.model.Bookmark;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientSettings;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.editor.PortfolioPart;
import name.abuchen.portfolio.ui.util.action.MenuContribution;
import name.abuchen.portfolio.ui.views.settings.SettingsView;

public class BookmarkMenu extends MenuManager
{
    private final List<Security> securities;
    private final Client client;
    private final PortfolioPart editor;

    public BookmarkMenu(PortfolioPart editor, Security security)
    {
        this(editor, Collections.singletonList(security));
    }

    public BookmarkMenu(PortfolioPart editor, List<Security> securities)
    {
        super(Messages.MenuOpenSecurityOnSite);

        this.securities = securities;
        this.editor = editor;
        this.client = editor.getClient();

        addDefaultPages();
    }

    private void addDefaultPages()
    {
        if (securities.size() > 15)
            return;

        for (Bookmark bookmark : client.getSettings().getBookmarks())
        {
            if (bookmark.isSeparator())
            {
                add(new Separator());
            }
            else
            {
                add(new MenuContribution(bookmark.getLabel(), () -> securities.stream()
                                .forEach(s -> DesktopAPI.browse(bookmark.constructURL(client, s)))));
            }
        }

        add(new Separator());
        if (securities.size() == 1)
        {
            securities.forEach(s -> s.getCustomBookmarks(client).forEach(
                            bm -> add(new MenuContribution(bm.getLabel(), () -> DesktopAPI.browse(bm.getPattern())))));
        }
        else
        {
            client.getSettings().getAttributeTypes() //
                            .filter(t -> t.getType() == Bookmark.class) //
                            .forEachOrdered(t -> {
                                var bookmarks = securities.stream().map(s -> (Bookmark) s.getAttributes().get(t))
                                                .filter(Objects::nonNull).toList();
                                if (!bookmarks.isEmpty())
                                {
                                    var name = (bookmarks.size() < securities.size())
                                                    ? String.format("%1$s\t(%2$s/%3$s)", //$NON-NLS-1$
                                                                    t.getName(), bookmarks.size(), securities.size())
                                                    : t.getName();
                                    add(new MenuContribution(name, () -> bookmarks
                                                    .forEach(bookmark -> DesktopAPI.browse(bookmark.getPattern()))));
                                }
                            });
        }

        add(new Separator());

        MenuManager templatesMenu = new MenuManager(Messages.LabelTaxonomyTemplates);
        add(templatesMenu);

        List<Bookmark> templates = ClientSettings.getDefaultBookmarks();
        Collections.sort(templates, (r, l) -> r.getLabel().compareTo(l.getLabel()));
        templates.forEach(bookmark -> templatesMenu.add(new MenuContribution(bookmark.getLabel(),
                        () -> securities.stream().forEach(s -> DesktopAPI.browse(bookmark.constructURL(client, s))))));

        add(new Separator());
        add(new SimpleAction(Messages.BookmarkMenu_EditBookmarks,
                        a -> editor.activateView(SettingsView.class, Integer.valueOf(0))));
    }

}
