package issues;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.io.IOException;

import org.junit.Test;

import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.Taxonomy;

public class IssueMigrationOfClassificationKeysTest
{
    @Test
    public void testMigrationOfClassificationKeys() throws IOException
    {
        Client client = ClientFactory.load(Issue1498FifoCrossPortfolioTest.class
                        .getResourceAsStream("IssueMigrationOfClassificationKeys.xml")); //$NON-NLS-1$

        assertThat(client.getFileVersionAfterRead(), is(54));

        Taxonomy taxonomy = client.getTaxonomies().get(0);

        assertThat(taxonomy.getRoot().getChildren().size(), is(2));

        Classification one = taxonomy.getRoot().getChildren().get(0);
        Classification two = taxonomy.getRoot().getChildren().get(1);

        assertThat(one.getName(), is(two.getName()));
        assertThat(one.getId(), is(not(two.getId())));
    }
}
