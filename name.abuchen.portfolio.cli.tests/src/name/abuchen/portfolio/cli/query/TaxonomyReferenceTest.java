package name.abuchen.portfolio.cli.query;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThrows;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import name.abuchen.portfolio.cli.command.CliException;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Taxonomy;

public class TaxonomyReferenceTest
{
    private final Client client = new Client();

    private Taxonomy taxonomy(String name)
    {
        var taxonomy = new Taxonomy(name);
        client.addTaxonomy(taxonomy);
        return taxonomy;
    }

    private Taxonomy resolve(String value) throws CliException
    {
        return TaxonomiesCommand.REFERENCE.resolve(client, "taxonomy", value);
    }

    @Test
    public void resolvesByIdAndName() throws CliException
    {
        var regions = taxonomy("Regions");
        taxonomy("Asset classes");

        assertThat(resolve(regions.getId()), is(sameInstance(regions)));
        assertThat(resolve("regions"), is(sameInstance(regions)));
        assertThat(assertThrows(CliException.class, () -> resolve("Sectors")).getCode(), is("reference-not-found"));
    }

    @Test
    public void ambiguousTaxonomiesCarryNoRetiredFlag()
    {
        var first = taxonomy("Regions");
        taxonomy("REGIONS");

        var e = assertThrows(CliException.class, () -> resolve("regions"));

        assertThat(e.getCode(), is("reference-ambiguous"));
        @SuppressWarnings("unchecked")
        var candidates = (List<Map<String, Object>>) e.toProblem().get("candidates");
        var candidate = candidates.stream().filter(c -> c.get("uuid").equals(first.getId())).findFirst()
                        .orElseThrow();
        assertThat(candidate.keySet().toString(), is("[uuid, name, matchedBy]"));
    }
}
