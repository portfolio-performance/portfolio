package name.abuchen.portfolio.online.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import org.json.simple.JSONArray;
import org.json.simple.JSONValue;
import org.junit.Test;

import name.abuchen.portfolio.online.SecuritySearchProvider.ResultItem;
import name.abuchen.portfolio.online.impl.TASE.jsondata.IndiceListing;

public class TASESearchProviderTest
{

    private String getIndicesList()

    {
        return getHistoricalTaseQuotes("response_tase_list_indices.txt");
    }

    private String getHistoricalTaseQuotes(String filename)
    {
        String responseBody = null;
        Scanner scanner = new Scanner(getClass().getResourceAsStream(filename), "UTF-8");
        responseBody = scanner.useDelimiter("\\A").next();
        scanner.close();

        return responseBody;
    }

    @SuppressWarnings("unchecked")
    @Test
    public void SearchProvidershouldNotLoadIndicesWhenCreated() throws NoSuchFieldException, IllegalAccessException
    {
        TASESearchProvider tase = new TASESearchProvider();

        Field tlvEntitiesField = TASESearchProvider.class.getDeclaredField("tlvEntities");
        tlvEntitiesField.setAccessible(true);

        List<IndiceListing> a = (List<IndiceListing>) tlvEntitiesField.get(tase);
        assertTrue(a == null);

    }

    @Test
    public void searchTASESearchProvider()
                    throws ClassNotFoundException, IllegalArgumentException, IllegalAccessException,
                    NoSuchFieldException, SecurityException
    {

        String mockedjsonResponse = getIndicesList();
        List<IndiceListing> mockedResponse = extract(mockedjsonResponse);

        TASESearchProvider tase = new TASESearchProvider();
        Field tlvEntitiesField = TASESearchProvider.class.getDeclaredField("tlvEntities");
        tlvEntitiesField.setAccessible(true);

        tlvEntitiesField.set(tase, mockedResponse);

        @SuppressWarnings("unchecked")
        List<IndiceListing> a = (List<IndiceListing>) tlvEntitiesField.get(tase);
        assertThat(a.size(), is(130));
        try
        {
            List<ResultItem> results = tase.search("NICE");
            verifyResult(results);
            results = tase.search("Nice");
            verifyResult(results);
            results = tase.search("IL0002730112");
            verifyResult(results);
            results = tase.search("273011");
            verifyResult(results);
            results = tase.search("NICE.TLV");
            assertThat(results.size(), is(0));
            results = tase.search("NICE.TASE");
            assertThat(results.size(), is(0));
            // verifyResult(results);

            results = tase.search("1188572");
            assertThat(results.size(), is(1));
            assertThat(results.get(0).getCurrencyCode(), is("ILA"));
            assertThat(results.get(0).getExchange(), is("TASE"));
            assertThat(results.get(0).getIsin(), is("IL0011885725"));
            assertThat(results.get(0).getSymbol(), is("ACRO.B1"));
            assertThat(results.get(0).getSymbolWithoutStockMarket(), is("ACRO.B1"));
            assertThat(results.get(0).getName(), is("ACRO B1"));
            assertThat(results.get(0).getType(), is("Corporate Bonds"));

            // @api
            // https://market.tase.co.il/en/market_data/security/1184902/major_data
            results = tase.search("1184902");
            assertThat(results.size(), is(1));
            assertThat(results.get(0).getCurrencyCode(), is("ILA"));
            assertThat(results.get(0).getExchange(), is("TASE"));
            assertThat(results.get(0).getIsin(), is("IL0011849028"));
            assertThat(results.get(0).getSymbol(), is("ACRO"));
            assertThat(results.get(0).getSymbolWithoutStockMarket(), is("ACRO"));
            assertThat(results.get(0).getName(), is("ACRO KVUT"));
            assertThat(results.get(0).getType(), is("Shares"));


            results = tase.search("1150192");
            assertThat(results.size(), is(1));
            assertThat(results.get(0).getCurrencyCode(), is("ILA"));
            assertThat(results.get(0).getExchange(), is("TASE"));
            assertThat(results.get(0).getIsin(), is("IL0011501926"));
            assertThat(results.get(0).getSymbol(), is("HRL.F204"));
            assertThat(results.get(0).getSymbolWithoutStockMarket(), is("HRL.F204"));
            assertThat(results.get(0).getName(), is("Harel Sal (4A) ISE Cyber Security Dollar-Hedged"));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

    }

    private List<IndiceListing> extract(String array)
    {
        var jsonArray = (JSONArray) JSONValue.parse(array);

        if (jsonArray.isEmpty())
            return Collections.emptyList();

        List<IndiceListing> answer = new ArrayList<>();
        for (Object element : jsonArray)
        {
            answer.add(IndiceListing.fromJson(element.toString()));
        }
        return answer;
    }

    private void verifyResult(List<ResultItem> results)
    {
        assertThat(results.size(), is(1));
        assertThat(results.get(0).getCurrencyCode(), is("ILA"));
        assertThat(results.get(0).getExchange(), is("TASE"));
        assertThat(results.get(0).getIsin(), is("IL0002730112"));
        assertThat(results.get(0).getSymbol(), is("NICE"));
        assertThat(results.get(0).getSymbolWithoutStockMarket(), is("NICE"));
        assertThat(results.get(0).getType(), is("Shares"));
    }
}
