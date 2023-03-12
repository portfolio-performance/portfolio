package issues;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.Taxonomy;

public class IssueUmarshallingArraysArrayListTest
{
    @Test
    public void testMigrationOfClassificationKeys() throws IOException
    {
        Client client = ClientFactory.load(IssueUmarshallingArraysArrayListTest.class
                        .getResourceAsStream("IssueUmarshallingArraysArrayList.xml")); //$NON-NLS-1$

        Taxonomy taxonomy = client.getTaxonomies().get(0);

        assertThat(taxonomy.getDimensions(), instanceOf(ArrayList.class));
        assertThat(taxonomy.getDimensions().size(), is(3));

        assertThat(taxonomy.getDimensions(), is(Arrays.asList("Markt", "Region", "Land"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }
}
