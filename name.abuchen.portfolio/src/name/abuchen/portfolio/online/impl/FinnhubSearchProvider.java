package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.online.SecuritySearchProvider;

/**
 * Use the <a href="https://finnhub.io/">Finnhub</a> API to search for securities.
 * 
 * @see FinnhubSymbolSearch
 */
public class FinnhubSearchProvider implements SecuritySearchProvider
{
    public static final String ID = "FINNHUB-SYMBOL-SEARCH"; //$NON-NLS-1$

    private String apiKey;

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getName()
    {
        return "Finnhub"; //$NON-NLS-1$
    }

    public void setApiKey(String apiKey)
    {
        this.apiKey = apiKey;
    }

    /**
     * <p>Search for the symbol or ISIN provided in  the <code>query</code> parameter.
     * The <code>type</code> parameter is not used. </p>
     * 
     * <p>If the FinnHub API key is null or blank then an empty <code>List</code> is returned. This prevents 
     * <code>401 Unauthorized</code> errors for those users who have not configured FinnHub.</p>
     * 
     * @param query symbol or ISIN to look up
     * @param type not used
     * @return <code>List</code> of the found securities.
     */
    @Override
    public List<ResultItem> search(String query, Type type) throws IOException
    {
        if (apiKey == null || apiKey.isBlank())
            return Collections.emptyList();
            
        List<ResultItem> answer = new ArrayList<>();

        addSymbolSearchResults(answer, query);

        if (answer.size() >= 10)
        {
            FinnhubSymbolSearch.Result item = new FinnhubSymbolSearch.Result(Messages.MsgMoreResultsAvailable);
            answer.add(item);
        }

        return answer;
    }

    private void addSymbolSearchResults(List<ResultItem> answer, String query) throws IOException
    {
        Set<String> existingSymbols = answer.stream().map(ResultItem::getSymbol).collect(Collectors.toSet());

        new FinnhubSymbolSearch(apiKey).search(query)//
                        .filter(r -> !existingSymbols.contains(r.getSymbol())).forEach(answer::add);
    }
}
