package name.abuchen.portfolio.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.AttributeType.AmountPlainConverter;
import name.abuchen.portfolio.model.AttributeType.ImageConverter;
import name.abuchen.portfolio.model.AttributeType.PercentConverter;
import name.abuchen.portfolio.model.AttributeType.StringConverter;
import name.abuchen.portfolio.model.ConfigurationSet.WellKnownConfigurationSets;

public class ClientSettings
{
    private List<Bookmark> bookmarks;
    private List<AttributeType> attributeTypes;
    private Map<String, ConfigurationSet> configurationSets;

    /* package */ ClientSettings()
    {
        doPostLoadInitialization();
    }

    public void doPostLoadInitialization()
    {
        if (bookmarks == null)
        {
            this.bookmarks = new ArrayList<>();
            this.bookmarks.addAll(getDefaultBookmarks());
        }

        if (attributeTypes == null)
        {
            this.attributeTypes = new ArrayList<>();
            addDefaultAttributeTypes();
        }

        if (configurationSets == null)
            configurationSets = new HashMap<>();
    }

    public static List<Bookmark> getDefaultBookmarks()
    {
        List<Bookmark> answer = new ArrayList<>();

        answer.add(new Bookmark("finance.yahoo.com", //$NON-NLS-1$
                        "https://finance.yahoo.com/quote/{tickerSymbol}")); //$NON-NLS-1$
        answer.add(new Bookmark("onvista.de", //$NON-NLS-1$
                        "https://www.onvista.de/suche.html?SEARCH_VALUE={isin}")); //$NON-NLS-1$
        answer.add(new Bookmark("finanzen.net", //$NON-NLS-1$
                        "https://www.finanzen.net/suchergebnis.asp?frmAktiensucheTextfeld={isin}")); //$NON-NLS-1$
        answer.add(new Bookmark("ariva.de", //$NON-NLS-1$
                        "https://www.ariva.de/{isin}")); //$NON-NLS-1$
        answer.add(new Bookmark("justetf.com  (ETF)", //$NON-NLS-1$
                        "https://www.justetf.com/etf-profile.html?isin={isin}")); //$NON-NLS-1$
        answer.add(new Bookmark("fondsweb.com", //$NON-NLS-1$
                        "https://www.fondsweb.com/{isin}")); //$NON-NLS-1$
        answer.add(new Bookmark("morningstar.de", //$NON-NLS-1$
                        "https://www.morningstar.de/de/funds/SecuritySearchResults.aspx?type=ALL&search={isin}")); //$NON-NLS-1$
        answer.add(new Bookmark("extraETF.com (ETF)", //$NON-NLS-1$
                        "https://extraetf.com/etf-profile/{isin}")); //$NON-NLS-1$
        answer.add(new Bookmark("alleaktien.de (" + Messages.LabelSearchShare + ")", //$NON-NLS-1$ //$NON-NLS-2$
                        "https://www.alleaktien.de/data/{isin}")); //$NON-NLS-1$
        answer.add(new Bookmark("comdirect.de (" + Messages.LabelSearchShare + ")", //$NON-NLS-1$ //$NON-NLS-2$
                        "https://www.comdirect.de/inf/aktien/{isin}")); //$NON-NLS-1$
        answer.add(new Bookmark("comdirect.de (ETF)", //$NON-NLS-1$
                        "https://www.comdirect.de/inf/etfs/{isin}")); //$NON-NLS-1$
        answer.add(new Bookmark("divvydiary.com", //$NON-NLS-1$
                        "https://divvydiary.com/symbols/{isin}")); //$NON-NLS-1$
        answer.add(new Bookmark("trackingdifferences.com (ETF)", //$NON-NLS-1$
                        "https://www.trackingdifferences.com/ETF/ISIN/{isin}")); //$NON-NLS-1$
        answer.add(new Bookmark("tradingview.com", //$NON-NLS-1$
                        "https://www.tradingview.com/chart/?symbol=XETR:{tickerSymbolPrefix}")); //$NON-NLS-1$
        answer.add(new Bookmark("cnbc.com (" + Messages.LabelSearchShare + ")", //$NON-NLS-1$ //$NON-NLS-2$
                        "https://www.cnbc.com/quotes/{tickerSymbolPrefix}")); //$NON-NLS-1$
        answer.add(new Bookmark("nasdaq.com (" + Messages.LabelSearchShare + ")", //$NON-NLS-1$ //$NON-NLS-2$
                        "https://www.nasdaq.com/market-activity/stocks/{tickerSymbolPrefix}")); //$NON-NLS-1$
        answer.add(new Bookmark("aktienfinder.net (" + Messages.LabelSearchShare + ")", //$NON-NLS-1$ //$NON-NLS-2$
                        "https://aktienfinder.net/aktien-profil/{isin}")); //$NON-NLS-1$
        answer.add(new Bookmark("aktien.guide (" + Messages.LabelSearchShare + ")", //$NON-NLS-1$ //$NON-NLS-2$
                        "http://aktien.guide/isin/aktien/{isin}")); //$NON-NLS-1$
        return answer;
    }

    private void addDefaultAttributeTypes()
    {
        Function<Class<? extends Attributable>, AttributeType> factory = target -> {
            AttributeType logoType = new AttributeType("logo"); //$NON-NLS-1$
            logoType.setName(Messages.AttributesLogoName);
            logoType.setColumnLabel(Messages.AttributesLogoColumn);
            logoType.setTarget(target);
            logoType.setType(String.class);
            logoType.setConverter(ImageConverter.class);
            return logoType;
        };

        attributeTypes.add(factory.apply(Security.class));
        attributeTypes.add(factory.apply(Account.class));
        attributeTypes.add(factory.apply(Portfolio.class));
        attributeTypes.add(factory.apply(InvestmentPlan.class));

        AttributeType ter = new AttributeType("ter"); //$NON-NLS-1$
        ter.setName(Messages.AttributesTERName);
        ter.setColumnLabel(Messages.AttributesTERColumn);
        ter.setTarget(Security.class);
        ter.setSource("ter"); //$NON-NLS-1$
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
        vendor.setSource("vendor"); //$NON-NLS-1$
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

    public void clearBookmarks()
    {
        bookmarks.clear();
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

    public int getAttributeTypeIndexOf(AttributeType type)
    {
        return attributeTypes.indexOf(type);
    }

    public void clearAttributeTypes()
    {
        attributeTypes.clear();
    }

    public boolean hasConfigurationSet(String key)
    {
        return configurationSets.containsKey(key);
    }

    public ConfigurationSet getConfigurationSet(String key)
    {
        return configurationSets.computeIfAbsent(key, k -> new ConfigurationSet());
    }

    public ConfigurationSet getConfigurationSet(WellKnownConfigurationSets wellKnown)
    {
        return getConfigurationSet(wellKnown.getKey());
    }

    public Set<Map.Entry<String, ConfigurationSet>> getConfigurationSets()
    {
        return configurationSets.entrySet();
    }

    public void clearConfigurationSets()
    {
        configurationSets.clear();
    }

    public void putAllConfigurationSets(Map<String, ConfigurationSet> newSets)
    {
        configurationSets.putAll(newSets);
    }

    @SuppressWarnings("unchecked")
    public Optional<AttributeType> getOptionalLogoAttributeType(Class<? extends Object> type)
    {
        return getAttributeTypes().filter(t -> t.getConverter() instanceof AttributeType.ImageConverter
                        && t.getName().equalsIgnoreCase("logo") //$NON-NLS-1$
                        && t.supports((Class<? extends Attributable>) type)).findFirst();
    }
}
