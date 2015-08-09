package name.abuchen.portfolio.model;

import java.util.ArrayList;
import java.util.List;

public class ClientSettings
{
    private List<Bookmark> bookmarks;

    public ClientSettings()
    {
        doPostLoadInitialization();
    }

    public void setDefaultBookmarks()
    {
        bookmarks.add(new Bookmark("Yahoo Finance", //$NON-NLS-1$
                        "http://de.finance.yahoo.com/q?s={tickerSymbol}")); //$NON-NLS-1$
        bookmarks.add(new Bookmark("OnVista", //$NON-NLS-1$
                        "http://www.onvista.de/suche.html?SEARCH_VALUE={isin}&SELECTED_TOOL=ALL_TOOLS")); //$NON-NLS-1$
        bookmarks.add(new Bookmark("Finanzen.net", //$NON-NLS-1$
                        "http://www.finanzen.net/suchergebnis.asp?frmAktiensucheTextfeld={isin}")); //$NON-NLS-1$
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
            this.bookmarks = new ArrayList<Bookmark>();

        if (bookmarks.isEmpty())
            setDefaultBookmarks();
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

    public void insertBookmarkAfter(Bookmark after, Bookmark bookmark)
    {
        if (after == null)
            bookmarks.add(bookmark);
        else
            bookmarks.add(bookmarks.indexOf(after) + 1, bookmark);
    }
}
