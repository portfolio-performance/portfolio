package name.abuchen.portfolio.online.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.junit.BeforeClass;
import org.junit.Test;

import name.abuchen.portfolio.SecurityBuilder;
import name.abuchen.portfolio.TaxonomyBuilder;
import name.abuchen.portfolio.TestUtilities;
import name.abuchen.portfolio.model.AttributeType;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Classification.Assignment;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientSettings;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Taxonomy;

@SuppressWarnings("nls")
public class ETFDataComTest
{
    private static ETFDataCom.OnlineItem item;

    @BeforeClass
    public static void setup()
    {
        JSONObject object = (JSONObject) JSONValue
                        .parse(TestUtilities.read(ETFDataComTest.class, "etf-data.com.response.txt"));
        item = ETFDataCom.OnlineItem.from(object);
    }

    @Test
    public void testItemAttributes()
    {
        assertThat(item.getIsin(), is("DE000A0H0744"));
        assertThat(item.getName(), is("iShares Dow Jones Asia Pacific Select Dividend 50 UCITS ETF (DE)"));

        // test creation of security

        ClientSettings settings = new ClientSettings();
        settings.getAttributeTypes().collect(Collectors.toList()).stream().forEach(settings::removeAttributeType);

        Security security = item.create(settings);

        assertThat(security.getIsin(), is(item.getIsin()));
        assertThat(security.getName(), is(item.getName()));

        assertThat(settings.getAttributeTypes().count(), is(7L));

        Function<String, Object> attribute2value = name -> {
            AttributeType type = settings.getAttributeTypes().filter(a -> name.equals(a.getSource())).findFirst()
                            .orElseThrow(IllegalArgumentException::new);
            return security.getAttributes().get(type);
        };

        assertThat(attribute2value.apply("etf-data.com$distributionType"), is("DISTRIBUTING"));
        assertThat(attribute2value.apply("etf-data.com$distributionFrequency"), is(nullValue()));
        assertThat(attribute2value.apply("etf-data.com$domicile"), is("DEU"));
    }

    @Test
    public void testUpdateOfTaxonomy()
    {
        final String someIdentifier = "SOMETHING";

        Client client = new Client();
        Security security = new SecurityBuilder().addTo(client);
        Taxonomy taxonomy = new TaxonomyBuilder().addClassification(someIdentifier).addTo(client);

        boolean hasModifications = item.updateCountryAllocation(taxonomy, security);
        assertThat(hasModifications, is(true));

        // second update does not modify the taxonomy
        hasModifications = item.updateCountryAllocation(taxonomy, security);
        assertThat(hasModifications, is(false));

        List<Classification> classifications = taxonomy.getClassifications(security);
        assertThat(classifications.size(), is(6));
        Assignment assignment = classifications.stream().filter(c -> c.getName().equals("CHN")).findAny()
                        .orElseThrow(IllegalArgumentException::new).getAssignments().get(0);
        assertThat(assignment.getWeight(), is(451));

        // modify taxonomy

        assignment.setWeight(111);

        classifications.stream().filter(c -> c.getName().equals("NZL")).findAny()
                        .orElseThrow(IllegalArgumentException::new).clearAssignments();

        taxonomy.getClassificationById(someIdentifier).addAssignment(new Assignment(security));

        hasModifications = item.updateCountryAllocation(taxonomy, security);
        assertThat(hasModifications, is(true));
        assertThat(assignment.getWeight(), is(451));
        assertThat(taxonomy.getClassificationById(someIdentifier).getAssignments().isEmpty(), is(true));

    }
}
