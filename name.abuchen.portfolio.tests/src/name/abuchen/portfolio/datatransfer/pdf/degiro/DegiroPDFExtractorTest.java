package name.abuchen.portfolio.datatransfer.pdf.degiro;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.dividend;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.fee;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.feeRefund;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasCurrencyCode;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasExDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasFees;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasForexGrossValue;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasGrossValue;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasIsin;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasName;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasNote;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasSecurity;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasShares;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasSource;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasTaxes;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasTicker;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.interestCharge;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.purchase;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.sale;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.security;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.skippedItem;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.taxRefund;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.withFailureMessage;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countAccountTransactions;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countAccountTransfers;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countBuySell;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countItemsWithFailureMessage;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countSecurities;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countSkippedItems;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertNull;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.hamcrest.number.IsCloseTo;
import org.junit.Test;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.Extractor.BuySellEntryItem;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.pdf.DegiroPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
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
public class DegiroPDFExtractorTest
{
    @Test
    public void testKontoauszug01()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check transaction
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(1L));

        Item item = iter.next();

        // assert transaction
        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-08-02T00:00")));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Einzahlung"));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(350.00))));
    }

    @Test
    public void testKontoauszug02()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(5L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(7));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security1 = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security1.getIsin(), is("DE000C27U079"));
        assertThat(security1.getName(), is("ODX1 C11500.00 01MAR19"));
        assertThat(security1.getCurrencyCode(), is(CurrencyUnit.EUR));

        Security security2 = results.stream().filter(SecurityItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security2.getIsin(), is("DE000C25KFF5"));
        assertThat(security2.getName(), is("ODX2 P11000.00 08FEB19"));
        assertThat(security2.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check transaction
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(5L));

        Item item = iter.next();

        // assert transaction
        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-02-07T11:53")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1000.00))));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-02-01T11:44")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.01))));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-03-01T13:21")));
        assertThat(transaction.getSource(), is("Kontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Gebühr für Ausübung/Zuteilung"));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.00))));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-02-08T13:27")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.00))));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES_REFUND));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-03-05T15:37")));
        assertThat(transaction.getSource(), is("Kontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Gutschrift für die Neukundenaktion"));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(18.00))));
    }

    @Test
    public void testKontoauszug03()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(3L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(12L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(15));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security1 = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security1.getIsin(), is("DE000A0D8Q49"));
        assertThat(security1.getName(), is("IS.DJ U.S.SELEC.DIV.U.ETF"));
        assertThat(security1.getCurrencyCode(), is(CurrencyUnit.USD));

        Security security2 = results.stream().filter(SecurityItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security2.getIsin(), is("DE0002635299"));
        assertThat(security2.getName(), is("ISH.S.EU.SEL.DIV.30 U.ETF"));
        assertThat(security2.getCurrencyCode(), is(CurrencyUnit.EUR));

        Security security3 = results.stream().filter(SecurityItem.class::isInstance).skip(2).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security3.getIsin(), is("DE000A0F5UH1"));
        assertThat(security3.getName(), is("IS.S.GL.SE.D.100 U.ETF A"));
        assertThat(security3.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check transaction
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(12L));

        Item item = iter.next();

        // assert transaction
        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-08-01T11:32")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1100.00))));
        assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Einzahlung"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-07-07T11:28")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1000.00))));
        assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Einzahlung"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-07-05T11:36")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1000.00))));
        assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Einzahlung"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-06-23T12:24")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1000.00))));
        assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Einzahlung"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-05-24T12:16")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1000.00))));
        assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Einzahlung"));

        // check 1st dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(5)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-07-17T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0L)));
        assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.75))));
        assertThat(transaction.getGrossValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.75))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnit(Unit.Type.GROSS_VALUE).get().getExchangeRate().doubleValue(),
                        IsCloseTo.closeTo(0.8674531575, 0.000001));

        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.86))));

        // @formatter:off
        // 17-07-2017 00:00 ISH.S.EU.SEL.DIV.30 U.ETF DE0002635299 Dividende EUR 2,07 EUR 521,41
        // 17-07-2017 00:00 ISH.S.EU.SEL.DIV.30 U.ETF DE0002635299 Dividendensteuer EUR -0,55 EUR 519,34
        // @formatter:on
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(6)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-07-17T00:00")));
        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.07 - 0.55))));
        Unit taxUnit = transaction.getUnit(Unit.Type.TAX).orElseThrow(IllegalArgumentException::new);
        assertThat(taxUnit.getAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.55))));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(7)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-07-17T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(22.64))));

        // @formatter:off
        // 17-07-2017 00:00 IS.S.GL.SE.D.100 U.ETF A DE000A0F5UH1 Dividende EUR 0,09 EUR 497,25
        // 17-07-2017 00:00 IS.S.GL.SE.D.100 U.ETF A DE000A0F5UH1 Dividendensteuer EUR -0,02 EUR 497,16
        // @formatter:on
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(8)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-07-17T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.07))));
        taxUnit = transaction.getUnit(Unit.Type.TAX).orElseThrow(IllegalArgumentException::new);
        assertThat(taxUnit.getAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.02))));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(9)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-07-17T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.74))));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(10)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST_CHARGE));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-07-31T00:00")));
        assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Zinsen"));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.07))));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(11)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Einrichtung von Handelsmodalitäten 2017"));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-06-30T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.50))));
    }

    @Test
    public void testKontoauszug04()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-08-31T00:00")));
        assertThat(transaction.getSource(), is("Kontoauszug04.txt"));
        assertThat(transaction.getNote(), is("Einrichtung von Handelsmodalitäten 2017"));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.89))));

    }

    @Test
    public void testKontoauszug05()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug05.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(5L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(33L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(38));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .skip(1).findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-06-06T15:34")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(200.00))));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(2)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-02-22T18:40")));
        assertThat(transaction.getSource(), is("Kontoauszug05.txt"));
        assertThat(transaction.getNote(), is("SOFORT Einzahlung"));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(27.00))));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(15)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-02-22T18:40")));
        assertThat(transaction.getSource(), is("Kontoauszug05.txt"));
        assertThat(transaction.getNote(), is("SOFORT Zahlungsgebühr"));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.00))));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(20)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-07-03T11:47")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.54))));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(21)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-07-03T11:47")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.54))));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(9)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-06-14T07:55")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.30))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.06))));
        assertThat(transaction.getUnit(Unit.Type.GROSS_VALUE).get().getExchangeRate().doubleValue(),
                        IsCloseTo.closeTo(0.8913450397, 0.000001));
        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.40))));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(10)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-06-06T09:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.23))));
        assertThat(transaction.getUnit(Unit.Type.GROSS_VALUE).get().getExchangeRate().doubleValue(),
                        IsCloseTo.closeTo(0.8859750155, 0.000001));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.06))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.04))));
        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.37))));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(11)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-05-16T08:21")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.58))));
        assertThat(transaction.getUnit(Unit.Type.GROSS_VALUE).get().getExchangeRate().doubleValue(),
                        IsCloseTo.closeTo(0.8939746111, 0.000001));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.11))));
        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.77))));
    }

    @Test
    public void testKontoauszug06()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug06.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-07-25T13:06")));
        assertThat(transaction.getSource(), is("Kontoauszug06.txt"));
        assertThat(transaction.getNote(), is("Einzahlung"));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1000.00))));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(1)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-08-05T00:09")));
        assertThat(transaction.getSource(), is("Kontoauszug06.txt"));
        assertThat(transaction.getNote(), is("Auszahlung"));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1000.00))));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(2)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-07-26T13:52")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.00))));
    }

    @Test
    public void testKontoauszug07()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug07.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(3L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(47L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(13L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(50));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-08-02T16:21")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(6695.00))));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(1)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-07-01T16:17")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2000.00))));
    }

    @Test
    public void testKontoauszug08_minimal_example_two_currencies()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(
                        PDFInputFile.loadTestCase(getClass(), "Kontoauszug08_extract_two_currencies.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // @formatter:off
        // 03-08-2019 06:55 02-08-2019 Währungswechsel (Einbuchung) EUR 3,45 EUR 1.552,27
        // 03-08-2019 06:55 02-08-2019 Währungswechsel (Ausbuchung) 1,1120 USD -3,84 USD -0,00
        // 03-08-2019 06:09 02-08-2019 FOOT LOCKER INC. US3448491049 Dividende USD 1,52 USD 3,84
        // 03-08-2019 06:09 02-08-2019 FOOT LOCKER INC. US3448491049 Dividendensteuer USD -0,23 USD 2,32
        // @formatter:on
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-08-03T06:09")));
        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize((1.52 - 0.23) * 0.8992805755))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.21))));
        assertThat(transaction.getUnit(Unit.Type.GROSS_VALUE).get().getExchangeRate().doubleValue(),
                        IsCloseTo.closeTo(0.8992805755, 0.000001));
        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(1.52))));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(1)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-08-02T07:38")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.08))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnit(Unit.Type.GROSS_VALUE).get().getExchangeRate().doubleValue(),
                        IsCloseTo.closeTo(1.0938525487, 0.000001));
        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of("GBP", Values.Amount.factorize(3.73))));
    }

    @Test
    public void testKontoauszug08()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug08.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(53L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(138L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(5L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(191));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security1 = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security1.getIsin(), is("US91913Y1001"));
        assertThat(security1.getName(), is("VALERO ENERGY CORPORAT"));
        assertThat(security1.getCurrencyCode(), is(CurrencyUnit.USD));

        Security security2 = results.stream().filter(SecurityItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security2.getIsin(), is("US4103451021"));
        assertThat(security2.getName(), is("HANESBRANDS INC. COMMO"));
        assertThat(security2.getCurrencyCode(), is(CurrencyUnit.USD));

        Security security3 = results.stream().filter(SecurityItem.class::isInstance).skip(2).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security3.getIsin(), is("US8326964058"));
        assertThat(security3.getName(), is("J.M. SMUCKER COMPANY ("));
        assertThat(security3.getCurrencyCode(), is(CurrencyUnit.USD));

        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-07-05T13:44")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1000.00))));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(1)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-05-24T19:51")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(300.00))));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(2)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-03-26T13:53")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.00))));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(10)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-08-09T11:33")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.00))));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(11)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-03-25T09:58")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(800.00))));

        // @formatter:off
        // 03-08-2019 06:55 02-08-2019 Währungswechsel (Einbuchung) EUR 3,45 EUR 1.552,27
        // 03-08-2019 06:55 02-08-2019 Währungswechsel (Ausbuchung) 1,1120 USD -3,84 USD -0,00
        // [...]
        // 03-08-2019 06:09 02-08-2019 FOOT LOCKER INC. US3448491049 Dividende USD 1,52 USD 3,84
        // 03-08-2019 06:09 02-08-2019 FOOT LOCKER INC. US3448491049 Dividendensteuer USD -0,23 USD 2,32
        // @formatter:on
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(24)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-08-03T06:09")));
        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize((1.52 - 0.23) / 1.1120))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.21))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getExchangeRate().doubleValue(), IsCloseTo.closeTo(1 / 1.1120, 0.000001));
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(1.52))));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(25)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-08-02T07:38")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.08))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnit(Unit.Type.GROSS_VALUE).get().getExchangeRate().doubleValue(),
                        IsCloseTo.closeTo(1.0938525487, 0.000001));
        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of("GBP", Values.Amount.factorize(3.73))));

        // @formatter:off
        // 13-06-2019 06:46 12-06-2019 Währungswechsel (Einbuchung) EUR 2,08 EUR 263,16
        // 13-06-2019 06:46 12-06-2019 Währungswechsel (Ausbuchung) 1,1298 USD -2,36 USD -0,00
        // [...]
        // 12-06-2019 07:54 12-06-2019 3M COMPANY COMMON STOC US88579Y1010 Dividende USD 1,44 USD 1,22
        // 12-06-2019 07:54 12-06-2019 3M COMPANY COMMON STOC US88579Y1010 Dividendensteuer USD -0,22 USD -0,22
        // @formatter:on
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .collect(Collectors.toList()).get(52).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-06-12T07:54")));
        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize((1.44 - 0.22) / 1.1298))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.19))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getExchangeRate().doubleValue(), IsCloseTo.closeTo(1 / 1.1298, 0.000001));
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(1.44))));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .collect(Collectors.toList()).get(123).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.TAX_REFUND));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-05-02T08:20")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.20))));
        assertThat(transaction.getSource(), is("Kontoauszug08.txt"));
        assertThat(transaction.getNote(), is("Dividendensteuer: NL0011821202"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .collect(Collectors.toList()).get(124).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.TAXES));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-05-02T08:20")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.25))));
        assertThat(transaction.getSource(), is("Kontoauszug08.txt"));
        assertThat(transaction.getNote(), is("Dividendensteuer: NL0011821202"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .collect(Collectors.toList()).get(126).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.TAXES));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-01-28T16:45")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.30))));
        assertThat(transaction.getSource(), is("Kontoauszug08.txt"));
        assertThat(transaction.getNote(), is("Dividendensteuer: US6907321029"));

        // @formatter:off
        // 22-08-2019 11:28 20-08-2019 BAIDU INC. - AMERICAN US0567521085 ADR/GDR Weitergabegebühr USD -0,01 USD -0,01
        //
        // The document does not contain an exchange rate for this fee, therefore
        // the transaction cannot be imported and is reported to the user.
        // @formatter:on
        // check transaction without exchange rate
        TransactionItem missingExchangeRate = (TransactionItem) results.stream()
                        .filter(TransactionItem.class::isInstance) //
                        .collect(Collectors.toList()).get(127);

        assertThat(missingExchangeRate.getFailureMessage(),
                        is(Messages.MsgErrorTransactionMissingExchangeRateIfInForex));

        transaction = (AccountTransaction) missingExchangeRate.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.USD));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-08-22T11:28")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.01))));
        assertThat(transaction.getSource(), is("Kontoauszug08.txt"));
        assertThat(transaction.getNote(), is("US0567521085: ADR/GDR Weitergabegebühr"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .collect(Collectors.toList()).get(128).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-06-24T17:20")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.27))));
        assertThat(transaction.getSource(), is("Kontoauszug08.txt"));
        assertThat(transaction.getNote(), is("US47215P1066: ADR/GDR Weitergabegebühr"));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.31))));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .collect(Collectors.toList()).get(129).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-05-21T08:34")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.01))));
        assertThat(transaction.getSource(), is("Kontoauszug08.txt"));
        assertThat(transaction.getNote(), is("US01609W1027: ADR/GDR Weitergabegebühr"));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.01))));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .collect(Collectors.toList()).get(130).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES_REFUND));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-02-01T16:32")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.50))));
        assertThat(transaction.getSource(), is("Kontoauszug08.txt"));
        assertThat(transaction.getNote(),
                        is("Einrichtung von Handelsmodalitäten 2019 (New York Stock Exchange - NSY)"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .collect(Collectors.toList()).get(131).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES_REFUND));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-02-01T16:32")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.50))));
        assertThat(transaction.getSource(), is("Kontoauszug08.txt"));
        assertThat(transaction.getNote(), is("Einrichtung von Handelsmodalitäten 2019 (NASDAQ - NDQ)"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .collect(Collectors.toList()).get(132).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-02-01T13:35")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.50))));
        assertThat(transaction.getSource(), is("Kontoauszug08.txt"));
        assertThat(transaction.getNote(),
                        is("Einrichtung von Handelsmodalitäten 2019 (New York Stock Exchange - NSY)"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .collect(Collectors.toList()).get(133).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-02-01T13:35")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.50))));
        assertThat(transaction.getSource(), is("Kontoauszug08.txt"));
        assertThat(transaction.getNote(), is("Einrichtung von Handelsmodalitäten 2019 (NASDAQ - NDQ)"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .collect(Collectors.toList()).get(134).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-02-01T13:34")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.50))));
        assertThat(transaction.getSource(), is("Kontoauszug08.txt"));
        assertThat(transaction.getNote(),
                        is("Einrichtung von Handelsmodalitäten 2019 (New York Stock Exchange - NSY)"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .collect(Collectors.toList()).get(135).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-02-01T13:34")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.50))));
        assertThat(transaction.getSource(), is("Kontoauszug08.txt"));
        assertThat(transaction.getNote(), is("Einrichtung von Handelsmodalitäten 2019 (NASDAQ - NDQ)"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .collect(Collectors.toList()).get(136).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-09-05T11:34")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.50))));
        assertThat(transaction.getSource(), is("Kontoauszug08.txt"));
        assertThat(transaction.getNote(),
                        is("Einrichtung von Handelsmodalitäten 2018 (New York Stock Exchange - NSY)"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .collect(Collectors.toList()).get(137).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-09-05T11:34")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.50))));
        assertThat(transaction.getSource(), is("Kontoauszug08.txt"));
        assertThat(transaction.getNote(), is("Einrichtung von Handelsmodalitäten 2018 (NASDAQ - NDQ)"));

        // check 1st cancellation (Storno) transaction
        TransactionItem cancellation = (TransactionItem) results.stream() //
                        .filter(i -> i.isFailure()) //
                        .filter(TransactionItem.class::isInstance) //
                        .findFirst().orElseThrow(IllegalArgumentException::new);

        assertThat(cancellation.getFailureMessage(), is(Messages.MsgErrorTransactionOrderCancellationUnsupported));
        assertThat(cancellation.getSource(), is("Kontoauszug08.txt"));

        // check 2nd cancellation (Storno) transaction
        cancellation = (TransactionItem) results.stream() //
                        .filter(i -> i.isFailure()) //
                        .filter(TransactionItem.class::isInstance) //
                        .skip(1).findFirst().orElseThrow(IllegalArgumentException::new);

        assertThat(cancellation.getFailureMessage(), is(Messages.MsgErrorTransactionOrderCancellationUnsupported));
        assertThat(cancellation.getSource(), is("Kontoauszug08.txt"));
    }

    @Test
    public void testKontoauszug09()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug09.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(4L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(4L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(8));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US20030N1019"));
        assertThat(security.getName(), is("COMCAST CORPORATION -"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // @formatter:off
        // 24-10-2019 07:16 23-10-2019 Währungswechsel (Einbuchung) EUR 1,29 EUR 691,18
        // 24-10-2019 07:16 23-10-2019 Währungswechsel (Ausbuchung) 1,1141 USD -1,44 USD 0,00
        // 24-10-2019 06:13 23-10-2019 COMCAST CORPORATION - US20030N1019 Dividende USD 0,42 USD 1,44
        // 24-10-2019 06:13 23-10-2019 COMCAST CORPORATION - US20030N1019 Dividendensteuer USD -0,06 USD 1,02
        // @formatter:on
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-10-24T06:13")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.33))));
        assertThat(transaction.getGrossValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.42 / 1.1141))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.05))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getExchangeRate().doubleValue(), IsCloseTo.closeTo(1 / 1.1141, 0.000001));
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.42))));

        // @formatter:off
        // 24-10-2019 07:16 23-10-2019 Währungswechsel (Einbuchung) EUR 1,29 EUR 691,18
        // 24-10-2019 07:16 23-10-2019 Währungswechsel (Ausbuchung) 1,1141 USD -1,44 USD 0,00
        // ...
        // 23-10-2019 07:46 23-10-2019 CISCO SYSTEMS INC. - US17275R1023 Dividende USD 1,40 USD 1,19
        // 23-10-2019 07:46 23-10-2019 CISCO SYSTEMS INC. - US17275R1023 Dividendensteuer USD -0,21 USD -0,21
        // @formatter:on
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(1)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-10-23T07:46")));
        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize((1.4 - 0.21) / 1.1141))));
        assertThat(transaction.getGrossValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.4 / 1.1141))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.19))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getExchangeRate().doubleValue(), IsCloseTo.closeTo(1 / 1.1141, 0.000001));
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(1.4))));

        // @formatter:off
        // 17-10-2019 07:09 16-10-2019 Währungswechsel (Einbuchung) EUR 2,80 EUR 506,43
        // 17-10-2019 07:09 16-10-2019 Währungswechsel (Ausbuchung) 1,1083 USD -3,11 USD 0,00
        // 17-10-2019 06:25 16-10-2019 LAM RESEARCH CORPORATI US5128071082 Dividende USD 2,30 USD 3,11
        // 17-10-2019 06:25 16-10-2019 LAM RESEARCH CORPORATI US5128071082 Dividendensteuer USD -0,35 USD 0,81
        // @formatter:on
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(2)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-10-17T06:25")));
        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize((2.3 - 0.35) / 1.1083))));
        assertThat(transaction.getGrossValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.3 / 1.1083))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.32))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getExchangeRate().doubleValue(), IsCloseTo.closeTo(1 / 1.1083, 0.000001));
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(2.3))));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(3)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-10-23T14:59")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.10))));
        assertThat(transaction.getSource(), is("Kontoauszug09.txt"));
        assertThat(transaction.getNote(), is("US9485961018: ADR/GDR Weitergabegebühr"));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.11))));
    }

    @Test
    public void testKontoauszug10()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug10.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(12L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(19L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(31));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        Security security1 = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security1.getIsin(), is("US3755581036"));
        assertThat(security1.getName(), is("GILEAD SCIENCES INC."));
        assertThat(security1.getCurrencyCode(), is(CurrencyUnit.USD));

        Security security2 = results.stream().filter(SecurityItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security2.getIsin(), is("IE00B14X4T88"));
        assertThat(security2.getName(), is("ISHS-ASIA PAC.DIV.DL D"));
        assertThat(security2.getCurrencyCode(), is(CurrencyUnit.USD));

        Security security3 = results.stream().filter(SecurityItem.class::isInstance).skip(2).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security3.getIsin(), is("US5894001008"));
        assertThat(security3.getName(), is("MERCURY GENERAL CORPOR"));
        assertThat(security3.getCurrencyCode(), is(CurrencyUnit.USD));

        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-10-28T08:26")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3500.00))));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(1)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-10-28T07:49")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(7000.00))));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(13)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-11-28T04:09")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.22))));
        assertThat(transaction.getGrossValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.27))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.05))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnit(Unit.Type.GROSS_VALUE).get().getExchangeRate().doubleValue(),
                        IsCloseTo.closeTo(0.9082652134, 0.000001));
        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.30))));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(14)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-11-23T05:32")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.46))));
        assertThat(transaction.getGrossValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.54))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.08))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnit(Unit.Type.GROSS_VALUE).get().getExchangeRate().doubleValue(),
                        IsCloseTo.closeTo(0.9064539521, 0.000001));
        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.60))));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(16)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-10-02T16:39")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.00))));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(17)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-11-01T09:35")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.50))));
    }

    @Test
    public void testKontoauszug11()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug11.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(11L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(23L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(3L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(34));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security1 = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security1.getIsin(), is("GB00B03MLX29"));
        assertThat(security1.getName(), is("ROYAL DUTCH SHELL PLC"));
        assertThat(security1.getCurrencyCode(), is(CurrencyUnit.EUR));

        Security security2 = results.stream().filter(SecurityItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security2.getIsin(), is("US1912161007"));
        assertThat(security2.getName(), is("COCA-COLA COMPANY (THE"));
        assertThat(security2.getCurrencyCode(), is(CurrencyUnit.USD));

        // @formatter:off
        // check deposit transaction
        // 26-10-2020 15:00 26-10-2020 flatex Einzahlung EUR 500,00 EUR 512,88
        // @formatter:on
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-10-26T15:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(500.00))));

        // @formatter:off
        // check dividends transaction EUR --> EUR
        // 16-12-2020 11:33 16-12-2020 ROYAL DUTCH SHELL PLC GB00B03MLX29 Dividende EUR 1,52 EUR 651,96
        // 16-12-2020 11:33 16-12-2020 ROYAL DUTCH SHELL PLC GB00B03MLX29 Dividendensteuer EUR -0,23 EUR 650,44
        // @formatter:on
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(1)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-12-16T11:33")));
        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.52 - 0.23))));
        assertThat(transaction.getGrossValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.52))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.23))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        // @formatter:off
        // check dividends transaction USD --> EUR
        // 17-12-2020 08:06 16-12-2020 Währungswechsel (Ausbuchung) EUR 5,87 EUR 657,83
        // 17-12-2020 08:06 16-12-2020 Währungswechsel (Ausbuchung) 1,2212 USD -7,18 USD 0,00
        // [...]
        // 16-12-2020 08:38 15-12-2020 COCA-COLA COMPANY (THE US1912161007 Dividende USD 1,23 USD 7,18
        // 16-12-2020 08:38 15-12-2020 COCA-COLA COMPANY (THE US1912161007 Dividendensteuer USD -0,37 USD 5,95
        // @formatter:on
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(2)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-12-16T08:38")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.71))));
        assertThat(transaction.getGrossValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.23 / 1.2212))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.30))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(1.23))));
        assertThat(grossValueUnit.getExchangeRate().doubleValue(), IsCloseTo.closeTo(1 / 1.2212, 0.000001));
    }

    @Test
    public void testKontoauszug12()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug12.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(17L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(19));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security1 = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security1.getIsin(), is("GB00B03MLX29"));
        assertThat(security1.getName(), is("ROYAL DUTCH SHELLA"));
        assertThat(security1.getCurrencyCode(), is(CurrencyUnit.EUR));

        Security security2 = results.stream().filter(SecurityItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security2.getIsin(), is("DE0007231334"));
        assertThat(security2.getName(), is("SIXT SE"));
        assertThat(security2.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-05-28T16:50")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2000.00))));
        assertThat(transaction.getSource(), is("Kontoauszug12.txt"));
        assertThat(transaction.getNote(), is("flatex Einzahlung"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(1)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-04-26T08:50")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1000.00))));
        assertThat(transaction.getSource(), is("Kontoauszug12.txt"));
        assertThat(transaction.getNote(), is("flatex Einzahlung"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(2)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-10-08T11:25")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(500.00))));
        assertThat(transaction.getSource(), is("Kontoauszug12.txt"));
        assertThat(transaction.getNote(), is("Einzahlung"));

        // @formatter:off
        // 29-03-2021 09:06 29-03-2021 ROYAL DUTCH SHELLA GB00B03MLX29 Dividende EUR 2,79 EUR 118,19
        // 29-03-2021 09:06 29-03-2021 ROYAL DUTCH SHELLA GB00B03MLX29 Dividendensteuer EUR -0,42 EUR 115,40
        // @formatter:on
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(3)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-03-29T09:06")));
        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.79 - 0.42))));
        assertThat(transaction.getGrossValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.79))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.42))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        // @formatter:off
        // 16-12-2020 11:33 16-12-2020 ROYAL DUTCH SHELLA GB00B03MLX29 Dividende EUR 2,77 EUR 117,19
        // 16-12-2020 11:33 16-12-2020 ROYAL DUTCH SHELLA GB00B03MLX29 Dividendensteuer EUR -0,41 EUR 114,42
        // @formatter:on
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(4)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-12-16T11:33")));
        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.77 - 0.41))));
        assertThat(transaction.getGrossValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.77))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.41))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        // @formatter:off
        // 21-09-2020 16:54 21-09-2020 ROYAL DUTCH SHELLA GB00B03MLX29 Dividende EUR 2,71 EUR 114,96
        // 21-09-2020 16:54 21-09-2020 ROYAL DUTCH SHELLA GB00B03MLX29 Dividendensteuer EUR -0,41 EUR 112,25
        // @formatter:on
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(5)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-09-21T16:54")));
        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.71 - 0.41))));
        assertThat(transaction.getGrossValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.71))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.41))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        // @formatter:off
        // 30-06-2020 06:25 29-06-2020 SIXT SE DE0007231334 Dividende EUR 0,05 EUR 112,81
        // 30-06-2020 06:25 29-06-2020 SIXT SE DE0007231334 Dividendensteuer EUR -0,01 EUR 112,76
        // @formatter:on
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(6)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-06-30T06:25")));
        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.05 - 0.01))));
        assertThat(transaction.getGrossValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.05))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.01))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        // @formatter:off
        // 24-06-2020 11:42 22-06-2020 ROYAL DUTCH SHELLA GB00B03MLX29 Dividende EUR 2,84 EUR 112,77
        // 24-06-2020 11:42 22-06-2020 ROYAL DUTCH SHELLA GB00B03MLX29 Dividendensteuer EUR -0,43 EUR 109,93
        // @formatter:on
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(7)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-06-24T11:42")));
        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.84 - 0.43))));
        assertThat(transaction.getGrossValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.84))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.43))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(8)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST_CHARGE));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-04-01T21:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.15))));
        assertThat(transaction.getSource(), is("Kontoauszug12.txt"));
        assertThat(transaction.getNote(), is("Flatex Interest"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(9)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST_CHARGE));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-12-31T03:42")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.05))));
        assertThat(transaction.getSource(), is("Kontoauszug12.txt"));
        assertThat(transaction.getNote(), is("Flatex Interest"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(10)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-05-03T17:14")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.12))));
        assertThat(transaction.getSource(), is("Kontoauszug12.txt"));
        assertThat(transaction.getNote(), is("Einrichtung von Handelsmodalitäten 2021 (Euronext Amsterdam - EAM)"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(11)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-04-01T12:03")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.06))));
        assertThat(transaction.getSource(), is("Kontoauszug12.txt"));
        assertThat(transaction.getNote(), is("Einrichtung von Handelsmodalitäten 2021 (Euronext Amsterdam - EAM)"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(12)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-03-01T11:21")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.02))));
        assertThat(transaction.getSource(), is("Kontoauszug12.txt"));
        assertThat(transaction.getNote(), is("Einrichtung von Handelsmodalitäten 2021 (Euronext Amsterdam - EAM)"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(13)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-01-31T13:16")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.30))));
        assertThat(transaction.getSource(), is("Kontoauszug12.txt"));
        assertThat(transaction.getNote(), is("Einrichtung von Handelsmodalitäten 2021 (Euronext Amsterdam - EAM)"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(14)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-08-05T18:46")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.01))));
        assertThat(transaction.getSource(), is("Kontoauszug12.txt"));
        assertThat(transaction.getNote(), is("Einrichtung von Handelsmodalitäten 2020 (Euronext Amsterdam - EAM)"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(15)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-04-01T11:20")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.26))));
        assertThat(transaction.getSource(), is("Kontoauszug12.txt"));
        assertThat(transaction.getNote(), is("Einrichtung von Handelsmodalitäten 2020 (Euronext Amsterdam - EAM)"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(16)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES_REFUND));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-08-08T15:59")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.02))));
        assertThat(transaction.getSource(), is("Kontoauszug12.txt"));
        assertThat(transaction.getNote(), is("Gutschrift für die Neukundenaktion"));
    }

    @Test
    public void testKontoauszug13()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug13.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(4L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, "CHF");

        List<Item> transactionList = results.stream().filter(TransactionItem.class::isInstance)
                        .collect(Collectors.toList());

        AccountTransaction transaction = (AccountTransaction) transactionList.get(0).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-11-26T09:01")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(569.00))));
        assertThat(transaction.getSource(), is("Kontoauszug13.txt"));
        assertThat(transaction.getNote(), is("Einzahlung"));

        transaction = (AccountTransaction) transactionList.get(1).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-10-26T09:26")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(569.00))));
        assertThat(transaction.getSource(), is("Kontoauszug13.txt"));
        assertThat(transaction.getNote(), is("Einzahlung"));

        // @formatter:off
        // 17-11-2021 07:38 16-11-2021 Währungswechsel (Einbuchung) CHF 2.02 CHF 1'185.08
        // 17-11-2021 07:38 16-11-2021 Währungswechsel (Ausbuchung) 1.0760 USD -2.18 USD 0.00
        // [...]
        // 16-11-2021 09:52 15-11-2021 ACCENTURE PLC. CLASS A IE00B4BNMY34 Dividende USD 2.91 USD 2.18
        // 16-11-2021 09:52 15-11-2021 ACCENTURE PLC. CLASS A IE00B4BNMY34 Dividendensteuer USD -0.73 USD -0.73
        // @formatter:on
        transaction = (AccountTransaction) transactionList.get(2).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-11-16T09:52")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(2.02))));
        assertThat(transaction.getGrossValue(), is(Money.of("CHF", Values.Amount.factorize(2.91 / 1.076))));
        assertThat(transaction.getSource(), is("Kontoauszug13.txt"));

        // @formatter:off
        // 13-11-2021 07:45 12-11-2021 Währungswechsel (Einbuchung) CHF 1.54 CHF 1'183.06
        // 13-11-2021 07:45 12-11-2021 Währungswechsel (Ausbuchung) 1.0866 USD -1.68 USD 0.00
        // 12-11-2021 07:31 11-11-2021 APPLE INC US0378331005 Dividende USD 1.98 USD 1.68
        // 12-11-2021 07:31 11-11-2021 APPLE INC US0378331005 Dividendensteuer USD -0.30 USD -0.30
        // @formatter:on
        transaction = (AccountTransaction) transactionList.get(3).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-11-12T07:31")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(1.54))));
        assertThat(transaction.getGrossValue(), is(Money.of("CHF", Values.Amount.factorize(1.98 / 1.0866))));
        assertThat(transaction.getSource(), is("Kontoauszug13.txt"));
    }

    @Test
    public void testKontoauszug14()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug14.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(8L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(12L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(20));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security1 = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security1.getIsin(), is("DE000BASF111"));
        assertThat(security1.getName(), is("BASF SE"));
        assertThat(security1.getCurrencyCode(), is(CurrencyUnit.EUR));

        Security security2 = results.stream().filter(SecurityItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security2.getIsin(), is("AT0000644505"));
        assertThat(security2.getName(), is("LENZING AG"));
        assertThat(security2.getCurrencyCode(), is(CurrencyUnit.EUR));

        Security security3 = results.stream().filter(SecurityItem.class::isInstance).skip(2).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security3.getIsin(), is("SE0006422390"));
        assertThat(security3.getName(), is("THULE GROUP"));
        assertThat(security3.getCurrencyCode(), is("SEK"));

        Security security4 = results.stream().filter(SecurityItem.class::isInstance).skip(3).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security4.getIsin(), is("US68629Y1038"));
        assertThat(security4.getName(), is("ORION OFFICE REIT INC. COMMON STOCK"));
        assertThat(security4.getCurrencyCode(), is(CurrencyUnit.USD));

        Security security5 = results.stream().filter(SecurityItem.class::isInstance).skip(4).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security5.getIsin(), is("US7561091049"));
        assertThat(security5.getName(), is("REALTY INCOME CORPORAT"));
        assertThat(security5.getCurrencyCode(), is(CurrencyUnit.USD));

        Security security6 = results.stream().filter(SecurityItem.class::isInstance).skip(5).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security6.getIsin(), is("US8621211007"));
        assertThat(security6.getName(), is("STORE CAPITAL CORPORAT"));
        assertThat(security6.getCurrencyCode(), is(CurrencyUnit.USD));

        Security security7 = results.stream().filter(SecurityItem.class::isInstance).skip(6).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security7.getIsin(), is("US7181721090"));
        assertThat(security7.getName(), is("PHILIP MORRIS INTERNAT"));
        assertThat(security7.getCurrencyCode(), is(CurrencyUnit.USD));

        // check transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-05-04T10:50")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1692.01))));
        assertThat(transaction.getSource(), is("Kontoauszug14.txt"));
        assertThat(transaction.getNote(), is("flatex Einzahlung"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(1)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-05-02T10:50")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1000.00))));
        assertThat(transaction.getSource(), is("Kontoauszug14.txt"));
        assertThat(transaction.getNote(), is("flatex Einzahlung"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(2)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-04-25T10:50")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1000.00))));
        assertThat(transaction.getSource(), is("Kontoauszug14.txt"));
        assertThat(transaction.getNote(), is("flatex Einzahlung"));

        // check 1st dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(3)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-05-05T10:54")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0L)));
        assertThat(transaction.getSource(), is("Kontoauszug14.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(97.63))));
        assertThat(transaction.getGrossValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(132.60))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(34.97))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        // check 2nd dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(4)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-05-04T08:37")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0L)));
        assertThat(transaction.getSource(), is("Kontoauszug14.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(116.69))));
        assertThat(transaction.getGrossValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(160.95))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(44.26))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        // check 3rd dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(5)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-05-04T07:41")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0L)));
        assertThat(transaction.getSource(), is("Kontoauszug14.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(13.15))));
        assertThat(transaction.getGrossValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(18.79))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5.64))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of("SEK", Values.Amount.factorize(195.00))));

        // check 4th dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(6)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-04-22T09:42")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0L)));
        assertThat(transaction.getSource(), is("Kontoauszug14.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.31))));
        assertThat(transaction.getGrossValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.37))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.06))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.40))));

        // check 5th dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(7)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-04-21T11:20")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0L)));
        assertThat(transaction.getSource(), is("Kontoauszug14.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(8.70))));
        assertThat(transaction.getGrossValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10.24))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.54))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(11.12))));

        // check 6th dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(8)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-04-21T10:35")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0L)));
        assertThat(transaction.getSource(), is("Kontoauszug14.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(31.94))));
        assertThat(transaction.getGrossValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(37.58))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5.64))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(40.81))));

        // check 7th dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(9)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-04-14T08:53")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0L)));
        assertThat(transaction.getSource(), is("Kontoauszug14.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(24.00))));
        assertThat(transaction.getGrossValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(24.18))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.18))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(26.25))));

        // check 1st fee refund transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(10)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES_REFUND));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-04-29T17:34")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(6.77))));
        assertThat(transaction.getSource(), is("Kontoauszug14.txt"));
        assertThat(transaction.getNote(), is("US3682872078: ADR/GDR Weitergabegebühr"));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(7.12))));

        // check 2nd fee refund transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(11)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES_REFUND));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-04-29T17:30")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.57))));
        assertThat(transaction.getSource(), is("Kontoauszug14.txt"));
        assertThat(transaction.getNote(), is("US3682872078: ADR/GDR Weitergabegebühr"));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(4.81))));
    }

    @Test
    public void testKontoauszug15()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug15.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(25L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(30L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(55));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security1 = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security1.getIsin(), is("US30231G1022"));
        assertThat(security1.getName(), is("EXXON MOBIL CORPORATIO"));
        assertThat(security1.getCurrencyCode(), is(CurrencyUnit.USD));

        Security security2 = results.stream().filter(SecurityItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security2.getIsin(), is("US5949181045"));
        assertThat(security2.getName(), is("MICROSOFT CORPORATION"));
        assertThat(security2.getCurrencyCode(), is(CurrencyUnit.USD));

        Security security3 = results.stream().filter(SecurityItem.class::isInstance).skip(2).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security3.getIsin(), is("NL0000009538"));
        assertThat(security3.getName(), is("PHILIPS KON"));
        assertThat(security3.getCurrencyCode(), is(CurrencyUnit.EUR));

        Security security4 = results.stream().filter(SecurityItem.class::isInstance).skip(3).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security4.getIsin(), is("DE000A1ML7J1"));
        assertThat(security4.getName(), is("VONOVIA SE"));
        assertThat(security4.getCurrencyCode(), is(CurrencyUnit.EUR));

        Security security5 = results.stream().filter(SecurityItem.class::isInstance).skip(4).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security5.getIsin(), is("US4781601046"));
        assertThat(security5.getName(), is("JOHNSON & JOHNSON COMM"));
        assertThat(security5.getCurrencyCode(), is(CurrencyUnit.USD));

        Security security6 = results.stream().filter(SecurityItem.class::isInstance).skip(5).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security6.getIsin(), is("US4581401001"));
        assertThat(security6.getName(), is("INTEL CORPORATION - CO"));
        assertThat(security6.getCurrencyCode(), is(CurrencyUnit.USD));

        Security security7 = results.stream().filter(SecurityItem.class::isInstance).skip(6).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security7.getIsin(), is("DE0007164600"));
        assertThat(security7.getName(), is("SAP SE"));
        assertThat(security7.getCurrencyCode(), is(CurrencyUnit.EUR));

        Security security8 = results.stream().filter(SecurityItem.class::isInstance).skip(7).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security8.getIsin(), is("DE0007664039"));
        assertThat(security8.getName(), is("VOLKSWAGEN AG"));
        assertThat(security8.getCurrencyCode(), is(CurrencyUnit.EUR));

        Security security9 = results.stream().filter(SecurityItem.class::isInstance).skip(8).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security9.getIsin(), is("DE0006047004"));
        assertThat(security9.getName(), is("HEIDELBERGCEMENT AG"));
        assertThat(security9.getCurrencyCode(), is(CurrencyUnit.EUR));

        Security security10 = results.stream().filter(SecurityItem.class::isInstance).skip(9).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security10.getIsin(), is("US7427181091"));
        assertThat(security10.getName(), is("PROCTER & GAMBLE COMPA"));
        assertThat(security10.getCurrencyCode(), is(CurrencyUnit.USD));

        Security security11 = results.stream().filter(SecurityItem.class::isInstance).skip(10).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security11.getIsin(), is("FR0000120644"));
        assertThat(security11.getName(), is("DANONE"));
        assertThat(security11.getCurrencyCode(), is(CurrencyUnit.EUR));

        Security security12 = results.stream().filter(SecurityItem.class::isInstance).skip(11).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security12.getIsin(), is("US0378331005"));
        assertThat(security12.getName(), is("APPLE INC. - COMMON ST"));
        assertThat(security12.getCurrencyCode(), is(CurrencyUnit.USD));

        Security security13 = results.stream().filter(SecurityItem.class::isInstance).skip(12).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security13.getIsin(), is("DE000BASF111"));
        assertThat(security13.getName(), is("BASF SE"));
        assertThat(security13.getCurrencyCode(), is(CurrencyUnit.EUR));

        Security security14 = results.stream().filter(SecurityItem.class::isInstance).skip(13).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security14.getIsin(), is("DE0007100000"));
        assertThat(security14.getName(), is("MERCEDES-BENZ GROUP AG"));
        assertThat(security14.getCurrencyCode(), is(CurrencyUnit.EUR));

        Security security15 = results.stream().filter(SecurityItem.class::isInstance).skip(14).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security15.getIsin(), is("DE000BAY0017"));
        assertThat(security15.getName(), is("BAYER AG"));
        assertThat(security15.getCurrencyCode(), is(CurrencyUnit.EUR));

        Security security16 = results.stream().filter(SecurityItem.class::isInstance).skip(15).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security16.getIsin(), is("DE0008430026"));
        assertThat(security16.getName(), is("MUENCHENER RUECKVERSICHERUNGS"));
        assertThat(security16.getCurrencyCode(), is(CurrencyUnit.EUR));

        Security security17 = results.stream().filter(SecurityItem.class::isInstance).skip(16).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security17.getIsin(), is("US2358511028"));
        assertThat(security17.getName(), is("DANAHER CORPORATION CO"));
        assertThat(security17.getCurrencyCode(), is(CurrencyUnit.USD));

        Security security18 = results.stream().filter(SecurityItem.class::isInstance).skip(17).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security18.getIsin(), is("US17275R1023"));
        assertThat(security18.getName(), is("CISCO SYSTEMS INC. -"));
        assertThat(security18.getCurrencyCode(), is(CurrencyUnit.USD));

        Security security19 = results.stream().filter(SecurityItem.class::isInstance).skip(18).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security19.getIsin(), is("DE0005200000"));
        assertThat(security19.getName(), is("BEIERSDORF AG"));
        assertThat(security19.getCurrencyCode(), is(CurrencyUnit.EUR));

        Security security20 = results.stream().filter(SecurityItem.class::isInstance).skip(19).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security20.getIsin(), is("DE0005557508"));
        assertThat(security20.getName(), is("DEUTSCHE TELEKOM AG"));
        assertThat(security20.getCurrencyCode(), is(CurrencyUnit.EUR));

        Security security21 = results.stream().filter(SecurityItem.class::isInstance).skip(20).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security21.getIsin(), is("DE0006048432"));
        assertThat(security21.getName(), is("HENKEL AG & CO. KGAA VZ"));
        assertThat(security21.getCurrencyCode(), is(CurrencyUnit.EUR));

        Security security22 = results.stream().filter(SecurityItem.class::isInstance).skip(21).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security22.getIsin(), is("US1912161007"));
        assertThat(security22.getName(), is("COCA-COLA COMPANY (THE"));
        assertThat(security22.getCurrencyCode(), is(CurrencyUnit.USD));

        Security security23 = results.stream().filter(SecurityItem.class::isInstance).skip(22).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security23.getIsin(), is("US6541061031"));
        assertThat(security23.getName(), is("NIKE INC. COMMON STOC"));
        assertThat(security23.getCurrencyCode(), is(CurrencyUnit.USD));

        Security security24 = results.stream().filter(SecurityItem.class::isInstance).skip(23).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security24.getIsin(), is("US92556H2067"));
        assertThat(security24.getName(), is("PARAMOUNT GLOBAL"));
        assertThat(security24.getCurrencyCode(), is(CurrencyUnit.USD));

        // check transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-05-20T08:50")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(600.00))));
        assertThat(transaction.getSource(), is("Kontoauszug15.txt"));
        assertThat(transaction.getNote(), is("flatex Einzahlung"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(1)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-05-03T09:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1000.00))));
        assertThat(transaction.getSource(), is("Kontoauszug15.txt"));
        assertThat(transaction.getNote(), is("flatex Einzahlung"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(2)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-05-02T08:50")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1500.00))));
        assertThat(transaction.getSource(), is("Kontoauszug15.txt"));
        assertThat(transaction.getNote(), is("flatex Einzahlung"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(3)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-04-08T08:50")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(600.00))));
        assertThat(transaction.getSource(), is("Kontoauszug15.txt"));
        assertThat(transaction.getNote(), is("flatex Einzahlung"));

        // check 1st dividende transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(4)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-06-10T09:04")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0L)));
        assertThat(transaction.getSource(), is("Kontoauszug15.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.97))));
        assertThat(transaction.getGrossValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5.84))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.87))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(6.16))));

        // check 2nd dividende transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(5)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-06-09T09:44")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0L)));
        assertThat(transaction.getSource(), is("Kontoauszug15.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.99))));
        assertThat(transaction.getGrossValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.17))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.18))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(1.24))));

        // check 3rd dividende transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(6)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-06-09T09:37")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0L)));
        assertThat(transaction.getSource(), is("Kontoauszug15.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(14.45))));
        assertThat(transaction.getGrossValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(17.00))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.55))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        // check 4th dividende transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(7)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-06-09T09:20")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0L)));
        assertThat(transaction.getSource(), is("Kontoauszug15.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(44.82))));
        assertThat(transaction.getGrossValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(44.82))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        // check 5th dividende transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(8)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-06-07T09:08")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0L)));
        assertThat(transaction.getSource(), is("Kontoauszug15.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.79))));
        assertThat(transaction.getGrossValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.11))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.32))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(2.26))));

        // check 6th dividende transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(9)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-06-01T08:23")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0L)));
        assertThat(transaction.getSource(), is("Kontoauszug15.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(6.40))));
        assertThat(transaction.getGrossValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(7.52))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.12))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(8.03))));

        // check 7th dividende transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(10)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-05-23T07:58")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0L)));
        assertThat(transaction.getSource(), is("Kontoauszug15.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(18.04))));
        assertThat(transaction.getGrossValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(24.50))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(6.46))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        // check 8th dividende transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(11)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-05-17T11:36")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0L)));
        assertThat(transaction.getSource(), is("Kontoauszug15.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(22.26))));
        assertThat(transaction.getGrossValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(30.24))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(7.98))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        // check 9th dividende transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(12)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-05-17T11:35")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0L)));
        assertThat(transaction.getSource(), is("Kontoauszug15.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3.53))));
        assertThat(transaction.getGrossValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.80))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.27))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        // check 10th dividende transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(13)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-05-17T07:40")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0L)));
        assertThat(transaction.getSource(), is("Kontoauszug15.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.20))));
        assertThat(transaction.getGrossValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.59))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.39))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(2.74))));

        // check 11th dividende transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(14)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-05-13T16:03")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0L)));
        assertThat(transaction.getSource(), is("Kontoauszug15.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.91))));
        assertThat(transaction.getGrossValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3.88))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.97))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        // check 12th dividende transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(15)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-05-12T10:34")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0L)));
        assertThat(transaction.getSource(), is("Kontoauszug15.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.37))));
        assertThat(transaction.getGrossValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.44))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.07))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.46))));

        // check 13th dividende transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(16)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-05-05T10:54")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0L)));
        assertThat(transaction.getSource(), is("Kontoauszug15.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(32.54))));
        assertThat(transaction.getGrossValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(44.20))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(11.66))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        // check 14th dividende transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(17)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-05-05T10:19")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0L)));
        assertThat(transaction.getSource(), is("Kontoauszug15.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(40.49))));
        assertThat(transaction.getGrossValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(55.00))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(14.51))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        // check 15th dividende transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(18)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-05-05T08:41")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0L)));
        assertThat(transaction.getSource(), is("Kontoauszug15.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(19.14))));
        assertThat(transaction.getGrossValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(26.00))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(6.86))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        // check 16th dividende transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(19)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-05-03T07:27")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0L)));
        assertThat(transaction.getSource(), is("Kontoauszug15.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(32.39))));
        assertThat(transaction.getGrossValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(44.00))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(11.61))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        // check 17th dividende transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(20)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-05-02T08:05")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0L)));
        assertThat(transaction.getSource(), is("Kontoauszug15.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.60))));
        assertThat(transaction.getGrossValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.71))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.11))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.75))));

        // check 18th dividende transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(21)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-04-28T08:10")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0L)));
        assertThat(transaction.getSource(), is("Kontoauszug15.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.62))));
        assertThat(transaction.getGrossValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.72))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.10))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.76))));

        // check 19th dividende transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(22)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-04-22T12:14")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0L)));
        assertThat(transaction.getSource(), is("Kontoauszug15.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3.61))));
        assertThat(transaction.getGrossValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.90))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.29))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        // check 20th dividende transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(23)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-04-13T14:02")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0L)));
        assertThat(transaction.getSource(), is("Kontoauszug15.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(16.00))));
        assertThat(transaction.getGrossValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(16.00))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        // check 21th dividende transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(24)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-04-08T11:45")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0L)));
        assertThat(transaction.getSource(), is("Kontoauszug15.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.09))));
        assertThat(transaction.getGrossValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5.55))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.46))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        // check 22th dividende transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(25)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-04-04T08:17")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0L)));
        assertThat(transaction.getSource(), is("Kontoauszug15.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.36))));
        assertThat(transaction.getGrossValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.60))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.24))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(1.76))));

        // check 23th dividende transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(26)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-04-04T07:31")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0L)));
        assertThat(transaction.getSource(), is("Kontoauszug15.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.13))));
        assertThat(transaction.getGrossValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.50))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.37))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(2.75))));

        // check 24th dividende transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(27)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-04-04T07:04")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0L)));
        assertThat(transaction.getSource(), is("Kontoauszug15.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3.34))));
        assertThat(transaction.getGrossValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3.93))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.59))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(4.32))));

        // check 1st interest charge transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(28)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST_CHARGE));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-04-02T08:10")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.88))));
        assertThat(transaction.getSource(), is("Kontoauszug15.txt"));
        assertThat(transaction.getNote(), is("Flatex Interest"));

        // check 1st fee transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(29)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-04-28T15:33")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.01))));
        assertThat(transaction.getSource(), is("Kontoauszug15.txt"));
        assertThat(transaction.getNote(), is("US47759T1007: ADR/GDR Weitergabegebühr"));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.01))));
    }

    @Test
    public void testRekeningoverzicht01()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Rekeningoverzicht01.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-08-25T08:42")));
        assertThat(transaction.getSource(), is("Rekeningoverzicht01.txt"));
        assertThat(transaction.getNote(), is("iDEAL Deposit"));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(123.00))));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(1)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-08-25T08:41")));
        assertThat(transaction.getSource(), is("Rekeningoverzicht01.txt"));
        assertThat(transaction.getNote(), is("iDEAL Deposit"));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1123.00))));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(2)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-07-27T08:43")));
        assertThat(transaction.getSource(), is("Rekeningoverzicht01.txt"));
        assertThat(transaction.getNote(), is("iDEAL Deposit"));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(123.00))));
    }

    @Test
    public void testRekeningoverzicht02()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Rekeningoverzicht02.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(4L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-08-26T08:41")));
        assertThat(transaction.getSource(), is("Rekeningoverzicht02.txt"));
        assertThat(transaction.getNote(), is("iDEAL Deposit"));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(20.00))));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(1)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-08-25T08:42")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10.00))));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(2)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-08-25T08:41")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1200.00))));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(3)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-09-02T21:29")));
        assertThat(transaction.getSource(), is("Rekeningoverzicht02.txt"));
        assertThat(transaction.getNote(), is("DEGIRO Aansluitingskosten 2021 (Borsa Italiana S.p.A. - MIL)"));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.50))));
    }

    /**
     * Test reading Dividends in USD from DeGiro AccountStatement in Dutch and
     * converting to EUR.
     */
    @Test
    public void testRekeningoverzicht03()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Rekeningoverzicht03.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-05-26T08:08")));
        assertThat(transaction.getShares(), is(0L));

        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.53))));
        assertThat(transaction.getGrossValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.53))));
        assertThat(transaction.getSecurity().getName(), is("ISHARES INFRA GLO"));
        assertThat(transaction.getSecurity().getIsin(), is("IE00B1FZS467"));
        // Parsed fxrate 1,0758 is inverted to give 0.92954
        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getExchangeRate().doubleValue(), IsCloseTo.closeTo(0.92954, 0.000001));
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.57))));
        assertNull(transaction.getCrossEntry());
    }

    /**
     * Test reading Dividend and Dividend Tax in EUR from DeGiro
     * AccountStatement in Dutch. Dividendbelasting is Dutch for Dividend Tax.
     */
    @Test
    public void testRekeningoverzicht04()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Rekeningoverzicht04.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("FR0007052782"));
        assertThat(security.getName(), is("LYXOR ETF CAC 40"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check dividend transaction including tax
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        // @formatter:off
        // 11-07-2022 07:45 08-07-2022 LYXOR ETF CAC 40 FR0007052782 Dividend EUR 1,50 EUR 23,21
        // 11-07-2022 07:45 08-07-2022 LYXOR ETF CAC 40 FR0007052782 Dividendbelasting EUR -0,38 EUR 21,71
        // @formatter:on
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-07-11T07:45")));
        assertThat(transaction.getShares(), is(0L));

        // 1.50 - 0.38 = 1.12
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.12))));
        assertThat(transaction.getGrossValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.50))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.38))));
        assertThat(transaction.getSecurity().getName(), is("LYXOR ETF CAC 40"));
        assertThat(transaction.getSecurity().getIsin(), is("FR0007052782"));
        assertNull(transaction.getCrossEntry());
    }

    /**
     * Test reading Exchange Connection Fee in EUR from DeGiro AccountStatement
     * in Dutch. These are fees that DeGiro charges for trading on foreign stock
     * exchanges.
     */
    @Test
    public void testRekeningoverzicht05()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Rekeningoverzicht05.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividend transaction including tax
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        // @formatter:off
        // 01-06-2022 19:55 31-05-2022 Giro Exchange Connection Fee 2022 EUR -1,01 EUR -6,39
        // @formatter:on
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getNote(), is("Giro Exchange Connection Fee 2022"));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-06-01T19:55")));
        assertThat(transaction.getShares(), is(0L));

        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.01))));
        assertThat(transaction.getGrossValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.01))));
        assertNull(transaction.getSecurity());
        assertNull(transaction.getCrossEntry());
    }

    /**
     * Test reading bank deposit in EUR from DeGiro AccountStatement in Dutch.
     * This is shown on the account statement as "flatex Storting".
     */
    @Test
    public void testRekeningoverzicht06()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Rekeningoverzicht06.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividend transaction including tax
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        // @formatter:off
        // 25-07-2022 10:50 25-07-2022 flatex Storting EUR 123,45 EUR 123,47
        // @formatter:on
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-07-25T10:50")));
        assertThat(transaction.getSource(), is("Rekeningoverzicht06.txt"));
        assertThat(transaction.getNote(), is("flatex Storting"));
        assertThat(transaction.getShares(), is(0L));

        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(123.45))));
        assertThat(transaction.getGrossValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(123.45))));
        assertNull(transaction.getSecurity());
        assertNull(transaction.getCrossEntry());
    }

    @Test
    public void testRekeningoverzicht07()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Rekeningoverzicht07.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(3L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(45L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(48));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        Security security1 = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security1.getIsin(), is("US9229087690"));
        assertThat(security1.getName(), is("VANGUARD TOTAL STOCK M"));
        assertThat(security1.getCurrencyCode(), is(CurrencyUnit.USD));

        Security security2 = results.stream().filter(SecurityItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security2.getIsin(), is("US9219097683"));
        assertThat(security2.getName(), is("VANGUARD TOTAL INTERNA"));
        assertThat(security2.getCurrencyCode(), is(CurrencyUnit.USD));

        Security security3 = results.stream().filter(SecurityItem.class::isInstance).skip(2).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security3.getIsin(), is("IE00B3RBWM25"));
        assertThat(security3.getName(), is("VANGUARD FTSE AW"));
        assertThat(security3.getCurrencyCode(), is(CurrencyUnit.USD));

        // check transaction
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(45L));

        Item item = iter.next();

        // assert transaction
        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-07-31T09:01")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1250.00))));
        assertThat(transaction.getSource(), is("Rekeningoverzicht07.txt"));
        assertThat(transaction.getNote(), is("iDEAL Deposit"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-07-01T18:27")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1250.00))));
        assertThat(transaction.getSource(), is("Rekeningoverzicht07.txt"));
        assertThat(transaction.getNote(), is("iDEAL Deposit"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-06-02T13:29")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1250.00))));
        assertThat(transaction.getSource(), is("Rekeningoverzicht07.txt"));
        assertThat(transaction.getNote(), is("iDEAL Deposit"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-04-29T02:40")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1250.00))));
        assertThat(transaction.getSource(), is("Rekeningoverzicht07.txt"));
        assertThat(transaction.getNote(), is("iDEAL Deposit"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-04-01T08:40")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1250.00))));
        assertThat(transaction.getSource(), is("Rekeningoverzicht07.txt"));
        assertThat(transaction.getNote(), is("iDEAL Deposit"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-03-02T07:43")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1250.00))));
        assertThat(transaction.getSource(), is("Rekeningoverzicht07.txt"));
        assertThat(transaction.getNote(), is("iDEAL Deposit"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-01-30T06:42")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(750.00))));
        assertThat(transaction.getSource(), is("Rekeningoverzicht07.txt"));
        assertThat(transaction.getNote(), is("iDEAL Deposit"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-06-30T18:20")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2900.00))));
        assertThat(transaction.getSource(), is("Rekeningoverzicht07.txt"));
        assertThat(transaction.getNote(), is("flatex terugstorting"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-09-10T02:58")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(38100.00))));
        assertThat(transaction.getSource(), is("Rekeningoverzicht07.txt"));
        assertThat(transaction.getNote(), is("flatex terugstorting"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-08-13T14:30")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(47250.00))));
        assertThat(transaction.getSource(), is("Rekeningoverzicht07.txt"));
        assertThat(transaction.getNote(), is("flatex terugstorting"));

        // check 1st dividend transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(10)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-12-29T03:53")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0)));
        assertThat(transaction.getSource(), is("Rekeningoverzicht07.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(328.78))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(469.67))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(140.89))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(499.68))));

        // check 2nd dividend transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(11)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-12-23T04:04")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0)));
        assertThat(transaction.getSource(), is("Rekeningoverzicht07.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(528.85))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(755.49))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(226.64))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(802.48))));

        // check 3rd dividend transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(12)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-09-29T05:13")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0)));
        assertThat(transaction.getSource(), is("Rekeningoverzicht07.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(306.37))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(437.68))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(131.31))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(427.18))));

        // check 4th dividend transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(13)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-09-23T05:31")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0)));
        assertThat(transaction.getSource(), is("Rekeningoverzicht07.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(253.97))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(362.83))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(108.86))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(357.79))));

        // check 5th dividend transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(14)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-06-29T04:35")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0)));
        assertThat(transaction.getSource(), is("Rekeningoverzicht07.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(266.98))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(381.41))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(114.43))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(402.27))));

        // check 6th dividend transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(15)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-06-26T06:41")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0)));
        assertThat(transaction.getSource(), is("Rekeningoverzicht07.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(497.42))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(710.59))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(213.17))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(751.95))));

        // check 7th dividend transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(16)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-03-29T07:03")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0)));
        assertThat(transaction.getSource(), is("Rekeningoverzicht07.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(239.54))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(342.21))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(102.67))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(380.30))));

        // check 8th dividend transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(17)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-03-25T06:14")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0)));
        assertThat(transaction.getSource(), is("Rekeningoverzicht07.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(81.33))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(116.20))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(34.87))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(128.11))));

        // check 9th dividend transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(18)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-12-31T08:18")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0)));
        assertThat(transaction.getSource(), is("Rekeningoverzicht07.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(343.98))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(404.69))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(60.71))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(461.39))));

        // check 10th dividend transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(19)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-12-24T05:20")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0)));
        assertThat(transaction.getSource(), is("Rekeningoverzicht07.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(898.93))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1057.56))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(158.63))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(1200.97))));

        // check 11th dividend transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(20)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-09-30T05:08")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0)));
        assertThat(transaction.getSource(), is("Rekeningoverzicht07.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(284.73))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(334.97))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(50.24))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(388.90))));

        // check 12th dividend transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(21)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-09-24T04:07")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0)));
        assertThat(transaction.getSource(), is("Rekeningoverzicht07.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(329.06))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(387.14))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(58.08))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(454.89))));

        // check 13th dividend transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(22)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-07-01T12:23")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0)));
        assertThat(transaction.getSource(), is("Rekeningoverzicht07.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(280.28))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(280.28))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(332.46))));

        // check 14th dividend transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(23)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-06-30T08:49")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0)));
        assertThat(transaction.getSource(), is("Rekeningoverzicht07.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(315.33))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(370.97))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(55.64))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(440.30))));

        // check 15th dividend transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(24)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-06-25T07:39")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0)));
        assertThat(transaction.getSource(), is("Rekeningoverzicht07.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(467.31))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(549.78))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(82.47))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(656.88))));

        // check 16th dividend transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(25)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-04-01T15:26")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0)));
        assertThat(transaction.getSource(), is("Rekeningoverzicht07.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(151.09))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(151.09))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(178.13))));

        // check 17th dividend transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(26)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-03-31T04:12")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0)));
        assertThat(transaction.getSource(), is("Rekeningoverzicht07.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(317.39))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(373.39))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(56.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(437.88))));

        // check 18th dividend transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(27)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-03-26T04:26")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0)));
        assertThat(transaction.getSource(), is("Rekeningoverzicht07.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(143.04))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(168.29))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(25.25))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(198.16))));

        // check transaction
        // get transactions
        iter = results.stream().filter(TransactionItem.class::isInstance).skip(28).iterator();
        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.TAXES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-12-16T18:26")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(9.23))));
        assertThat(transaction.getSource(), is("Rekeningoverzicht07.txt"));
        assertThat(transaction.getNote(), is("Dividendbelasting: US9219097683"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.TAXES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-12-16T18:26")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5.29))));
        assertThat(transaction.getSource(), is("Rekeningoverzicht07.txt"));
        assertThat(transaction.getNote(), is("Dividendbelasting: US9219097683"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.TAXES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-12-16T18:26")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3.95))));
        assertThat(transaction.getSource(), is("Rekeningoverzicht07.txt"));
        assertThat(transaction.getNote(), is("Dividendbelasting: US9219097683"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.TAXES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-12-16T18:26")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.65))));
        assertThat(transaction.getSource(), is("Rekeningoverzicht07.txt"));
        assertThat(transaction.getNote(), is("Dividendbelasting: US9219097683"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST_CHARGE));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-10-01T15:01")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.01))));
        assertThat(transaction.getSource(), is("Rekeningoverzicht07.txt"));
        assertThat(transaction.getNote(), is("Flatex Interest"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST_CHARGE));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-07-01T15:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.72))));
        assertThat(transaction.getSource(), is("Rekeningoverzicht07.txt"));
        assertThat(transaction.getNote(), is("Flatex Interest"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST_CHARGE));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-04-02T09:20")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.29))));
        assertThat(transaction.getSource(), is("Rekeningoverzicht07.txt"));
        assertThat(transaction.getNote(), is("Flatex Interest"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST_CHARGE));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-12-30T22:40")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.86))));
        assertThat(transaction.getSource(), is("Rekeningoverzicht07.txt"));
        assertThat(transaction.getNote(), is("Flatex Interest"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST_CHARGE));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-10-01T21:20")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3.53))));
        assertThat(transaction.getSource(), is("Rekeningoverzicht07.txt"));
        assertThat(transaction.getNote(), is("Flatex Interest"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST_CHARGE));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-07-02T05:40")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.26))));
        assertThat(transaction.getSource(), is("Rekeningoverzicht07.txt"));
        assertThat(transaction.getNote(), is("Flatex Interest"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST_CHARGE));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-04-01T21:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.29))));
        assertThat(transaction.getSource(), is("Rekeningoverzicht07.txt"));
        assertThat(transaction.getNote(), is("Flatex Interest"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-01-03T14:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.50))));
        assertThat(transaction.getSource(), is("Rekeningoverzicht07.txt"));
        assertThat(transaction.getNote(), is("DEGIRO Aansluitingskosten 2023 (NYSE Arca - NYA)"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-01-03T14:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.50))));
        assertThat(transaction.getSource(), is("Rekeningoverzicht07.txt"));
        assertThat(transaction.getNote(), is("DEGIRO Aansluitingskosten 2023 (NASDAQ - NDQ)"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-02-03T10:06")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.50))));
        assertThat(transaction.getSource(), is("Rekeningoverzicht07.txt"));
        assertThat(transaction.getNote(), is("DEGIRO Aansluitingskosten 2022 (NYSE Arca - NYA)"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-02-03T10:06")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.50))));
        assertThat(transaction.getSource(), is("Rekeningoverzicht07.txt"));
        assertThat(transaction.getNote(), is("DEGIRO Aansluitingskosten 2022 (NASDAQ - NDQ)"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-01-31T13:18")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.50))));
        assertThat(transaction.getSource(), is("Rekeningoverzicht07.txt"));
        assertThat(transaction.getNote(), is("DEGIRO Aansluitingskosten 2021 (NYSE Arca - NYA)"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-01-31T13:18")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.50))));
        assertThat(transaction.getSource(), is("Rekeningoverzicht07.txt"));
        assertThat(transaction.getNote(), is("DEGIRO Aansluitingskosten 2021 (NASDAQ - NDQ)"));
    }

    @Test
    public void testAccountStatement01()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "AccountStatement01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(4L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(5L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(9));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        Security security1 = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security1.getIsin(), is("CA56501R1064"));
        assertThat(security1.getName(), is("MANULIFE FINANCIAL COR"));
        assertThat(security1.getCurrencyCode(), is("CAD"));

        Security security2 = results.stream().filter(SecurityItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security2.getIsin(), is("US9024941034"));
        assertThat(security2.getName(), is("TYSON FOODS INC. COMM"));
        assertThat(security2.getCurrencyCode(), is(CurrencyUnit.USD));

        Security security3 = results.stream().filter(SecurityItem.class::isInstance).skip(2).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security3.getIsin(), is("US7443201022"));
        assertThat(security3.getName(), is("PRUDENTIAL FINANCIAL"));
        assertThat(security3.getCurrencyCode(), is(CurrencyUnit.USD));

        Security security4 = results.stream().filter(SecurityItem.class::isInstance).skip(3).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security4.getIsin(), is("CA29250N1050"));
        assertThat(security4.getName(), is("ENBRIDGE INC COMMON ST"));
        assertThat(security4.getCurrencyCode(), is("CAD"));

        // check 1st dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-03-22T07:39")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(9.12))));
        assertThat(transaction.getGrossValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(12.17))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3.05))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnit(Unit.Type.GROSS_VALUE).get().getExchangeRate().doubleValue(),
                        IsCloseTo.closeTo(0.6685385747, 0.000001));
        Unit grossValueUnit1 = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit1.getForex(), is(Money.of("CAD", Values.Amount.factorize(18.20))));

        // check 2nd dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(1)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-03-16T03:39")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(31.66))));
        assertThat(transaction.getGrossValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(37.26))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5.60))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnit(Unit.Type.GROSS_VALUE).get().getExchangeRate().doubleValue(),
                        IsCloseTo.closeTo(0.8373806733, 0.000001));
        Unit grossValueUnit2 = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit2.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(44.50))));

        // check 3rd dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(2)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-03-12T08:05")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(32.59))));
        assertThat(transaction.getGrossValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(38.34))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5.75))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnit(Unit.Type.GROSS_VALUE).get().getExchangeRate().doubleValue(),
                        IsCloseTo.closeTo(0.8335417188, 0.000001));
        Unit grossValueUnit3 = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit3.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(46.00))));

        // check 4th dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(3)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-03-02T14:53")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(38.09))));
        assertThat(transaction.getGrossValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(50.78))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(12.69))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnit(Unit.Type.GROSS_VALUE).get().getExchangeRate().doubleValue(),
                        IsCloseTo.closeTo(0.6539366989, 0.000001));
        Unit grossValueUnit4 = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit4.getForex(), is(Money.of("CAD", Values.Amount.factorize(77.66))));

        // check 1th interest transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(4)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST_CHARGE));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-03-01T18:47")));
        assertThat(transaction.getSource(), is("AccountStatement01.txt"));
        assertThat(transaction.getNote(), is("Interest"));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.88))));
    }

    @Test
    public void testAccountStatement02()
    {
        var extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "AccountStatement02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(73L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(3L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(74));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("PLOPTTC00011"), hasTicker(null), //
                        hasName("CD PROJEKT SA"), //
                        hasCurrencyCode("PLN"))));

        // check dividende transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2024-06-28T04:43"), hasExDate(null), //
                        hasShares(0.00), //
                        hasSource("AccountStatement02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1.87), hasGrossValue("EUR", 2.31), //
                        hasTaxes("EUR", 0.44), hasFees("EUR", 0.00), //
                        hasForexGrossValue("PLN", 10.00))));

        assertThat(results, hasItem(dividend( //
                        hasDate("2023-06-21T06:26"), hasExDate(null), //
                        hasShares(0.00), //
                        hasSource("AccountStatement02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1.82), hasGrossValue("EUR", 2.25), //
                        hasTaxes("EUR", 0.43), hasFees("EUR", 0.00), //
                        hasForexGrossValue("PLN", 10.00))));

        assertThat(results, hasItem(dividend( //
                        hasDate("2022-07-14T08:47"), hasExDate(null), //
                        hasShares(0.00), //
                        hasSource("AccountStatement02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1.68), hasGrossValue("EUR", 2.07), //
                        hasTaxes("EUR", 0.39), hasFees("EUR", 0.00), //
                        hasForexGrossValue("PLN", 10.00))));

        assertThat(results, hasItem(dividend( //
                        hasDate("2021-06-09T07:31"), hasExDate(null), //
                        hasShares(0.00), //
                        hasSource("AccountStatement02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 9.06), hasGrossValue("EUR", 11.19), //
                        hasTaxes("EUR", 2.13), hasFees("EUR", 0.00), //
                        hasForexGrossValue("PLN", 50.00))));

        // check cancellation (Storno) transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionOrderCancellationUnsupported, //
                        dividend( //
                                        hasDate("2021-07-23T15:02"), hasExDate(null), //
                                        hasShares(0.00), //
                                        hasSource("AccountStatement02.txt"), //
                                        hasNote(null), //
                                        hasAmount("PLN", 50.00), hasGrossValue("PLN", 50.00), //
                                        hasTaxes("PLN", 0.00), hasFees("PLN", 0.00)))));

        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionOrderCancellationUnsupported, //
                        taxRefund( //
                                        hasDate("2021-07-23T15:02"), //
                                        hasSource("AccountStatement02.txt"), //
                                        hasNote("Dividend Tax: PLOPTTC00011"), //
                                        hasAmount("PLN", 9.50), hasGrossValue("PLN", 9.50), //
                                        hasTaxes("PLN", 0.00), hasFees("PLN", 0.00)))));

        // check transaction without exchange rate
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionMissingExchangeRateIfInForex, //
                        dividend( //
                                        hasDate("2021-07-23T15:03"), hasExDate(null), //
                                        hasShares(0.00), //
                                        hasSource("AccountStatement02.txt"), //
                                        hasNote(null), //
                                        hasAmount("PLN", 40.50), hasGrossValue("PLN", 50.00), //
                                        hasTaxes("PLN", 9.50), hasFees("PLN", 0.00)))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2024-04-30T09:10"), hasAmount("EUR", 2550.00), //
                        hasSource("AccountStatement02.txt"), hasNote("flatex Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2024-04-08T09:10"), hasAmount("EUR", 2500.00), //
                        hasSource("AccountStatement02.txt"), hasNote("flatex Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2024-02-29T11:30"), hasAmount("EUR", 2500.00), //
                        hasSource("AccountStatement02.txt"), hasNote("flatex Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2024-01-30T11:10"), hasAmount("EUR", 2500.00), //
                        hasSource("AccountStatement02.txt"), hasNote("flatex Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2024-01-03T11:30"), hasAmount("EUR", 2500.00), //
                        hasSource("AccountStatement02.txt"), hasNote("flatex Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2023-12-08T11:10"), hasAmount("EUR", 2500.00), //
                        hasSource("AccountStatement02.txt"), hasNote("flatex Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2023-11-06T11:10"), hasAmount("EUR", 2500.00), //
                        hasSource("AccountStatement02.txt"), hasNote("flatex Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2023-10-03T11:41"), hasAmount("EUR", 2500.00), //
                        hasSource("AccountStatement02.txt"), hasNote("flatex Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2023-09-14T11:21"), hasAmount("EUR", 2500.00), //
                        hasSource("AccountStatement02.txt"), hasNote("flatex Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2023-08-01T11:31"), hasAmount("EUR", 2500.00), //
                        hasSource("AccountStatement02.txt"), hasNote("flatex Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2023-07-18T11:21"), hasAmount("EUR", 3000.00), //
                        hasSource("AccountStatement02.txt"), hasNote("flatex Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2023-07-14T11:11"), hasAmount("EUR", 2500.00), //
                        hasSource("AccountStatement02.txt"), hasNote("flatex Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2023-07-12T17:27"), hasAmount("EUR", 1.00), //
                        hasSource("AccountStatement02.txt"), hasNote("Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2023-03-30T21:11"), hasAmount("EUR", 2300.00), //
                        hasSource("AccountStatement02.txt"), hasNote("flatex Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2023-02-28T16:03"), hasAmount("EUR", 2200.00), //
                        hasSource("AccountStatement02.txt"), hasNote("flatex Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2023-02-03T15:11"), hasAmount("EUR", 2000.00), //
                        hasSource("AccountStatement02.txt"), hasNote("flatex Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2023-01-05T09:21"), hasAmount("EUR", 4000.00), //
                        hasSource("AccountStatement02.txt"), hasNote("flatex Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2022-11-08T11:10"), hasAmount("EUR", 2700.00), //
                        hasSource("AccountStatement02.txt"), hasNote("flatex Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2022-10-06T14:50"), hasAmount("EUR", 2500.00), //
                        hasSource("AccountStatement02.txt"), hasNote("flatex Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2022-09-08T20:50"), hasAmount("EUR", 2500.00), //
                        hasSource("AccountStatement02.txt"), hasNote("flatex Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2022-08-17T20:50"), hasAmount("EUR", 2000.00), //
                        hasSource("AccountStatement02.txt"), hasNote("flatex Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2022-07-20T08:50"), hasAmount("EUR", 3000.00), //
                        hasSource("AccountStatement02.txt"), hasNote("flatex Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2022-06-01T20:50"), hasAmount("EUR", 2200.00), //
                        hasSource("AccountStatement02.txt"), hasNote("flatex Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2022-05-02T15:50"), hasAmount("EUR", 3000.00), //
                        hasSource("AccountStatement02.txt"), hasNote("flatex Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2022-04-01T15:50"), hasAmount("EUR", 4000.00), //
                        hasSource("AccountStatement02.txt"), hasNote("flatex Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2022-03-01T22:20"), hasAmount("EUR", 2500.00), //
                        hasSource("AccountStatement02.txt"), hasNote("flatex Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2022-02-14T14:50"), hasAmount("EUR", 2000.00), //
                        hasSource("AccountStatement02.txt"), hasNote("flatex Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2022-01-24T20:50"), hasAmount("EUR", 5000.00), //
                        hasSource("AccountStatement02.txt"), hasNote("flatex Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2021-11-12T23:20"), hasAmount("EUR", 2900.00), //
                        hasSource("AccountStatement02.txt"), hasNote("flatex Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2021-09-29T20:50"), hasAmount("EUR", 730.00), //
                        hasSource("AccountStatement02.txt"), hasNote("flatex Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2021-07-06T15:50"), hasAmount("EUR", 1500.00), //
                        hasSource("AccountStatement02.txt"), hasNote("flatex Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2021-06-07T14:50"), hasAmount("EUR", 1000.00), //
                        hasSource("AccountStatement02.txt"), hasNote("flatex Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2021-05-06T20:50"), hasAmount("EUR", 3105.70), //
                        hasSource("AccountStatement02.txt"), hasNote("flatex Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2021-03-30T18:56"), hasAmount("EUR", 2000.00), //
                        hasSource("AccountStatement02.txt"), hasNote("Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2021-03-05T18:55"), hasAmount("EUR", 6185.00), //
                        hasSource("AccountStatement02.txt"), hasNote("Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2021-01-27T12:11"), hasAmount("EUR", 13041.39), //
                        hasSource("AccountStatement02.txt"), hasNote("Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2021-01-25T14:09"), hasAmount("EUR", 2000.00), //
                        hasSource("AccountStatement02.txt"), hasNote("Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2020-12-03T14:45"), hasAmount("EUR", 2000.00), //
                        hasSource("AccountStatement02.txt"), hasNote("Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2020-11-17T16:11"), hasAmount("EUR", 2500.00), //
                        hasSource("AccountStatement02.txt"), hasNote("Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2020-10-30T19:02"), hasAmount("EUR", 2500.00), //
                        hasSource("AccountStatement02.txt"), hasNote("Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2020-07-10T18:41"), hasAmount("EUR", 7908.40), //
                        hasSource("AccountStatement02.txt"), hasNote("Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2020-07-10T14:48"), hasAmount("EUR", 2146.92), //
                        hasSource("AccountStatement02.txt"), hasNote("Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2020-06-11T18:43"), hasAmount("EUR", 3308.58), //
                        hasSource("AccountStatement02.txt"), hasNote("Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2020-05-07T18:54"), hasAmount("EUR", 4609.20), //
                        hasSource("AccountStatement02.txt"), hasNote("Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2020-04-14T13:55"), hasAmount("EUR", 6321.60), //
                        hasSource("AccountStatement02.txt"), hasNote("Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2020-04-14T13:55"), hasAmount("EUR", 2509.20), //
                        hasSource("AccountStatement02.txt"), hasNote("Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2020-03-06T12:36"), hasAmount("EUR", 1796.00), //
                        hasSource("AccountStatement02.txt"), hasNote("Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2020-02-10T18:29"), hasAmount("EUR", 1322.00), //
                        hasSource("AccountStatement02.txt"), hasNote("Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2020-02-03T15:34"), hasAmount("EUR", 1465.00), //
                        hasSource("AccountStatement02.txt"), hasNote("Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2019-12-10T11:08"), hasAmount("EUR", 50.00), //
                        hasSource("AccountStatement02.txt"), hasNote("Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2019-11-22T12:26"), hasAmount("EUR", 0.01), //
                        hasSource("AccountStatement02.txt"), hasNote("Deposit"))));

        assertThat(results, hasItem(deposit(hasDate("2018-06-06T16:56"), hasAmount("EUR", 20.00), //
                        hasSource("AccountStatement02.txt"), hasNote("Deposit"))));

        // check interest charge transaction
        assertThat(results, hasItem(interestCharge( //
                        hasDate("2022-10-01T14:01"), //
                        hasSource("AccountStatement02.txt"), //
                        hasNote("Flatex Interest"), //
                        hasAmount("EUR", 0.08), hasGrossValue("EUR", 0.08), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(interestCharge( //
                        hasDate("2022-07-02T14:12"), //
                        hasSource("AccountStatement02.txt"), //
                        hasNote("Flatex Interest"), //
                        hasAmount("EUR", 0.53), hasGrossValue("EUR", 0.53), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(interestCharge( //
                        hasDate("2022-04-02T15:20"), //
                        hasSource("AccountStatement02.txt"), //
                        hasNote("Flatex Interest"), //
                        hasAmount("EUR", 0.06), hasGrossValue("EUR", 0.06), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(interestCharge( //
                        hasDate("2021-12-31T03:00"), //
                        hasSource("AccountStatement02.txt"), //
                        hasNote("Flatex Interest"), //
                        hasAmount("EUR", 0.41), hasGrossValue("EUR", 0.41), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(interestCharge( //
                        hasDate("2021-10-01T23:30"), //
                        hasSource("AccountStatement02.txt"), //
                        hasNote("Flatex Interest"), //
                        hasAmount("EUR", 0.20), hasGrossValue("EUR", 0.20), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(interestCharge( //
                        hasDate("2021-07-02T15:40"), //
                        hasSource("AccountStatement02.txt"), //
                        hasNote("Flatex Interest"), //
                        hasAmount("EUR", 0.02), hasGrossValue("EUR", 0.02), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2024-02-05T07:17"), //
                        hasSource("AccountStatement02.txt"), //
                        hasNote("Giro Exchange Connection Fee 2024"), //
                        hasAmount("EUR", 2.50), hasGrossValue("EUR", 2.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(fee( //
                        hasDate("2024-02-05T07:17"), //
                        hasSource("AccountStatement02.txt"), //
                        hasNote("Giro Exchange Connection Fee 2024"), //
                        hasAmount("EUR", 2.50), hasGrossValue("EUR", 2.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(fee( //
                        hasDate("2024-02-05T07:17"), //
                        hasSource("AccountStatement02.txt"), //
                        hasNote("Giro Exchange Connection Fee 2024"), //
                        hasAmount("EUR", 2.50), hasGrossValue("EUR", 2.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(fee( //
                        hasDate("2023-03-01T15:07"), //
                        hasSource("AccountStatement02.txt"), //
                        hasNote("Giro Exchange Connection Fee 2023"), //
                        hasAmount("EUR", 2.50), hasGrossValue("EUR", 2.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(fee( //
                        hasDate("2023-01-03T13:59"), //
                        hasSource("AccountStatement02.txt"), //
                        hasNote("Giro Exchange Connection Fee 2023"), //
                        hasAmount("EUR", 2.50), hasGrossValue("EUR", 2.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(fee( //
                        hasDate("2023-01-03T13:59"), //
                        hasSource("AccountStatement02.txt"), //
                        hasNote("Giro Exchange Connection Fee 2023"), //
                        hasAmount("EUR", 2.50), hasGrossValue("EUR", 2.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(fee( //
                        hasDate("2022-02-03T10:00"), //
                        hasSource("AccountStatement02.txt"), //
                        hasNote("Giro Exchange Connection Fee 2022"), //
                        hasAmount("EUR", 2.50), hasGrossValue("EUR", 2.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(fee( //
                        hasDate("2022-02-03T10:00"), //
                        hasSource("AccountStatement02.txt"), //
                        hasNote("Giro Exchange Connection Fee 2022"), //
                        hasAmount("EUR", 2.50), hasGrossValue("EUR", 2.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testAccountStatement_french01()
    {
        var extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "AccountStatement_french01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(3L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(7L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(10));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("SE0020050417"), hasTicker(null), //
                        hasName("BOLIDEN AB"), //
                        hasCurrencyCode("SEK"))));

        assertThat(results, hasItem(security( //
                        hasIsin("FI0009003727"), hasTicker(null), //
                        hasName("WARTSILA OYJ ABP"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US02079K3059"), hasTicker(null), //
                        hasName("ALPHABET INC CLASS A"), //
                        hasCurrencyCode("USD"))));

        // check dividende transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2026-05-07T07:10"), hasExDate(null), //
                        hasShares(0.00), //
                        hasSource("AccountStatement_french01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 22.59), hasGrossValue("EUR", 32.27), //
                        hasTaxes("EUR", 9.68), hasFees("EUR", 0.00), //
                        hasForexGrossValue("SEK", 352.00))));

        assertThat(results, hasItem(dividend( //
                        hasDate("2026-03-24T07:04"), hasExDate(null), //
                        hasShares(0.00), //
                        hasSource("AccountStatement_french01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 15.92), hasGrossValue("EUR", 24.49), //
                        hasTaxes("EUR", 8.57), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(dividend( //
                        hasDate("2025-12-16T07:13"), hasExDate(null), //
                        hasShares(0.00), //
                        hasSource("AccountStatement_french01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1.06), hasGrossValue("EUR", 1.25), //
                        hasTaxes("EUR", 0.19), hasFees("EUR", 0.00), //
                        hasForexGrossValue("USD", 1.47))));

        // check deposit transaction
        assertThat(results, hasItem(deposit(hasDate("2025-10-08T14:50"), hasAmount("EUR", 9500.00), //
                        hasSource("AccountStatement_french01.txt"), hasNote("Dépôt flatex"))));

        assertThat(results, hasItem(deposit(hasDate("2025-10-07T21:51"), hasAmount("EUR", 500.00), //
                        hasSource("AccountStatement_french01.txt"), hasNote("Dépôt flatex"))));

        // check fee transaction
        assertThat(results, hasItem(fee( //
                        hasDate("2026-04-07T09:44"), //
                        hasSource("AccountStatement_french01.txt"), //
                        hasNote("Frais de connexion aux places boursières 2026 (London Stock Exchange (LSE) - LSE)"), //
                        hasAmount("EUR", 2.50), hasGrossValue("EUR", 2.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        // check fee refund transaction
        assertThat(results, hasItem(feeRefund( //
                        hasDate("2026-02-25T16:19"), //
                        hasSource("AccountStatement_french01.txt"), //
                        hasNote("Remboursement offre promotionnelle"), //
                        hasAmount("EUR", 100.00), hasGrossValue("EUR", 100.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }


    @Test
    public void testTransaktionsuebersicht01()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transaktionsuebersicht01.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(17L));
        assertThat(countBuySell(results), is(32L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(49));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        Security security1 = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security1.getIsin(), is("DE000C21EMZ1"));
        assertThat(security1.getName(), is("ODX5 C11500.00 29MAR19"));
        assertThat(security1.getCurrencyCode(), is(CurrencyUnit.EUR));

        Security security2 = results.stream().filter(SecurityItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security2.getIsin(), is("DE000C21EMV0"));
        assertThat(security2.getName(), is("ODX5 C11400.00 29MAR19"));
        assertThat(security2.getCurrencyCode(), is(CurrencyUnit.EUR));

        Security security3 = results.stream().filter(SecurityItem.class::isInstance).skip(2).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security3.getIsin(), is("DE000C21EMX6"));
        assertThat(security3.getName(), is("ODX5 C11450.00 29MAR19"));
        assertThat(security3.getCurrencyCode(), is(CurrencyUnit.EUR));

        Security security4 = results.stream().filter(SecurityItem.class::isInstance).skip(3).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security4.getIsin(), is("DE000C21EMY4"));
        assertThat(security4.getName(), is("ODX5 P11450.00 29MAR19"));
        assertThat(security4.getCurrencyCode(), is(CurrencyUnit.EUR));

        // @formatter:off
        // 29-03-2019 12:18 29-03-2019 ODX5 C11500.00 29MAR19 DE000C21EMZ1
        // Verkauf 3 zu je 30 EUR (DE000C21EMZ1) EUR 450,00 EUR 2.604,06
        // @formatter:on
        // check first buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-03-29T12:18")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(3)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(447.30))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(450.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.70))));

        // check xx buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(27).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-02-08T12:44")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(50.90))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(50.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.90))));

        // check xx buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(29).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-02-08T10:54")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(132.80))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(131.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.80))));

        // check xx buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(30).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-02-07T13:02")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(100.90))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(100.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.90))));

    }

    @Test
    public void testTransaktionsuebersicht02()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transaktionsuebersicht02.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(3L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE0005140008"));
        assertThat(security.getName(), is("DEUTSCHE BANK AG NA O.N"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check first buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-04-01T15:35")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(136)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1010.94))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1013.20))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.26))));

        // check xx buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-04-01T12:20")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(18)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(132.38))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(132.35))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.03))));

        // check xx buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(2).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-04-01T12:20")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(118)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(869.88))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(867.65))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.23))));

    }

    @Test
    public void testTransaktionsuebersicht03()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transaktionsuebersicht03.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(4L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        Security security1 = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security1.getIsin(), is("US88160R1014"));
        assertThat(security1.getName(), is("TESLA MOTORS INC. - C"));
        assertThat(security1.getCurrencyCode(), is(CurrencyUnit.USD));

        Security security2 = results.stream().filter(SecurityItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security2.getIsin(), is("DE000C34JCK6"));
        assertThat(security2.getName(), is("ODX1 P12300.00 03MAY19"));
        assertThat(security2.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check 1st buy/sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-04-29T16:11")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(3)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(644.53))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(645.04))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.51))));
        Unit grossValueUnit = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(720.00))));

        // check 2nd buy/sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(2).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-04-26T20:23")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(210.42))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(209.92))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.50))));
        grossValueUnit = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(234.00))));

        // check 3rd buy transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(3).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-04-26T17:52")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(431.20))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(430.69))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.51))));
        grossValueUnit = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(480.00))));
    }

    @Test
    public void testTransaktionsuebersicht04()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transaktionsuebersicht04.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(2L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getName(), is("ARCHER-DANIELS-MIDLAND"));
        assertThat(security.getIsin(), is("US0394831020"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check 1st buy/sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-04-25T19:03")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(75.91))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(75.90))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.01))));
        Unit grossValueUnit = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(84.52))));

        // check 2nd buy/sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-04-25T19:03")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(48)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1822.39))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1821.72))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.67))));
        grossValueUnit = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(2028.48))));
    }

    @Test
    public void testTransaktionsuebersicht05()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transaktionsuebersicht05.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(66L));
        assertThat(countBuySell(results), is(95L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(161));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getName(), is("CATERPILLAR INC. COMM"));
        assertThat(security.getIsin(), is("US1491231015"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check first transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-08-16T19:04")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(105.50))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(105.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.50))));
        Unit grossValueUnit = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(116.50))));

        // check 5th transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(5).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-08-06T20:20")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(58.35))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(57.85))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.50))));
        grossValueUnit = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(64.70))));

        // check 17th transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(17).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-07-23T15:30")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(3)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(163.32))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(163.83))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.51))));
        grossValueUnit = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(183.12))));

        // check 94th transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(94).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2018-08-14T16:08")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(15)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(200.33))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(199.78))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.55))));
        grossValueUnit = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(227.10))));

    }

    @Test
    public void testTransaktionsuebersicht06()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transaktionsuebersicht06.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(17L));
        assertThat(countBuySell(results), is(36L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(53));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getName(), is("CALL 20.09.19 TRADDESK 180"));
        assertThat(security.getIsin(), is("DE000GA2N3L5"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // @formatter:off
        // 20-08-2019 19:03 CALL 20.09.19 TRADDESK 180 DE000GA2N3L5 FRA -114 EUR
        // 6,60 EUR 752,40 EUR 752,40 EUR -2,89 EUR 749,51
        // @formatter:off
        // check first transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-08-20T19:03")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(114)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(749.51))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(752.40))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.89))));

        // check 29th transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(29).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-07-22T19:16")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(76.42))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(76.42))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        Unit grossValueUnit = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(85.73))));

        // check 34th transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(34).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-07-12T20:27")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(50)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3183.28))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3183.96))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.68))));
        grossValueUnit = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(3588.00))));

    }

    @Test
    public void testTransaktionsuebersicht07()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transaktionsuebersicht07.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(2L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getName(), is("TURBOC O.END GOLD 1109,22"));
        assertThat(security.getIsin(), is("DE000VA5DDR3"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // @formatter:off
        // 09-01-2020 09:31 TURBOC O.END GOLD 1109,22 DE000VA5DDR3 FRA -55 EUR
        // 42,46 EUR 2.335,30 EUR 2.335,30 EUR -4,76 EUR 2.330,54
        // @formatter:on
        // check first transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-01-09T09:31")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(55)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2330.54))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2335.30))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.76))));

        // check 2nd transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-01-08T09:30")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(25)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2342.61))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2340.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.61))));
    }

    @Test
    public void testTransaktionsuebersicht08()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transaktionsuebersicht08.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(11L));
        assertThat(countBuySell(results), is(21L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(32));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getName(), is("SALESFORCE.COM INC COM"));
        assertThat(security.getIsin(), is("US79466L3024"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // @formatter:off
        // 23-12-2020 21:51 SALESFORCE.COM INC COM US79466L3024 NSY 3 USD 228,00
        // USD -684,00 EUR -561,44 1,2171 EUR -0,51 EUR -561,95
        // @formatter:off
        // check buy transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-12-23T21:51")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(3)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(561.95))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(561.44))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.51))));

        // check sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(2).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-12-02T15:34")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(899.55))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(900.06))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.51))));
        Unit grossValueUnit = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(1088.64))));
    }

    @Test
    public void testTransaktionsuebersicht09()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transaktionsuebersicht09.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(3L));
        assertThat(countBuySell(results), is(4L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(7));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getName(), is("WALT DISNEY COMPANY (T"));
        assertThat(security.getIsin(), is("US2546871060"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check 1st sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-01-07T16:54")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(289.64))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(290.15))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.51))));
        Unit grossValueUnit = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(356.50))));

        // check 2nd buy transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-01-07T09:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(20)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(197.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(194.96))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.04))));

        // check 3rd sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(2).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-01-07T09:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(15)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(230.34))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(228.30))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.04))));

        // check 4rd sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(3).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-01-07T09:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(25)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(973.16))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(972.53))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.63))));
    }

    @Test
    public void testTransaktionsuebersicht10()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transaktionsuebersicht10.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(3L));
        assertThat(countBuySell(results), is(4L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(7));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getName(), is("CALL 16.12.22 LEONI 18"));
        assertThat(security.getIsin(), is("DE000DV0VC30"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check 1st buy transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-02-11T15:41")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(79)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(251.12))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(248.85))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.27))));

        // check 2nd buy transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-02-11T15:30")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(34)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(508.26))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(508.87))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.61))));
        Unit grossValueUnit1 = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit1.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(618.80))));

        // check 3rd sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(2).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-02-11T15:30")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(120.05))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(120.06))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.01))));
        Unit grossValueUnit2 = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit2.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(146.00))));

        // check 4rd sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(3).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-02-11T15:30")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(5)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(299.64))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(300.16))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.52))));
        Unit grossValueUnit3 = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit3.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(365.00))));
    }

    @Test
    public void testTransaktionsuebersicht11()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transaktionsuebersicht11.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getName(), is("VARTA AG"));
        assertThat(security.getIsin(), is("DE000A0TGJ55"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check 1st sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-12-21T10:33")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(11)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1225.38))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1227.60))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.22))));
    }

    @Test
    public void testTransaktionsuebersicht12()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transaktionsuebersicht12.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(7L));
        assertThat(countBuySell(results), is(7L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(14));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getName(), is("VARTA AG"));
        assertThat(security.getIsin(), is("DE000A0TGJ55"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check 1st sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-12-21T10:33")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(11)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1225.38))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1227.60))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.22))));

        // check 2nd buy transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-11-13T13:37")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(120)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1571.88))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1574.16))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.28))));

        // check 3rd sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(2).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-11-13T13:36")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(22)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(819.55))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(821.70))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.15))));

        // check 4rd sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(3).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-11-09T15:34")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(7)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1261.66))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1262.18))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.52))));
        Unit grossValueUnit1 = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit1.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(1505.00))));

        // check 5rd sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(4).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-11-09T13:16")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(11)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1999.64))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2002.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.36))));

        // check 6rd sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(5).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-11-09T09:06")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(10)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1907.86))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1910.20))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.34))));

        // check 7rd sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(6).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-10-22T16:35")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(40)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1053.69))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1054.33))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.64))));
        Unit grossValueUnit2 = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit2.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(1248.40))));
    }

    @Test
    public void testTransaktionsuebersicht13()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transaktionsuebersicht13.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(6L));
        assertThat(countBuySell(results), is(9L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(15));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getName(), is("IROBOT CORPORATION - C"));
        assertThat(security.getIsin(), is("US4627261005"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check 1st buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2018-03-16T20:45")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(9)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(511.82))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(511.29))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.53))));
        Unit grossValueUnit1 = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit1.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(628.20))));

        // check 5th buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(4).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2018-03-13T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(175)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(644.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(644.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testTransaktionsuebersicht14()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transaktionsuebersicht14.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(8L));
        assertThat(countBuySell(results), is(12L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(20));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getName(), is("DEUTSCHE TELEKOM AG"));
        assertThat(security.getIsin(), is("DE0005557508"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check 1st buy transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2017-08-01T17:21")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(50)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(782.56))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(780.50))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.06))));
    }

    @Test
    public void testTransaktionsuebersicht15()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transaktionsuebersicht15.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(2L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getName(), is("GENERAL ELECTRIC COMPANY COMMON STOCK"));
        assertThat(security.getIsin(), is("US3696043013"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check 1st buy transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-08-02T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(175.97))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(175.97))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        // check 2nd buy/sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-08-02T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(20)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(219.97))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(219.97))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testTransaktionsuebersicht16()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transaktionsuebersicht16.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(186L));
        assertThat(countBuySell(results), is(471L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(657));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security1 = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security1.getName(), is("CD PROJEKT RED SA"));
        assertThat(security1.getIsin(), is("PLOPTTC00011"));
        assertThat(security1.getCurrencyCode(), is("PLN"));

        Security security2 = results.stream().filter(SecurityItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security2.getName(), is("EBAY INC"));
        assertThat(security2.getIsin(), is("US2786421030"));
        assertThat(security2.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check 1st buy transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-10-12T15:34")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(16)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(691.80))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(697.92))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(6.12))));

        // check 2nd buy/sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-10-08T16:07")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(8)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(517.91))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(520.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.09))));

        // check 455th buy/sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(455).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-08-27T09:48")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(30)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(74.24))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(70.20))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.04))));
    }

    @Test
    public void testTransaktionsuebersicht17()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transaktionsuebersicht17.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(11L));
        assertThat(countBuySell(results), is(20L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(31));
        new AssertImportActions().check(results, "CHF");

        // check security
        Security security1 = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security1.getName(), is("PLUG POWER INC. - COM"));
        assertThat(security1.getIsin(), is("US72919P2020"));
        assertThat(security1.getCurrencyCode(), is(CurrencyUnit.USD));

        Security security2 = results.stream().filter(SecurityItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security2.getName(), is("NIKOLA CORP"));
        assertThat(security2.getIsin(), is("US6541101050"));
        assertThat(security2.getCurrencyCode(), is(CurrencyUnit.USD));

        Security security3 = results.stream().filter(SecurityItem.class::isInstance).skip(2).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security3.getName(), is("NIO INC - ADR"));
        assertThat(security3.getIsin(), is("US62914V1061"));
        assertThat(security3.getCurrencyCode(), is(CurrencyUnit.USD));

        Security security4 = results.stream().filter(SecurityItem.class::isInstance).skip(3).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security4.getName(), is("BALLARD POWER SYSTEMS"));
        assertThat(security4.getIsin(), is("CA0585861085"));
        assertThat(security4.getCurrencyCode(), is(CurrencyUnit.USD));

        Security security5 = results.stream().filter(SecurityItem.class::isInstance).skip(4).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security5.getName(), is("TESLA MOTORS INC. - C"));
        assertThat(security5.getIsin(), is("US88160R1014"));
        assertThat(security5.getCurrencyCode(), is(CurrencyUnit.USD));

        Security security6 = results.stream().filter(SecurityItem.class::isInstance).skip(5).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security6.getName(), is("ALPHABET INC. - CLASS A"));
        assertThat(security6.getIsin(), is("US02079K3059"));
        assertThat(security6.getCurrencyCode(), is(CurrencyUnit.USD));

        Security security7 = results.stream().filter(SecurityItem.class::isInstance).skip(6).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security7.getName(), is("ADOBE SYSTEMS INCORPOR"));
        assertThat(security7.getIsin(), is("US00724F1012"));
        assertThat(security7.getCurrencyCode(), is(CurrencyUnit.USD));

        Security security8 = results.stream().filter(SecurityItem.class::isInstance).skip(7).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security8.getName(), is("SALESFORCE.COM INC COM"));
        assertThat(security8.getIsin(), is("US79466L3024"));
        assertThat(security8.getCurrencyCode(), is(CurrencyUnit.USD));

        Security security9 = results.stream().filter(SecurityItem.class::isInstance).skip(8).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security9.getName(), is("DEUTSCHE LUFTHANSA AG"));
        assertThat(security9.getIsin(), is("DE0008232125"));
        assertThat(security9.getCurrencyCode(), is(CurrencyUnit.EUR));

        Security security10 = results.stream().filter(SecurityItem.class::isInstance).skip(9).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security10.getName(), is("VOLKSWAGEN AG"));
        assertThat(security10.getIsin(), is("DE0007664039"));
        assertThat(security10.getCurrencyCode(), is(CurrencyUnit.EUR));

        Security security11 = results.stream().filter(SecurityItem.class::isInstance).skip(10).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security11.getName(), is("WAL-MART STORES INC."));
        assertThat(security11.getIsin(), is("US9311421039"));
        assertThat(security11.getCurrencyCode(), is(CurrencyUnit.USD));

        // check 1st buy transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-08-26T17:55")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(50)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(613.90))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CHF", Values.Amount.factorize(613.18))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CHF", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CHF", Values.Amount.factorize(0.72))));

        // check 6th buy/sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(5).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-08-14T21:13")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(2965.31))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CHF", Values.Amount.factorize(2964.76))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CHF", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CHF", Values.Amount.factorize(0.55))));

        // check 19th buy/sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(18).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-08-07T10:29")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(7)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(1006.58))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CHF", Values.Amount.factorize(1001.76))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CHF", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CHF", Values.Amount.factorize(4.82))));
    }

    @Test
    public void testTransaktionsuebersicht18()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transaktionsuebersicht18.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(2L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security1 = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security1.getName(), is("GENERAL ELECTRIC COMPANY COMMON STOCK"));
        assertThat(security1.getIsin(), is("US3696043013"));
        assertThat(security1.getCurrencyCode(), is(CurrencyUnit.USD));

        Security security2 = results.stream().filter(SecurityItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security2.getName(), is("GENERAL ELECTRIC COMPA"));
        assertThat(security2.getIsin(), is("US3696041033"));
        assertThat(security2.getCurrencyCode(), is(CurrencyUnit.USD));

        // check 1st buy transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-08-02T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(175.97))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(175.97))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        // check 2nd buy/sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-08-02T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(20)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(219.97))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(219.97))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testTransaktionsuebersicht19()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transaktionsuebersicht19.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(7L));
        assertThat(countBuySell(results), is(14L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(21));
        new AssertImportActions().check(results, "CHF");

        // check security
        Security security1 = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security1.getName(), is("ISHARES MSCI EUROPE ESG SCREENED UCITS ETF EUR ACC"));
        assertThat(security1.getIsin(), is("IE00BFNM3D14"));
        assertThat(security1.getCurrencyCode(), is(CurrencyUnit.EUR));

        Security security2 = results.stream().filter(SecurityItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security2.getName(), is("ISHARES DJ EUROPE SUSTAINABILITY (BLACKROCK ASSET MA..."));
        assertThat(security2.getIsin(), is("IE00B52VJ196"));
        assertThat(security2.getCurrencyCode(), is(CurrencyUnit.EUR));

        Security security3 = results.stream().filter(SecurityItem.class::isInstance).skip(2).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security3.getName(), is("HSBC SP 500 ETF"));
        assertThat(security3.getIsin(), is("IE00B5KQNG97"));
        assertThat(security3.getCurrencyCode(), is(CurrencyUnit.EUR));

        Security security4 = results.stream().filter(SecurityItem.class::isInstance).skip(3).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security4.getName(), is("AMUNDI ETF STOX600"));
        assertThat(security4.getIsin(), is("LU1681040223"));
        assertThat(security4.getCurrencyCode(), is(CurrencyUnit.EUR));

        Security security5 = results.stream().filter(SecurityItem.class::isInstance).skip(4).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security5.getName(), is("XIAOMI CORP. CL.B"));
        assertThat(security5.getIsin(), is("KYG9830T1067"));
        assertThat(security5.getCurrencyCode(), is(CurrencyUnit.EUR));

        Security security6 = results.stream().filter(SecurityItem.class::isInstance).skip(5).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security6.getName(), is("APPLE INC"));
        assertThat(security6.getIsin(), is("US0378331005"));
        assertThat(security6.getCurrencyCode(), is(CurrencyUnit.EUR));

        Security security7 = results.stream().filter(SecurityItem.class::isInstance).skip(6).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security7.getName(), is("GAMESTOP CORPORATION C"));
        assertThat(security7.getIsin(), is("US36467W1099"));
        assertThat(security7.getCurrencyCode(), is(CurrencyUnit.USD));

        // check 1st buy transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-10-06T09:06")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(40)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(290.78))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CHF", Values.Amount.factorize(290.78))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CHF", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CHF", Values.Amount.factorize(0.00))));

        // check 2nd buy/sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance)
                        .skip(1).findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-08-25T13:42")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(4)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(272.37))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CHF", Values.Amount.factorize(270.14))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CHF", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CHF", Values.Amount.factorize(2.23))));

        // check 3rd buy/sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance)
                        .skip(2).findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-08-20T09:22")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(35)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(262.01))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CHF", Values.Amount.factorize(262.01))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CHF", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CHF", Values.Amount.factorize(0.00))));

        // check 4th buy/sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance)
                        .skip(3).findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-08-19T09:05")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(36)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(1453.29))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CHF", Values.Amount.factorize(1453.29))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CHF", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CHF", Values.Amount.factorize(0.00))));

        // check 5th buy/sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance)
                        .skip(4).findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-08-19T09:05")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(14)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(565.17))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CHF", Values.Amount.factorize(565.17))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CHF", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CHF", Values.Amount.factorize(0.00))));

        // check 6th buy/sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance)
                        .skip(5).findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-07-22T13:26")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(170)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(1256.66))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CHF", Values.Amount.factorize(1256.66))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CHF", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CHF", Values.Amount.factorize(0.00))));

        // check 7th buy/sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance)
                        .skip(6).findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-07-22T10:18")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(20)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(2234.69))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CHF", Values.Amount.factorize(2234.69))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CHF", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CHF", Values.Amount.factorize(0.00))));

        // check 8th buy/sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance)
                        .skip(7).findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-06-18T12:46")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(28)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(1097.86))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CHF", Values.Amount.factorize(1097.86))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CHF", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CHF", Values.Amount.factorize(0.00))));

        // check 9th buy/sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance)
                        .skip(8).findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-05-26T17:41")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(300)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(989.88))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CHF", Values.Amount.factorize(999.01))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CHF", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CHF", Values.Amount.factorize(9.13))));

        // check 10th buy/sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance)
                        .skip(9).findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-03-05T15:42")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(33)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(1168.89))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CHF", Values.Amount.factorize(1168.89))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CHF", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CHF", Values.Amount.factorize(0.00))));

        // check 11th buy/sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance)
                        .skip(10).findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-03-05T15:34")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(45)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(303.44))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CHF", Values.Amount.factorize(301.13))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CHF", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CHF", Values.Amount.factorize(2.31))));

        // check 12th buy/sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance)
                        .skip(11).findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-02-12T14:12")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(9)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(1087.01))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CHF", Values.Amount.factorize(1082.14))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CHF", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CHF", Values.Amount.factorize(4.87))));

        // check 13th buy/sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance)
                        .skip(12).findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-01-29T19:31")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(275.33))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CHF", Values.Amount.factorize(275.87))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CHF", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CHF", Values.Amount.factorize(0.54))));

        // check 14th buy/sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance)
                        .skip(13).findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-01-29T15:30")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(338.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CHF", Values.Amount.factorize(337.46))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CHF", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CHF", Values.Amount.factorize(0.54))));
    }

    @Test
    public void testTransaktionsuebersicht20()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transaktionsuebersicht20.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(18L));
        assertThat(countBuySell(results), is(61L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(79));
        new AssertImportActions().check(results, "CHF");

        // check security
        Security security1 = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security1.getName(), is("ISHARES DJ EUROPE SUSTAINABILITY (BLACKROCK ASSET MA..."));
        assertThat(security1.getIsin(), is("IE00B52VJ196"));
        assertThat(security1.getCurrencyCode(), is(CurrencyUnit.EUR));

        Security security2 = results.stream().filter(SecurityItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security2.getName(), is("AMUNDI ETF STOX600"));
        assertThat(security2.getIsin(), is("LU1681040223"));
        assertThat(security2.getCurrencyCode(), is(CurrencyUnit.EUR));

        Security security3 = results.stream().filter(SecurityItem.class::isInstance).skip(2).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security3.getName(), is("ISHARES MSCI EUROPE ESG SCREENED UCITS ETF EUR ACC"));
        assertThat(security3.getIsin(), is("IE00BFNM3D14"));
        assertThat(security3.getCurrencyCode(), is(CurrencyUnit.EUR));

        Security security4 = results.stream().filter(SecurityItem.class::isInstance).skip(3).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security4.getName(), is("HSBC SP 500 ETF"));
        assertThat(security4.getIsin(), is("IE00B5KQNG97"));
        assertThat(security4.getCurrencyCode(), is(CurrencyUnit.EUR));

        Security security5 = results.stream().filter(SecurityItem.class::isInstance).skip(4).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security5.getName(), is("XIAOMI CORP. CL.B"));
        assertThat(security5.getIsin(), is("KYG9830T1067"));
        assertThat(security5.getCurrencyCode(), is(CurrencyUnit.EUR));

        Security security6 = results.stream().filter(SecurityItem.class::isInstance).skip(5).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security6.getName(), is("APPLE INC"));
        assertThat(security6.getIsin(), is("US0378331005"));
        assertThat(security6.getCurrencyCode(), is(CurrencyUnit.EUR));

        Security security7 = results.stream().filter(SecurityItem.class::isInstance).skip(6).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security7.getName(), is("GAMESTOP CORPORATION C"));
        assertThat(security7.getIsin(), is("US36467W1099"));
        assertThat(security7.getCurrencyCode(), is(CurrencyUnit.USD));

        Security security8 = results.stream().filter(SecurityItem.class::isInstance).skip(7).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security8.getName(), is("AIRBUS SE"));
        assertThat(security8.getIsin(), is("NL0000235190"));
        assertThat(security8.getCurrencyCode(), is(CurrencyUnit.EUR));

        Security security9 = results.stream().filter(SecurityItem.class::isInstance).skip(8).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security9.getName(), is("RWE AG"));
        assertThat(security9.getIsin(), is("DE0007037129"));
        assertThat(security9.getCurrencyCode(), is(CurrencyUnit.EUR));

        Security security10 = results.stream().filter(SecurityItem.class::isInstance).skip(9).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security10.getName(), is("ETF ISHARES S&P 500 CHF HEDGED (ISHARES)"));
        assertThat(security10.getIsin(), is("IE00B88DZ566"));
        assertThat(security10.getCurrencyCode(), is("CHF"));

        Security security11 = results.stream().filter(SecurityItem.class::isInstance).skip(10).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security11.getName(), is("SERVICENOW INC. COMMO"));
        assertThat(security11.getIsin(), is("US81762P1021"));
        assertThat(security11.getCurrencyCode(), is(CurrencyUnit.USD));

        Security security12 = results.stream().filter(SecurityItem.class::isInstance).skip(11).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security12.getName(), is("ACCENTURE PLC. CLASS A"));
        assertThat(security12.getIsin(), is("IE00B4BNMY34"));
        assertThat(security12.getCurrencyCode(), is(CurrencyUnit.USD));

        Security security13 = results.stream().filter(SecurityItem.class::isInstance).skip(12).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security13.getName(), is("LYXOR UCITS ETF S&P500 VIX FU EN ROL LUX"));
        assertThat(security13.getIsin(), is("LU0832435464"));
        assertThat(security13.getCurrencyCode(), is(CurrencyUnit.EUR));

        Security security14 = results.stream().filter(SecurityItem.class::isInstance).skip(13).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security14.getName(), is("WIRECARD AG"));
        assertThat(security14.getIsin(), is("DE0007472060"));
        assertThat(security14.getCurrencyCode(), is(CurrencyUnit.EUR));

        Security security15 = results.stream().filter(SecurityItem.class::isInstance).skip(14).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security15.getName(), is("ALLIANZ SE"));
        assertThat(security15.getIsin(), is("DE0008404005"));
        assertThat(security15.getCurrencyCode(), is(CurrencyUnit.EUR));

        Security security16 = results.stream().filter(SecurityItem.class::isInstance).skip(16).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security16.getName(), is("CSAM ISHARES SPI (CH) (CREDIT SUISSE ASSET MANAGEMENT)"));
        assertThat(security16.getIsin(), is("CH0237935652"));
        assertThat(security16.getCurrencyCode(), is("CHF"));

        Security security17 = results.stream().filter(SecurityItem.class::isInstance).skip(17).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security17.getName(), is("ISHARES MSCI USA MINIM"));
        assertThat(security17.getIsin(), is("US46429B6974"));
        assertThat(security17.getCurrencyCode(), is(CurrencyUnit.USD));

        // check 1st buy transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-11-29T16:46")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(6)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(390.17))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CHF", Values.Amount.factorize(387.96))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CHF", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CHF", Values.Amount.factorize(2.21))));

        // check 2nd buy/sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance)
                        .skip(1).findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-11-29T16:45")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(12)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(1328.58))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CHF", Values.Amount.factorize(1328.58))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CHF", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CHF", Values.Amount.factorize(0.00))));

        // check 3rd buy/sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance)
                        .skip(2).findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-10-06T09:06")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(40)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(290.78))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CHF", Values.Amount.factorize(290.78))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CHF", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CHF", Values.Amount.factorize(0.00))));

        // check 4th buy/sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance)
                        .skip(3).findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-08-25T13:42")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(4)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(272.37))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CHF", Values.Amount.factorize(270.14))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CHF", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CHF", Values.Amount.factorize(2.23))));

        // check 5th buy/sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance)
                        .skip(4).findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-08-20T09:22")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(35)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(262.01))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CHF", Values.Amount.factorize(262.01))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CHF", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CHF", Values.Amount.factorize(0.00))));

        // check 6th buy/sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance)
                        .skip(5).findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-08-19T09:05")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(36)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(1453.29))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CHF", Values.Amount.factorize(1453.29))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CHF", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CHF", Values.Amount.factorize(0.00))));

        // check 7th buy/sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance)
                        .skip(6).findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-08-19T09:05")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(14)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(565.17))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CHF", Values.Amount.factorize(565.17))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CHF", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CHF", Values.Amount.factorize(0.00))));

        // check 8th buy/sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance)
                        .skip(7).findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-07-22T13:26")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(170)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(1256.66))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CHF", Values.Amount.factorize(1256.66))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CHF", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CHF", Values.Amount.factorize(0.00))));

        // check 9th buy/sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance)
                        .skip(8).findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-07-22T10:18")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(20)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(2234.69))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CHF", Values.Amount.factorize(2234.69))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CHF", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CHF", Values.Amount.factorize(0.00))));

        // check 23th buy/sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance)
                        .skip(22).findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-10-26T09:26")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(20)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(1162.29))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CHF", Values.Amount.factorize(1159.80))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CHF", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CHF", Values.Amount.factorize(2.49))));

        // check 29th buy/sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance)
                        .skip(28).findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-09-04T09:04")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(52)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(300.47))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CHF", Values.Amount.factorize(298.22))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CHF", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CHF", Values.Amount.factorize(2.25))));
    }
    
    @Test
    public void testTransaktionsuebersicht21()
    {
        var extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transaktionsuebersicht21.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(4L));
        assertThat(countBuySell(results), is(10L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(1L));
        assertThat(results.size(), is(15));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0007472060"), hasTicker(null), //
                        hasName("WIRECARD AG"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("DE000A0D8Q49"), hasTicker(null), //
                        hasName("ISHARES DOW JONES U.S. SELECT DIVIDEND UCITS (DE) ETF"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("DE000C5F3ZF0"), hasTicker(null), //
                        hasName("ODX1 C12700.00 05JUN20"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("DE000C5F3ZG8"), hasTicker(null), //
                        hasName("ODX1 P12700.00 05JUN20"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("DE0007472060")), //
                        hasDate("2021-01-07T20:36"), hasShares(90.00), //
                        hasSource("Transaktionsuebersicht21.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 97.92), hasGrossValue("EUR", 97.92), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("DE0007472060")), //
                        hasDate("2021-01-07T20:36"), hasShares(90.00), //
                        hasSource("Transaktionsuebersicht21.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 97.92), hasGrossValue("EUR", 97.92), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("DE0007472060")), //
                        hasDate("2020-06-24T16:26"), hasShares(30.00), //
                        hasSource("Transaktionsuebersicht21.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 347.06), hasGrossValue("EUR", 345.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.06))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("DE0007472060")), //
                        hasDate("2020-06-19T11:24"), hasShares(20.00), //
                        hasSource("Transaktionsuebersicht21.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 442.08), hasGrossValue("EUR", 440.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.08))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("DE0007472060")), //
                        hasDate("2020-06-18T14:04"), hasShares(10.00), //
                        hasSource("Transaktionsuebersicht21.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 452.08), hasGrossValue("EUR", 450.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.08))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("DE0007472060")), //
                        hasDate("2020-06-18T10:57"), hasShares(18.00), //
                        hasSource("Transaktionsuebersicht21.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1074.27), hasGrossValue("EUR", 1072.08), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.19))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("DE000A0D8Q49")), //
                        hasDate("2020-06-10T16:18"), hasShares(4.00), //
                        hasSource("Transaktionsuebersicht21.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 222.07), hasGrossValue("EUR", 220.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.07))));

        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        sale( //
                                        hasSecurity(hasIsin("DE000C5F3ZF0")), //
                                        hasDate("2020-06-05T13:30"), hasShares(1.00), //
                                        hasSource("Transaktionsuebersicht21.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("DE000C5F3ZG8")), //
                        hasDate("2020-06-05T11:32"), hasShares(1.00), //
                        hasSource("Transaktionsuebersicht21.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 499.25), hasGrossValue("EUR", 500.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.75))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("DE000C5F3ZG8")), //
                        hasDate("2020-06-05T10:43"), hasShares(1.00), //
                        hasSource("Transaktionsuebersicht21.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 150.75), hasGrossValue("EUR", 150.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.75))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("DE000C5F3ZF0")), //
                        hasDate("2020-06-05T10:22"), hasShares(1.00), //
                        hasSource("Transaktionsuebersicht21.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 150.75), hasGrossValue("EUR", 150.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.75))));
    }
    
    @Test
    public void testTransaktionsuebersicht22()
    {
        var extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transaktionsuebersicht22.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(6L));
        assertThat(countBuySell(results), is(11L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(2L));
        assertThat(results.size(), is(19));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US88160R1014"), hasTicker(null), //
                        hasName("TESLA INC"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("DE000C34JCK6"), hasTicker(null), //
                        hasName("ODX1 P12300.00 03MAY19"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("DE000C3311N4"), hasTicker(null), //
                        hasName("ODX4 C12300.00 26APR19"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("DE000C3311K0"), hasTicker(null), //
                        hasName("ODX4 P12200.00 26APR19"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("DE000C3311P9"), hasTicker(null), //
                        hasName("ODX4 P12300.00 26APR19"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("DE000C34JCJ8"), hasTicker(null), //
                        hasName("ODX1 C12300.00 03MAY19"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("US88160R1014")), //
                        hasDate("2019-04-29T16:11"), hasShares(3.00), //
                        hasSource("Transaktionsuebersicht22.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 645.18), hasGrossValue("EUR", 646.34), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", (0.65 + 0.51)), //
                        hasForexGrossValue("USD", 720.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("DE000C34JCK6")), //
                        hasDate("2019-04-29T09:16"), hasShares(1.00), //
                        hasSource("Transaktionsuebersicht22.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 250.90), hasGrossValue("EUR", 250.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.90))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("US88160R1014")), //
                        hasDate("2019-04-26T20:23"), hasShares(1.00), //
                        hasSource("Transaktionsuebersicht22.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 210.21), hasGrossValue("EUR", 209.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", (0.21 + 0.50)), //
                        hasForexGrossValue("USD", 234.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("US88160R1014")), //
                        hasDate("2019-04-26T17:52"), hasShares(2.00), //
                        hasSource("Transaktionsuebersicht22.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 430.77), hasGrossValue("EUR", 429.83), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", (0.43 + 0.51)), //
                        hasForexGrossValue("USD", 480.00))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("DE000C3311N4")), //
                        hasDate("2019-04-26T13:39"), hasShares(5.00), //
                        hasSource("Transaktionsuebersicht22.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 89.00), hasGrossValue("EUR", 89.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        sale( //
                                        hasSecurity(hasIsin("DE000C3311K0")), //
                                        hasDate("2019-04-26T13:39"), hasShares(1.00), //
                                        hasSource("Transaktionsuebersicht22.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        sale( //
                                        hasSecurity(hasIsin("DE000C3311P9")), //
                                        hasDate("2019-04-26T13:39"), hasShares(4.00), //
                                        hasSource("Transaktionsuebersicht22.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("DE000C34JCK6")), //
                        hasDate("2019-04-26T13:01"), hasShares(1.00), //
                        hasSource("Transaktionsuebersicht22.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 375.90), hasGrossValue("EUR", 375.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.90))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("DE000C34JCJ8")), //
                        hasDate("2019-04-26T13:01"), hasShares(1.00), //
                        hasSource("Transaktionsuebersicht22.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 375.90), hasGrossValue("EUR", 375.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.90))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("DE000C3311P9")), //
                        hasDate("2019-04-26T12:30"), hasShares(1.00), //
                        hasSource("Transaktionsuebersicht22.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 50.90), hasGrossValue("EUR", 50.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.90))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("DE000C3311N4")), //
                        hasDate("2019-04-26T10:40"), hasShares(1.00), //
                        hasSource("Transaktionsuebersicht22.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 25.90), hasGrossValue("EUR", 25.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.90))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("DE000C3311P9")), //
                        hasDate("2019-04-26T10:00"), hasShares(1.00), //
                        hasSource("Transaktionsuebersicht22.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 100.90), hasGrossValue("EUR", 100.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.90))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("DE000C3311N4")), //
                        hasDate("2019-04-26T09:11"), hasShares(1.00), //
                        hasSource("Transaktionsuebersicht22.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 50.90), hasGrossValue("EUR", 50.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.90))));
    }

    @Test
    public void testTransacties01()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transacties01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(3L));
        assertThat(countBuySell(results), is(3L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security1 = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security1.getName(), is("MCDONALD'S CORPORATION"));
        assertThat(security1.getIsin(), is("US5801351017"));
        assertThat(security1.getCurrencyCode(), is(CurrencyUnit.USD));

        Security security2 = results.stream().filter(SecurityItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security2.getName(), is("VANGUARD FTSE AW"));
        assertThat(security2.getIsin(), is("IE00B3RBWM25"));
        assertThat(security2.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check 1st buy/sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-07-09T15:30")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(4)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(757.83))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(757.32))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.51))));
        Unit grossValueUnit1 = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit1.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(847.96))));

        // check 2nd buy/sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-07-09T14:08")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(3)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(231.30))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(231.30))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        // check 3rd buy/sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(2).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2019-07-05T20:52")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(20)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(564.36))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(563.79))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.57))));
        Unit grossValueUnit2 = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit2.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(632))));
    }

    @Test
    public void testTransacciones01()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transacciones01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(4L));
        assertThat(countBuySell(results), is(4L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(8));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getName(), is("MACYS INC COMMON STOC"));
        assertThat(security.getIsin(), is("US55616P1049"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check first transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-11-27T16:27")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(30)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(269.10))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(268.50))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.60))));

        // check 2nd transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-11-27T15:49")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(55)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(606.04))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(606.72))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.68))));

        // check 3rd transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(2).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-11-09T13:07")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(247)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(486.18))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(488.42))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.24))));

        // check 4th transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(3).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-03-17T17:15")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(3)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(416.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(416.51))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.51))));

    }

    @Test
    public void testTransacciones02()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transacciones02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(2L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getName(), is("APPLE INC. - COMMON ST"));
        assertThat(security.getIsin(), is("US0378331005"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check 1st buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-01-27T20:55")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(9)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1054.32))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1053.79))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.53))));

        // check 2nd buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-01-27T20:54")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(48)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1107.68))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1108.34))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.66))));
        Unit grossValueUnit = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(1344.00))));
    }

    @Test
    public void testTransacciones03()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transacciones03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(4L));
        assertThat(countBuySell(results), is(7L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(11));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getName(), is("LUMINAR TECHNOLOGIES INC. - CLASS A COMMON STOCK"));
        assertThat(security.getIsin(), is("US5504241051"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check 1st sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-02-03T21:57")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(19)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(508.51))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(507.95))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.56))));
        Unit grossValueUnit = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(611.21))));

        // check 2nd buy transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-02-03T21:55")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(20)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(526.87))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(526.30))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.57))));

        // check 3rd sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(2).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-02-03T21:28")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(14)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(416.66))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(416.71))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.05))));

        // check 4th sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(3).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-02-03T21:28")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(59.54))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(59.55))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.01))));

        // check 5th sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(4).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-02-03T21:28")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(29.27))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(29.77))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.50))));

        // check 6th sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(5).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-02-03T21:18")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(20)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(780.01))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(780.08))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.07))));

        // check 7th sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(6).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();
        
        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-02-03T21:18")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(39.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(39.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testTransacciones04()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transacciones04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getName(), is("GOODFOOD MARKET CORP"));
        assertThat(security.getIsin(), is("CA38217M1005"));
        assertThat(security.getCurrencyCode(), is("CAD"));

        // check 1st sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-04-26T15:35")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(100)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(549.34))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(546.67))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.67))));

        Unit grossValueUnit = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of("CAD", Values.Amount.factorize(820.00))));
    }

    @Test
    public void testTransactions_english01()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transactions_english01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(12L));
        assertThat(countBuySell(results), is(12L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(24));
        new AssertImportActions().check(results, "CHF");

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getName(), is("NRG ENERGY INC. COMMO"));
        assertThat(security.getIsin(), is("US6293775085"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check 1st buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-02-02T17:39")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(25)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(973.16))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CHF", Values.Amount.factorize(972.53))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CHF", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CHF", Values.Amount.factorize(0.63))));
        Unit grossValueUnit1 = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit1.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(1082.50))));

        // check 2nd buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-02-02T16:14")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(10)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(721.99))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CHF", Values.Amount.factorize(721.95))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CHF", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CHF", Values.Amount.factorize(0.04))));
        Unit grossValueUnit2 = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit2.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(803.50))));

        // check 3rd buy sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(2).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-02-02T15:47")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(4)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(978.49))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CHF", Values.Amount.factorize(977.94))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CHF", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CHF", Values.Amount.factorize(0.55))));
        Unit grossValueUnit3 = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit3.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(1087.76))));

        // check 4th buy transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(3).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-02-02T14:11")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(11)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(944.02))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CHF", Values.Amount.factorize(939.22))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CHF", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CHF", Values.Amount.factorize(4.80))));
        Unit grossValueUnit4 = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit4.getForex(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(869.00))));

        // check 5th sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(4).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-01-28T15:30")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(50)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(842.14))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CHF", Values.Amount.factorize(842.86))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CHF", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CHF", Values.Amount.factorize(0.72))));
        Unit grossValueUnit5 = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit5.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(950.00))));

        // check 6th buy transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(5).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-12-30T10:09")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(15)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(1005.96))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CHF", Values.Amount.factorize(998.93))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CHF", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CHF", Values.Amount.factorize(7.03))));
        Unit grossValueUnit6 = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit6.getForex(), is(Money.of("PLN", Values.Amount.factorize(4167.00))));

        // check 7th buy transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(6).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-08-03T09:30")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(15)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(1248.40))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CHF", Values.Amount.factorize(1248.40))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CHF", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CHF", Values.Amount.factorize(0.00))));
        Unit grossValueUnit7 = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit7.getForex(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1156.35))));

        // check 8th buy transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(7).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-05-08T17:52")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(9)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(510.50))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CHF", Values.Amount.factorize(509.94))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CHF", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CHF", Values.Amount.factorize(0.56))));
        Unit grossValueUnit8 = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit8.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(525.78))));

        // check 9th buy transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(8).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-05-08T17:34")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(15)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of("CHF", Values.Amount.factorize(202.85))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of("CHF", Values.Amount.factorize(202.26))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of("CHF", Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of("CHF", Values.Amount.factorize(0.59))));
        Unit grossValueUnit9 = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit9.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(208.58))));
    }

    @Test
    public void testTransactions_english02()
    {
        var extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transactions_english02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(39L));
        assertThat(countBuySell(results), is(72L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(5L));
        assertThat(results.size(), is(116));
        new AssertImportActions().check(results, "EUR");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("JE00B1VS3333"), hasTicker(null), //
                        hasName("WISDOMTREE PHYSICAL SILVER INDIVIDUAL SECURITIES ETC"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US17888H1032"), hasTicker(null), //
                        hasName("CIVITAS RESOURCES INC"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US78454L1008"), hasTicker(null), //
                        hasName("SM ENERGY CO"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US5322751042"), hasTicker(null), //
                        hasName("LIGHTWAVE LOGIC INC"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US7731211089"), hasTicker(null), //
                        hasName("ROCKET LAB CORP"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("CA29872L2066"), hasTicker(null), //
                        hasName("EURO SUN MINING INC"), //
                        hasCurrencyCode("CAD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US88262P1021"), hasTicker(null), //
                        hasName("TEXAS PACIFIC LAND CORP"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("AN8068571086"), hasTicker(null), //
                        hasName("SLB NV"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("BMG9460G1015"), hasTicker(null), //
                        hasName("VALARIS LTD"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("NL0000009082"), hasTicker(null), //
                        hasName("KONINKLIJKE KPN NV"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US76655K1034"), hasTicker(null), //
                        hasName("RIGETTI COMPUTING INC"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US18539C2044"), hasTicker(null), //
                        hasName("CLEARWAY ENERGY INC CLASS C"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("FR0000124141"), hasTicker(null), //
                        hasName("VEOLIA ENVIRONNEMENT SA"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US1270971039"), hasTicker(null), //
                        hasName("COTERRA ENERGY INC"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US20825C1045"), hasTicker(null), //
                        hasName("CONOCOPHILLIPS"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US03674X1063"), hasTicker(null), //
                        hasName("ANTERO RESOURCES CORP"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("NL0000371243"), hasTicker(null), //
                        hasName("NEDAP NV"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("BMG0112X1056"), hasTicker(null), //
                        hasName("AEGON LTD"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US6516391066"), hasTicker(null), //
                        hasName("NEWMONT CORPORATION"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("GB00BZ3CNK81"), hasTicker(null), //
                        hasName("TORM PLC CLASS A"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("NL0011540547"), hasTicker(null), //
                        hasName("ABN AMRO BANK NV"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("NL0000302636"), hasTicker(null), //
                        hasName("VAN LANSCHOT KEMPEN NV"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("NL0010273215"), hasTicker(null), //
                        hasName("ASML HOLDING NV"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("NO0011082075"), hasTicker(null), //
                        hasName("HOEGH AUTOLINERS ASA"), //
                        hasCurrencyCode("NOK"))));

        assertThat(results, hasItem(security( //
                        hasIsin("NL0000289213"), hasTicker(null), //
                        hasName("WERELDHAVE NV"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("BE0003816338"), hasTicker(null), //
                        hasName("CMB TECH NV"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("NL0000360618"), hasTicker(null), //
                        hasName("SBM OFFSHORE NV"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("NL0010558797"), hasTicker(null), //
                        hasName("OCI NV"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("NL0000303709"), hasTicker(null), //
                        hasName("AEGON"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("FR0010478248"), hasTicker(null), //
                        hasName("ATARI - TD"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("FR0014008D33"), hasTicker(null), //
                        hasName("ATARI - NON TRADEABLE"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("GB00BP6MXD84"), hasTicker(null), //
                        hasName("SHELL PLC"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("GB00B03MLX29"), hasTicker(null), //
                        hasName("ROYAL DUTCH SHELLA"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("NL0009739416"), hasTicker(null), //
                        hasName("POSTNL NV"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("NL00150003E1"), hasTicker(null), //
                        hasName("FUGRO NV CLASS C"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("NL00150004A7"), hasTicker(null), //
                        hasName("FUGRO"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("NL00150001Y3"), hasTicker(null), //
                        hasName("FUGRO RIGHTS"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("NL0000352565"), hasTicker(null), //
                        hasName("FUGRO"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("BE0003818359"), hasTicker(null), //
                        hasName("GALAPAGOS NV"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("JE00B1VS3333")), //
                        hasDate("2026-02-04T10:32"), hasShares(30.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2054.28), hasGrossValue("EUR", 2051.28), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.00))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("US17888H1032")), //
                        hasDate("2026-01-30T00:00"), hasShares(40.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 928.84), hasGrossValue("EUR", 928.84), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        hasForexGrossValue("USD", 1095.20))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("US78454L1008")), //
                        hasDate("2026-01-30T00:00"), hasShares(58.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 928.84), hasGrossValue("EUR", 928.84), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        hasForexGrossValue("USD", 1095.20))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("US5322751042")), //
                        hasDate("2026-01-16T21:59"), hasShares(130.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 518.81), hasGrossValue("EUR", 515.52), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", (1.29 + 2.00)), //
                        hasForexGrossValue("USD", 598.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("US7731211089")), //
                        hasDate("2026-01-07T21:28"), hasShares(6.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 432.52), hasGrossValue("EUR", 429.45), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", (1.07 + 2.00)), //
                        hasForexGrossValue("USD", 501.63))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("CA29872L2066")), //
                        hasDate("2026-01-07T21:26"), hasShares(1500.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 357.91), hasGrossValue("EUR", 357.02), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.89), //
                        hasForexGrossValue("CAD", 577.50))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("CA29872L2066")), //
                        hasDate("2026-01-07T21:26"), hasShares(2000.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 477.21), hasGrossValue("EUR", 476.02), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 1.19), //
                        hasForexGrossValue("CAD", 770.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("CA29872L2066")), //
                        hasDate("2026-01-07T21:26"), hasShares(500.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 121.30), hasGrossValue("EUR", 119.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", (0.30 + 2.00)), //
                        hasForexGrossValue("CAD", 192.50))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("US88262P1021")), //
                        hasDate("2025-12-23T00:00"), hasShares(6.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1542.46), hasGrossValue("EUR", 1542.46), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        hasForexGrossValue("USD", 1816.80))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("US88262P1021")), //
                        hasDate("2025-12-23T00:00"), hasShares(2.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1542.46), hasGrossValue("EUR", 1542.46), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00), //
                        hasForexGrossValue("USD", 1816.80))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("AN8068571086")), //
                        hasDate("2025-12-16T15:30"), hasShares(65.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2131.15), hasGrossValue("EUR", 2123.84), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", (5.31 + 2.00)), //
                        hasForexGrossValue("USD", 2502.50))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("BMG9460G1015")), //
                        hasDate("2025-12-08T15:30"), hasShares(40.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2037.47), hasGrossValue("EUR", 2030.39), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", (5.08 + 2.00)), //
                        hasForexGrossValue("USD", 2365.60))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("NL0000009082")), //
                        hasDate("2025-11-06T15:13"), hasShares(600.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2289.00), hasGrossValue("EUR", 2286.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("NL0000009082")), //
                        hasDate("2025-11-06T15:12"), hasShares(100.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 384.10), hasGrossValue("EUR", 381.10), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.00))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("US76655K1034")), //
                        hasDate("2025-10-16T18:33"), hasShares(100.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 4131.98), hasGrossValue("EUR", 4144.34), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", (10.36 + 2.00)), //
                        hasForexGrossValue("USD", 4838.07))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("US18539C2044")), //
                        hasDate("2025-09-29T16:23"), hasShares(100.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2420.05), hasGrossValue("EUR", 2412.02), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", (6.03 + 2.00)), //
                        hasForexGrossValue("USD", 2830.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("US88262P1021")), //
                        hasDate("2025-06-23T20:30"), hasShares(2.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1827.58), hasGrossValue("EUR", 1821.03), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", (4.55 + 2.00)), //
                        hasForexGrossValue("USD", 2106.91))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("US76655K1034")), //
                        hasDate("2025-06-13T15:30"), hasShares(100.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1008.34), hasGrossValue("EUR", 1003.83), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", (2.51 + 2.00)), //
                        hasForexGrossValue("USD", 1155.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("FR0000124141")), //
                        hasDate("2025-04-28T13:45"), hasShares(75.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2383.90), hasGrossValue("EUR", 2379.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 4.90))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("US1270971039")), //
                        hasDate("2025-01-31T15:30"), hasShares(75.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2053.31), hasGrossValue("EUR", 2046.19), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", (5.12 + 2.00)), //
                        hasForexGrossValue("USD", 2122.50))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("US17888H1032")), //
                        hasDate("2025-01-30T21:32"), hasShares(40.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1999.13), hasGrossValue("EUR", 1992.15), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", (4.98 + 2.00)), //
                        hasForexGrossValue("USD", 2078.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("US20825C1045")), //
                        hasDate("2025-01-27T20:17"), hasShares(20.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1959.48), hasGrossValue("EUR", 1952.60), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", (4.88 + 2.00)), //
                        hasForexGrossValue("USD", 2046.90))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("US03674X1063")), //
                        hasDate("2025-01-27T19:04"), hasShares(70.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2439.03), hasGrossValue("EUR", 2447.15), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", (6.12 + 2.00)), //
                        hasForexGrossValue("USD", 2566.55))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("NL0000371243")), //
                        hasDate("2025-01-13T09:00"), hasShares(18.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 996.60), hasGrossValue("EUR", 993.60), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.00))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("BMG0112X1056")), //
                        hasDate("2024-12-17T11:08"), hasShares(211.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1184.09), hasGrossValue("EUR", 1187.09), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("US6516391066")), //
                        hasDate("2024-11-22T17:56"), hasShares(11.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 461.24), hasGrossValue("EUR", 458.09), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", (1.15 + 2.00)), //
                        hasForexGrossValue("USD", 476.19))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("US03674X1063")), //
                        hasDate("2024-11-22T15:31"), hasShares(70.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2219.20), hasGrossValue("EUR", 2211.67), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", (5.53 + 2.00)), //
                        hasForexGrossValue("USD", 2303.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("US6516391066")), //
                        hasDate("2024-11-22T15:30"), hasShares(13.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 546.43), hasGrossValue("EUR", 543.07), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", (1.36 + 2.00)), //
                        hasForexGrossValue("USD", 565.50))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("GB00BZ3CNK81")), //
                        hasDate("2024-10-15T21:32"), hasShares(75.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2073.86), hasGrossValue("EUR", 2066.69), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", (5.17 + 2.00)), //
                        hasForexGrossValue("USD", 2250.00))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("NL0011540547")), //
                        hasDate("2024-10-10T09:32"), hasShares(155.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2446.00), hasGrossValue("EUR", 2449.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("NL0000302636")), //
                        hasDate("2024-10-10T09:14"), hasShares(60.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2643.00), hasGrossValue("EUR", 2640.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("NL0010273215")), //
                        hasDate("2024-09-16T09:00"), hasShares(2.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1470.20), hasGrossValue("EUR", 1467.20), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("NO0011082075")), //
                        hasDate("2024-06-07T11:04"), hasShares(300.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2932.31), hasGrossValue("EUR", 2920.11), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", (7.30 + 4.90)), //
                        hasForexGrossValue("NOK", 33600.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("NL0000289213")), //
                        hasDate("2024-04-25T09:00"), hasShares(150.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2151.00), hasGrossValue("EUR", 2148.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.00))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("BE0003816338")), //
                        hasDate("2024-04-03T10:10"), hasShares(145.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2379.45), hasGrossValue("EUR", 2379.45), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("NL0000371243")), //
                        hasDate("2024-03-27T17:09"), hasShares(30.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2037.00), hasGrossValue("EUR", 2034.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("BE0003816338")), //
                        hasDate("2024-03-11T12:49"), hasShares(145.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2354.80), hasGrossValue("EUR", 2354.80), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("BE0003816338")), //
                        hasDate("2024-03-11T12:49"), hasShares(145.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2354.80), hasGrossValue("EUR", 2354.80), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("NL0000360618")), //
                        hasDate("2024-02-28T09:07"), hasShares(91.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1178.45), hasGrossValue("EUR", 1178.45), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("NL0000360618")), //
                        hasDate("2024-02-28T09:07"), hasShares(69.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 896.55), hasGrossValue("EUR", 893.55), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("NL0010558797")), //
                        hasDate("2023-11-01T09:52"), hasShares(100.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2188.00), hasGrossValue("EUR", 2185.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("BMG0112X1056")), //
                        hasDate("2023-10-02T08:17"), hasShares(211.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 966.80), hasGrossValue("EUR", 966.80), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("NL0000303709")), //
                        hasDate("2023-10-02T08:17"), hasShares(211.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 966.80), hasGrossValue("EUR", 966.80), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("NL0011540547")), //
                        hasDate("2023-09-22T10:05"), hasShares(155.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2033.50), hasGrossValue("EUR", 2030.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("NL0000303709")), //
                        hasDate("2023-08-18T11:13"), hasShares(211.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 999.98), hasGrossValue("EUR", 996.98), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.00))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("FR0010478248")), //
                        hasDate("2023-01-23T19:53"), hasShares(1765.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 333.06), hasGrossValue("EUR", 333.06), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("FR0010478248")), //
                        hasDate("2023-01-23T19:53"), hasShares(1765.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 333.06), hasGrossValue("EUR", 333.06), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("FR0010478248")), //
                        hasDate("2023-01-23T19:53"), hasShares(1765.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 333.06), hasGrossValue("EUR", 333.06), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("FR0010478248")), //
                        hasDate("2023-01-23T19:53"), hasShares(1765.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 333.06), hasGrossValue("EUR", 333.06), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("FR0014008D33")), //
                        hasDate("2022-04-05T09:52"), hasShares(353.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 57.89), hasGrossValue("EUR", 57.89), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("FR0010478248")), //
                        hasDate("2022-04-05T09:52"), hasShares(353.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 57.89), hasGrossValue("EUR", 57.89), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        sale( //
                                        hasSecurity(hasIsin("FR0014008D33")), //
                                        hasDate("2022-03-25T00:00"), hasShares(1412.00), //
                                        hasSource("Transactions_english02.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("FR0014008D33")), //
                        hasDate("2022-03-25T00:00"), hasShares(353.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 57.89), hasGrossValue("EUR", 57.89), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        purchase( //
                                        hasSecurity(hasIsin("FR0014008D33")), //
                                        hasDate("2022-03-11T00:00"), hasShares(1412.00), //
                                        hasSource("Transactions_english02.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("GB00BP6MXD84")), //
                        hasDate("2022-01-31T06:45"), hasShares(213.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 4855.34), hasGrossValue("EUR", 4855.34), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("GB00B03MLX29")), //
                        hasDate("2022-01-31T06:45"), hasShares(213.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 4855.34), hasGrossValue("EUR", 4855.34), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("NL0009739416")), //
                        hasDate("2021-12-16T13:43"), hasShares(300.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1094.33), hasGrossValue("EUR", 1092.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.33))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("NL00150003E1")), //
                        hasDate("2021-05-26T08:10"), hasShares(304.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2693.44), hasGrossValue("EUR", 2693.44), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("NL00150004A7")), //
                        hasDate("2021-05-26T08:10"), hasShares(304.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2693.44), hasGrossValue("EUR", 2693.44), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("FR0010478248")), //
                        hasDate("2021-05-13T09:53"), hasShares(1412.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1004.20), hasGrossValue("EUR", 999.70), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 4.50))));

        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        sale( //
                                        hasSecurity(hasIsin("NL00150001Y3")), //
                                        hasDate("2020-12-29T17:21"), hasShares(8.00), //
                                        hasSource("Transactions_english02.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("NL00150004A7")), //
                        hasDate("2020-12-21T00:00"), hasShares(304.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2333.50), hasGrossValue("EUR", 2333.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("NL0000352565")), //
                        hasDate("2020-12-21T00:00"), hasShares(608.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 2333.50), hasGrossValue("EUR", 2333.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("NL0000352565")), //
                        hasDate("2020-12-15T09:37"), hasShares(275.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 583.00), hasGrossValue("EUR", 583.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("NL0000352565")), //
                        hasDate("2020-12-15T09:37"), hasShares(275.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 583.00), hasGrossValue("EUR", 583.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        sale( //
                                        hasSecurity(hasIsin("NL00150001Y3")), //
                                        hasDate("2020-12-08T00:00"), hasShares(325.00), //
                                        hasSource("Transactions_english02.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("NL0000352565")), //
                        hasDate("2020-12-08T00:00"), hasShares(275.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 583.00), hasGrossValue("EUR", 583.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        purchase( //
                                        hasSecurity(hasIsin("NL00150001Y3")), //
                                        hasDate("2020-12-02T00:00"), hasShares(333.00), //
                                        hasSource("Transactions_english02.txt"), //
                                        hasNote(null), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("BE0003816338")), //
                        hasDate("2020-11-19T17:23"), hasShares(145.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 981.04), hasGrossValue("EUR", 978.75), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.29))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("GB00B03MLX29")), //
                        hasDate("2020-09-24T09:05"), hasShares(92.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1034.55), hasGrossValue("EUR", 1032.24), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.31))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("BE0003818359")), //
                        hasDate("2020-09-08T12:43"), hasShares(12.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1233.57), hasGrossValue("EUR", 1231.20), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.37))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("FR0010478248")), //
                        hasDate("2020-08-20T16:50"), hasShares(401.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 125.85), hasGrossValue("EUR", 125.91), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.06))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("FR0010478248")), //
                        hasDate("2020-08-20T16:50"), hasShares(3200.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1000.30), hasGrossValue("EUR", 1004.80), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 4.50))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("FR0010478248")), //
                        hasDate("2020-07-13T13:27"), hasShares(3600.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1003.50), hasGrossValue("EUR", 999.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 4.50))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("FR0010478248")), //
                        hasDate("2020-07-01T12:01"), hasShares(1.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 4.28), hasGrossValue("EUR", 0.28), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 4.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("NL0000352565")), //
                        hasDate("2020-05-06T15:40"), hasShares(333.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1074.58), hasGrossValue("EUR", 1072.26), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.32))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("GB00B03MLX29")), //
                        hasDate("2020-05-04T16:11"), hasShares(121.00), //
                        hasSource("Transactions_english02.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 1805.44), hasGrossValue("EUR", 1802.90), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.54))));
    }

    @Test
    public void testTransakcje01()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transakcje01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(55L));
        assertThat(countBuySell(results), is(187L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(242));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getName(), is("INVESCO EQQQ NASDAQ-100"));
        assertThat(security.getIsin(), is("IE0032077012"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check 1st buy transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-02-23T15:56")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(8)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2082.62))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2080.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.62))));

        // check 11th buy/sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(12).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-02-23T15:30")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(224)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1158.52))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1159.26))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.74))));
        Unit grossValueUnit1 = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit1.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(1411.20))));

        // check 37th buy/sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(38).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-02-23T09:42")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(128)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3108.15))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3113.71))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5.56))));
        Unit grossValueUnit2 = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit2.getForex(), is(Money.of("GBX", Values.Amount.factorize(269440.00))));
    }

    @Test
    public void testTransakcje02()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transakcje02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(95L));
        assertThat(countBuySell(results), is(723L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(818));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getName(), is("APHRIA INC. - COMMON SHARES"));
        assertThat(security.getIsin(), is("CA03765K1049"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check 1st buy transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-02-11T15:30")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(37.81))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(37.82))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.01))));
        Unit grossValueUnit1 = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit1.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(46.00))));

        // check 7th buy/sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(8).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-02-11T14:35")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(64)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2614.14))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2624.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(9.86))));

        // check 280th buy/sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(281).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-01-14T15:36")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2048)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(12245.87))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(12245.87))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        Unit grossValueUnit2 = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit2.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(14950.40))));

        // check 412th buy/sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(412).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-12-22T04:41")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(8000)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4865.22))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4868.15))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.93))));
        Unit grossValueUnit3 = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit3.getForex(), is(Money.of("HKD", Values.Amount.factorize(46240.00))));

        // check 505th buy/sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(506).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-12-14T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(51)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(791.47))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(791.47))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        Unit grossValueUnit4 = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit4.getForex(), is(Money.of("GBX", Values.Amount.factorize(72267.00))));
    }

    @Test
    public void testTransakcje03()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transakcje03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(108L));
        assertThat(countBuySell(results), is(1126L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(1234));

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getName(), is("AURORA CANNABIS"));
        assertThat(security.getIsin(), is("CA05156X8843"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check 1st buy transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-11-24T15:43")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(256)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1853.22))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1862.40))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(9.18))));

        // check 233th buy/sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(234).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-10-06T12:47")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(128)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1342.27))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1337.60))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.67))));

        // check 645th buy/sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(646).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-07-22T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(21)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2407.86))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2407.86))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        // check 850th buy/sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(851).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2020-02-28T09:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(8.93))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(8.93))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testEstrattoConto01()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "EstrattoConto01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(7L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(7));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-10-28T08:50")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(200.00))));
        assertThat(transaction.getSource(), is("EstrattoConto01.txt"));
        assertThat(transaction.getNote(), is("Deposito flatex"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(1)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-09-29T08:50")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(200.00))));
        assertThat(transaction.getSource(), is("EstrattoConto01.txt"));
        assertThat(transaction.getNote(), is("Deposito flatex"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(2)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-09-20T08:50")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(300.00))));
        assertThat(transaction.getSource(), is("EstrattoConto01.txt"));
        assertThat(transaction.getNote(), is("Deposito flatex"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(3)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-09-01T02:44")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.01))));
        assertThat(transaction.getSource(), is("EstrattoConto01.txt"));
        assertThat(transaction.getNote(), is("Deposito"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(4)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST_CHARGE));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-10-02T05:50")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.03))));
        assertThat(transaction.getSource(), is("EstrattoConto01.txt"));
        assertThat(transaction.getNote(), is("Flatex Interest"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(5)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-11-01T14:26")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.56))));
        assertThat(transaction.getSource(), is("EstrattoConto01.txt"));
        assertThat(transaction.getNote(), is("DEGIRO Costi di connessione 2021 (Xetra - XET)"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(6)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-10-02T10:46")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.24))));
        assertThat(transaction.getSource(), is("EstrattoConto01.txt"));
        assertThat(transaction.getNote(), is("DEGIRO Costi di connessione 2021 (Xetra - XET)"));
    }

    @Test
    public void testEstrattoConto02()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "EstrattoConto02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(49L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(51));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security1 = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security1.getIsin(), is("IE00B74DQ490"));
        assertThat(security1.getName(), is("ISHARES GLOB HIG YLD CORP BOND UCITS"));
        assertThat(security1.getCurrencyCode(), is(CurrencyUnit.USD));

        Security security2 = results.stream().filter(SecurityItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security2.getIsin(), is("US0378331005"));
        assertThat(security2.getName(), is("APPLE INC. - COMMON ST"));
        assertThat(security2.getCurrencyCode(), is(CurrencyUnit.USD));

        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-10-12T10:50")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(6000.00))));
        assertThat(transaction.getSource(), is("EstrattoConto02.txt"));
        assertThat(transaction.getNote(), is("Deposito flatex"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .skip(1).findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-10-11T10:50")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3000.00))));
        assertThat(transaction.getSource(), is("EstrattoConto02.txt"));
        assertThat(transaction.getNote(), is("Deposito flatex"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .skip(2).findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-08-09T10:51")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1000.00))));
        assertThat(transaction.getSource(), is("EstrattoConto02.txt"));
        assertThat(transaction.getNote(), is("Deposito flatex"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .skip(3).findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-08-05T11:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1000.00))));
        assertThat(transaction.getSource(), is("EstrattoConto02.txt"));
        assertThat(transaction.getNote(), is("Deposito flatex"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .skip(4).findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-07-12T10:50")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1750.00))));
        assertThat(transaction.getSource(), is("EstrattoConto02.txt"));
        assertThat(transaction.getNote(), is("Deposito flatex"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .skip(5).findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-05-13T08:50")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2400.00))));
        assertThat(transaction.getSource(), is("EstrattoConto02.txt"));
        assertThat(transaction.getNote(), is("Deposito flatex"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .skip(6).findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-02-25T09:20")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1700.00))));
        assertThat(transaction.getSource(), is("EstrattoConto02.txt"));
        assertThat(transaction.getNote(), is("Deposito flatex"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .skip(7).findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-01-11T09:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1500.00))));
        assertThat(transaction.getSource(), is("EstrattoConto02.txt"));
        assertThat(transaction.getNote(), is("Deposito flatex"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .skip(8).findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-11-12T08:50")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1300.00))));
        assertThat(transaction.getSource(), is("EstrattoConto02.txt"));
        assertThat(transaction.getNote(), is("Deposito flatex"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .skip(9).findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-11-10T08:50")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2200.00))));
        assertThat(transaction.getSource(), is("EstrattoConto02.txt"));
        assertThat(transaction.getNote(), is("Deposito flatex"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .skip(10).findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-09-29T08:50")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2500.00))));
        assertThat(transaction.getSource(), is("EstrattoConto02.txt"));
        assertThat(transaction.getNote(), is("Deposito flatex"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .skip(11).findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-08-12T08:50")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4600.00))));
        assertThat(transaction.getSource(), is("EstrattoConto02.txt"));
        assertThat(transaction.getNote(), is("Deposito flatex"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .skip(12).findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-08-06T08:50")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4032.40))));
        assertThat(transaction.getSource(), is("EstrattoConto02.txt"));
        assertThat(transaction.getNote(), is("Deposito flatex"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .skip(13).findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-05-06T08:50")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4253.00))));
        assertThat(transaction.getSource(), is("EstrattoConto02.txt"));
        assertThat(transaction.getNote(), is("Deposito flatex"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .skip(14).findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-04-01T08:50")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4253.00))));
        assertThat(transaction.getSource(), is("EstrattoConto02.txt"));
        assertThat(transaction.getNote(), is("Deposito flatex"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .skip(15).findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-02-23T08:50")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3319.00))));
        assertThat(transaction.getSource(), is("EstrattoConto02.txt"));
        assertThat(transaction.getNote(), is("Deposito flatex"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .skip(16).findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-01-21T08:50")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1197.00))));
        assertThat(transaction.getSource(), is("EstrattoConto02.txt"));
        assertThat(transaction.getNote(), is("Deposito flatex"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .skip(17).findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-01-20T08:50")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(7765.00))));
        assertThat(transaction.getSource(), is("EstrattoConto02.txt"));
        assertThat(transaction.getNote(), is("Deposito flatex"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .skip(18).findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-01-08T09:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.00))));
        assertThat(transaction.getSource(), is("EstrattoConto02.txt"));
        assertThat(transaction.getNote(), is("Deposito flatex"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .skip(19).findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-11-06T01:40")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2500.00))));
        assertThat(transaction.getSource(), is("EstrattoConto02.txt"));
        assertThat(transaction.getNote(), is("Deposito"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .skip(20).findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-09-24T01:24")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1000.00))));
        assertThat(transaction.getSource(), is("EstrattoConto02.txt"));
        assertThat(transaction.getNote(), is("Deposito"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .skip(21).findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-06-23T18:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(26600.00))));
        assertThat(transaction.getSource(), is("EstrattoConto02.txt"));
        assertThat(transaction.getNote(), is("Prelievo flatex"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .skip(22).findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-06-14T13:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1000.00))));
        assertThat(transaction.getSource(), is("EstrattoConto02.txt"));
        assertThat(transaction.getNote(), is("Prelievo flatex"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .skip(23).findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-05-06T15:50")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(563.94))));
        assertThat(transaction.getSource(), is("EstrattoConto02.txt"));
        assertThat(transaction.getNote(), is("Prelievo flatex"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .skip(24).findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-04-20T17:30")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1046.92))));
        assertThat(transaction.getSource(), is("EstrattoConto02.txt"));
        assertThat(transaction.getNote(), is("Prelievo flatex"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .skip(25).findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-04-06T18:30")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2100.00))));
        assertThat(transaction.getSource(), is("EstrattoConto02.txt"));
        assertThat(transaction.getNote(), is("Prelievo flatex"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .skip(26).findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-11-08T15:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(500.00))));
        assertThat(transaction.getSource(), is("EstrattoConto02.txt"));
        assertThat(transaction.getNote(), is("Prelievo flatex"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .skip(27).findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-04-20T17:01")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(650.00))));
        assertThat(transaction.getSource(), is("EstrattoConto02.txt"));
        assertThat(transaction.getNote(), is("Prelievo flatex"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .skip(28).findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-12-30T12:10")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1586.30))));
        assertThat(transaction.getSource(), is("EstrattoConto02.txt"));
        assertThat(transaction.getNote(), is("Prelievo flatex"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .skip(29).findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-12-10T17:20")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(25.23))));
        assertThat(transaction.getSource(), is("EstrattoConto02.txt"));
        assertThat(transaction.getNote(), is("Prelievo flatex"));

        // check dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(30)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-09-29T07:55")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0L)));
        assertThat(transaction.getSource(), is("EstrattoConto02.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(68.23))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(68.23))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(67.14))));

        // check dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(31)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-03-31T07:36")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0L)));
        assertThat(transaction.getSource(), is("EstrattoConto02.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(21.80))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(21.80))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(24.19))));

        // check dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(32)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-11-12T07:31")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0L)));
        assertThat(transaction.getSource(), is("EstrattoConto02.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.14))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.34))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.20))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(01.54))));

        // check dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(33)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-09-30T09:34")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0L)));
        assertThat(transaction.getSource(), is("EstrattoConto02.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(23.46))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(23.46))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(27.19))));

        // check dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(34)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-08-13T07:49")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0L)));
        assertThat(transaction.getSource(), is("EstrattoConto02.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.42))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.68))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.26))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(1.98))));

        // check dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(35)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-05-14T07:54")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0L)));
        assertThat(transaction.getSource(), is("EstrattoConto02.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.38))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.64))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.26))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(1.98))));

        // check dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(36)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-03-25T07:40")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0L)));
        assertThat(transaction.getSource(), is("EstrattoConto02.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(15.01))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(15.01))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(17.68))));

        // check dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(37)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-02-12T07:41")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0L)));
        assertThat(transaction.getSource(), is("EstrattoConto02.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.29))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.52))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.23))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(1.85))));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .collect(Collectors.toList()).get(38).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST_CHARGE));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-10-01T16:02")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.63))));
        assertThat(transaction.getSource(), is("EstrattoConto02.txt"));
        assertThat(transaction.getNote(), is("Flatex Interest"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .collect(Collectors.toList()).get(39).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST_CHARGE));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-07-01T10:20")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10.35))));
        assertThat(transaction.getSource(), is("EstrattoConto02.txt"));
        assertThat(transaction.getNote(), is("Flatex Interest"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .collect(Collectors.toList()).get(40).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST_CHARGE));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-04-02T02:40")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10.53))));
        assertThat(transaction.getSource(), is("EstrattoConto02.txt"));
        assertThat(transaction.getNote(), is("Flatex Interest"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .collect(Collectors.toList()).get(41).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST_CHARGE));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-12-30T22:20")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.67))));
        assertThat(transaction.getSource(), is("EstrattoConto02.txt"));
        assertThat(transaction.getNote(), is("Flatex Interest"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .collect(Collectors.toList()).get(42).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST_CHARGE));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-10-01T18:30")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.39))));
        assertThat(transaction.getSource(), is("EstrattoConto02.txt"));
        assertThat(transaction.getNote(), is("Flatex Interest"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .collect(Collectors.toList()).get(43).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST_CHARGE));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-07-02T01:10")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.55))));
        assertThat(transaction.getSource(), is("EstrattoConto02.txt"));
        assertThat(transaction.getNote(), is("Flatex Interest"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .collect(Collectors.toList()).get(44).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST_CHARGE));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-04-01T20:30")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.82))));
        assertThat(transaction.getSource(), is("EstrattoConto02.txt"));
        assertThat(transaction.getNote(), is("Flatex Interest"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .collect(Collectors.toList()).get(45).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST_CHARGE));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-12-31T07:10")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.11))));
        assertThat(transaction.getSource(), is("EstrattoConto02.txt"));
        assertThat(transaction.getNote(), is("Flatex Interest"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .collect(Collectors.toList()).get(46).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST_CHARGE));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-11-02T16:06")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.01))));
        assertThat(transaction.getSource(), is("EstrattoConto02.txt"));
        assertThat(transaction.getNote(), is("Interesse"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .collect(Collectors.toList()).get(47).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-01-31T13:15")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.50))));
        assertThat(transaction.getSource(), is("EstrattoConto02.txt"));
        assertThat(transaction.getNote(), is("DEGIRO Costi di connessione 2021 (New York Stock Exchange - NSY)"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .collect(Collectors.toList()).get(48).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-01-31T13:15")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.50))));
        assertThat(transaction.getSource(), is("EstrattoConto02.txt"));
        assertThat(transaction.getNote(), is("DEGIRO Costi di connessione 2021 (NASDAQ - NDQ)"));
    }

    @Test
    public void testEstrattoConto03()
    {
        Security security = new Security("ISHARES GLOB HIG YLD CORP BOND UCITS", CurrencyUnit.EUR);
        security.setIsin("IE00B74DQ490");

        Client client = new Client();
        client.addSecurity(security);

        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "EstrattoConto03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-09-29T07:55")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0L)));
        assertThat(transaction.getSource(), is("EstrattoConto03.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(68.23))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(68.23))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(67.14))));
    }

    @Test
    public void testOperazioni01()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Operazioni01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(2L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getName(), is("VANGUARD FTSE ALL- WORLD UCITS ETF - (USD) ACCUMULATING"));
        assertThat(security.getIsin(), is("IE00BK5BQT80"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check 1st buy transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-10-04T09:04")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2)));
        assertThat(entry.getSource(), is("Operazioni01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(191.22))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(191.22))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        // check 2th buy/sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-09-24T09:04")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(3)));
        assertThat(entry.getSource(), is("Operazioni01.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(291.75))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(291.75))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende01()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("GB00B03MLX29"));
        assertThat(security.getName(), is("ROYAL DUTCH SHELLA"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-06-22T00:00")));
        assertThat(transaction.getExDate(), is(LocalDateTime.parse("2020-05-14T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(20)));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.41))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.84))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.43))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testTransactions_french01()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transactions_french01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(62L));
        assertThat(countBuySell(results), is(246L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(308));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check 1st security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getName(), is("THE TRADE DESK CL A"));
        assertThat(security.getIsin(), is("US88339J1051"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check 1st buy transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-02-09T16:44")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(1)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(68.87))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(68.37))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.50))));

        Unit grossValueUnit = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(78.21))));
    }
    
   
    
    @Test
    public void testTransactions_french02()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transactions_french02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(5L));
        assertThat(countBuySell(results), is(6L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(11));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check 1st security
        
        assertThat(results, hasItem(security( 
                        hasIsin("US02319V1035"), 
                        hasName("ADR ON AMBEV"), 
                        hasCurrencyCode("USD"))));
        assertThat(results, hasItem(security( 
                        hasIsin("IE00B1FZSF77"), 
                        hasName("ISHARES PROP US"), 
                        hasCurrencyCode("EUR"))));
        assertThat(results, hasItem(security( 
                        hasIsin("US91324P1021"), 
                        hasName("UNITEDHEALTH GROUP INC"), 
                        hasCurrencyCode("USD"))));
        assertThat(results, hasItem(security( 
                        hasIsin("US0367521038"), 
                        hasName("ELEVANCE HEALTH INC"), 
                        hasCurrencyCode("USD"))));
        assertThat(results, hasItem(security( 
                        hasIsin("US5951121038"), 
                        hasName("MICRON TECHNOLOGY INC"), 
                        hasCurrencyCode("USD"))));
                      
        // check 1st buy transaction
        
        assertThat(results, hasItem(sale( 
                        hasDate("2024-04-16T21:56"), hasShares(150.00), 
                        hasSource("Transactions_french02.txt"), 
                        hasAmount("EUR", 317.95), hasGrossValue("EUR", 319.95), 
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.00), 
                        hasForexGrossValue("USD", 339.75))));
        assertThat(results, hasItem(sale( 
                        hasDate("2024-03-26T10:27"), hasShares(4.00), 
                        hasSource("Transactions_french02.txt"), 
                        hasAmount("EUR", 101.00), hasGrossValue("EUR", 101.00), 
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
        assertThat(results, hasItem(sale( 
                        hasDate("2024-03-26T10:13"), hasShares(12.00), 
                        hasSource("Transactions_french02.txt"), 
                        hasAmount("EUR", 300.00), hasGrossValue("EUR", 303.00), 
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.00))));
        assertThat(results, hasItem(purchase( 
                        hasDate("2024-01-22T16:51"), hasShares(1.00), 
                        hasSource("Transactions_french02.txt"), 
                        hasAmount("EUR", 468.94), hasGrossValue("EUR", 466.94), 
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.00), 
                        hasForexGrossValue("USD", 508.40))));
        assertThat(results, hasItem(sale( 
                        hasDate("2024-01-22T16:31"), hasShares(1.00), 
                        hasSource("Transactions_french02.txt"), 
                        hasAmount("EUR", 428.59), hasGrossValue("EUR", 430.59), 
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.00), 
                        hasForexGrossValue("USD", 469.00))));
        assertThat(results, hasItem(sale( 
                        hasDate("2024-01-22T16:30"), hasShares(4.00), 
                        hasSource("Transactions_french02.txt"), 
                        hasAmount("EUR", 322.22), hasGrossValue("EUR", 324.22), 
                        hasTaxes("EUR", 0.00), hasFees("EUR", 2.00), 
                        hasForexGrossValue("USD", 353.24))));
                
    }

    @Test
    public void testTransactions_french03()
    {
        var extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transactions_french03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(64L));
        assertThat(countBuySell(results), is(213L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(2L));
        assertThat(results.size(), is(279));
        new AssertImportActions().check(results, "CHF");

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("NL0010273215"), hasTicker(null), //
                        hasName("ASML HOLDING NV"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US21873S1087"), hasTicker(null), //
                        hasName("COREWEAVE INC"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US7655041058"), hasTicker(null), //
                        hasName("RICHTECH ROBOTICS INC"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US36317J2096"), hasTicker(null), //
                        hasName("GALAXY DIGITAL INC"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("FI4000297767"), hasTicker(null), //
                        hasName("NORDEA BANK ABP"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US94106L1098"), hasTicker(null), //
                        hasName("WASTE MANAGEMENT INC."), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("FR0013341781"), hasTicker(null), //
                        hasName("2CRSI PROMESSES"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("DK0062498333"), hasTicker(null), //
                        hasName("NOVO-NORDISK AS"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("IE0002PG6CA6"), hasTicker(null), //
                        hasName("VANECK RARE EARTH AND STRATEGIC METALS UCITS ETF"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("NL0000226223"), hasTicker(null), //
                        hasName("STMICROELECTRONICS"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("FR0000121014"), hasTicker(null), //
                        hasName("LVMH MOET HENNESSY LOUIS VUITTON SE"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("LU1829221024"), hasTicker(null), //
                        hasName("AMUNDI NASDAQ-100 II UCITS ETF ACC"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US6098391054"), hasTicker(null), //
                        hasName("MONOLITHIC POWER SYSTE"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("IE000I8KRLL9"), hasTicker(null), //
                        hasName("ISHARES MSCI GLOBAL SEMICONDUCTORS UCITS ETF"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US8716071076"), hasTicker(null), //
                        hasName("SYNOPSYS INC. - COMMO"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("FR0000120321"), hasTicker(null), //
                        hasName("L'OREAL"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("IE00B0M63516"), hasTicker(null), //
                        hasName("ISHARES MSCI BRAZIL UCITS ETF USD (DIST)"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("SE0000202624"), hasTicker(null), //
                        hasName("GETINGE AB"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US2172041061"), hasTicker(null), //
                        hasName("COPART INC. - COMMON"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US0404132054"), hasTicker(null), //
                        hasName("ARISTA NETWORKS  INC. COMMON STOCK"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("IE00BTJRMP35"), hasTicker(null), //
                        hasName("XTRACKERS MSCI EMERGING MARKETS UCITS ETF 1C"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("LU1841731745"), hasTicker(null), //
                        hasName("AMUNDI MSCI CHINA UCITS ETF ACC"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("FR0013269123"), hasTicker(null), //
                        hasName("RUBIS"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US78573M1045"), hasTicker(null), //
                        hasName("SABRE CORPORATION - CO"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("IE00BZCQB185"), hasTicker(null), //
                        hasName("ISHARES MSCI INDIA UCITS ETF USD ACC (EUR)"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("FR0000121485"), hasTicker(null), //
                        hasName("KERING"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("CH0012032048"), hasTicker(null), //
                        hasName("ROCHE HOLDING AG"), //
                        hasCurrencyCode("CHF"))));

        assertThat(results, hasItem(security( //
                        hasIsin("SE0012673267"), hasTicker(null), //
                        hasName("EVOLUTION AB (PUBL)"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US7561091049"), hasTicker(null), //
                        hasName("REALTY INCOME CORPORAT"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("FR0000031775"), hasTicker(null), //
                        hasName("VICAT SA"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US1912161007"), hasTicker(null), //
                        hasName("COCA-COLA COMPANY (THE"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US02079K3059"), hasTicker(null), //
                        hasName("ALPHABET INC. - CLASS A"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("FR0000053381"), hasTicker(null), //
                        hasName("DERICHEBOURG SA"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("IE00B3WJKG14"), hasTicker(null), //
                        hasName("ISHARES S&P 500 INF TECH SECTOR UCITS ETF USD(ACC)"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US88160R1014"), hasTicker(null), //
                        hasName("TESLA MOTORS INC. - C"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US2546871060"), hasTicker(null), //
                        hasName("WALT DISNEY COMPANY (T"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US5949181045"), hasTicker(null), //
                        hasName("MICROSOFT CORPORATION"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US0231351067"), hasTicker(null), //
                        hasName("AMAZON.COM INC. - COM"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("DE000ZAL1111"), hasTicker(null), //
                        hasName("ZALANDO SE"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US0079031078"), hasTicker(null), //
                        hasName("ADVANCED MICRO DEVICES"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("FR0014008VX5"), hasTicker(null), //
                        hasName("EUROAPI SA"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US70450Y1038"), hasTicker(null), //
                        hasName("PAYPAL HOLDINGS INC."), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("FR0000120859"), hasTicker(null), //
                        hasName("IMERYS"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US5801351017"), hasTicker(null), //
                        hasName("MCDONALDS CORPORATION"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("FR0000051732"), hasTicker(null), //
                        hasName("ATOS SE"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("FR0011052257"), hasTicker(null), //
                        hasName("GLOBAL BIOENERGIES"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("FR0011726835"), hasTicker(null), //
                        hasName("GTT"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US30303M1027"), hasTicker(null), //
                        hasName("META PLATFORMS INC"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US8740391003"), hasTicker(null), //
                        hasName("ADR ON TAIWAN SEMICONDUCTOR MANUFACTURING CO"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US88579Y1010"), hasTicker(null), //
                        hasName("3M COMPANY COMMON STOC"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("FR0000121964"), hasTicker(null), //
                        hasName("KLEPIERRE"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("CH0210483332"), hasTicker(null), //
                        hasName("COMPAGNIE FINANCIERE RICHEMONT SA"), //
                        hasCurrencyCode("CHF"))));

        assertThat(results, hasItem(security( //
                        hasIsin("CH0244767585"), hasTicker(null), //
                        hasName("UBS GROUP AG REGISTERE"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("IE00BTN1Y115"), hasTicker(null), //
                        hasName("MEDTRONIC PLC. ORDINAR"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("NL00150001Q9"), hasTicker(null), //
                        hasName("STELLANTIS"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US2254011081"), hasTicker(null), //
                        hasName("CREDIT SUISSE GROUP AM"), //
                        hasCurrencyCode("USD"))));

        assertThat(results, hasItem(security( //
                        hasIsin("FR001400GG91"), hasTicker(null), //
                        hasName("GLOBAL BIOENERGIES - NON TRADEABLE"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("FR0010112524"), hasTicker(null), //
                        hasName("NEXITY"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("FR0000124141"), hasTicker(null), //
                        hasName("VEOLIA ENVIRON."), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("FR0004007813"), hasTicker(null), //
                        hasName("KAUFMAN ET BROAD"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("FR0000121972"), hasTicker(null), //
                        hasName("SCHNEIDER ELECTRIC"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("FR0000045072"), hasTicker(null), //
                        hasName("CREDIT AGRICOLE"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("FR0010220475"), hasTicker(null), //
                        hasName("ALSTOM"), //
                        hasCurrencyCode("EUR"))));

        assertThat(results, hasItem(security( //
                        hasIsin("US0378331005"), hasTicker(null), //
                        hasName("APPLE INC. - COMMON ST"), //
                        hasCurrencyCode("USD"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("NL0010273215")), //
                        hasDate("2025-12-05T15:22"), hasShares(1.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 894.15), hasGrossValue("CHF", 901.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (2.25 + 4.60)), //
                        hasForexGrossValue("EUR", 960.40))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("US21873S1087")), //
                        hasDate("2025-12-03T19:06"), hasShares(10.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 621.51), hasGrossValue("CHF", 624.94), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.56 + 1.87)), //
                        hasForexGrossValue("USD", 780.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("US21873S1087")), //
                        hasDate("2025-12-02T17:11"), hasShares(10.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 644.23), hasGrossValue("CHF", 640.75), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.61 + 1.87)), //
                        hasForexGrossValue("USD", 798.49))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("US7655041058")), //
                        hasDate("2025-12-02T17:00"), hasShares(100.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 283.39), hasGrossValue("CHF", 280.82), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.70 + 1.87)), //
                        hasForexGrossValue("USD", 350.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("US36317J2096")), //
                        hasDate("2025-12-02T16:57"), hasShares(20.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 412.15), hasGrossValue("CHF", 409.25), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.03 + 1.87)), //
                        hasForexGrossValue("USD", 510.00))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("NL0010273215")), //
                        hasDate("2025-12-02T15:30"), hasShares(1.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 872.91), hasGrossValue("CHF", 879.69), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (2.19 + 4.59)), //
                        hasForexGrossValue("EUR", 939.00))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("FI4000297767")), //
                        hasDate("2025-12-01T19:04"), hasShares(100.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 1431.30), hasGrossValue("CHF", 1438.54), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (3.59 + 3.65)), //
                        hasForexGrossValue("EUR", 1536.50))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("US94106L1098")), //
                        hasDate("2025-11-17T15:30"), hasShares(3.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 500.62), hasGrossValue("CHF", 497.52), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.25 + 1.85)), //
                        hasForexGrossValue("USD", 627.00))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("FR0013341781")), //
                        hasDate("2025-11-13T17:29"), hasShares(50.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 625.40), hasGrossValue("CHF", 631.50), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.57 + 4.53)), //
                        hasForexGrossValue("EUR", 683.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("DK0062498333")), //
                        hasDate("2025-11-04T09:33"), hasShares(20.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 786.61), hasGrossValue("CHF", 780.08), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.96 + 4.57)), //
                        hasForexGrossValue("EUR", 840.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("IE0002PG6CA6")), //
                        hasDate("2025-11-04T09:04"), hasShares(50.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 510.14), hasGrossValue("CHF", 506.07), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.27 + 2.80)), //
                        hasForexGrossValue("EUR", 545.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("FR0013341781")), //
                        hasDate("2025-11-03T17:17"), hasShares(50.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 470.02), hasGrossValue("CHF", 464.29), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.16 + 4.57)), //
                        hasForexGrossValue("EUR", 500.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("US7655041058")), //
                        hasDate("2025-10-20T19:41"), hasShares(100.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 433.69), hasGrossValue("CHF", 430.76), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.08 + 1.85)), //
                        hasForexGrossValue("USD", 545.00))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("NL0000226223")), //
                        hasDate("2025-10-20T16:45"), hasShares(40.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 954.88), hasGrossValue("CHF", 961.81), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (2.40 + 4.53)), //
                        hasForexGrossValue("EUR", 1040.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("IE0002PG6CA6")), //
                        hasDate("2025-10-17T09:15"), hasShares(50.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 544.10), hasGrossValue("CHF", 539.97), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.35 + 2.78)), //
                        hasForexGrossValue("EUR", 586.60))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("FR0000121014")), //
                        hasDate("2025-10-15T09:13"), hasShares(1.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 555.53), hasGrossValue("CHF", 561.50), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.40 + 4.57)), //
                        hasForexGrossValue("EUR", 602.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("US7655041058")), //
                        hasDate("2025-10-14T15:38"), hasShares(100.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 479.61), hasGrossValue("CHF", 476.56), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.19 + 1.86)), //
                        hasForexGrossValue("USD", 595.00))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("LU1829221024")), //
                        hasDate("2025-10-07T13:52"), hasShares(6.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 483.24), hasGrossValue("CHF", 487.26), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.22 + 2.80)), //
                        hasForexGrossValue("EUR", 522.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("US36317J2096")), //
                        hasDate("2025-10-03T19:50"), hasShares(20.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 574.66), hasGrossValue("CHF", 571.36), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.43 + 1.87)), //
                        hasForexGrossValue("USD", 720.00))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("US6098391054")), //
                        hasDate("2025-10-03T15:57"), hasShares(1.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 754.83), hasGrossValue("CHF", 758.59), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.89 + 1.87)), //
                        hasForexGrossValue("USD", 950.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("FR0013341781")), //
                        hasDate("2025-10-03T15:52"), hasShares(50.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 649.61), hasGrossValue("CHF", 643.41), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.61 + 4.59)), //
                        hasForexGrossValue("EUR", 690.00))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("NL0010273215")), //
                        hasDate("2025-10-01T15:55"), hasShares(1.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 772.37), hasGrossValue("CHF", 778.91), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.94 + 4.60)), //
                        hasForexGrossValue("EUR", 830.00))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("IE000I8KRLL9")), //
                        hasDate("2025-10-01T15:44"), hasShares(40.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 304.39), hasGrossValue("CHF", 307.98), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.77 + 2.82)), //
                        hasForexGrossValue("EUR", 328.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("US8716071076")), //
                        hasDate("2025-09-24T20:54"), hasShares(1.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 373.28), hasGrossValue("CHF", 370.48), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.93 + 1.87)), //
                        hasForexGrossValue("USD", 466.90))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("FR0000120321")), //
                        hasDate("2025-09-24T10:34"), hasShares(2.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 696.05), hasGrossValue("CHF", 689.73), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.73 + 4.59)), //
                        hasForexGrossValue("EUR", 740.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("FR0000120321")), //
                        hasDate("2025-09-24T10:34"), hasShares(2.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 696.05), hasGrossValue("CHF", 689.73), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.73 + 4.59)), //
                        hasForexGrossValue("EUR", 740.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("US8716071076")), //
                        hasDate("2025-09-23T18:57"), hasShares(1.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 393.81), hasGrossValue("CHF", 390.96), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.98 + 1.87)), //
                        hasForexGrossValue("USD", 495.00))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("IE00B0M63516")), //
                        hasDate("2025-09-23T15:07"), hasShares(20.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 385.09), hasGrossValue("CHF", 388.87), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.97 + 2.81)), //
                        hasForexGrossValue("EUR", 415.00))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("SE0000202624")), //
                        hasDate("2025-09-22T09:03"), hasShares(15.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 264.14), hasGrossValue("CHF", 268.46), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.67 + 3.65)), //
                        hasForexGrossValue("EUR", 286.50))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("DK0062498333")), //
                        hasDate("2025-08-13T14:00"), hasShares(10.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 409.30), hasGrossValue("CHF", 403.67), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.01 + 4.62)), //
                        hasForexGrossValue("EUR", 430.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("DK0062498333")), //
                        hasDate("2025-07-30T14:30"), hasShares(30.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 1249.05), hasGrossValue("CHF", 1241.37), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (3.11 + 4.57)), //
                        hasForexGrossValue("EUR", 1338.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("US2172041061")), //
                        hasDate("2025-07-17T18:16"), hasShares(25.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 928.90), hasGrossValue("CHF", 924.71), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (2.32 + 1.87)), //
                        hasForexGrossValue("USD", 1152.50))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("US0404132054")), //
                        hasDate("2025-07-17T15:30"), hasShares(10.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 875.37), hasGrossValue("CHF", 879.43), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (2.19 + 1.87)), //
                        hasForexGrossValue("USD", 1090.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("DK0062498333")), //
                        hasDate("2025-06-27T15:30"), hasShares(10.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 553.87), hasGrossValue("CHF", 547.90), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.37 + 4.60)), //
                        hasForexGrossValue("EUR", 586.70))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("NL0000226223")), //
                        hasDate("2025-06-26T17:38"), hasShares(46.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 1107.25), hasGrossValue("CHF", 1114.63), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (2.78 + 4.60)), //
                        hasForexGrossValue("EUR", 1187.49))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("US2172041061")), //
                        hasDate("2025-06-13T20:03"), hasShares(25.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 995.76), hasGrossValue("CHF", 991.40), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (2.48 + 1.88)), //
                        hasForexGrossValue("USD", 1225.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("FR0000121014")), //
                        hasDate("2025-06-06T13:57"), hasShares(2.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 889.81), hasGrossValue("CHF", 882.99), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (2.21 + 4.61)), //
                        hasForexGrossValue("EUR", 943.10))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("DK0062498333")), //
                        hasDate("2025-05-13T17:19"), hasShares(10.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 554.69), hasGrossValue("CHF", 548.69), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.38 + 4.62)), //
                        hasForexGrossValue("EUR", 585.30))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("NL0010273215")), //
                        hasDate("2025-04-30T15:14"), hasShares(2.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 1081.58), hasGrossValue("CHF", 1074.29), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (2.69 + 4.60)), //
                        hasForexGrossValue("EUR", 1150.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("IE00BTJRMP35")), //
                        hasDate("2025-04-22T16:30"), hasShares(5.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 235.25), hasGrossValue("CHF", 231.86), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.58 + 2.81)), //
                        hasForexGrossValue("EUR", 248.92))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("LU1841731745")), //
                        hasDate("2025-04-22T14:39"), hasShares(20.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 304.28), hasGrossValue("CHF", 305.04), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.76), //
                        hasForexGrossValue("EUR", 326.48))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("LU1841731745")), //
                        hasDate("2025-04-22T14:39"), hasShares(20.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 301.67), hasGrossValue("CHF", 305.23), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.76 + 2.80)), //
                        hasForexGrossValue("EUR", 326.68))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("DK0062498333")), //
                        hasDate("2025-04-16T11:47"), hasShares(15.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 790.58), hasGrossValue("CHF", 784.05), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.97 + 4.56)), //
                        hasForexGrossValue("EUR", 846.45))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("NL0000226223")), //
                        hasDate("2025-04-07T16:30"), hasShares(36.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 580.64), hasGrossValue("CHF", 574.58), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.44 + 4.62)), //
                        hasForexGrossValue("EUR", 612.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("DK0062498333")), //
                        hasDate("2025-04-01T16:21"), hasShares(9.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 552.81), hasGrossValue("CHF", 546.76), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.37 + 4.68)), //
                        hasForexGrossValue("EUR", 575.10))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("LU1829221024")), //
                        hasDate("2025-03-13T16:36"), hasShares(6.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 417.79), hasGrossValue("CHF", 413.86), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.04 + 2.89)), //
                        hasForexGrossValue("EUR", 432.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("DK0062498333")), //
                        hasDate("2025-03-13T11:38"), hasShares(8.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 553.49), hasGrossValue("CHF", 547.41), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.37 + 4.71)), //
                        hasForexGrossValue("EUR", 572.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("NL0010273215")), //
                        hasDate("2025-02-28T15:16"), hasShares(1.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 642.54), hasGrossValue("CHF", 636.33), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.59 + 4.62)), //
                        hasForexGrossValue("EUR", 679.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("US0404132054")), //
                        hasDate("2025-02-26T20:16"), hasShares(10.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 858.83), hasGrossValue("CHF", 854.81), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (2.14 + 1.88)), //
                        hasForexGrossValue("USD", 958.80))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("FR0013269123")), //
                        hasDate("2025-02-13T09:01"), hasShares(40.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 977.16), hasGrossValue("CHF", 984.27), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (2.45 + 4.66)), //
                        hasForexGrossValue("EUR", 1036.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("US6098391054")), //
                        hasDate("2025-02-06T18:11"), hasShares(1.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 590.30), hasGrossValue("CHF", 586.95), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.47 + 1.88)), //
                        hasForexGrossValue("USD", 650.00))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("US78573M1045")), //
                        hasDate("2025-01-31T15:30"), hasShares(50.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 152.84), hasGrossValue("CHF", 155.12), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.39 + 1.89)), //
                        hasForexGrossValue("USD", 170.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("DK0062498333")), //
                        hasDate("2025-01-17T16:31"), hasShares(2.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 150.64), hasGrossValue("CHF", 145.65), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.37 + 4.62)), //
                        hasForexGrossValue("EUR", 155.26))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("IE00BZCQB185")), //
                        hasDate("2025-01-17T10:32"), hasShares(50.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 411.66), hasGrossValue("CHF", 409.69), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.03 + 0.94)), //
                        hasForexGrossValue("EUR", 438.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("FR0000121485")), //
                        hasDate("2025-01-17T09:01"), hasShares(2.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 450.07), hasGrossValue("CHF", 444.35), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.11 + 4.61)), //
                        hasForexGrossValue("EUR", 475.00))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("CH0012032048")), //
                        hasDate("2025-01-14T09:27"), hasShares(1.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 262.00), hasGrossValue("CHF", 268.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 6.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("SE0012673267")), //
                        hasDate("2024-12-23T14:22"), hasShares(2.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 134.81), hasGrossValue("CHF", 130.83), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.33 + 3.65)), //
                        hasForexGrossValue("EUR", 140.52))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("SE0012673267")), //
                        hasDate("2024-12-09T15:31"), hasShares(5.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 395.71), hasGrossValue("CHF", 391.10), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.98 + 3.63)), //
                        hasForexGrossValue("EUR", 422.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("US7561091049")), //
                        hasDate("2024-12-09T15:30"), hasShares(10.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 493.73), hasGrossValue("CHF", 490.64), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.23 + 1.86)), //
                        hasForexGrossValue("USD", 560.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("SE0012673267")), //
                        hasDate("2024-11-27T09:02"), hasShares(5.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 400.99), hasGrossValue("CHF", 396.37), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.99 + 3.63)), //
                        hasForexGrossValue("EUR", 428.10))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("DK0062498333")), //
                        hasDate("2024-11-26T11:30"), hasShares(6.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 570.92), hasGrossValue("CHF", 576.93), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.44 + 4.57)), //
                        hasForexGrossValue("EUR", 618.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("FR0000120321")), //
                        hasDate("2024-11-25T13:02"), hasShares(3.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 928.45), hasGrossValue("CHF", 921.56), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (2.31 + 4.58)), //
                        hasForexGrossValue("EUR", 990.00))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("FR0000031775")), //
                        hasDate("2024-11-07T15:12"), hasShares(20.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 683.84), hasGrossValue("CHF", 690.19), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.72 + 4.63)), //
                        hasForexGrossValue("EUR", 730.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("DK0062498333")), //
                        hasDate("2024-11-07T14:30"), hasShares(4.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 381.00), hasGrossValue("CHF", 375.43), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.94 + 4.63)), //
                        hasForexGrossValue("EUR", 399.20))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("FR0000121485")), //
                        hasDate("2024-11-01T14:56"), hasShares(1.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 220.73), hasGrossValue("CHF", 215.55), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.54 + 4.64)), //
                        hasForexGrossValue("EUR", 229.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("FR0000120321")), //
                        hasDate("2024-10-30T09:32"), hasShares(2.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 660.32), hasGrossValue("CHF", 654.07), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.64 + 4.61)), //
                        hasForexGrossValue("EUR", 698.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("SE0012673267")), //
                        hasDate("2024-10-29T13:45"), hasShares(5.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 420.63), hasGrossValue("CHF", 415.93), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.04 + 3.66)), //
                        hasForexGrossValue("EUR", 445.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("FI4000297767")), //
                        hasDate("2024-10-21T16:23"), hasShares(40.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 414.76), hasGrossValue("CHF", 410.06), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.03 + 3.67)), //
                        hasForexGrossValue("EUR", 438.40))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("NL0010273215")), //
                        hasDate("2024-10-16T10:19"), hasShares(1.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 600.56), hasGrossValue("CHF", 594.46), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.49 + 4.61)), //
                        hasForexGrossValue("EUR", 635.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("FR0000120321")), //
                        hasDate("2024-10-16T09:04"), hasShares(2.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 654.22), hasGrossValue("CHF", 647.99), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.62 + 4.61)), //
                        hasForexGrossValue("EUR", 692.40))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("FR0000121014")), //
                        hasDate("2024-10-16T09:00"), hasShares(2.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 1108.11), hasGrossValue("CHF", 1100.74), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (2.76 + 4.61)), //
                        hasForexGrossValue("EUR", 1175.80))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("NL0010273215")), //
                        hasDate("2024-10-15T17:13"), hasShares(1.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 652.88), hasGrossValue("CHF", 646.64), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.62 + 4.62)), //
                        hasForexGrossValue("EUR", 690.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("LU1841731745")), //
                        hasDate("2024-10-15T09:14"), hasShares(40.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 624.22), hasGrossValue("CHF", 619.85), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.55 + 2.82)), //
                        hasForexGrossValue("EUR", 661.76))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("US1912161007")), //
                        hasDate("2024-10-01T16:16"), hasShares(10.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 609.78), hasGrossValue("CHF", 613.19), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.53 + 1.88)), //
                        hasForexGrossValue("USD", 725.00))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("US02079K3059")), //
                        hasDate("2024-10-01T15:32"), hasShares(2.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 284.44), hasGrossValue("CHF", 287.04), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.72 + 1.88)), //
                        hasForexGrossValue("USD", 338.00))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("FR0000053381")), //
                        hasDate("2024-09-27T17:12"), hasShares(60.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 301.52), hasGrossValue("CHF", 305.96), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.76 + 3.68)), //
                        hasForexGrossValue("EUR", 324.30))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("IE00B3WJKG14")), //
                        hasDate("2024-09-26T09:04"), hasShares(15.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 413.72), hasGrossValue("CHF", 415.71), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.04 + 0.95)), //
                        hasForexGrossValue("EUR", 436.95))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("US88160R1014")), //
                        hasDate("2024-09-24T15:38"), hasShares(2.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 430.54), hasGrossValue("CHF", 433.51), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.08 + 1.89)), //
                        hasForexGrossValue("USD", 510.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("NL0000226223")), //
                        hasDate("2024-09-20T17:01"), hasShares(30.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 710.24), hasGrossValue("CHF", 703.83), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.76 + 4.65)), //
                        hasForexGrossValue("EUR", 744.60))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("FR0000121014")), //
                        hasDate("2024-09-18T15:47"), hasShares(1.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 567.44), hasGrossValue("CHF", 561.41), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.41 + 4.62)), //
                        hasForexGrossValue("EUR", 599.00))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("US2546871060")), //
                        hasDate("2024-09-16T15:32"), hasShares(5.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 386.44), hasGrossValue("CHF", 389.29), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.97 + 1.88)), //
                        hasForexGrossValue("USD", 460.00))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("US5949181045")), //
                        hasDate("2024-09-11T21:54"), hasShares(2.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 717.41), hasGrossValue("CHF", 721.09), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.80 + 1.88)), //
                        hasForexGrossValue("USD", 844.00))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("US0231351067")), //
                        hasDate("2024-09-10T19:45"), hasShares(2.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 302.92), hasGrossValue("CHF", 305.55), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.76 + 1.87)), //
                        hasForexGrossValue("USD", 360.00))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("US88160R1014")), //
                        hasDate("2024-09-05T15:33"), hasShares(3.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 569.23), hasGrossValue("CHF", 572.54), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.43 + 1.88)), //
                        hasForexGrossValue("USD", 675.00))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("DE000ZAL1111")), //
                        hasDate("2024-08-21T10:46"), hasShares(15.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 339.54), hasGrossValue("CHF", 345.07), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.86 + 4.67)), //
                        hasForexGrossValue("EUR", 362.10))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("US0079031078")), //
                        hasDate("2024-08-20T16:03"), hasShares(5.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 689.27), hasGrossValue("CHF", 692.91), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.73 + 1.91)), //
                        hasForexGrossValue("USD", 805.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("NL0000226223")), //
                        hasDate("2024-08-13T10:52"), hasShares(20.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 512.57), hasGrossValue("CHF", 506.65), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.27 + 4.65)), //
                        hasForexGrossValue("EUR", 536.00))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("FR0014008VX5")), //
                        hasDate("2024-08-13T10:47"), hasShares(2.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 7.56), hasGrossValue("CHF", 7.58), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.02), //
                        hasForexGrossValue("EUR", 7.98))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("FR0014008VX5")), //
                        hasDate("2024-08-13T10:47"), hasShares(23.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 82.31), hasGrossValue("CHF", 87.18), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.22 + 4.65)), //
                        hasForexGrossValue("EUR", 91.77))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("US70450Y1038")), //
                        hasDate("2024-07-30T15:40"), hasShares(3.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 170.74), hasGrossValue("CHF", 173.09), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.43 + 1.92)), //
                        hasForexGrossValue("USD", 195.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("NL0000226223")), //
                        hasDate("2024-07-29T16:33"), hasShares(15.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 456.12), hasGrossValue("CHF", 450.28), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.13 + 4.71)), //
                        hasForexGrossValue("EUR", 471.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("DK0062498333")), //
                        hasDate("2024-07-19T15:49"), hasShares(3.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 358.87), hasGrossValue("CHF", 353.22), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.89 + 4.76)), //
                        hasForexGrossValue("EUR", 365.70))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("FR0000120859")), //
                        hasDate("2024-07-19T10:51"), hasShares(5.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 155.29), hasGrossValue("CHF", 160.44), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.40 + 4.75)), //
                        hasForexGrossValue("EUR", 165.50))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("US0079031078")), //
                        hasDate("2024-07-17T21:55"), hasShares(5.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 708.75), hasGrossValue("CHF", 705.04), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.77 + 1.94)), //
                        hasForexGrossValue("USD", 800.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("IE00B3WJKG14")), //
                        hasDate("2024-07-17T16:12"), hasShares(15.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 425.64), hasGrossValue("CHF", 423.61), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.06 + 0.97)), //
                        hasForexGrossValue("EUR", 438.75))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("DK0062498333")), //
                        hasDate("2024-07-02T13:56"), hasShares(5.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 649.35), hasGrossValue("CHF", 642.98), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.61 + 4.76)), //
                        hasForexGrossValue("EUR", 665.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("IE000I8KRLL9")), //
                        hasDate("2024-06-20T15:36"), hasShares(40.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 308.96), hasGrossValue("CHF", 305.31), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.77 + 2.88)), //
                        hasForexGrossValue("EUR", 320.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("IE00B0M63516")), //
                        hasDate("2024-06-20T15:24"), hasShares(10.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 200.86), hasGrossValue("CHF", 197.49), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.49 + 2.88)), //
                        hasForexGrossValue("EUR", 207.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("FR0013269123")), //
                        hasDate("2024-06-17T09:00"), hasShares(15.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 389.46), hasGrossValue("CHF", 383.82), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.96 + 4.68)), //
                        hasForexGrossValue("EUR", 403.80))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("US5801351017")), //
                        hasDate("2024-06-11T15:30"), hasShares(1.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 229.10), hasGrossValue("CHF", 226.60), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.57 + 1.93)), //
                        hasForexGrossValue("USD", 253.01))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("US0231351067")), //
                        hasDate("2024-06-06T22:00"), hasShares(3.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 492.00), hasGrossValue("CHF", 495.17), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.23 + 1.94)), //
                        hasForexGrossValue("USD", 555.00))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("US02079K3059")), //
                        hasDate("2024-06-05T15:30"), hasShares(3.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 466.84), hasGrossValue("CHF", 469.96), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.17 + 1.95)), //
                        hasForexGrossValue("USD", 525.45))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("IE00BZCQB185")), //
                        hasDate("2024-06-05T10:42"), hasShares(40.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 334.28), hasGrossValue("CHF", 332.48), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.83 + 0.97)), //
                        hasForexGrossValue("EUR", 343.40))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("FR0000051732")), //
                        hasDate("2024-06-05T10:29"), hasShares(60.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 56.93), hasGrossValue("CHF", 61.85), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.15 + 4.77)), //
                        hasForexGrossValue("EUR", 63.60))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("US5949181045")), //
                        hasDate("2024-04-30T15:46"), hasShares(2.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 731.79), hasGrossValue("CHF", 728.01), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.82 + 1.96)), //
                        hasForexGrossValue("USD", 798.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("FI4000297767")), //
                        hasDate("2024-04-24T15:33"), hasShares(30.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 325.84), hasGrossValue("CHF", 321.21), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.81 + 3.82)), //
                        hasForexGrossValue("EUR", 330.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("NL0000226223")), //
                        hasDate("2024-04-24T09:29"), hasShares(15.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 579.04), hasGrossValue("CHF", 572.80), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.44 + 4.80)), //
                        hasForexGrossValue("EUR", 588.00))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("FR0011052257")), //
                        hasDate("2024-04-23T11:46"), hasShares(12.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 16.57), hasGrossValue("CHF", 21.39), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.05 + 4.77)), //
                        hasForexGrossValue("EUR", 21.96))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("US1912161007")), //
                        hasDate("2024-04-12T17:32"), hasShares(10.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 535.26), hasGrossValue("CHF", 531.99), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.33 + 1.94)), //
                        hasForexGrossValue("USD", 585.00))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("FR0011726835")), //
                        hasDate("2024-04-05T14:17"), hasShares(3.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 407.80), hasGrossValue("CHF", 413.64), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.03 + 4.81)), //
                        hasForexGrossValue("EUR", 421.50))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("US5801351017")), //
                        hasDate("2024-04-04T21:59"), hasShares(1.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 245.30), hasGrossValue("CHF", 242.73), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.61 + 1.96)), //
                        hasForexGrossValue("USD", 270.00))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("FR0000120859")), //
                        hasDate("2024-04-04T09:00"), hasShares(5.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 146.82), hasGrossValue("CHF", 152.03), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.38 + 4.83)), //
                        hasForexGrossValue("EUR", 154.30))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("FR0000121485")), //
                        hasDate("2024-03-21T10:26"), hasShares(1.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 365.46), hasGrossValue("CHF", 359.77), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.90 + 4.79)), //
                        hasForexGrossValue("EUR", 369.80))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("LU1841731745")), //
                        hasDate("2024-03-20T15:23"), hasShares(19.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 248.20), hasGrossValue("CHF", 244.69), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.61 + 2.90)), //
                        hasForexGrossValue("EUR", 253.95))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("LU1841731745")), //
                        hasDate("2024-03-20T10:46"), hasShares(1.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 15.76), hasGrossValue("CHF", 12.82), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.03 + 2.91)), //
                        hasForexGrossValue("EUR", 13.30))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("US30303M1027")), //
                        hasDate("2024-03-14T14:30"), hasShares(1.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 439.29), hasGrossValue("CHF", 442.32), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.10 + 1.93)), //
                        hasForexGrossValue("USD", 501.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("US7561091049")), //
                        hasDate("2024-03-13T15:48"), hasShares(10.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 466.77), hasGrossValue("CHF", 463.69), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.16 + 1.92)), //
                        hasForexGrossValue("USD", 530.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("IE00B0M63516")), //
                        hasDate("2024-03-13T13:48"), hasShares(10.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 233.22), hasGrossValue("CHF", 229.75), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.58 + 2.89)), //
                        hasForexGrossValue("EUR", 240.00))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("US8740391003")), //
                        hasDate("2024-03-12T16:34"), hasShares(3.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 384.26), hasGrossValue("CHF", 387.15), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.97 + 1.92)), //
                        hasForexGrossValue("USD", 439.50))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("US88579Y1010")), //
                        hasDate("2024-03-12T14:31"), hasShares(4.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 351.09), hasGrossValue("CHF", 353.89), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.88 + 1.92)), //
                        hasForexGrossValue("USD", 402.00))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("FR0000121964")), //
                        hasDate("2024-03-12T10:49"), hasShares(10.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 227.02), hasGrossValue("CHF", 232.30), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.58 + 4.70)), //
                        hasForexGrossValue("EUR", 242.00))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("CH0210483332")), //
                        hasDate("2024-03-12T09:00"), hasShares(3.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 430.80), hasGrossValue("CHF", 436.80), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 6.00))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("CH0244767585")), //
                        hasDate("2024-03-11T20:42"), hasShares(6.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 161.32), hasGrossValue("CHF", 163.65), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.41 + 1.92)), //
                        hasForexGrossValue("USD", 186.00))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("FR0011052257")), //
                        hasDate("2024-03-07T09:14"), hasShares(50.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 95.62), hasGrossValue("CHF", 100.59), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.25 + 4.72)), //
                        hasForexGrossValue("EUR", 104.50))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("IE00BTN1Y115")), //
                        hasDate("2024-02-27T16:00"), hasShares(6.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 441.17), hasGrossValue("CHF", 444.19), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.11 + 1.91)), //
                        hasForexGrossValue("USD", 504.00))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("US30303M1027")), //
                        hasDate("2024-02-20T15:33"), hasShares(1.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 415.50), hasGrossValue("CHF", 418.45), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.04 + 1.91)), //
                        hasForexGrossValue("USD", 475.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("CH0210483332")), //
                        hasDate("2023-11-10T17:30"), hasShares(3.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 326.10), hasGrossValue("CHF", 320.10), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 6.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("FR0014008VX5")), //
                        hasDate("2023-10-18T11:59"), hasShares(19.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 86.39), hasGrossValue("CHF", 86.17), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.22), //
                        hasForexGrossValue("EUR", 91.01))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("FR0014008VX5")), //
                        hasDate("2023-10-18T11:58"), hasShares(6.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 31.95), hasGrossValue("CHF", 27.22), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.07 + 4.66)), //
                        hasForexGrossValue("EUR", 28.74))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("US5801351017")), //
                        hasDate("2023-10-16T21:34"), hasShares(1.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 226.40), hasGrossValue("CHF", 223.94), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.56 + 1.90)), //
                        hasForexGrossValue("USD", 249.50))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("NL00150001Q9")), //
                        hasDate("2023-10-16T20:37"), hasShares(20.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 354.37), hasGrossValue("CHF", 357.16), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.89 + 1.90)), //
                        hasForexGrossValue("USD", 396.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("DE000ZAL1111")), //
                        hasDate("2023-10-12T15:32"), hasShares(15.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 319.84), hasGrossValue("CHF", 314.36), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.79 + 4.69)), //
                        hasForexGrossValue("EUR", 330.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("FR0000121014")), //
                        hasDate("2023-09-27T16:56"), hasShares(1.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 679.93), hasGrossValue("CHF", 673.49), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.69 + 4.75)), //
                        hasForexGrossValue("EUR", 698.80))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("FR0000051732")), //
                        hasDate("2023-08-07T14:10"), hasShares(40.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 300.00), hasGrossValue("CHF", 294.53), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.74 + 4.73)), //
                        hasForexGrossValue("EUR", 306.80))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("IE00BZCQB185")), //
                        hasDate("2023-07-14T14:54"), hasShares(30.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 199.85), hasGrossValue("CHF", 198.38), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.50 + 0.97)), //
                        hasForexGrossValue("EUR", 205.86))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("US30303M1027")), //
                        hasDate("2023-07-13T15:30"), hasShares(1.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 268.50), hasGrossValue("CHF", 271.11), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.68 + 1.93)), //
                        hasForexGrossValue("USD", 313.70))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("SE0000202624")), //
                        hasDate("2023-06-27T14:52"), hasShares(15.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 228.56), hasGrossValue("CHF", 224.17), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.56 + 3.83)), //
                        hasForexGrossValue("EUR", 229.50))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("FR0011726835")), //
                        hasDate("2023-06-23T09:00"), hasShares(3.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 269.54), hasGrossValue("CHF", 264.06), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.66 + 4.82)), //
                        hasForexGrossValue("EUR", 269.85))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("FR0000053381")), //
                        hasDate("2023-06-22T09:05"), hasShares(60.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 292.19), hasGrossValue("CHF", 287.64), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.72 + 3.83)), //
                        hasForexGrossValue("EUR", 294.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("FR0013269123")), //
                        hasDate("2023-06-13T15:56"), hasShares(10.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 248.27), hasGrossValue("CHF", 242.86), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.61 + 4.80)), //
                        hasForexGrossValue("EUR", 249.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("CH0244767585")), //
                        hasDate("2023-06-13T00:00"), hasShares(6.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 108.16), hasGrossValue("CHF", 108.16), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00), //
                        hasForexGrossValue("USD", 119.48))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("US2254011081")), //
                        hasDate("2023-06-13T00:00"), hasShares(150.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 120.28), hasGrossValue("CHF", 120.28), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00), //
                        hasForexGrossValue("USD", 132.87))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("US78573M1045")), //
                        hasDate("2023-06-08T18:31"), hasShares(16.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 48.09), hasGrossValue("CHF", 47.97), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.12), //
                        hasForexGrossValue("USD", 53.44))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("US78573M1045")), //
                        hasDate("2023-06-08T18:31"), hasShares(23.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 69.13), hasGrossValue("CHF", 68.96), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.17), //
                        hasForexGrossValue("USD", 76.82))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("US78573M1045")), //
                        hasDate("2023-06-08T18:31"), hasShares(10.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 30.06), hasGrossValue("CHF", 29.98), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.08), //
                        hasForexGrossValue("USD", 33.40))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("US78573M1045")), //
                        hasDate("2023-06-08T18:31"), hasShares(1.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 4.96), hasGrossValue("CHF", 3.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.01 + 1.95)), //
                        hasForexGrossValue("USD", 3.34))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("US88160R1014")), //
                        hasDate("2023-05-30T15:30"), hasShares(3.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 539.98), hasGrossValue("CHF", 543.27), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.35 + 1.94)), //
                        hasForexGrossValue("USD", 600.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("US70450Y1038")), //
                        hasDate("2023-05-22T16:37"), hasShares(3.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 170.82), hasGrossValue("CHF", 168.45), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.42 + 1.95)), //
                        hasForexGrossValue("USD", 188.04))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("US30303M1027")), //
                        hasDate("2023-05-22T15:42"), hasShares(1.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 222.33), hasGrossValue("CHF", 224.84), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.56 + 1.95)), //
                        hasForexGrossValue("USD", 250.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("FI4000297767")), //
                        hasDate("2023-05-02T10:34"), hasShares(30.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 299.32), hasGrossValue("CHF", 294.73), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.74 + 3.85)), //
                        hasForexGrossValue("EUR", 300.00))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("FR001400GG91")), //
                        hasDate("2023-04-03T08:39"), hasShares(12.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 24.18), hasGrossValue("CHF", 24.18), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00), //
                        hasForexGrossValue("EUR", 24.84))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("FR0011052257")), //
                        hasDate("2023-04-03T08:39"), hasShares(12.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 24.18), hasGrossValue("CHF", 24.18), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00), //
                        hasForexGrossValue("EUR", 24.84))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("FR001400GG91")), //
                        hasDate("2023-03-23T00:00"), hasShares(12.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 24.18), hasGrossValue("CHF", 24.18), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00), //
                        hasForexGrossValue("EUR", 24.84))));

        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        sale( //
                                        hasSecurity(hasIsin("FR001400GG91")), //
                                        hasDate("2023-03-23T00:00"), hasShares(50.00), //
                                        hasSource("Transactions_french03.txt"), //
                                        hasNote(null), //
                                        hasAmount("CHF", 0.00), hasGrossValue("CHF", 0.00), //
                                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00)))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("US2254011081")), //
                        hasDate("2023-03-15T17:23"), hasShares(100.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 176.69), hasGrossValue("CHF", 175.27), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.44 + 0.98)), //
                        hasForexGrossValue("USD", 190.00))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("US30303M1027")), //
                        hasDate("2023-03-15T17:19"), hasShares(1.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 179.01), hasGrossValue("CHF", 180.44), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.45 + 0.98)), //
                        hasForexGrossValue("USD", 194.62))));

        assertThat(results, hasItem(skippedItem( //
                        Messages.MsgErrorTransactionTypeNotSupportedOrRequired, //
                        purchase( //
                                        hasSecurity(hasIsin("FR001400GG91")), //
                                        hasDate("2023-03-08T00:00"), hasShares(50.00), //
                                        hasSource("Transactions_french03.txt"), //
                                        hasNote(null), //
                                        hasAmount("CHF", 0.00), hasGrossValue("CHF", 0.00), //
                                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.00)))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("US88579Y1010")), //
                        hasDate("2023-03-02T15:30"), hasShares(4.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 414.43), hasGrossValue("CHF", 412.40), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.03 + 1.00)), //
                        hasForexGrossValue("USD", 438.68))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("IE00BTJRMP35")), //
                        hasDate("2023-02-17T15:54"), hasShares(5.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 232.67), hasGrossValue("CHF", 232.09), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.58), //
                        hasForexGrossValue("EUR", 235.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("FR0013269123")), //
                        hasDate("2023-02-17T15:54"), hasShares(15.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 391.00), hasGrossValue("CHF", 385.17), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.97 + 4.86)), //
                        hasForexGrossValue("EUR", 390.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("FR0000120859")), //
                        hasDate("2023-02-17T14:41"), hasShares(10.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 419.24), hasGrossValue("CHF", 413.34), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.04 + 4.86)), //
                        hasForexGrossValue("EUR", 419.20))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("IE00BTN1Y115")), //
                        hasDate("2023-01-26T19:45"), hasShares(6.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 450.24), hasGrossValue("CHF", 448.12), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.12 + 1.00)), //
                        hasForexGrossValue("USD", 488.16))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("CH0012032048")), //
                        hasDate("2023-01-24T10:16"), hasShares(1.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 301.00), hasGrossValue("CHF", 295.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 6.00))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("FR0010112524")), //
                        hasDate("2023-01-16T10:31"), hasShares(20.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 556.43), hasGrossValue("CHF", 562.75), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.40 + 4.92)), //
                        hasForexGrossValue("EUR", 560.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("FR0000031775")), //
                        hasDate("2023-01-13T14:55"), hasShares(20.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 496.24), hasGrossValue("CHF", 490.07), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.23 + 4.94)), //
                        hasForexGrossValue("EUR", 489.00))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("NL00150001Q9")), //
                        hasDate("2023-01-12T16:19"), hasShares(30.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 445.20), hasGrossValue("CHF", 447.33), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.12 + 1.01)), //
                        hasForexGrossValue("USD", 480.00))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("FR0000051732")), //
                        hasDate("2023-01-12T10:19"), hasShares(30.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 386.28), hasGrossValue("CHF", 392.19), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.98 + 4.93)), //
                        hasForexGrossValue("EUR", 390.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("US88160R1014")), //
                        hasDate("2023-01-06T15:38"), hasShares(3.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 288.03), hasGrossValue("CHF", 286.32), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.72 + 0.99)), //
                        hasForexGrossValue("USD", 306.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("IE00BZCQB185")), //
                        hasDate("2023-01-04T16:36"), hasShares(30.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 199.80), hasGrossValue("CHF", 199.30), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.50), //
                        hasForexGrossValue("EUR", 202.80))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("US8740391003")), //
                        hasDate("2023-01-04T16:31"), hasShares(3.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 209.64), hasGrossValue("CHF", 208.13), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.52 + 0.99)), //
                        hasForexGrossValue("USD", 224.25))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("US2546871060")), //
                        hasDate("2022-12-16T16:45"), hasShares(5.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 417.47), hasGrossValue("CHF", 415.44), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.04 + 0.99)), //
                        hasForexGrossValue("USD", 447.50))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("US2254011081")), //
                        hasDate("2022-12-14T15:30"), hasShares(50.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 150.76), hasGrossValue("CHF", 149.40), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.37 + 0.99)), //
                        hasForexGrossValue("USD", 161.50))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("US88160R1014")), //
                        hasDate("2022-12-13T16:29"), hasShares(2.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 304.77), hasGrossValue("CHF", 303.02), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.76 + 0.99)), //
                        hasForexGrossValue("USD", 327.96))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("US88160R1014")), //
                        hasDate("2022-12-06T15:35"), hasShares(3.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 507.69), hasGrossValue("CHF", 505.43), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.27 + 0.99)), //
                        hasForexGrossValue("USD", 540.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("LU1841731745")), //
                        hasDate("2022-12-02T12:39"), hasShares(20.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 298.74), hasGrossValue("CHF", 295.04), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.74 + 2.96)), //
                        hasForexGrossValue("EUR", 300.68))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("FR0000124141")), //
                        hasDate("2022-11-22T09:08"), hasShares(20.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 486.42), hasGrossValue("CHF", 492.48), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.23 + 4.83)), //
                        hasForexGrossValue("EUR", 500.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("FR0011052257")), //
                        hasDate("2022-11-18T16:16"), hasShares(50.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 204.30), hasGrossValue("CHF", 198.96), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.50 + 4.84)), //
                        hasForexGrossValue("EUR", 202.25))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("NL00150001Q9")), //
                        hasDate("2022-11-10T15:30"), hasShares(25.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 344.39), hasGrossValue("CHF", 346.24), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.86 + 0.99)), //
                        hasForexGrossValue("USD", 355.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("US02079K3059")), //
                        hasDate("2022-11-09T18:12"), hasShares(5.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 437.44), hasGrossValue("CHF", 435.36), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.09 + 0.99)), //
                        hasForexGrossValue("USD", 443.70))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("FR0004007813")), //
                        hasDate("2022-11-04T17:00"), hasShares(20.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 492.02), hasGrossValue("CHF", 498.11), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.24 + 4.85)), //
                        hasForexGrossValue("EUR", 503.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("US0231351067")), //
                        hasDate("2022-11-04T16:58"), hasShares(5.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 446.34), hasGrossValue("CHF", 444.24), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.11 + 0.99)), //
                        hasForexGrossValue("USD", 446.20))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("NL00150001Q9")), //
                        hasDate("2022-11-04T15:22"), hasShares(25.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 335.35), hasGrossValue("CHF", 337.18), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.84 + 0.99)), //
                        hasForexGrossValue("USD", 337.50))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("FR0000121972")), //
                        hasDate("2022-11-04T14:54"), hasShares(5.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 646.06), hasGrossValue("CHF", 652.53), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.63 + 4.84)), //
                        hasForexGrossValue("EUR", 660.00))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("FR0000124141")), //
                        hasDate("2022-11-04T14:53"), hasShares(20.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 452.76), hasGrossValue("CHF", 458.74), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.14 + 4.84)), //
                        hasForexGrossValue("EUR", 464.00))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("FR0000045072")), //
                        hasDate("2022-11-02T09:03"), hasShares(30.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 273.51), hasGrossValue("CHF", 279.06), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.70 + 4.85)), //
                        hasForexGrossValue("EUR", 282.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("US30303M1027")), //
                        hasDate("2022-10-28T16:35"), hasShares(5.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 498.33), hasGrossValue("CHF", 496.10), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.24 + 0.99)), //
                        hasForexGrossValue("USD", 499.04))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("FR0000121964")), //
                        hasDate("2022-10-28T16:13"), hasShares(20.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 396.57), hasGrossValue("CHF", 402.44), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.00 + 4.87)), //
                        hasForexGrossValue("EUR", 405.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("FR0000051732")), //
                        hasDate("2022-10-28T16:11"), hasShares(50.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 484.56), hasGrossValue("CHF", 478.49), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.20 + 4.87)), //
                        hasForexGrossValue("EUR", 483.90))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("FR0004007813")), //
                        hasDate("2022-10-26T10:52"), hasShares(10.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 242.73), hasGrossValue("CHF", 248.22), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.62 + 4.87)), //
                        hasForexGrossValue("EUR", 250.00))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("FR0000121972")), //
                        hasDate("2022-10-26T09:28"), hasShares(5.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 651.61), hasGrossValue("CHF", 658.12), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.64 + 4.87)), //
                        hasForexGrossValue("EUR", 662.50))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("FR0004007813")), //
                        hasDate("2022-09-20T12:41"), hasShares(14.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 296.83), hasGrossValue("CHF", 291.36), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.73 + 4.74)), //
                        hasForexGrossValue("EUR", 302.40))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("FR0000124141")), //
                        hasDate("2022-09-19T15:29"), hasShares(20.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 413.11), hasGrossValue("CHF", 407.35), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.02 + 4.74)), //
                        hasForexGrossValue("EUR", 423.00))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("FR0010220475")), //
                        hasDate("2022-09-19T09:22"), hasShares(25.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 454.07), hasGrossValue("CHF", 459.96), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.15 + 4.74)), //
                        hasForexGrossValue("EUR", 475.00))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("FR0000045072")), //
                        hasDate("2022-08-08T13:50"), hasShares(30.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 285.48), hasGrossValue("CHF", 290.50), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.72 + 4.30)), //
                        hasForexGrossValue("EUR", 297.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("FR0004007813")), //
                        hasDate("2022-07-12T16:55"), hasShares(16.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 412.91), hasGrossValue("CHF", 407.54), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.02 + 4.35)), //
                        hasForexGrossValue("EUR", 414.40))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("FR0000121972")), //
                        hasDate("2022-07-05T10:06"), hasShares(5.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 566.75), hasGrossValue("CHF", 560.95), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.41 + 4.39)), //
                        hasForexGrossValue("EUR", 564.50))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("FR0010112524")), //
                        hasDate("2022-06-29T15:40"), hasShares(7.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 176.37), hasGrossValue("CHF", 175.93), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.44), //
                        hasForexGrossValue("EUR", 176.40))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("FR0010112524")), //
                        hasDate("2022-06-29T15:40"), hasShares(12.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 302.34), hasGrossValue("CHF", 301.58), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.76), //
                        hasForexGrossValue("EUR", 302.40))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("FR0010112524")), //
                        hasDate("2022-06-29T15:40"), hasShares(1.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 29.61), hasGrossValue("CHF", 25.14), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.06 + 4.41)), //
                        hasForexGrossValue("EUR", 25.20))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("FR0000121964")), //
                        hasDate("2022-06-29T15:15"), hasShares(15.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 299.28), hasGrossValue("CHF", 294.12), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.74 + 4.42)), //
                        hasForexGrossValue("EUR", 294.30))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("FR0000045072")), //
                        hasDate("2022-05-06T09:55"), hasShares(60.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 626.71), hasGrossValue("CHF", 620.57), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.56 + 4.58)), //
                        hasForexGrossValue("EUR", 599.70))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("FR0000121964")), //
                        hasDate("2022-03-25T10:22"), hasShares(15.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 354.98), hasGrossValue("CHF", 349.60), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (0.88 + 4.50)), //
                        hasForexGrossValue("EUR", 343.50))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("FR0000124141")), //
                        hasDate("2022-03-25T09:04"), hasShares(20.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 576.83), hasGrossValue("CHF", 570.89), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.43 + 4.51)), //
                        hasForexGrossValue("EUR", 560.00))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("US0378331005")), //
                        hasDate("2022-03-24T20:57"), hasShares(6.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 970.89), hasGrossValue("CHF", 973.83), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (2.43 + 0.51)), //
                        hasForexGrossValue("USD", 1044.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("FR0010220475")), //
                        hasDate("2022-03-11T11:41"), hasShares(25.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 511.43), hasGrossValue("CHF", 505.65), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.27 + 4.51)), //
                        hasForexGrossValue("EUR", 496.25))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("NL00150001Q9")), //
                        hasDate("2022-03-08T16:38"), hasShares(50.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 646.07), hasGrossValue("CHF", 643.95), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.61 + 0.51)), //
                        hasForexGrossValue("USD", 695.00))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("CH0210483332")), //
                        hasDate("2022-02-22T09:01"), hasShares(4.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 494.50), hasGrossValue("CHF", 500.00), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 5.50))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("FR0000121972")), //
                        hasDate("2022-02-01T10:57"), hasShares(5.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 792.37), hasGrossValue("CHF", 785.82), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.97 + 4.58)), //
                        hasForexGrossValue("EUR", 758.00))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("FR0000124141")), //
                        hasDate("2022-01-28T13:29"), hasShares(30.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 976.71), hasGrossValue("CHF", 983.74), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (2.45 + 4.58)), //
                        hasForexGrossValue("EUR", 945.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("CH0210483332")), //
                        hasDate("2022-01-27T11:19"), hasShares(4.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 530.70), hasGrossValue("CHF", 525.20), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 5.50))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("US0378331005")), //
                        hasDate("2022-01-19T20:46"), hasShares(6.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 922.91), hasGrossValue("CHF", 920.08), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (2.31 + 0.52)), //
                        hasForexGrossValue("USD", 1007.40))));

        assertThat(results, hasItem(sale( //
                        hasSecurity(hasIsin("US0378331005")), //
                        hasDate("2022-01-19T17:21"), hasShares(5.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 769.15), hasGrossValue("CHF", 771.59), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (1.92 + 0.52)), //
                        hasForexGrossValue("USD", 840.00))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("FR0000124141")), //
                        hasDate("2022-01-13T14:25"), hasShares(30.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 1014.91), hasGrossValue("CHF", 1007.77), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (2.53 + 4.61)), //
                        hasForexGrossValue("EUR", 966.60))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("NL00150001Q9")), //
                        hasDate("2022-01-05T18:24"), hasShares(50.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 960.46), hasGrossValue("CHF", 957.54), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", (2.40 + 0.52)), //
                        hasForexGrossValue("USD", 1048.50))));

        assertThat(results, hasItem(purchase( //
                        hasSecurity(hasIsin("US0378331005")), //
                        hasDate("2021-12-16T15:54"), hasShares(5.00), //
                        hasSource("Transactions_french03.txt"), //
                        hasNote(null), //
                        hasAmount("CHF", 817.42), hasGrossValue("CHF", 816.60), //
                        hasTaxes("CHF", 0.00), hasFees("CHF", 0.82), //
                        hasForexGrossValue("USD", 887.30))));
    }

    @Test
    public void testTransakce01()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transakce01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(55L));
        assertThat(countBuySell(results), is(90L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(145));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security1 = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security1.getName(), is("PETROLEO BRASILEIRO S."));
        assertThat(security1.getIsin(), is("US71654V4086"));
        assertThat(security1.getCurrencyCode(), is(CurrencyUnit.USD));

        Security security40 = results.stream().filter(SecurityItem.class::isInstance).skip(40).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security40.getName(), is("SOLARWINDS CORPORATION COMMON STOCK"));
        assertThat(security40.getIsin(), is("US83417Q2049"));
        assertThat(security40.getCurrencyCode(), is(CurrencyUnit.USD));

        // check 1st buy transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-08-08T16:42")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(16)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(232.79))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(232.29))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.50))));

        Unit grossValueUnit = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(237.12))));

        // check 2nd buy/sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(55).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-08-02T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(8)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(152.74))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(152.74))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        grossValueUnit = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(179.84))));
    }

    @Test
    public void testEstadoDeCuenta01()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "EstadoDeCuenta01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(7L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(47L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(2L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(54));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security1 = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security1.getName(), is("INDITEX"));
        assertThat(security1.getIsin(), is("ES0148396007"));
        assertThat(security1.getCurrencyCode(), is(CurrencyUnit.EUR));

        Security security2 = results.stream().filter(SecurityItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security2.getName(), is("LOWES COMPANIES INC."));
        assertThat(security2.getIsin(), is("US5486611073"));
        assertThat(security2.getCurrencyCode(), is(CurrencyUnit.USD));

        Security security3 = results.stream().filter(SecurityItem.class::isInstance).skip(2).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security3.getName(), is("JP MORGAN CHASE & CO."));
        assertThat(security3.getIsin(), is("US46625H1005"));
        assertThat(security3.getCurrencyCode(), is(CurrencyUnit.USD));

        Security security4 = results.stream().filter(SecurityItem.class::isInstance).skip(3).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security4.getName(), is("T. ROWE PRICE GROUP I"));
        assertThat(security4.getIsin(), is("US74144T1088"));
        assertThat(security4.getCurrencyCode(), is(CurrencyUnit.USD));

        Security security5 = results.stream().filter(SecurityItem.class::isInstance).skip(4).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security5.getName(), is("3M COMPANY COMMON STOC"));
        assertThat(security5.getIsin(), is("US88579Y1010"));
        assertThat(security5.getCurrencyCode(), is(CurrencyUnit.USD));

        Security security6 = results.stream().filter(SecurityItem.class::isInstance).skip(5).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security6.getName(), is("JOHNSON & JOHNSON COMM"));
        assertThat(security6.getIsin(), is("US4781601046"));
        assertThat(security6.getCurrencyCode(), is(CurrencyUnit.USD));

        Security security7 = results.stream().filter(SecurityItem.class::isInstance).skip(6).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security7.getName(), is("ISHARES GLOBAL WATER UCITS ETF USD"));
        assertThat(security7.getIsin(), is("IE00B1TXK627"));
        assertThat(security7.getCurrencyCode(), is(CurrencyUnit.USD));

        // check transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-09-03T02:25")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2000.00))));
        assertThat(transaction.getSource(), is("EstadoDeCuenta01.txt"));
        assertThat(transaction.getNote(), is("flatex Deposit"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(1)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-08-03T02:24")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2000.00))));
        assertThat(transaction.getSource(), is("EstadoDeCuenta01.txt"));
        assertThat(transaction.getNote(), is("flatex Deposit"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(2)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-07-26T07:54")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5000.00))));
        assertThat(transaction.getSource(), is("EstadoDeCuenta01.txt"));
        assertThat(transaction.getNote(), is("flatex Deposit"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(3)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-07-03T02:19")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2000.00))));
        assertThat(transaction.getSource(), is("EstadoDeCuenta01.txt"));
        assertThat(transaction.getNote(), is("flatex Deposit"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(4)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-06-28T10:27")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5000.00))));
        assertThat(transaction.getSource(), is("EstadoDeCuenta01.txt"));
        assertThat(transaction.getNote(), is("flatex Deposit"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(5)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-06-24T07:51")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5000.00))));
        assertThat(transaction.getSource(), is("EstadoDeCuenta01.txt"));
        assertThat(transaction.getNote(), is("flatex Deposit"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(6)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-06-21T06:39")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5000.00))));
        assertThat(transaction.getSource(), is("EstadoDeCuenta01.txt"));
        assertThat(transaction.getNote(), is("flatex Deposit"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(7)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-06-19T12:12")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5000.00))));
        assertThat(transaction.getSource(), is("EstadoDeCuenta01.txt"));
        assertThat(transaction.getNote(), is("flatex Deposit"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(8)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-06-16T16:10")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5000.00))));
        assertThat(transaction.getSource(), is("EstadoDeCuenta01.txt"));
        assertThat(transaction.getNote(), is("flatex Deposit"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(9)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-06-15T19:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5000.00))));
        assertThat(transaction.getSource(), is("EstadoDeCuenta01.txt"));
        assertThat(transaction.getNote(), is("flatex Deposit"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(10)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-06-03T02:40")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4000.00))));
        assertThat(transaction.getSource(), is("EstadoDeCuenta01.txt"));
        assertThat(transaction.getNote(), is("flatex Deposit"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(11)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-05-25T08:01")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5000.00))));
        assertThat(transaction.getSource(), is("EstadoDeCuenta01.txt"));
        assertThat(transaction.getNote(), is("flatex Deposit"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(12)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-05-17T07:38")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5000.00))));
        assertThat(transaction.getSource(), is("EstadoDeCuenta01.txt"));
        assertThat(transaction.getNote(), is("flatex Deposit"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(13)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-05-03T03:49")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4000.00))));
        assertThat(transaction.getSource(), is("EstadoDeCuenta01.txt"));
        assertThat(transaction.getNote(), is("flatex Deposit"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(14)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-04-27T11:43")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5000.00))));
        assertThat(transaction.getSource(), is("EstadoDeCuenta01.txt"));
        assertThat(transaction.getNote(), is("flatex Deposit"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(15)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-04-03T02:18")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4000.00))));
        assertThat(transaction.getSource(), is("EstadoDeCuenta01.txt"));
        assertThat(transaction.getNote(), is("flatex Deposit"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(16)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-03-29T07:59")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5000.00))));
        assertThat(transaction.getSource(), is("EstadoDeCuenta01.txt"));
        assertThat(transaction.getNote(), is("flatex Deposit"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(17)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-03-17T10:04")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5000.00))));
        assertThat(transaction.getSource(), is("EstadoDeCuenta01.txt"));
        assertThat(transaction.getNote(), is("flatex Deposit"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(18)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-03-15T08:02")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4000.00))));
        assertThat(transaction.getSource(), is("EstadoDeCuenta01.txt"));
        assertThat(transaction.getNote(), is("flatex Deposit"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(19)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-03-10T16:10")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4000.00))));
        assertThat(transaction.getSource(), is("EstadoDeCuenta01.txt"));
        assertThat(transaction.getNote(), is("flatex Deposit"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(20)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-03-03T02:39")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4000.00))));
        assertThat(transaction.getSource(), is("EstadoDeCuenta01.txt"));
        assertThat(transaction.getNote(), is("flatex Deposit"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(21)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-01-31T22:40")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(6000.00))));
        assertThat(transaction.getSource(), is("EstadoDeCuenta01.txt"));
        assertThat(transaction.getNote(), is("flatex Deposit"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(22)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-01-31T22:30")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4000.00))));
        assertThat(transaction.getSource(), is("EstadoDeCuenta01.txt"));
        assertThat(transaction.getNote(), is("flatex Deposit"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(23)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-01-31T22:10")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.00))));
        assertThat(transaction.getSource(), is("EstadoDeCuenta01.txt"));
        assertThat(transaction.getNote(), is("flatex Deposit"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(24)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-12-21T16:30")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.00))));
        assertThat(transaction.getSource(), is("EstadoDeCuenta01.txt"));
        assertThat(transaction.getNote(), is("flatex Deposit"));

        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(25)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-12-20T02:53")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.00))));
        assertThat(transaction.getSource(), is("EstadoDeCuenta01.txt"));
        assertThat(transaction.getNote(), is("Ingreso"));

        // @formatter:off
        // 07-11-2022 09:59 02-11-2022 INDITEX ES0148396007 Dividendo EUR -317,70 EUR 534,43
        // 07-11-2022 09:59 02-11-2022 INDITEX ES0148396007 Retención del dividendo EUR 60,36 EUR 852,13
        // @formatter:on
        // check 1st cancellation (Storno) transaction
        TransactionItem cancellation = (TransactionItem) results.stream() //
                        .filter(i -> i.isFailure()) //
                        .filter(TransactionItem.class::isInstance) //
                        .findFirst().orElseThrow(IllegalArgumentException::new);

        assertThat(cancellation.getFailureMessage(), is(Messages.MsgErrorTransactionOrderCancellationUnsupported));
        assertThat(cancellation.getSource(), is("EstadoDeCuenta01.txt"));

        // check 1st dividende transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(27)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-11-07T07:28")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0L)));
        assertThat(transaction.getSource(), is("EstadoDeCuenta01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(257.34))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(317.70))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(60.36))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        // check 2nd dividende transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(28)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-11-04T10:35")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0L)));
        assertThat(transaction.getSource(), is("EstadoDeCuenta01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(257.34))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(317.70))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(60.36))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        // check 3rd dividende transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(29)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-11-04T09:47")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0L)));
        assertThat(transaction.getSource(), is("EstadoDeCuenta01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(141.54))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(174.74))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(33.20))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        // check 4th dividende transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(30)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-11-03T07:06")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0L)));
        assertThat(transaction.getSource(), is("EstadoDeCuenta01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(9.13))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10.74))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.61))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(10.50))));

        // check 5th dividende transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(31)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-11-01T07:04")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0L)));
        assertThat(transaction.getSource(), is("EstadoDeCuenta01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(8.58))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10.10))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.52))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(10.00))));

        // check 6th dividende transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(32)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-09-30T07:25")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0L)));
        assertThat(transaction.getSource(), is("EstadoDeCuenta01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(26.99))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(31.75))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.76))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(31.20))));

        // check 7th dividende transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(33)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-09-13T07:20")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0L)));
        assertThat(transaction.getSource(), is("EstadoDeCuenta01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(6.33))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(7.45))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.12))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(7.45))));

        // check 8th dividende transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(34)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-09-07T07:44")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0L)));
        assertThat(transaction.getSource(), is("EstadoDeCuenta01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(57.45))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(67.59))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10.14))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(67.80))));

        // check 9th dividende transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(35)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-08-04T07:23")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0L)));
        assertThat(transaction.getSource(), is("EstadoDeCuenta01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(8.68))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10.22))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.54))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(10.50))));

        // check 10th dividende transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(36)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-08-01T07:11")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0L)));
        assertThat(transaction.getSource(), is("EstadoDeCuenta01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(8.26))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(9.72))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.46))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(10.00))));

        // check 11th dividende transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(37)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-05-26T08:06")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0L)));
        assertThat(transaction.getSource(), is("EstadoDeCuenta01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(21.21))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(21.21))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(22.82))));

        // check 12th dividende transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(38)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-05-02T07:40")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0L)));
        assertThat(transaction.getSource(), is("EstadoDeCuenta01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(267.05))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(329.69))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(62.64))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        // check 2nd cancellation (Storno) transaction - counter booking of the
        // cancelled dividend above
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(39)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.TAX_REFUND));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-11-07T09:59")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(60.36))));
        assertThat(transaction.getSource(), is("EstadoDeCuenta01.txt"));
        assertThat(transaction.getNote(), is("Retención del dividendo: ES0148396007"));

        // check 1st interest charge transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(40)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST_CHARGE));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-10-01T09:31")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.15))));
        assertThat(transaction.getSource(), is("EstadoDeCuenta01.txt"));
        assertThat(transaction.getNote(), is("Flatex Interest"));

        // check 2nd interest charge transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(41)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST_CHARGE));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-07-02T06:20")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5.31))));
        assertThat(transaction.getSource(), is("EstadoDeCuenta01.txt"));
        assertThat(transaction.getNote(), is("Flatex Interest"));

        // check 3rd interest charge transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(42)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST_CHARGE));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-04-02T02:30")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.45))));
        assertThat(transaction.getSource(), is("EstadoDeCuenta01.txt"));
        assertThat(transaction.getNote(), is("Flatex Interest"));

        // check 1st fee transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(43)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-07-05T08:32")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.50))));
        assertThat(transaction.getSource(), is("EstadoDeCuenta01.txt"));
        assertThat(transaction.getNote(), is("Giro Exchange Connection Fee 2022"));

        // check 2nd fee transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(44)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-07-05T08:32")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.50))));
        assertThat(transaction.getSource(), is("EstadoDeCuenta01.txt"));
        assertThat(transaction.getNote(), is("Giro Exchange Connection Fee 2022"));

        // check 3rd fee transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(45)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-04-01T23:24")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.50))));
        assertThat(transaction.getSource(), is("EstadoDeCuenta01.txt"));
        assertThat(transaction.getNote(), is("Giro Exchange Connection Fee 2022"));

        // check 4th fee transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(46)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-03-02T08:42")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.50))));
        assertThat(transaction.getSource(), is("EstadoDeCuenta01.txt"));
        assertThat(transaction.getNote(), is("Giro Exchange Connection Fee 2022"));
    }

    @Test
    public void testPrehleductu01()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Prehleductu01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(43L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(44));

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("IE00B3XXRP09"));
        assertThat(security.getName(), is("VANGUARD S&P500"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check transaction
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(43L));

        Item item = iter.next();

        // assert transaction
        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-12-21T16:59")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CZK", Values.Amount.factorize(50000.00))));
        assertThat(transaction.getSource(), is("Prehleductu01.txt"));
        assertThat(transaction.getNote(), is("Vklad"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-11-23T15:10")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CZK", Values.Amount.factorize(50000.00))));
        assertThat(transaction.getSource(), is("Prehleductu01.txt"));
        assertThat(transaction.getNote(), is("Vklad"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-10-18T11:40")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CZK", Values.Amount.factorize(50000.00))));
        assertThat(transaction.getSource(), is("Prehleductu01.txt"));
        assertThat(transaction.getNote(), is("Vklad"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-09-21T14:39")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CZK", Values.Amount.factorize(50000.00))));
        assertThat(transaction.getSource(), is("Prehleductu01.txt"));
        assertThat(transaction.getNote(), is("Vklad"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-07-20T14:49")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CZK", Values.Amount.factorize(90000.00))));
        assertThat(transaction.getSource(), is("Prehleductu01.txt"));
        assertThat(transaction.getNote(), is("Vklad"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-07-07T11:29")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CZK", Values.Amount.factorize(270000.00))));
        assertThat(transaction.getSource(), is("Prehleductu01.txt"));
        assertThat(transaction.getNote(), is("Vklad"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-05-31T12:43")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CZK", Values.Amount.factorize(400000.00))));
        assertThat(transaction.getSource(), is("Prehleductu01.txt"));
        assertThat(transaction.getNote(), is("Vklad"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-12-21T12:08")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CZK", Values.Amount.factorize(28000.00))));
        assertThat(transaction.getSource(), is("Prehleductu01.txt"));
        assertThat(transaction.getNote(), is("Vklad"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-06-11T11:43")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CZK", Values.Amount.factorize(30000.00))));
        assertThat(transaction.getSource(), is("Prehleductu01.txt"));
        assertThat(transaction.getNote(), is("Vklad"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-06-01T13:08")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CZK", Values.Amount.factorize(1.00))));
        assertThat(transaction.getSource(), is("Prehleductu01.txt"));
        assertThat(transaction.getNote(), is("Vklad"));

        // check 1st dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(10)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-12-29T08:50")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0L)));
        assertThat(transaction.getSource(), is("Prehleductu01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(33.27))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(33.27))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(35.56))));

        // check 2nd dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(11)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-12-29T08:50")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0L)));
        assertThat(transaction.getSource(), is("Prehleductu01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.61))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.61))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(4.93))));

        // check 3rd dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(12)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-09-29T08:03")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0L)));
        assertThat(transaction.getSource(), is("Prehleductu01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(36.78))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(36.78))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(36.19))));

        // check 4th dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(13)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-09-29T08:03")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0L)));
        assertThat(transaction.getSource(), is("Prehleductu01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5.10))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5.10))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(5.02))));

        // check 5th dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(14)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-06-30T10:12")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0L)));
        assertThat(transaction.getSource(), is("Prehleductu01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(33.33))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(33.33))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(35.03))));

        // check 6th dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(15)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-06-30T10:12")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0L)));
        assertThat(transaction.getSource(), is("Prehleductu01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.62))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.62))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(4.86))));

        // check 7th dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(16)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-04-01T09:37")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0L)));
        assertThat(transaction.getSource(), is("Prehleductu01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(31.85))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(31.85))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(35.28))));

        // check 8th dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(17)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-04-01T09:37")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0L)));
        assertThat(transaction.getSource(), is("Prehleductu01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.41))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.41))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(4.89))));

        // check 9th dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(18)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-12-30T09:04")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0L)));
        assertThat(transaction.getSource(), is("Prehleductu01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(28.09))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(28.09))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(31.90))));

        // check 10th dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(19)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-12-30T09:04")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0L)));
        assertThat(transaction.getSource(), is("Prehleductu01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3.98))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3.98))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(4.52))));

        // check 11th dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(20)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-09-30T14:50")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0L)));
        assertThat(transaction.getSource(), is("Prehleductu01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(25.79))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(25.79))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(29.89))));

        // check 12th dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(21)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-09-30T14:50")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0L)));
        assertThat(transaction.getSource(), is("Prehleductu01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.19))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.19))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(4.85))));

        // check 13th dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(22)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-07-01T10:56")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0L)));
        assertThat(transaction.getSource(), is("Prehleductu01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(9.74))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(9.74))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(11.55))));

        // check 14th dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(23)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-07-01T10:56")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0L)));
        assertThat(transaction.getSource(), is("Prehleductu01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3.63))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3.63))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(4.30))));

        // check 15th dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(24)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-04-01T13:56")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0L)));
        assertThat(transaction.getSource(), is("Prehleductu01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3.86))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3.86))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(4.56))));

        // check 16th dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(25)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-12-31T09:20")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0L)));
        assertThat(transaction.getSource(), is("Prehleductu01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3.32))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3.32))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(4.07))));

        // check 17th dividends transaction
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance).skip(26)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-10-07T08:05")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0L)));
        assertThat(transaction.getSource(), is("Prehleductu01.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.04))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.04))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(4.76))));

        // check transaction
        // get transactions
        iter = results.stream().filter(TransactionItem.class::isInstance).skip(27).iterator();

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST_CHARGE));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-10-01T20:01")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.06))));
        assertThat(transaction.getSource(), is("Prehleductu01.txt"));
        assertThat(transaction.getNote(), is("Flatex Interest"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST_CHARGE));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-07-03T00:11")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.12))));
        assertThat(transaction.getSource(), is("Prehleductu01.txt"));
        assertThat(transaction.getNote(), is("Flatex Interest"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST_CHARGE));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-04-02T16:50")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.08))));
        assertThat(transaction.getSource(), is("Prehleductu01.txt"));
        assertThat(transaction.getNote(), is("Flatex Interest"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST_CHARGE));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-12-31T00:40")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.33))));
        assertThat(transaction.getSource(), is("Prehleductu01.txt"));
        assertThat(transaction.getNote(), is("Flatex Interest"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST_CHARGE));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-10-02T00:20")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.25))));
        assertThat(transaction.getSource(), is("Prehleductu01.txt"));
        assertThat(transaction.getNote(), is("Flatex Interest"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-01-03T14:01")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.50))));
        assertThat(transaction.getSource(), is("Prehleductu01.txt"));
        assertThat(transaction.getNote(), is("DEGIRO poplatek za Obchodování 2023 (Borsa Italiana S.p.A. - MIL)"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-01-03T14:01")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.50))));
        assertThat(transaction.getSource(), is("Prehleductu01.txt"));
        assertThat(transaction.getNote(), is("DEGIRO poplatek za Obchodování 2023 (Euronext Amsterdam - EAM)"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-01-03T14:01")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.50))));
        assertThat(transaction.getSource(), is("Prehleductu01.txt"));
        assertThat(transaction.getNote(), is("DEGIRO poplatek za Obchodování 2023 (Xetra - XET)"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-02-03T10:03")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.50))));
        assertThat(transaction.getSource(), is("Prehleductu01.txt"));
        assertThat(transaction.getNote(), is("DEGIRO poplatek za Obchodování 2022 (Borsa Italiana S.p.A. - MIL)"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-02-03T10:03")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.50))));
        assertThat(transaction.getSource(), is("Prehleductu01.txt"));
        assertThat(transaction.getNote(), is("DEGIRO poplatek za Obchodování 2022 (Euronext Amsterdam - EAM)"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-02-03T10:03")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.50))));
        assertThat(transaction.getSource(), is("Prehleductu01.txt"));
        assertThat(transaction.getNote(), is("DEGIRO poplatek za Obchodování 2022 (Xetra - XET)"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-07-02T08:23")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.50))));
        assertThat(transaction.getSource(), is("Prehleductu01.txt"));
        assertThat(transaction.getNote(), is("DEGIRO poplatek za Obchodování 2021 (Xetra - XET)"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-01-31T13:20")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.50))));
        assertThat(transaction.getSource(), is("Prehleductu01.txt"));
        assertThat(transaction.getNote(), is("DEGIRO poplatek za Obchodování 2021 (Borsa Italiana S.p.A. - MIL)"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-01-31T13:20")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.50))));
        assertThat(transaction.getSource(), is("Prehleductu01.txt"));
        assertThat(transaction.getNote(), is("DEGIRO poplatek za Obchodování 2021 (Euronext Amsterdam - EAM)"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-01-04T11:09")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.50))));
        assertThat(transaction.getSource(), is("Prehleductu01.txt"));
        assertThat(transaction.getNote(), is("DEGIRO poplatek za Obchodování 2020 (Euronext Amsterdam - EAM)"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-10-01T11:58")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.50))));
        assertThat(transaction.getSource(), is("Prehleductu01.txt"));
        assertThat(transaction.getNote(), is("DEGIRO poplatek za Obchodování 2020 (Borsa Italiana S.p.A. - MIL)"));
    }

    @Test
    public void testTransacoes01()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transacoes01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(2L));
        assertThat(countBuySell(results), is(2L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(countAccountTransfers(results), is(0L));
        assertThat(countItemsWithFailureMessage(results), is(0L));
        assertThat(countSkippedItems(results), is(0L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security1 = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security1.getName(), is("ENERGY FUELS INC"));
        assertNull(security1.getWkn());
        assertNull(security1.getTickerSymbol());
        assertThat(security1.getIsin(), is("CA2926717083"));
        assertThat(security1.getCurrencyCode(), is(CurrencyUnit.USD));

        Security security2 = results.stream().filter(SecurityItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security2.getName(), is("VANGUARD FTSE ALL- WORLD UCITS ETF - (USD) ACCUMULATING"));
        assertNull(security2.getWkn());
        assertNull(security2.getTickerSymbol());
        assertThat(security2.getIsin(), is("IE00BK5BQT80"));
        assertThat(security2.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check 1st buy/sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2023-03-13T14:31")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(410)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2069.01))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2068.01))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.00))));

        Unit grossValueUnit = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(2214.00))));

        // check 2nd buy/sell transaction
        entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-09-15T17:01")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(52)));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4992.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4992.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }
   
}
