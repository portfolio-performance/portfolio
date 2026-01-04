package name.abuchen.portfolio.datatransfer.pdf.questrade;

import java.time.LocalDateTime;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasNote;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasSource;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countAccountTransactions;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countAccountTransfers;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countBuySell;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countItemsWithFailureMessage;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countSecurities;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.QuestradePDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class QuestradePDFExtractorTest
{

    @Test
    public void testDeposit()
    {
        var extractor = new QuestradePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "contribution.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, "CAD");
        
        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2025-04-09"), hasAmount("CAD", 10000.00), //
                        hasSource("contribution.txt"), hasNote("Contribution"))));
    }

    @Test
    public void testBuy()
    {
        var extractor = new QuestradePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "buy.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CAD");
        
        // check security
        var security = results.stream().filter(Extractor.SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertNull(security.getIsin());
        assertNull(security.getWkn());
        assertThat(security.getTickerSymbol(), is("VEQT.TO"));
        assertThat(security.getName(), is("VANGUARD ALL-EQUITY ETF  PORTFOLIO"));
        assertThat(security.getCurrencyCode(), is("CAD"));

        // check buy sell transaction
        var transaction = (BuySellEntry) results.stream().filter(Extractor.BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(transaction.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(transaction.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2025-04-10T00:00:00")));
        assertThat(transaction.getPortfolioTransaction().getShares(), is(Values.Share.factorize(50)));
        assertThat(transaction.getSource(), is("buy.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CAD", Values.Amount.factorize(2046.50))));
        assertThat(transaction.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CAD", Values.Amount.factorize(2046.50))));
        assertThat(transaction.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(transaction.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividend()
    {
        var extractor = new QuestradePDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "dividend.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, "CAD");

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertNull(security.getIsin());
        assertNull(security.getWkn());
        assertThat(security.getTickerSymbol(), is("VEQT.TO"));
        // assertThat(security.getName(), is("VANGUARD ALL-EQUITY ETF PORTFOLIO"));
        assertNull(security.getName());
        assertThat(security.getCurrencyCode(), is("CAD"));

        // check dividends transaction
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2025-01-07T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(29)));
        assertThat(transaction.getSource(), is("dividend.txt"));
        assertThat(transaction.getNote(), is("REC 12/30/24"));

        assertThat(transaction.getMonetaryAmount(), is(Money.of("CAD", Values.Amount.factorize(20.69))));
        assertThat(transaction.getGrossValue(), is(Money.of("CAD", Values.Amount.factorize(20.69))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of("CAD", Values.Amount.factorize(0.00))));
    }

}
