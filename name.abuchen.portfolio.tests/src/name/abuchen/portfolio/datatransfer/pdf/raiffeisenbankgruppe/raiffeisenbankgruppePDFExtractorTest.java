package name.abuchen.portfolio.datatransfer.pdf.raiffeisenbankgruppe;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor.BuySellEntryItem;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.datatransfer.pdf.RaiffeisenBankgruppePDFExtractor;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class raiffeisenbankgruppePDFExtractorTest
{
    @Test
    public void testWertpapierKauf01()
    {
        RaiffeisenBankgruppePDFExtractor extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000BAY0017"));
        assertThat(security.getName(), is("Bayer AG Namens-Aktien o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList()).get(0).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-05-03T13:45:18")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(107.26))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(106.94))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.32))));
    }

    @Test
    public void testWertpapierVerkauf01()
    {
        RaiffeisenBankgruppePDFExtractor extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("AT0000837307"));
        assertThat(security.getName(), is("Zumtobel Group AG Inhaber-Aktien o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList()).get(0).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-04-07T11:17:43")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(4500)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(36115.76))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(36923.18))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(696.65))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(110.77))));
    }

    @Test
    public void testWertpapierDividende01()
    {
        RaiffeisenBankgruppePDFExtractor extractor = new RaiffeisenBankgruppePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000BAY0017"));
        assertThat(security.getName(), is("Bayer AG Namens-Aktien o.N."));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-04-28T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(90)));
        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(110.02))));
        assertThat(transaction.getGrossValue(), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(180.00))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(47.48 + 22.50))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), 
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }
}
