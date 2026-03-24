package name.abuchen.portfolio.ui.wizards.datatransfer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityEvent.DividendEvent;
import name.abuchen.portfolio.money.CurrencyUnit;

@SuppressWarnings("nls")
public class ImportControllerTest
{
    @Test
    public void testEnrichMissingExDateForDividendTransaction()
    {
        var security = new Security();
        security.addEvent(new DividendEvent(LocalDate.parse("2024-03-14"), LocalDate.parse("2024-03-18"), null, null));

        var transaction = new AccountTransaction();
        transaction.setType(AccountTransaction.Type.DIVIDENDS);
        transaction.setCurrencyCode(CurrencyUnit.EUR);
        transaction.setDateTime(LocalDateTime.parse("2024-03-19T00:00:00"));
        transaction.setSecurity(security);

        var changed = new ImportController(new Client())
                        .enrichMissingExDate(new Extractor.TransactionItem(transaction));

        assertThat(changed, is(true));
        assertThat(transaction.getExDate(), is(LocalDateTime.parse("2024-03-14T00:00:00")));
    }

    @Test
    public void testDoesNotOverwriteExistingExDate()
    {
        var security = new Security();
        security.addEvent(new DividendEvent(LocalDate.parse("2024-03-14"), LocalDate.parse("2024-03-18"), null, null));

        var transaction = new AccountTransaction();
        transaction.setType(AccountTransaction.Type.DIVIDENDS);
        transaction.setCurrencyCode(CurrencyUnit.EUR);
        transaction.setDateTime(LocalDateTime.parse("2024-03-19T00:00:00"));
        transaction.setExDate(LocalDateTime.parse("2024-03-01T00:00:00"));
        transaction.setSecurity(security);

        var changed = new ImportController(new Client())
                        .enrichMissingExDate(new Extractor.TransactionItem(transaction));

        assertThat(changed, is(false));
        assertThat(transaction.getExDate(), is(LocalDateTime.parse("2024-03-01T00:00:00")));
    }

    @Test
    public void testTransactionNotModifiedIfNoMatchingExDateFound()
    {
        Security noMatchSecurity = new Security();
        noMatchSecurity.addEvent(
                        new DividendEvent(LocalDate.parse("2024-05-01"), LocalDate.parse("2024-05-02"), null, null));

        var transaction = new AccountTransaction();
        transaction.setType(AccountTransaction.Type.DIVIDENDS);
        transaction.setCurrencyCode(CurrencyUnit.EUR);
        transaction.setDateTime(LocalDateTime.parse("2024-05-16T00:00:00"));
        transaction.setSecurity(noMatchSecurity);

        var changed = new ImportController(new Client())
                        .enrichMissingExDate(new Extractor.TransactionItem(transaction));

        new ImportController(new Client()).enrichMissingExDate(new Extractor.TransactionItem(transaction));

        assertThat(changed, is(false));
        assertThat(transaction.getExDate(), is(nullValue()));
    }
}
