package name.abuchen.portfolio.ui.handlers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.SecurityCache;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityEvent.DividendEvent;
import name.abuchen.portfolio.money.CurrencyUnit;

public class ImportPDFHandlerTest
{
    @Test
    public void testEnrichMissingExDateForDividendTransaction()
    {
        Security security = new Security("CISCO SYSTEMS INC.  SHARES REGISTERED SHARES DL-,001", "EUR");
        security.setIsin("US17275R1023");
        security.setWkn("878841");
        security.addEvent(new DividendEvent(LocalDate.parse("2024-03-14"), LocalDate.parse("2024-03-18"), null,
                        "divvydiary"));

        AccountTransaction transaction = new AccountTransaction();
        transaction.setType(AccountTransaction.Type.DIVIDENDS);
        transaction.setCurrencyCode(CurrencyUnit.EUR);
        transaction.setDateTime(LocalDateTime.parse("2024-03-19T00:00:00"));
        transaction.setSecurity(security);

        boolean changed = ImportPDFHandler.enrichMissingExDate(new Extractor.TransactionItem(transaction));

        assertThat(changed, is(true));
        assertThat(transaction.getExDate(), is(LocalDateTime.parse("2024-03-14T00:00:00")));
    }

    @Test
    public void testDoesNotOverwriteExistingExDate()
    {
        Security security = new Security();
        security.addEvent(new DividendEvent(LocalDate.parse("2024-03-14"), LocalDate.parse("2024-03-18"), null,
                        "divvydiary"));

        AccountTransaction transaction = new AccountTransaction();
        transaction.setType(AccountTransaction.Type.DIVIDENDS);
        transaction.setCurrencyCode(CurrencyUnit.EUR);
        transaction.setDateTime(LocalDateTime.parse("2024-03-19T00:00:00"));
        transaction.setExDate(LocalDateTime.parse("2024-03-01T00:00:00"));
        transaction.setSecurity(security);

        boolean changed = ImportPDFHandler.enrichMissingExDate(new Extractor.TransactionItem(transaction));

        assertThat(changed, is(false));
        assertThat(transaction.getExDate(), is(LocalDateTime.parse("2024-03-01T00:00:00")));
    }

    @Test
    public void testEnrichMissingExDatesAcrossImportResult()
    {
        Security matchingSecurity = new Security("Xtrackers MSCI World UCITS ETF 1C", "EUR");
        matchingSecurity.setIsin("IE00BJ0KDQ92");
        matchingSecurity.setWkn("A1XB5U");
        matchingSecurity.addEvent(new DividendEvent(LocalDate.parse("2024-05-10"), LocalDate.parse("2024-05-15"), null,
                        "divvydiary"));

        Security noMatchSecurity = new Security();
        noMatchSecurity.addEvent(new DividendEvent(LocalDate.parse("2024-05-01"), LocalDate.parse("2024-05-02"), null,
                        "divvydiary"));

        AccountTransaction matchingTransaction = new AccountTransaction();
        matchingTransaction.setType(AccountTransaction.Type.DIVIDENDS);
        matchingTransaction.setCurrencyCode(CurrencyUnit.EUR);
        matchingTransaction.setDateTime(LocalDateTime.parse("2024-05-16T00:00:00"));
        matchingTransaction.setSecurity(matchingSecurity);

        AccountTransaction noMatchTransaction = new AccountTransaction();
        noMatchTransaction.setType(AccountTransaction.Type.DIVIDENDS);
        noMatchTransaction.setCurrencyCode(CurrencyUnit.EUR);
        noMatchTransaction.setDateTime(LocalDateTime.parse("2024-05-16T00:00:00"));
        noMatchTransaction.setSecurity(noMatchSecurity);

        Map<Extractor, List<Extractor.Item>> result = Map.of(new DummyExtractor(),
                        List.of(new Extractor.TransactionItem(matchingTransaction),
                                        new Extractor.TransactionItem(noMatchTransaction)));

        ImportPDFHandler.enrichMissingExDates(result);

        assertThat(matchingTransaction.getExDate(), is(LocalDateTime.parse("2024-05-10T00:00:00")));
        assertThat(noMatchTransaction.getExDate(), is((LocalDateTime) null));
    }

    private static class DummyExtractor implements Extractor
    {
        @Override
        public String getLabel()
        {
            return "dummy";
        }

        @Override
        public List<Item> extract(SecurityCache securityCache, InputFile inputFile, List<Exception> errors)
        {
            throw new UnsupportedOperationException();
        }
    }
}
