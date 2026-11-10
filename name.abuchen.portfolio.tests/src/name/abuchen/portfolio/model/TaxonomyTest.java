package name.abuchen.portfolio.model;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.beans.PropertyChangeEvent;
import java.util.ArrayList;

import org.junit.Test;

@SuppressWarnings("nls")
public class TaxonomyTest
{
    @Test
    public void testRenameAlsoRenamesRootNode()
    {
        var taxonomy = new Taxonomy("Old");
        taxonomy.setRootNode(new Classification("root", "Old"));

        taxonomy.rename("New");

        assertThat(taxonomy.getName(), is("New"));
        assertThat(taxonomy.getRoot().getName(), is("New"));
    }

    @Test
    public void testRenameNotifiesNameListener()
    {
        var taxonomy = new Taxonomy("Old");
        taxonomy.setRootNode(new Classification("root", "Old"));

        var events = new ArrayList<PropertyChangeEvent>();
        taxonomy.addPropertyChangeListener("name", events::add);

        taxonomy.rename("New");

        assertThat(events.size(), is(1));
        assertThat(events.get(0).getOldValue(), is("Old"));
        assertThat(events.get(0).getNewValue(), is("New"));
    }

    @Test
    public void testRenameWithDifferentRootName()
    {
        var taxonomy = new Taxonomy("Old");
        taxonomy.setRootNode(new Classification("root", "Renamed root"));

        taxonomy.rename("New");

        assertThat(taxonomy.getRoot().getName(), is("New"));
    }

    @Test
    public void testRenameWithoutRootNode()
    {
        var taxonomy = new Taxonomy("Old");

        taxonomy.rename("New");

        assertThat(taxonomy.getName(), is("New"));
    }

    @Test
    public void testSetNameDoesNotRenameRootNode()
    {
        var taxonomy = new Taxonomy("Old");
        taxonomy.setRootNode(new Classification("root", "Old"));

        taxonomy.setName("New");

        assertThat(taxonomy.getRoot().getName(), is("Old"));
    }
}
