package name.abuchen.portfolio.ui.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.util.List;

import org.junit.Test;

@SuppressWarnings("nls")
public class UnrecognizedPDFCacheTest
{
    @Test
    public void capAtFive()
    {
        var cache = new UnrecognizedPDFCache();
        cache.add("doc1", "text1", null);
        cache.add("doc2", "text2", null);
        cache.add("doc3", "text3", null);
        cache.add("doc4", "text4", null);
        cache.add("doc5", "text5", null);
        cache.add("doc6", "text6", null);

        List<UnrecognizedPDFCache.Entry> entries = cache.getEntries();
        assertThat(entries, hasSize(5));

        // doc1 (first added) should have been evicted
        boolean containsDoc1 = entries.stream().anyMatch(e -> e.getName().equals("doc1"));
        assertThat(containsDoc1, is(false));
    }

    @Test
    public void mostRecentFirst()
    {
        var cache = new UnrecognizedPDFCache();
        cache.add("A", "textA", null);
        cache.add("B", "textB", null);
        cache.add("C", "textC", null);

        var entries = cache.getEntries();
        assertThat(entries.get(0).getName(), is("C"));
        assertThat(entries.get(1).getName(), is("B"));
        assertThat(entries.get(2).getName(), is("A"));
    }

    @Test
    public void dedupeMovesToFront()
    {
        var cache = new UnrecognizedPDFCache();
        cache.add("A", "textA", null);
        cache.add("B", "textB", null);
        // re-add A with same name+text
        cache.add("A", "textA", null);

        var entries = cache.getEntries();
        assertThat(entries, hasSize(2));
        assertThat(entries.get(0).getName(), is("A"));
        assertThat(entries.get(1).getName(), is("B"));
    }

    @Test
    public void reAddEvictedGoesToFront()
    {
        var cache = new UnrecognizedPDFCache();
        // fill to capacity (d1..d5)
        cache.add("d1", "text1", null);
        cache.add("d2", "text2", null);
        cache.add("d3", "text3", null);
        cache.add("d4", "text4", null);
        cache.add("d5", "text5", null);
        // d6 evicts d1
        cache.add("d6", "text6", null);

        // re-add with same name+text as d2 → should become most-recent
        cache.add("d2", "text2", null);

        var entries = cache.getEntries();
        assertThat(entries, hasSize(5));
        assertThat(entries.get(0).getName(), is("d2"));
    }

    @Test
    public void dedupeKeyUsesNameAndText()
    {
        var cache = new UnrecognizedPDFCache();
        cache.add("doc", "textA", null);
        cache.add("doc", "textB", null);

        // same name but different text → two distinct entries
        assertThat(cache.getEntries(), hasSize(2));
    }

    @Test
    public void clearEmpties()
    {
        var cache = new UnrecognizedPDFCache();
        cache.add("doc", "text", null);
        cache.clear();

        assertThat(cache.getEntries(), is(empty()));
    }
}
