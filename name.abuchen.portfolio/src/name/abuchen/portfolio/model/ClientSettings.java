package name.abuchen.portfolio.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.AttributeType.AmountPlainConverter;
import name.abuchen.portfolio.model.AttributeType.PercentConverter;
import name.abuchen.portfolio.model.AttributeType.StringConverter;

public class ClientSettings
{
    private List<Bookmark> bookmarks;
    private List<AttributeType> attributeTypes;
    private Map<String, ConfigurationSet> configurationSets;

    public ClientSettings()
    {
        doPostLoadInitialization();
    }

    public void doPostLoadInitialization()
    {
        if (bookmarks == null)
        {
            this.bookmarks = new ArrayList<>();
            addDefaultBookmarks();
        }

        if (attributeTypes == null)
        {
            this.attributeTypes = new ArrayList<>();
            addDefaultAttributeTypes();
        }

        if (configurationSets == null)
            configurationSets = new HashMap<>();
    }

    private void addDefaultBookmarks()
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

    private void addDefaultAttributeTypes()
    {
        AttributeType ter = new AttributeType("ter"); //$NON-NLS-1$
        ter.setName(Messages.AttributesTERName);
        ter.setColumnLabel(Messages.AttributesTERColumn);
        ter.setTarget(Security.class);
        ter.setType(Double.class);
        ter.setConverter(PercentConverter.class);
        attributeTypes.add(ter);

        AttributeType aum = new AttributeType("aum"); //$NON-NLS-1$
        aum.setName(Messages.AttributesAUMName);
        aum.setColumnLabel(Messages.AttributesAUMColumn);
        aum.setTarget(Security.class);
        aum.setType(Long.class);
        aum.setConverter(AmountPlainConverter.class);
        attributeTypes.add(aum);

        AttributeType vendor = new AttributeType("vendor"); //$NON-NLS-1$
        vendor.setName(Messages.AttributesVendorName);
        vendor.setColumnLabel(Messages.AttributesVendorColumn);
        vendor.setTarget(Security.class);
        vendor.setType(String.class);
        vendor.setConverter(StringConverter.class);
        attributeTypes.add(vendor);

        AttributeType fee = new AttributeType("acquisitionFee"); //$NON-NLS-1$
        fee.setName(Messages.AttributesAcquisitionFeeName);
        fee.setColumnLabel(Messages.AttributesAcquisitionFeeColumn);
        fee.setTarget(Security.class);
        fee.setType(Double.class);
        fee.setConverter(PercentConverter.class);
        attributeTypes.add(fee);

        AttributeType managementFee = new AttributeType("managementFee"); //$NON-NLS-1$
        managementFee.setName(Messages.AttributesManagementFeeName);
        managementFee.setColumnLabel(Messages.AttributesManagementFeeColumn);
        managementFee.setTarget(Security.class);
        managementFee.setType(Double.class);
        managementFee.setConverter(PercentConverter.class);
        attributeTypes.add(managementFee);
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

    public void removeAttributeType(AttributeType type)
    {
        attributeTypes.remove(type);
    }

    public void addAttributeType(AttributeType type)
    {
        attributeTypes.add(type);
    }

    public void addAttributeType(int index, AttributeType type)
    {
        attributeTypes.add(index, type);
    }

    public ConfigurationSet getConfigurationSet(String key)
    {
        return configurationSets.computeIfAbsent(key, k -> new ConfigurationSet());
    }
}
