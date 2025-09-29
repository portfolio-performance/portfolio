package name.abuchen.portfolio.online.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.online.SecuritySearchProvider.ResultItem;
import name.abuchen.portfolio.online.impl.TASE.jsondata.IndiceListing;

public class TASESearchProviderTest
{

    @Test
    public void SearchProvidershouldLoadIndicesWhenCreated() throws NoSuchFieldException, IllegalAccessException
    {
        TASESearchProvider tase = new TASESearchProvider();

        Field tlvEntitiesField = TASESearchProvider.class.getDeclaredField("tlvEntities");
        tlvEntitiesField.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<IndiceListing> a = (List<IndiceListing>) tlvEntitiesField.get(tase);
        assertThat(a.size(), greaterThan(0));

    }

    @Test
    public void SearchProvidershouldFindaStock()
                    throws ClassNotFoundException, IllegalArgumentException, IllegalAccessException,
                    NoSuchFieldException, SecurityException
    {
        List<IndiceListing> mockedResponse = new ArrayList<>();
        IndiceListing a = new IndiceListing("100", "NICE", "NICE", "IL00012345");
        
        mockedResponse.add(a);

        TASESearchProvider tase = new TASESearchProvider();
        Field tlvEntitiesField = TASESearchProvider.class.getDeclaredField("tlvEntities");
        tlvEntitiesField.setAccessible(true);

        tlvEntitiesField.set(tase, mockedResponse);
        
        try
        {
            List<ResultItem> results = tase.search("NICE");
            verifyResult(results);
            results = tase.search("Nice");
            verifyResult(results);
            results = tase.search("IL00012345");
            verifyResult(results);
            results = tase.search("100");
            verifyResult(results);
            results = tase.search("NICE.TLV");
            verifyResult(results);

        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    private void verifyResult(List<ResultItem> results)
    {
        assertThat(results.size(), is(1));
        assertThat(results.get(0).getCurrencyCode(), is("ILA"));
        assertThat(results.get(0).getExchange(), is("TLV"));
        assertThat(results.get(0).getIsin(), is("IL00012345"));
        assertThat(results.get(0).getSymbol(), is("NICE"));
        assertThat(results.get(0).getSymbolWithoutStockMarket(), is("NICE"));
    }
}
