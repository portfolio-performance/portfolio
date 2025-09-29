package name.abuchen.portfolio.online.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.online.impl.TASE.jsondata.IndiceListing;

public class TaseSearchProviderTest
{

    @Test
    public void SearchProvidershouldLoadIndicesWhenCreated() throws NoSuchFieldException, IllegalAccessException
    {
        TASESearchProvider tase = new TASESearchProvider();

        Field tlvEntitiesField = TASESearchProvider.class.getDeclaredField("tlvEntities");
        tlvEntitiesField.setAccessible(true);

        List<IndiceListing> a = (List<IndiceListing>) tlvEntitiesField.get(tase);
        assertThat(a.size(), greaterThan(0));

    }
}
