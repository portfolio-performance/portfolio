package name.abuchen.portfolio.ui.handlers.tools;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.Test;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.SaveFlag;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerAccountOnlyTransactionCreator;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerDividendTransactionCreator;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class BackfillExDatesHandlerTest
{
    private static final LocalDateTime DATE_TIME = LocalDateTime.of(2026, 6, 7, 8, 9);
    private static final LocalDate BACKFILLED_DATE = LocalDate.of(2026, 6, 1);
    private static final LocalDateTime BACKFILLED_EX_DATE = BACKFILLED_DATE.atStartOfDay();
    private static final Instant UPDATED_AT = Instant.parse("2026-06-07T08:09:00Z");

    @Test
    public void testLegacyDividendBackfillUsesLegacySetter()
    {
        var fixture = fixture();
        var transaction = createLegacyDividend(fixture);
        var dirtyEvents = trackDirtyEvents(fixture.client());

        BackfillExDatesHandler.applyExDates(fixture.client(), List.of(match(fixture.account(), transaction)));

        assertThat(transaction.getExDate(), is(BACKFILLED_EX_DATE));
        assertThat(dirtyEvents.size(), is(1));
    }

    @Test
    public void testLedgerBackedDividendBackfillRoutesThroughLedgerEditor() throws Exception
    {
        var fixture = fixture();
        var transaction = createLedgerDividend(fixture);
        var dirtyEvents = trackDirtyEvents(fixture.client());

        assertThat(transaction.getClass().getSimpleName(), is("LedgerBackedAccountTransaction"));
        assertThat(transaction.getExDate(), nullValue());
        assertThrows(UnsupportedOperationException.class, () -> transaction.setExDate(BACKFILLED_EX_DATE));

        BackfillExDatesHandler.applyExDates(fixture.client(), List.of(match(fixture.account(), transaction)));

        assertThat(transaction.getExDate(), is(BACKFILLED_EX_DATE));
        assertThat(ledgerPostingExDate(transaction), is(BACKFILLED_EX_DATE));
        assertThat(fixture.account().getTransactions().size(), is(1));
        assertThat(dirtyEvents.size(), is(1));
        assertLedgerStructurallyValid(fixture.client());

        var xmlLoaded = reloadXml(fixture.client());
        var xmlTransaction = xmlLoaded.getAccounts().get(0).getTransactions().get(0);
        assertThat(xmlTransaction.getExDate(), is(BACKFILLED_EX_DATE));
        assertThat(ledgerPostingExDate(xmlTransaction), is(BACKFILLED_EX_DATE));

        var protobufLoaded = reloadProtobuf(fixture.client());
        var protobufTransaction = protobufLoaded.getAccounts().get(0).getTransactions().get(0);
        assertThat(protobufTransaction.getExDate(), is(BACKFILLED_EX_DATE));
        assertThat(ledgerPostingExDate(protobufTransaction), is(BACKFILLED_EX_DATE));
    }

    @Test
    public void testMixedLegacyAndLedgerBackfillUpdatesBoth() throws Exception
    {
        var fixture = fixture();
        var legacy = createLegacyDividend(fixture);
        var ledger = createLedgerDividend(fixture);

        BackfillExDatesHandler.applyExDates(fixture.client(),
                        List.of(match(fixture.account(), legacy), match(fixture.account(), ledger)));

        assertThat(legacy.getExDate(), is(BACKFILLED_EX_DATE));
        assertThat(ledger.getExDate(), is(BACKFILLED_EX_DATE));
        assertThat(ledgerPostingExDate(ledger), is(BACKFILLED_EX_DATE));
        assertThat(fixture.account().getTransactions().size(), is(2));
    }

    @Test
    public void testPreflightFailureLeavesLegacyDividendUnchanged()
    {
        var fixture = fixture();
        var legacy = createLegacyDividend(fixture);
        var unsupportedLedgerRow = createUnsupportedLedgerAccountTransaction(fixture);
        var dirtyEvents = trackDirtyEvents(fixture.client());

        assertThrows(IllegalArgumentException.class, () -> BackfillExDatesHandler.applyExDates(fixture.client(),
                        List.of(match(fixture.account(), legacy), match(fixture.account(), unsupportedLedgerRow))));

        assertThat(legacy.getExDate(), nullValue());
        assertThat(unsupportedLedgerRow.getExDate(), nullValue());
        assertThat(dirtyEvents.size(), is(0));
    }

    private BackfillExDatesHandler.MatchedTransaction match(Account account, AccountTransaction transaction)
    {
        return new BackfillExDatesHandler.MatchedTransaction(new TransactionPair<>(account, transaction),
                        BACKFILLED_DATE);
    }

    private AccountTransaction createLegacyDividend(Fixture fixture)
    {
        var transaction = new AccountTransaction(AccountTransaction.Type.DIVIDENDS);

        transaction.setDateTime(DATE_TIME);
        transaction.setAmount(Values.Amount.factorize(123));
        transaction.setCurrencyCode(CurrencyUnit.EUR);
        transaction.setSecurity(fixture.security());
        transaction.setShares(Values.Share.factorize(5));
        transaction.setNote("legacy note");
        fixture.account().addTransaction(transaction);

        return transaction;
    }

    private AccountTransaction createLedgerDividend(Fixture fixture)
    {
        return new LedgerDividendTransactionCreator(fixture.client()).create(fixture.account(), DATE_TIME,
                        Values.Amount.factorize(123), CurrencyUnit.EUR, fixture.security(),
                        Values.Share.factorize(5), null, null, null, List.of(), "ledger note", "ledger source");
    }

    private AccountTransaction createUnsupportedLedgerAccountTransaction(Fixture fixture)
    {
        return new LedgerAccountOnlyTransactionCreator(fixture.client()).create(fixture.account(),
                        AccountTransaction.Type.FEES, DATE_TIME, Values.Amount.factorize(123), CurrencyUnit.EUR,
                        fixture.security(), List.of(), "fee note", "fee source");
    }

    private List<Object> trackDirtyEvents(Client client)
    {
        var events = new ArrayList<>();

        client.addPropertyChangeListener("dirty", event -> events.add(event));

        return events;
    }

    private LocalDateTime ledgerPostingExDate(AccountTransaction transaction) throws Exception
    {
        var entry = transaction.getClass().getMethod("getLedgerEntry").invoke(transaction);
        var postings = (List<?>) entry.getClass().getMethod("getPostings").invoke(entry);

        for (var posting : postings)
        {
            var parameters = (List<?>) posting.getClass().getMethod("getParameters").invoke(posting);
            for (var parameter : parameters)
            {
                if ("EX_DATE".equals(parameter.getClass().getMethod("getType").invoke(parameter).toString()))
                    return (LocalDateTime) parameter.getClass().getMethod("getValue").invoke(parameter);
            }
        }

        return null;
    }

    private Client reloadXml(Client client) throws Exception
    {
        var file = File.createTempFile("backfill-ex-date", ".xml");

        try
        {
            ClientFactory.save(client, file);
            return ClientFactory.load(new ByteArrayInputStream(Files.readString(file.toPath(), StandardCharsets.UTF_8)
                            .getBytes(StandardCharsets.UTF_8)));
        }
        finally
        {
            Files.deleteIfExists(file.toPath());
        }
    }

    private Client reloadProtobuf(Client client) throws Exception
    {
        var file = File.createTempFile("backfill-ex-date", ".portfolio");

        try
        {
            ClientFactory.saveAs(client, file, null, EnumSet.of(SaveFlag.BINARY, SaveFlag.COMPRESSED));
            return ClientFactory.load(file, null, new NullProgressMonitor());
        }
        finally
        {
            Files.deleteIfExists(file.toPath());
        }
    }

    private void assertLedgerStructurallyValid(Client client) throws Exception
    {
        var ledger = Client.class.getMethod("getLedger").invoke(client);
        Class<?> validator = Class.forName("name.abuchen.portfolio.model.ledger.LedgerStructuralValidator", true,
                        Client.class.getClassLoader());
        Method validate = validator.getMethod("validate", ledger.getClass());
        var result = validate.invoke(null, ledger);

        assertThat(result.getClass().getMethod("getIssues").invoke(result).toString(),
                        (Boolean) result.getClass().getMethod("isOK").invoke(result), is(true));
    }

    private Fixture fixture()
    {
        var client = new Client();
        var account = new Account("Account");
        account.setCurrencyCode(CurrencyUnit.EUR);
        account.setUpdatedAt(UPDATED_AT);

        var security = new Security("Security", CurrencyUnit.EUR);
        security.setUpdatedAt(UPDATED_AT);

        client.addAccount(account);
        client.addSecurity(security);

        return new Fixture(client, account, security);
    }

    private record Fixture(Client client, Account account, Security security)
    {
    }
}
