package name.abuchen.portfolio.online.impl;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

import org.junit.Test;
import org.mockito.Mockito;

import name.abuchen.portfolio.online.impl.TASE.TASEEntities;
import name.abuchen.portfolio.online.impl.TASE.jsondata.IndiceListing;
import name.abuchen.portfolio.online.impl.TASE.utils.TASEHelper.Language;
import name.abuchen.portfolio.online.impl.TASE.utils.TASEHelper.TaseSecurityType;

public class TASEEntitiesTest
{

    /**
     * Load JSON response of TLV entities supported
     */
    private String getEntitiesList()

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

    @Test
    public void testTaseGetAllEntitiesResponse()
    {
        String mockedresponse = getEntitiesList();
        assertTrue(mockedresponse.length() > 0);


        try
        {
            TASEEntities indices = Mockito.spy(new TASEEntities());

            Mockito.doReturn((mockedresponse)).when(indices).rpcAllIndices(Language.ENGLISH);


            Optional<List<IndiceListing>> iListOptional = indices.getAllListings(Language.ENGLISH);

            assertFalse(iListOptional.isEmpty());
            List<IndiceListing> iList = iListOptional.get();

            assertThat(iList.size(), is(130));

            IndiceListing listing = iList.get(0);

            assertTrue(listing.getId().equals("2442"));
            assertTrue(iList.get(0).getType() == TaseSecurityType.DELETED.getValue());

            verify(indices).rpcAllIndices(Language.ENGLISH);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            assertTrue(false);
        }

    }

}
