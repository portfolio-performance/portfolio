package name.abuchen.portfolio.datatransfer.actions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.SecurityUpdate;
import name.abuchen.portfolio.datatransfer.SecurityUpdate.AttributeUpdate;
import name.abuchen.portfolio.datatransfer.SecurityUpdate.NoteUpdate;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityUpdateItem;
import name.abuchen.portfolio.model.AttributeType;
import name.abuchen.portfolio.model.AttributeType.StringConverter;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyUnit;

@SuppressWarnings("nls")
public class InsertActionSecurityUpdateTest
{
    private AttributeType stringAttribute()
    {
        var type = new AttributeType("rating");
        type.setName("Rating");
        type.setColumnLabel("Rating");
        type.setTarget(Security.class);
        type.setType(String.class);
        type.setConverter(StringConverter.class);
        return type;
    }

    @Test
    public void testUpdatesAreAppliedOnInsert()
    {
        var attribute = stringAttribute();
        var security = new Security("SAP SE", CurrencyUnit.EUR);

        var client = new Client();
        client.getSettings().addAttributeType(attribute);
        client.addSecurity(security);

        List<SecurityUpdate> updates = new ArrayList<>();
        updates.add(new AttributeUpdate(attribute, "A"));
        updates.add(new NoteUpdate("hello"));

        var item = new SecurityUpdateItem(security, updates);
        item.apply(new InsertAction(client), null);

        assertThat(security.getAttributes().get(attribute), is("A"));
        assertThat(security.getNote(), is("hello"));
    }

    @Test
    public void testSetNoteNullRemovesNoteUpdate()
    {
        var attribute = stringAttribute();
        var security = new Security("SAP SE", CurrencyUnit.EUR);

        var client = new Client();
        client.getSettings().addAttributeType(attribute);
        client.addSecurity(security);
        security.setNote("keep me");

        List<SecurityUpdate> updates = new ArrayList<>();
        updates.add(new AttributeUpdate(attribute, "A"));
        updates.add(new NoteUpdate("overwrite"));

        var item = new SecurityUpdateItem(security, updates);
        item.setNote(null); // "do not import notes from source"
        item.apply(new InsertAction(client), null);

        assertThat(security.getAttributes().get(attribute), is("A"));
        assertThat(security.getNote(), is("keep me")); // untouched
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSetSecurityThrows()
    {
        var item = new SecurityUpdateItem(new Security("A", CurrencyUnit.EUR), new ArrayList<>());
        item.setSecurity(new Security("B", CurrencyUnit.EUR));
    }

    @Test
    public void testTypeInformationDoesNotNpeOnNullAttributeName()
    {
        var type = new AttributeType("rating"); // name deliberately left null
        type.setTarget(Security.class);
        type.setType(String.class);
        type.setConverter(StringConverter.class);

        List<SecurityUpdate> updates = new ArrayList<>();
        updates.add(new AttributeUpdate(type, "A"));

        var item = new SecurityUpdateItem(new Security("SAP SE", CurrencyUnit.EUR), updates);

        // must not throw; label falls back to the id
        assertThat(item.getTypeInformation().contains("rating"), is(true));
    }

    @Test
    public void testEmptyUpdateListIsHarmlessNoop()
    {
        var security = new Security("SAP SE", CurrencyUnit.EUR);
        var client = new Client();
        client.addSecurity(security);

        var item = new SecurityUpdateItem(security, new ArrayList<>());
        item.apply(new InsertAction(client), null);

        assertThat(security.getNote(), is(nullValue()));
    }
}
