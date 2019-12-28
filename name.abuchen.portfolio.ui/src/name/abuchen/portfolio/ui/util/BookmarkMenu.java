package name.abuchen.portfolio.ui.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;

import name.abuchen.portfolio.model.Bookmark;
import name.abuchen.portfolio.model.Client;
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
        List<String> urls = getUrlsFromNotes(security);
        if (!urls.isEmpty())
        {
            add(new Separator());
            for (String url : urls)
            {
                add(new SimpleAction(url, a -> DesktopAPI.browse(url)));
            }
        }

        add(new Separator());
        add(new SimpleAction(Messages.BookmarkMenu_EditBookmarks,
                        a -> editor.activateView(SettingsView.class, Integer.valueOf(0))));
    }
    
    /**
     * Gets all URLs contained in the notes of the given {@link Security}.
     * 
     * @param security
     *            {@link Security}
     * @return list of notes
     */
    private static List<String> getUrlsFromNotes(Security security)
    {
        ArrayList<String> urls = new ArrayList<String>();
        String notes = security.getNote();
        if ((notes != null) && (notes.length() > 0))
        {
            Pattern urlPattern = Pattern.compile("(https?\\:\\/\\/[^ \\t\\r\\n]+)", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$
            Matcher m = urlPattern.matcher(notes);
            while (m.find())
            {
                urls.add(m.group());
            }
        }
        return urls;
    }
}
