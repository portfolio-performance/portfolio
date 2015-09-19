package name.abuchen.portfolio.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class ClientSettings
{
    private List<Bookmark> bookmarks;
    private List<AttributeType> attributeTypes;

    public ClientSettings()
    {}

    public void setDefaultBookmarks()
    {

        bookmarks.add(new Bookmark(
                        "OnVista", "http://www.onvista.de/suche.html?SEARCH_VALUE={isin}&SELECTED_TOOL=ALL_TOOLS")); //$NON-NLS-1$ //$NON-NLS-2$
        bookmarks.add(new Bookmark("Finanzen.net", //$NON-NLS-1$
                        "http://www.finanzen.net/suchergebnis.asp?frmAktiensucheTextfeld={isin}")); //$NON-NLS-1$
        bookmarks.add(new Bookmark("finanztreff.de", //$NON-NLS-1$
                        "http://www.finanztreff.de/kurse_einzelkurs_suche.htn?suchbegriff={isin}")); //$NON-NLS-1$
        bookmarks.add(new Bookmark("Ariva.de Fundamentaldaten", //$NON-NLS-1$
                        "http://www.ariva.de/{isin}/bilanz-guv")); //$NON-NLS-1$
        bookmarks.add(new Bookmark("justETF", //$NON-NLS-1$
                        "https://www.justetf.com/de/etf-profile.html?isin={isin}")); //$NON-NLS-1$
        bookmarks.add(new Bookmark("fondsweb.de", //$NON-NLS-1$
                        "http://www.fondsweb.de/{isin}")); //$NON-NLS-1$
        bookmarks.add(new Bookmark("Morningstar.de", //$NON-NLS-1$
                        "http://www.morningstar.de/de/funds/SecuritySearchResults.aspx?type=ALL&search={isin}")); //$NON-NLS-1$
        bookmarks.add(new Bookmark(
                        "maxblue Kauforder", //$NON-NLS-1$
                        "https://meine.deutsche-bank.de/trxm/db/init.do" //$NON-NLS-1$
                                        + "?style=mb&style=mb&login=br24order&action=PurchaseSecurity2And3Steps&wknOrIsin={isin}")); //$NON-NLS-1$         
    }

    public void doPostLoadInitialization()
    {
        if (bookmarks == null)
        {
            this.bookmarks = new ArrayList<Bookmark>();
            setDefaultBookmarks();
        }

        if (attributeTypes == null)
        {
            this.attributeTypes = new ArrayList<AttributeType>();
            this.attributeTypes.addAll(AttributeTypes.getDefaultTypes());
        }
    }

    public List<Bookmark> getBookmarks()
    {
        return bookmarks;
    }

    public boolean removeBookmark(Bookmark bookmark)
    {
        return bookmarks.remove(bookmark);
    }

    public void insertBookmark(Bookmark before, Bookmark bookmark)
    {
        if (before == null)
            bookmarks.add(bookmark);
        else
            bookmarks.add(bookmarks.indexOf(before), bookmark);
    }

    public void insertBookmark(int index, Bookmark bookmark)
    {
        bookmarks.add(index, bookmark);
    }

    public void insertBookmarkAfter(Bookmark after, Bookmark bookmark)
    {
        if (after == null)
            bookmarks.add(bookmark);
        else
            bookmarks.add(bookmarks.indexOf(after) + 1, bookmark);
    }

    public Stream<AttributeType> getAttributeTypes()
    {
        return attributeTypes.stream();
    }
}
