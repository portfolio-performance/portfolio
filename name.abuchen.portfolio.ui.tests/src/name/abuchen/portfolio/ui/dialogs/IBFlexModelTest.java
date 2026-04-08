package name.abuchen.portfolio.ui.dialogs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceStore;
import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.ui.dialogs.IBFlexConfigurationDialog.Cutoff;
import name.abuchen.portfolio.ui.dialogs.IBFlexConfigurationDialog.Credential;

public class IBFlexModelTest
{
    private Client client;
    private IPreferenceStore store;

    @Before
    public void setUp()
    {
        client = new Client();
        store = new PreferenceStore();
    }

    @Test
    public void testGetCredentialsReturnsEmptyListWhenNotConfigured()
    {
        var credentials = IBFlexConfigurationDialog.getCredentials(store);
        assertThat(credentials, hasSize(0));
    }

    @Test
    public void testSetAndGetCredentials()
    {
        var credentials = List.of(new Credential("12345", "mytoken", "Main Account"));
        IBFlexConfigurationDialog.setCredentials(store, credentials);

        var loaded = IBFlexConfigurationDialog.getCredentials(store);
        assertThat(loaded, hasSize(1));
        assertThat(loaded.get(0).queryId(), is("12345"));
        assertThat(loaded.get(0).token(), is("mytoken"));
        assertThat(loaded.get(0).name(), is("Main Account"));
    }

    @Test
    public void testMultipleCredentials()
    {
        var credentials = List.of(
                        new Credential("11111", "token1"),
                        new Credential("22222", "token2"),
                        new Credential("33333", "token3"));
        IBFlexConfigurationDialog.setCredentials(store, credentials);

        var loaded = IBFlexConfigurationDialog.getCredentials(store);
        assertThat(loaded, hasSize(3));
        assertThat(loaded.get(0).queryId(), is("11111"));
        assertThat(loaded.get(0).token(), is("token1"));
        assertThat(loaded.get(0).name(), is(nullValue()));
        assertThat(loaded.get(1).queryId(), is("22222"));
        assertThat(loaded.get(2).queryId(), is("33333"));
    }

    @Test
    public void testClearCredentials()
    {
        var credentials = List.of(new Credential("12345", "mytoken"));
        IBFlexConfigurationDialog.setCredentials(store, credentials);

        IBFlexConfigurationDialog.setCredentials(store, new ArrayList<>());

        var loaded = IBFlexConfigurationDialog.getCredentials(store);
        assertThat(loaded, hasSize(0));
    }

    @Test
    public void testSerializeDeserializeRoundTrip()
    {
        var credentials = List.of(
                        new Credential("12345", "token1", "Primary"),
                        new Credential("67890", "tok;en|2", "Taxable \\ Main"));

        var serialized = IBFlexConfigurationDialog.serialize(credentials);
        var deserialized = IBFlexConfigurationDialog.deserialize(serialized);

        assertThat(deserialized, hasSize(2));
        assertThat(deserialized.get(0).queryId(), is("12345"));
        assertThat(deserialized.get(0).token(), is("token1"));
        assertThat(deserialized.get(0).name(), is("Primary"));
        assertThat(deserialized.get(1).queryId(), is("67890"));
        assertThat(deserialized.get(1).token(), is("tok;en|2"));
        assertThat(deserialized.get(1).name(), is("Taxable \\ Main"));
    }

    @Test
    public void testSerializeTrimsValues()
    {
        var credentials = List.of(new Credential("  query1  ", "  token1  ", "  Primary  "));
        IBFlexConfigurationDialog.setCredentials(store, credentials);

        var loaded = IBFlexConfigurationDialog.getCredentials(store);
        assertThat(loaded, hasSize(1));
        assertThat(loaded.get(0).queryId(), is("query1"));
        assertThat(loaded.get(0).token(), is("token1"));
        assertThat(loaded.get(0).name(), is("Primary"));
    }

    @Test
    public void testGetLastImportDateReturnsNullWhenNotSet()
    {
        assertThat(IBFlexConfigurationDialog.getLastImportDate(client, "12345"), is(nullValue()));
    }

    @Test
    public void testLastImportDateRoundTripAndRemoval()
    {
        LocalDateTime date = LocalDateTime.of(2024, 6, 15, 14, 30, 45);

        IBFlexConfigurationDialog.setLastImportDate(client, "12345", date);

        assertThat(IBFlexConfigurationDialog.getLastImportDate(client, "12345"), is(date));

        IBFlexConfigurationDialog.setLastImportDate(client, "12345", null);

        assertThat(IBFlexConfigurationDialog.getLastImportDate(client, "12345"), is(nullValue()));
    }

    @Test
    public void testMultipleLastImportDatesAreTrackedPerQueryId()
    {
        var firstDate = LocalDateTime.of(2024, 6, 15, 14, 30, 45);
        var secondDate = LocalDateTime.of(2025, 1, 2, 3, 4, 5);

        IBFlexConfigurationDialog.setLastImportDate(client, "22222", secondDate);
        IBFlexConfigurationDialog.setLastImportDate(client, "11111", firstDate);

        assertThat(IBFlexConfigurationDialog.getLastImportDate(client, "11111"), is(firstDate));
        assertThat(IBFlexConfigurationDialog.getLastImportDate(client, "22222"), is(secondDate));
        assertThat(IBFlexConfigurationDialog.getLastImportDates(client).stream().map(Cutoff::queryId)
                        .collect(Collectors.toList()), contains("11111", "22222"));
    }

    @Test
    public void testClearingOneQueryIdLeavesOtherCutoffsUntouched()
    {
        var firstDate = LocalDateTime.of(2024, 6, 15, 14, 30, 45);
        var secondDate = LocalDateTime.of(2025, 1, 2, 3, 4, 5);

        IBFlexConfigurationDialog.setLastImportDate(client, "11111", firstDate);
        IBFlexConfigurationDialog.setLastImportDate(client, "22222", secondDate);

        IBFlexConfigurationDialog.clearLastImportDate(client, "11111");

        assertThat(IBFlexConfigurationDialog.getLastImportDate(client, "11111"), is(nullValue()));
        assertThat(IBFlexConfigurationDialog.getLastImportDate(client, "22222"), is(secondDate));
        assertThat(IBFlexConfigurationDialog.getLastImportDates(client).stream().map(Cutoff::queryId)
                        .collect(Collectors.toList()), contains("22222"));
    }

    @Test
    public void testSetLastImportDatesStoresStableSortedList()
    {
        var cutoffs = List.of(
                        new Cutoff("33333", LocalDateTime.of(2024, 3, 3, 3, 3)),
                        new Cutoff("11111", LocalDateTime.of(2024, 1, 1, 1, 1)),
                        new Cutoff("22222", LocalDateTime.of(2024, 2, 2, 2, 2)));

        IBFlexConfigurationDialog.setLastImportDates(client, cutoffs);

        var loaded = IBFlexConfigurationDialog.getLastImportDates(client);
        assertThat(loaded.stream().map(Cutoff::queryId).collect(Collectors.toList()),
                        contains("11111", "22222", "33333"));
    }

    @Test
    public void testClearAllLastImportDatesRemovesQueryScopedCutoffs()
    {
        IBFlexConfigurationDialog.setLastImportDate(client, "11111", LocalDateTime.of(2024, 1, 1, 1, 1));
        IBFlexConfigurationDialog.setLastImportDate(client, "22222", LocalDateTime.of(2024, 2, 2, 2, 2));

        IBFlexConfigurationDialog.clearLastImportDates(client);

        assertThat(IBFlexConfigurationDialog.getLastImportDates(client), hasSize(0));
    }
}
