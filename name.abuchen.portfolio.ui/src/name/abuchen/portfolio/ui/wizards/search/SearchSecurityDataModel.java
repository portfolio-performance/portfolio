package name.abuchen.portfolio.ui.wizards.search;

import java.util.HashSet;
import java.util.Set;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.online.SecuritySearchProvider.ResultItem;

public class SearchSecurityDataModel
{
    private final Client client;

    /**
     * The selected instrument (which might require the user to pick a market).
     */
    private ResultItem selectedInstrument;

    /**
     * The selected market of the instrument. Might be identical to the selected
     * instrument, if no markets are available.
     */
    private ResultItem selectedMarket;

    /**
     * Selected to currencies to filter the search results.
     */
    private Set<CurrencyUnit> filterByCurrency = new HashSet<>();

    public SearchSecurityDataModel(Client client)
    {
        this.client = client;
    }

    public Client getClient()
    {
        return client;
    }

    public ResultItem getSelectedInstrument()
    {
        return selectedInstrument;
    }

    public void setSelectedInstrument(ResultItem item)
    {
        this.selectedInstrument = item;
    }

    public void clearSelectedInstrument()
    {
        this.selectedInstrument = null;
        this.selectedMarket = null;
    }

    public ResultItem getSelectedMarket()
    {
        return selectedMarket;
    }

    public void setSelectedMarket(ResultItem market)
    {
        this.selectedMarket = market;
    }

    public void clearSelectedMarket()
    {
        this.selectedMarket = null;
    }

    public void addCurrency(CurrencyUnit currency)
    {
        filterByCurrency.add(currency);
    }

    public void removeCurrency(CurrencyUnit currency)
    {
        filterByCurrency.remove(currency);
    }

    public boolean hasCurrencies()
    {
        return !filterByCurrency.isEmpty();
    }

    public Set<CurrencyUnit> getCurrencies()
    {
        return filterByCurrency;
    }
}
