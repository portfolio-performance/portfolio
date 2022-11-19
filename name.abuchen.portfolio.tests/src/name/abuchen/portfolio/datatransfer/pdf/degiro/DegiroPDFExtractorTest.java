package name.abuchen.portfolio.datatransfer.pdf.degiro;

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
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check transaction
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(i -> i instanceof TransactionItem).iterator();
        assertThat(results.stream().filter(i -> i instanceof TransactionItem).count(), is(1L));

        Item item = iter.next();

        // assert transaction
        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-08-02T00:00")));
        assertThat(transaction.getSource(), is("Kontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Einzahlung"));
        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(350.00))));
    }

    @Test
    public void testKontoauszug02()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(7));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security1 = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security1.getIsin(), is("DE000C27U079"));
        assertThat(security1.getName(), is("ODX1 C11500.00 01MAR19"));
        assertThat(security1.getCurrencyCode(), is(CurrencyUnit.EUR));

        Security security2 = results.stream().filter(i -> i instanceof SecurityItem).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security2.getIsin(), is("DE000C25KFF5"));
        assertThat(security2.getName(), is("ODX2 P11000.00 08FEB19"));
        assertThat(security2.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check transaction
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(i -> i instanceof TransactionItem).iterator();
        assertThat(results.stream().filter(i -> i instanceof TransactionItem).count(), is(5L));

        Item item = iter.next();

        // assert transaction
        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-02-07T11:53")));
        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1000.00))));

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
        assertThat(results.size(), is(15));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security1 = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security1.getIsin(), is("DE000A0D8Q49"));
        assertThat(security1.getName(), is("IS.DJ U.S.SELEC.DIV.U.ETF"));
        assertThat(security1.getCurrencyCode(), is(CurrencyUnit.USD));

        Security security2 = results.stream().filter(i -> i instanceof SecurityItem).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security2.getIsin(), is("DE0002635299"));
        assertThat(security2.getName(), is("ISH.S.EU.SEL.DIV.30 U.ETF"));
        assertThat(security2.getCurrencyCode(), is(CurrencyUnit.EUR));

        Security security3 = results.stream().filter(i -> i instanceof SecurityItem).skip(2).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security3.getIsin(), is("DE000A0F5UH1"));
        assertThat(security3.getName(), is("IS.S.GL.SE.D.100 U.ETF A"));
        assertThat(security3.getCurrencyCode(), is(CurrencyUnit.EUR));

        AccountTransaction transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(5).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-07-17T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.75))));
        assertThat(transaction.getUnit(Unit.Type.GROSS_VALUE).get().getExchangeRate().doubleValue(),
                        IsCloseTo.closeTo(0.8674531575, 0.000001));
        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.86))));

        // 17-07-2017 00:00 ISH.S.EU.SEL.DIV.30 U.ETF DE0002635299 Dividende EUR 2,07 EUR 521,41
        // 17-07-2017 00:00 ISH.S.EU.SEL.DIV.30 U.ETF DE0002635299 Dividendensteuer EUR -0,55 EUR 519,34
        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(6).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-07-17T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.07 - 0.55))));
        Unit taxUnit = transaction.getUnit(Unit.Type.TAX).orElseThrow(IllegalArgumentException::new);
        assertThat(taxUnit.getAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.55))));

        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(7).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-07-17T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(22.64))));

        // 17-07-2017 00:00 IS.S.GL.SE.D.100 U.ETF A DE000A0F5UH1 Dividende EUR 0,09 EUR 497,25
        // 17-07-2017 00:00 IS.S.GL.SE.D.100 U.ETF A DE000A0F5UH1 Dividendensteuer EUR -0,02 EUR 497,16
        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(8).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-07-17T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.07))));
        taxUnit = transaction.getUnit(Unit.Type.TAX).orElseThrow(IllegalArgumentException::new);
        assertThat(taxUnit.getAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.02))));

        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(9).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-07-17T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.74))));

        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(10).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST_CHARGE));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-07-31T00:00")));
        assertThat(transaction.getSource(), is("Kontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Zinsen"));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.07))));

        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(11).getSubject();
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
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        AccountTransaction transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(0).getSubject();
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
        assertThat(results.size(), is(38));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        AccountTransaction transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(1).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-06-06T15:34")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(200.00))));

        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(2).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-02-22T18:40")));
        assertThat(transaction.getSource(), is("Kontoauszug05.txt"));
        assertThat(transaction.getNote(), is("SOFORT Einzahlung"));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(27.00))));

        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(15).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-02-22T18:40")));
        assertThat(transaction.getSource(), is("Kontoauszug05.txt"));
        assertThat(transaction.getNote(), is("SOFORT Zahlungsgebühr"));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.00))));

        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(20).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-07-03T11:47")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.54))));

        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(21).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-07-03T11:47")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.54))));

        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(9).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-06-14T07:55")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.30))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.06))));
        assertThat(transaction.getUnit(Unit.Type.GROSS_VALUE).get().getExchangeRate().doubleValue(),
                        IsCloseTo.closeTo(0.8913450397, 0.000001));
        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.40))));

        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(10).getSubject();
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

        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(11).getSubject();
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
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        AccountTransaction transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(0).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-07-25T13:06")));
        assertThat(transaction.getSource(), is("Kontoauszug06.txt"));
        assertThat(transaction.getNote(), is("Einzahlung"));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1000.00))));

        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(1).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-08-05T00:09")));
        assertThat(transaction.getSource(), is("Kontoauszug06.txt"));
        assertThat(transaction.getNote(), is("Auszahlung"));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1000.00))));

        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(2).getSubject();
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
        assertThat(results.size(), is(35));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        AccountTransaction transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(0).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-08-02T16:21")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(6695.00))));

        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(1).getSubject();
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
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // 03-08-2019 06:55 02-08-2019 Währungswechsel (Einbuchung) EUR 3,45 EUR 1.552,27
        // 03-08-2019 06:55 02-08-2019 Währungswechsel (Ausbuchung) 1,1120 USD -3,84 USD -0,00
        // 03-08-2019 06:09 02-08-2019 FOOT LOCKER INC. US3448491049 Dividende USD 1,52 USD 3,84
        // 03-08-2019 06:09 02-08-2019 FOOT LOCKER INC. US3448491049 Dividendensteuer USD -0,23 USD 2,32
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(0).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-08-03T06:09")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize((1.52 - 0.23) * 0.8992805755))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.21))));
        assertThat(transaction.getUnit(Unit.Type.GROSS_VALUE).get().getExchangeRate().doubleValue(),
                        IsCloseTo.closeTo(0.8992805755, 0.000001));
        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(1.52))));

        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(1).getSubject();
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
        assertThat(results.size(), is(176));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security1 = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security1.getIsin(), is("US91913Y1001"));
        assertThat(security1.getName(), is("VALERO ENERGY CORPORAT"));
        assertThat(security1.getCurrencyCode(), is(CurrencyUnit.USD));

        Security security2 = results.stream().filter(i -> i instanceof SecurityItem).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security2.getIsin(), is("US4103451021"));
        assertThat(security2.getName(), is("HANESBRANDS INC. COMMO"));
        assertThat(security2.getCurrencyCode(), is(CurrencyUnit.USD));

        Security security3 = results.stream().filter(i -> i instanceof SecurityItem).skip(2).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security3.getIsin(), is("US8326964058"));
        assertThat(security3.getName(), is("J.M. SMUCKER COMPANY ("));
        assertThat(security3.getCurrencyCode(), is(CurrencyUnit.USD));

        AccountTransaction transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(0).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-07-05T13:44")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1000.00))));

        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(1).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-05-24T19:51")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(300.00))));

        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(2).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-03-26T13:53")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.00))));

        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(10).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-08-09T11:33")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.00))));

        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(11).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-03-25T09:58")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(800.00))));

        // 03-08-2019 06:55 02-08-2019 Währungswechsel (Einbuchung) EUR 3,45 EUR 1.552,27
        // 03-08-2019 06:55 02-08-2019 Währungswechsel (Ausbuchung) 1,1120 USD -3,84 USD -0,00
        // [...]
        // 03-08-2019 06:09 02-08-2019 FOOT LOCKER INC. US3448491049 Dividende USD 1,52 USD 3,84
        // 03-08-2019 06:09 02-08-2019 FOOT LOCKER INC. US3448491049 Dividendensteuer USD -0,23 USD 2,32
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .collect(Collectors.toList()).get(24).getSubject();
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

        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(25).getSubject();
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

        // 13-06-2019 06:46 12-06-2019 Währungswechsel (Einbuchung) EUR 2,08 EUR 263,16
        // 13-06-2019 06:46 12-06-2019 Währungswechsel (Ausbuchung) 1,1298 USD -2,36 USD -0,00
        // [...]
        // 12-06-2019 07:54 12-06-2019 3M COMPANY COMMON STOC US88579Y1010 Dividende USD 1,44 USD 1,22
        // 12-06-2019 07:54 12-06-2019 3M COMPANY COMMON STOC US88579Y1010 Dividendensteuer USD -0,22 USD -0,22
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .collect(Collectors.toList()).get(50).getSubject();
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

        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(125).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-09-05T11:34")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.50))));
    }

    @Test
    public void testKontoauszug09()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug09.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US20030N1019"));
        assertThat(security.getName(), is("COMCAST CORPORATION -"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // 24-10-2019 07:16 23-10-2019 Währungswechsel (Einbuchung) EUR 1,29 EUR 691,18
        // 24-10-2019 07:16 23-10-2019 Währungswechsel (Ausbuchung) 1,1141 USD -1,44 USD 0,00
        // 24-10-2019 06:13 23-10-2019 COMCAST CORPORATION - US20030N1019 Dividende USD 0,42 USD 1,44
        // 24-10-2019 06:13 23-10-2019 COMCAST CORPORATION - US20030N1019 Dividendensteuer USD -0,06 USD 1,02
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .collect(Collectors.toList()).get(0).getSubject();
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

        // 24-10-2019 07:16 23-10-2019 Währungswechsel (Einbuchung) EUR 1,29 EUR 691,18
        // 24-10-2019 07:16 23-10-2019 Währungswechsel (Ausbuchung) 1,1141 USD -1,44 USD 0,00
        // ...
        // 23-10-2019 07:46 23-10-2019 CISCO SYSTEMS INC. - US17275R1023 Dividende USD 1,40 USD 1,19
        // 23-10-2019 07:46 23-10-2019 CISCO SYSTEMS INC. - US17275R1023 Dividendensteuer USD -0,21 USD -0,21
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .collect(Collectors.toList()).get(1).getSubject();
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

        // 17-10-2019 07:09 16-10-2019 Währungswechsel (Einbuchung) EUR 2,80 EUR 506,43
        // 17-10-2019 07:09 16-10-2019 Währungswechsel (Ausbuchung) 1,1083 USD -3,11 USD 0,00
        // 17-10-2019 06:25 16-10-2019 LAM RESEARCH CORPORATI US5128071082 Dividende USD 2,30 USD 3,11
        // 17-10-2019 06:25 16-10-2019 LAM RESEARCH CORPORATI US5128071082 Dividendensteuer USD -0,35 USD 0,81
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .collect(Collectors.toList()).get(2).getSubject();
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
    }

    @Test
    public void testKontoauszug10()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kontoauszug10.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(31));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        Security security1 = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security1.getIsin(), is("US3755581036"));
        assertThat(security1.getName(), is("GILEAD SCIENCES INC."));
        assertThat(security1.getCurrencyCode(), is(CurrencyUnit.USD));

        Security security2 = results.stream().filter(i -> i instanceof SecurityItem).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security2.getIsin(), is("IE00B14X4T88"));
        assertThat(security2.getName(), is("ISHS-ASIA PAC.DIV.DL D"));
        assertThat(security2.getCurrencyCode(), is(CurrencyUnit.USD));

        Security security3 = results.stream().filter(i -> i instanceof SecurityItem).skip(2).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security3.getIsin(), is("US5894001008"));
        assertThat(security3.getName(), is("MERCURY GENERAL CORPOR"));
        assertThat(security3.getCurrencyCode(), is(CurrencyUnit.USD));

        AccountTransaction transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(0).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-10-28T08:26")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3500.00))));

        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(1).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-10-28T07:49")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(7000.00))));

        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(13).getSubject();
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

        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(14).getSubject();
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

        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(16).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-10-02T16:39")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.00))));

        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(17).getSubject();
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
        assertThat(results.size(), is(31));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security1 = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security1.getIsin(), is("GB00B03MLX29"));
        assertThat(security1.getName(), is("ROYAL DUTCH SHELL PLC"));
        assertThat(security1.getCurrencyCode(), is(CurrencyUnit.EUR));

        Security security2 = results.stream().filter(i -> i instanceof SecurityItem).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security2.getIsin(), is("US1912161007"));
        assertThat(security2.getName(), is("COCA-COLA COMPANY (THE"));
        assertThat(security2.getCurrencyCode(), is(CurrencyUnit.USD));

        // check deposit transaction
        // 26-10-2020 15:00 26-10-2020 flatex Einzahlung EUR 500,00 EUR 512,88
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(0).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-10-26T15:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(500.00))));

        // check dividends transaction EUR --> EUR
        // 16-12-2020 11:33 16-12-2020 ROYAL DUTCH SHELL PLC GB00B03MLX29 Dividende EUR 1,52 EUR 651,96
        // 16-12-2020 11:33 16-12-2020 ROYAL DUTCH SHELL PLC GB00B03MLX29 Dividendensteuer EUR -0,23 EUR 650,44
        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(1).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-12-16T11:33")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.52 - 0.23))));
        assertThat(transaction.getGrossValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.52))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.23))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        // check dividends transaction USD --> EUR
        // 17-12-2020 08:06 16-12-2020 Währungswechsel (Ausbuchung) EUR 5,87 EUR 657,83
        // 17-12-2020 08:06 16-12-2020 Währungswechsel (Ausbuchung) 1,2212 USD -7,18 USD 0,00
        // [...]
        // 16-12-2020 08:38 15-12-2020 COCA-COLA COMPANY (THE US1912161007 Dividende USD 1,23 USD 7,18
        // 16-12-2020 08:38 15-12-2020 COCA-COLA COMPANY (THE US1912161007 Dividendensteuer USD -0,37 USD 5,95
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .collect(Collectors.toList()).get(2).getSubject();
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
        assertThat(results.size(), is(19));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security1 = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security1.getIsin(), is("GB00B03MLX29"));
        assertThat(security1.getName(), is("ROYAL DUTCH SHELLA"));
        assertThat(security1.getCurrencyCode(), is(CurrencyUnit.EUR));

        Security security2 = results.stream().filter(i -> i instanceof SecurityItem).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security2.getIsin(), is("DE0007231334"));
        assertThat(security2.getName(), is("SIXT SE"));
        assertThat(security2.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(0).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-05-28T16:50")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2000.00))));
        assertThat(transaction.getSource(), is("Kontoauszug12.txt"));
        assertThat(transaction.getNote(), is("flatex Einzahlung"));

        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(1).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-04-26T08:50")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1000.00))));
        assertThat(transaction.getSource(), is("Kontoauszug12.txt"));
        assertThat(transaction.getNote(), is("flatex Einzahlung"));

        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(2).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-10-08T11:25")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(500.00))));
        assertThat(transaction.getSource(), is("Kontoauszug12.txt"));
        assertThat(transaction.getNote(), is("Einzahlung"));

        // 29-03-2021 09:06 29-03-2021 ROYAL DUTCH SHELLA GB00B03MLX29 Dividende EUR 2,79 EUR 118,19
        // 29-03-2021 09:06 29-03-2021 ROYAL DUTCH SHELLA GB00B03MLX29 Dividendensteuer EUR -0,42 EUR 115,40
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .collect(Collectors.toList()).get(3).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-03-29T09:06")));
        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.79 - 0.42))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.79))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.42))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        // 16-12-2020 11:33 16-12-2020 ROYAL DUTCH SHELLA GB00B03MLX29 Dividende EUR 2,77 EUR 117,19
        // 16-12-2020 11:33 16-12-2020 ROYAL DUTCH SHELLA GB00B03MLX29 Dividendensteuer EUR -0,41 EUR 114,42
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .collect(Collectors.toList()).get(4).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-12-16T11:33")));
        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.77 - 0.41))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.77))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.41))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        // 21-09-2020 16:54 21-09-2020 ROYAL DUTCH SHELLA GB00B03MLX29 Dividende EUR 2,71 EUR 114,96
        // 21-09-2020 16:54 21-09-2020 ROYAL DUTCH SHELLA GB00B03MLX29 Dividendensteuer EUR -0,41 EUR 112,25
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .collect(Collectors.toList()).get(5).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-09-21T16:54")));
        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.71 - 0.41))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.71))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.41))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        // 30-06-2020 06:25 29-06-2020 SIXT SE DE0007231334 Dividende EUR 0,05 EUR 112,81
        // 30-06-2020 06:25 29-06-2020 SIXT SE DE0007231334 Dividendensteuer EUR -0,01 EUR 112,76
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .collect(Collectors.toList()).get(6).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-06-30T06:25")));
        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.05 - 0.01))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.05))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.01))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        // 24-06-2020 11:42 22-06-2020 ROYAL DUTCH SHELLA GB00B03MLX29 Dividende EUR 2,84 EUR 112,77
        // 24-06-2020 11:42 22-06-2020 ROYAL DUTCH SHELLA GB00B03MLX29 Dividendensteuer EUR -0,43 EUR 109,93
        transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .collect(Collectors.toList()).get(7).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-06-24T11:42")));
        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.84 - 0.43))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.84))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.43))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(8).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST_CHARGE));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-04-01T21:00")));
        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.15))));
        assertThat(transaction.getSource(), is("Kontoauszug12.txt"));
        assertThat(transaction.getNote(), is("Flatex Interest"));

        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(9).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST_CHARGE));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-12-31T03:42")));
        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.05))));
        assertThat(transaction.getSource(), is("Kontoauszug12.txt"));
        assertThat(transaction.getNote(), is("Flatex Interest"));

        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(10).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-05-03T17:14")));
        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.12))));
        assertThat(transaction.getSource(), is("Kontoauszug12.txt"));
        assertThat(transaction.getNote(), is("Einrichtung von Handelsmodalitäten 2021"));

        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(11).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-04-01T12:03")));
        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.06))));
        assertThat(transaction.getSource(), is("Kontoauszug12.txt"));
        assertThat(transaction.getNote(), is("Einrichtung von Handelsmodalitäten 2021"));

        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(12).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-03-01T11:21")));
        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.02))));
        assertThat(transaction.getSource(), is("Kontoauszug12.txt"));
        assertThat(transaction.getNote(), is("Einrichtung von Handelsmodalitäten 2021"));

        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(13).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-01-31T13:16")));
        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.30))));
        assertThat(transaction.getSource(), is("Kontoauszug12.txt"));
        assertThat(transaction.getNote(), is("Einrichtung von Handelsmodalitäten 2021"));

        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(14).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-08-05T18:46")));
        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.01))));
        assertThat(transaction.getSource(), is("Kontoauszug12.txt"));
        assertThat(transaction.getNote(), is("Einrichtung von Handelsmodalitäten 2020"));

        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(15).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-04-01T11:20")));
        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.26))));
        assertThat(transaction.getSource(), is("Kontoauszug12.txt"));
        assertThat(transaction.getNote(), is("Einrichtung von Handelsmodalitäten 2020"));

        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(16).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES_REFUND));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-08-08T15:59")));
        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.02))));
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

        // 17-11-2021 07:38 16-11-2021 Währungswechsel (Einbuchung) CHF 2.02 CHF 1'185.08
        // 17-11-2021 07:38 16-11-2021 Währungswechsel (Ausbuchung) 1.0760 USD -2.18 USD 0.00
        // [...]
        // 16-11-2021 09:52 15-11-2021 ACCENTURE PLC. CLASS A IE00B4BNMY34 Dividende USD 2.91 USD 2.18
        // 16-11-2021 09:52 15-11-2021 ACCENTURE PLC. CLASS A IE00B4BNMY34 Dividendensteuer USD -0.73 USD -0.73
        transaction = (AccountTransaction) transactionList.get(2).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-11-16T09:52")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of("CHF", Values.Amount.factorize(2.02))));
        assertThat(transaction.getGrossValue(), is(Money.of("CHF", Values.Amount.factorize(2.91 / 1.076))));
        assertThat(transaction.getSource(), is("Kontoauszug13.txt"));

        // 13-11-2021 07:45 12-11-2021 Währungswechsel (Einbuchung) CHF 1.54 CHF 1'183.06
        // 13-11-2021 07:45 12-11-2021 Währungswechsel (Ausbuchung) 1.0866 USD -1.68 USD 0.00
        // 12-11-2021 07:31 11-11-2021 APPLE INC US0378331005 Dividende USD 1.98 USD 1.68
        // 12-11-2021 07:31 11-11-2021 APPLE INC US0378331005 Dividendensteuer USD -0.30 USD -0.30
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
        assertThat(results.size(), is(17));
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

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(97.63))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(132.60))));
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

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(116.69))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(160.95))));
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

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(13.15))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(18.79))));
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

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.31))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.37))));
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

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(8.70))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10.24))));
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

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(31.94))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(37.58))));
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

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(24.00))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(24.18))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.18))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(26.25))));
    }

    @Test
    public void testRekeningoverzicht01()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Rekeningoverzicht01.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        AccountTransaction transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(0).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-08-25T08:42")));
        assertThat(transaction.getSource(), is("Rekeningoverzicht01.txt"));
        assertThat(transaction.getNote(), is("iDEAL Deposit"));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(123.00))));

        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(1).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-08-25T08:41")));
        assertThat(transaction.getSource(), is("Rekeningoverzicht01.txt"));
        assertThat(transaction.getNote(), is("iDEAL Deposit"));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1123.00))));

        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(2).getSubject();
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
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        AccountTransaction transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(0).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-08-26T08:41")));
        assertThat(transaction.getSource(), is("Rekeningoverzicht02.txt"));
        assertThat(transaction.getNote(), is("iDEAL Deposit"));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(20.00))));

        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(1).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-08-25T08:42")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10.00))));

        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(2).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-08-25T08:41")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1200.00))));

        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(3).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-09-02T21:29")));
        assertThat(transaction.getSource(), is("Rekeningoverzicht02.txt"));
        assertThat(transaction.getNote(), is("DEGIRO Aansluitingskosten 2021"));
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
        var extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Rekeningoverzicht04.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividend transaction including tax
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        // 11-07-2022 07:45 08-07-2022 LYXOR ETF CAC 40 FR0007052782 Dividend EUR 1,50 EUR 23,21
        // 11-07-2022 07:45 08-07-2022 LYXOR ETF CAC 40 FR0007052782 Dividendbelasting EUR -0,38 EUR 21,71
        
        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-07-11T07:45")));
        assertThat(transaction.getShares(), is(0L));

        // 1.50 - 0.38 = 1.12
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.12))));
        assertThat(transaction.getGrossValue(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.50))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.38))));
        assertThat(transaction.getSecurity().getName(), is("LYXOR ETF CAC 40"));
        assertThat(transaction.getSecurity().getIsin(), is("FR0007052782"));
        assertNull(transaction.getCrossEntry());

        // check security
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("FR0007052782"));
        assertThat(security.getName(), is("LYXOR ETF CAC 40"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));
    }

    /**
     * Test reading Exchange Connection Fee in EUR from DeGiro AccountStatement
     * in Dutch. These are fees that DeGiro charges for trading on foreign stock
     * exchanges.
     */
    @Test
    public void testRekeningoverzicht05()
    {
        var extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Rekeningoverzicht05.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividend transaction including tax
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        // 01-06-2022 19:55 31-05-2022 Giro Exchange Connection Fee 2022 EUR -1,01 EUR -6,39

        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getNote(), is("Giro Exchange Connection Fee"));

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
        var extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Rekeningoverzicht06.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividend transaction including tax
        var transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        // 25-07-2022 10:50 25-07-2022 flatex Storting EUR 123,45 EUR 123,47

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
    public void testAccountStatement01()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "AccountStatement01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(9));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        Security security1 = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security1.getIsin(), is("CA56501R1064"));
        assertThat(security1.getName(), is("MANULIFE FINANCIAL COR"));
        assertThat(security1.getCurrencyCode(), is("CAD"));

        Security security2 = results.stream().filter(i -> i instanceof SecurityItem).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security2.getIsin(), is("US9024941034"));
        assertThat(security2.getName(), is("TYSON FOODS INC. COMM"));
        assertThat(security2.getCurrencyCode(), is(CurrencyUnit.USD));

        Security security3 = results.stream().filter(i -> i instanceof SecurityItem).skip(2).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security3.getIsin(), is("US7443201022"));
        assertThat(security3.getName(), is("PRUDENTIAL FINANCIAL"));
        assertThat(security3.getCurrencyCode(), is(CurrencyUnit.USD));

        Security security4 = results.stream().filter(i -> i instanceof SecurityItem).skip(3).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security4.getIsin(), is("CA29250N1050"));
        assertThat(security4.getName(), is("ENBRIDGE INC COMMON ST"));
        assertThat(security4.getCurrencyCode(), is("CAD"));

        // check 1st dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(0).getSubject();
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
        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(1).getSubject();
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
        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(2).getSubject();
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
        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(3).getSubject();
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
        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(4).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST_CHARGE));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-03-01T18:47")));
        assertThat(transaction.getSource(), is("AccountStatement01.txt"));
        assertThat(transaction.getNote(), is("Interest"));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.88))));
    }

    @Test
    public void testTransaktionsuebersicht01()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transaktionsuebersicht01.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(49));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        Security security1 = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security1.getIsin(), is("DE000C21EMZ1"));
        assertThat(security1.getName(), is("ODX5 C11500.00 29MAR19"));
        assertThat(security1.getCurrencyCode(), is(CurrencyUnit.EUR));

        Security security2 = results.stream().filter(i -> i instanceof SecurityItem).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security2.getIsin(), is("DE000C21EMV0"));
        assertThat(security2.getName(), is("ODX5 C11400.00 29MAR19"));
        assertThat(security2.getCurrencyCode(), is(CurrencyUnit.EUR));

        Security security3 = results.stream().filter(i -> i instanceof SecurityItem).skip(2).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security3.getIsin(), is("DE000C21EMX6"));
        assertThat(security3.getName(), is("ODX5 C11450.00 29MAR19"));
        assertThat(security3.getCurrencyCode(), is(CurrencyUnit.EUR));

        Security security4 = results.stream().filter(i -> i instanceof SecurityItem).skip(3).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security4.getIsin(), is("DE000C21EMY4"));
        assertThat(security4.getName(), is("ODX5 P11450.00 29MAR19"));
        assertThat(security4.getCurrencyCode(), is(CurrencyUnit.EUR));

        // 29-03-2019 12:18 29-03-2019 ODX5 C11500.00 29MAR19 DE000C21EMZ1
        // Verkauf 3 zu je 30 EUR (DE000C21EMZ1) EUR 450,00 EUR 2.604,06
        // check first buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList()).get(0).getSubject();
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
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(27).getSubject();
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
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(29).getSubject();
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
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(30).getSubject();
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
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE0005140008"));
        assertThat(security.getName(), is("DEUTSCHE BANK AG NA O.N"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check first buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList()).get(0).getSubject();
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
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(1).getSubject();
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
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(2).getSubject();
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
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        Security security1 = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security1.getIsin(), is("US88160R1014"));
        assertThat(security1.getName(), is("TESLA MOTORS INC. - C"));
        assertThat(security1.getCurrencyCode(), is(CurrencyUnit.USD));

        Security security2 = results.stream().filter(i -> i instanceof SecurityItem).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security2.getIsin(), is("DE000C34JCK6"));
        assertThat(security2.getName(), is("ODX1 P12300.00 03MAY19"));
        assertThat(security2.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check 1st buy/sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList()).get(0).getSubject();
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
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(2).getSubject();
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

        // check first buy transaction
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(3).getSubject();
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
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getName(), is("ARCHER-DANIELS-MIDLAND"));
        assertThat(security.getIsin(), is("US0394831020"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check 1st buy/sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList()).get(0).getSubject();

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
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(1).getSubject();

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
        assertThat(results.size(), is(161));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getName(), is("CATERPILLAR INC. COMM"));
        assertThat(security.getIsin(), is("US1491231015"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check first transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList()).get(0).getSubject();

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
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(5).getSubject();

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
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(17).getSubject();

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
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(94).getSubject();

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
        assertThat(results.size(), is(53));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // 20-08-2019 19:03 CALL 20.09.19 TRADDESK 180 DE000GA2N3L5 FRA -114 EUR
        // 6,60 EUR 752,40 EUR 752,40 EUR -2,89 EUR 749,51
        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getName(), is("CALL 20.09.19 TRADDESK 180"));
        assertThat(security.getIsin(), is("DE000GA2N3L5"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check first transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList()).get(0).getSubject();

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
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(29).getSubject();

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
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(34).getSubject();

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
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // 09-01-2020 09:31 TURBOC O.END GOLD 1109,22 DE000VA5DDR3 FRA -55 EUR
        // 42,46 EUR 2.335,30 EUR 2.335,30 EUR -4,76 EUR 2.330,54
        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getName(), is("TURBOC O.END GOLD 1109,22"));
        assertThat(security.getIsin(), is("DE000VA5DDR3"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check first transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList()).get(0).getSubject();

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
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(1).getSubject();

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
        assertThat(results.size(), is(32));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // 23-12-2020 21:51 SALESFORCE.COM INC COM US79466L3024 NSY 3 USD 228,00
        // USD -684,00 EUR -561,44 1,2171 EUR -0,51 EUR -561,95
        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getName(), is("SALESFORCE.COM INC COM"));
        assertThat(security.getIsin(), is("US79466L3024"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check buy transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList()).get(0).getSubject();

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
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(2).getSubject();

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
        assertThat(results.size(), is(7));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getName(), is("WALT DISNEY COMPANY (T"));
        assertThat(security.getIsin(), is("US2546871060"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check 1st sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList()).get(0).getSubject();

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
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(1).getSubject();

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
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(2).getSubject();

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
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(3).getSubject();

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
        assertThat(results.size(), is(7));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getName(), is("CALL 16.12.22 LEONI 18"));
        assertThat(security.getIsin(), is("DE000DV0VC30"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check 1st buy transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList()).get(0).getSubject();

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
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(1).getSubject();

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
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(2).getSubject();

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
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(3).getSubject();

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
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getName(), is("VARTA AG"));
        assertThat(security.getIsin(), is("DE000A0TGJ55"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check 1st sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList()).get(0).getSubject();

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
        assertThat(results.size(), is(14));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getName(), is("VARTA AG"));
        assertThat(security.getIsin(), is("DE000A0TGJ55"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check 1st sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList()).get(0).getSubject();

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
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(1).getSubject();

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
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(2).getSubject();

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
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(3).getSubject();

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
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(4).getSubject();

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
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(5).getSubject();

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
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(6).getSubject();

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
        assertThat(results.size(), is(15));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getName(), is("IROBOT CORPORATION - C"));
        assertThat(security.getIsin(), is("US4627261005"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check 1st buy transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList()).get(0).getSubject();

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

        // check 2nd buy transaction
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(4).getSubject();

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
        assertThat(results.size(), is(20));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getName(), is("DEUTSCHE TELEKOM AG"));
        assertThat(security.getIsin(), is("DE0005557508"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check 1st buy transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList()).get(0).getSubject();

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
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getName(), is("GENERAL ELECTRIC"));
        assertThat(security.getIsin(), is("US3696043013"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check 1st buy transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList()).get(0).getSubject();

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
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(1).getSubject();

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
        assertThat(results.size(), is(657));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security1 = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security1.getName(), is("CD PROJEKT RED SA"));
        assertThat(security1.getIsin(), is("PLOPTTC00011"));
        assertThat(security1.getCurrencyCode(), is("PLN"));

        Security security2 = results.stream().filter(i -> i instanceof SecurityItem).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security2.getName(), is("EBAY INC"));
        assertThat(security2.getIsin(), is("US2786421030"));
        assertThat(security2.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check 1st buy transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList()).get(0).getSubject();

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
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(1).getSubject();

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
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(455).getSubject();

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
        assertThat(results.size(), is(31));
        new AssertImportActions().check(results, "CHF");

        // check security
        Security security1 = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security1.getName(), is("PLUG POWER INC. - COM"));
        assertThat(security1.getIsin(), is("US72919P2020"));
        assertThat(security1.getCurrencyCode(), is(CurrencyUnit.USD));

        Security security2 = results.stream().filter(i -> i instanceof SecurityItem).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security2.getName(), is("NIKOLA CORP"));
        assertThat(security2.getIsin(), is("US6541101050"));
        assertThat(security2.getCurrencyCode(), is(CurrencyUnit.USD));

        Security security3 = results.stream().filter(i -> i instanceof SecurityItem).skip(2).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security3.getName(), is("NIO INC - ADR"));
        assertThat(security3.getIsin(), is("US62914V1061"));
        assertThat(security3.getCurrencyCode(), is(CurrencyUnit.USD));

        Security security4 = results.stream().filter(i -> i instanceof SecurityItem).skip(3).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security4.getName(), is("BALLARD POWER SYSTEMS"));
        assertThat(security4.getIsin(), is("CA0585861085"));
        assertThat(security4.getCurrencyCode(), is(CurrencyUnit.USD));

        Security security5 = results.stream().filter(i -> i instanceof SecurityItem).skip(4).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security5.getName(), is("TESLA MOTORS INC. - C"));
        assertThat(security5.getIsin(), is("US88160R1014"));
        assertThat(security5.getCurrencyCode(), is(CurrencyUnit.USD));

        Security security6 = results.stream().filter(i -> i instanceof SecurityItem).skip(5).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security6.getName(), is("ALPHABET INC. - CLASS A"));
        assertThat(security6.getIsin(), is("US02079K3059"));
        assertThat(security6.getCurrencyCode(), is(CurrencyUnit.USD));

        Security security7 = results.stream().filter(i -> i instanceof SecurityItem).skip(6).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security7.getName(), is("ADOBE SYSTEMS INCORPOR"));
        assertThat(security7.getIsin(), is("US00724F1012"));
        assertThat(security7.getCurrencyCode(), is(CurrencyUnit.USD));

        Security security8 = results.stream().filter(i -> i instanceof SecurityItem).skip(7).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security8.getName(), is("SALESFORCE.COM INC COM"));
        assertThat(security8.getIsin(), is("US79466L3024"));
        assertThat(security8.getCurrencyCode(), is(CurrencyUnit.USD));

        Security security9 = results.stream().filter(i -> i instanceof SecurityItem).skip(8).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security9.getName(), is("DEUTSCHE LUFTHANSA AG"));
        assertThat(security9.getIsin(), is("DE0008232125"));
        assertThat(security9.getCurrencyCode(), is(CurrencyUnit.EUR));

        Security security10 = results.stream().filter(i -> i instanceof SecurityItem).skip(9).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security10.getName(), is("VOLKSWAGEN AG"));
        assertThat(security10.getIsin(), is("DE0007664039"));
        assertThat(security10.getCurrencyCode(), is(CurrencyUnit.EUR));

        Security security11 = results.stream().filter(i -> i instanceof SecurityItem).skip(10).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security11.getName(), is("WAL-MART STORES INC."));
        assertThat(security11.getIsin(), is("US9311421039"));
        assertThat(security11.getCurrencyCode(), is(CurrencyUnit.USD));

        // check 1st buy transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList()).get(0).getSubject();

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
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(5).getSubject();

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
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(18).getSubject();

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
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security1 = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security1.getName(), is("GENERAL ELECTRIC"));
        assertThat(security1.getIsin(), is("US3696043013"));
        assertThat(security1.getCurrencyCode(), is(CurrencyUnit.USD));

        Security security2 = results.stream().filter(i -> i instanceof SecurityItem).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security2.getName(), is("GENERAL ELECTRIC COMPA"));
        assertThat(security2.getIsin(), is("US3696041033"));
        assertThat(security2.getCurrencyCode(), is(CurrencyUnit.USD));

        // check 1st buy transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList()).get(0).getSubject();

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
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(1).getSubject();

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
        assertThat(results.size(), is(21));
        new AssertImportActions().check(results, "CHF");

        // check security
        Security security1 = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security1.getName(), is("ISHARES MSCI EUROPE ESG"));
        assertThat(security1.getIsin(), is("IE00BFNM3D14"));
        assertThat(security1.getCurrencyCode(), is(CurrencyUnit.EUR));

        Security security2 = results.stream().filter(i -> i instanceof SecurityItem).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security2.getName(), is("ISHARES DJ EUROPE"));
        assertThat(security2.getIsin(), is("IE00B52VJ196"));
        assertThat(security2.getCurrencyCode(), is(CurrencyUnit.EUR));

        Security security3 = results.stream().filter(i -> i instanceof SecurityItem).skip(2).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security3.getName(), is("HSBC SP 500 ETF"));
        assertThat(security3.getIsin(), is("IE00B5KQNG97"));
        assertThat(security3.getCurrencyCode(), is(CurrencyUnit.EUR));

        Security security4 = results.stream().filter(i -> i instanceof SecurityItem).skip(3).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security4.getName(), is("AMUNDI ETF STOX600"));
        assertThat(security4.getIsin(), is("LU1681040223"));
        assertThat(security4.getCurrencyCode(), is(CurrencyUnit.EUR));

        Security security5 = results.stream().filter(i -> i instanceof SecurityItem).skip(4).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security5.getName(), is("XIAOMI CORP. CL.B"));
        assertThat(security5.getIsin(), is("KYG9830T1067"));
        assertThat(security5.getCurrencyCode(), is(CurrencyUnit.EUR));

        Security security6 = results.stream().filter(i -> i instanceof SecurityItem).skip(5).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security6.getName(), is("APPLE INC"));
        assertThat(security6.getIsin(), is("US0378331005"));
        assertThat(security6.getCurrencyCode(), is(CurrencyUnit.EUR));

        Security security7 = results.stream().filter(i -> i instanceof SecurityItem).skip(6).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security7.getName(), is("GAMESTOP CORPORATION C"));
        assertThat(security7.getIsin(), is("US36467W1099"));
        assertThat(security7.getCurrencyCode(), is(CurrencyUnit.USD));

        // check 1st buy transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList()).get(0).getSubject();

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
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList()).get(1).getSubject();

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
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList()).get(2).getSubject();

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
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList()).get(3).getSubject();

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
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList()).get(4).getSubject();

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
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList()).get(5).getSubject();

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
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList()).get(6).getSubject();

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
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList()).get(7).getSubject();

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
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList()).get(8).getSubject();

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
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList()).get(9).getSubject();

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
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList()).get(10).getSubject();

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
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList()).get(11).getSubject();

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
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList()).get(12).getSubject();

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
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList()).get(13).getSubject();

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
        assertThat(results.size(), is(79));
        new AssertImportActions().check(results, "CHF");

        // check security
        Security security1 = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security1.getName(), is("ISHARES DJ EUROPE"));
        assertThat(security1.getIsin(), is("IE00B52VJ196"));
        assertThat(security1.getCurrencyCode(), is(CurrencyUnit.EUR));

        Security security2 = results.stream().filter(i -> i instanceof SecurityItem).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security2.getName(), is("AMUNDI ETF STOX600"));
        assertThat(security2.getIsin(), is("LU1681040223"));
        assertThat(security2.getCurrencyCode(), is(CurrencyUnit.EUR));

        Security security3 = results.stream().filter(i -> i instanceof SecurityItem).skip(2).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security3.getName(), is("ISHARES MSCI EUROPE ESG"));
        assertThat(security3.getIsin(), is("IE00BFNM3D14"));
        assertThat(security3.getCurrencyCode(), is(CurrencyUnit.EUR));

        Security security4 = results.stream().filter(i -> i instanceof SecurityItem).skip(3).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security4.getName(), is("HSBC SP 500 ETF"));
        assertThat(security4.getIsin(), is("IE00B5KQNG97"));
        assertThat(security4.getCurrencyCode(), is(CurrencyUnit.EUR));

        Security security5 = results.stream().filter(i -> i instanceof SecurityItem).skip(4).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security5.getName(), is("XIAOMI CORP. CL.B"));
        assertThat(security5.getIsin(), is("KYG9830T1067"));
        assertThat(security5.getCurrencyCode(), is(CurrencyUnit.EUR));

        Security security6 = results.stream().filter(i -> i instanceof SecurityItem).skip(5).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security6.getName(), is("APPLE INC"));
        assertThat(security6.getIsin(), is("US0378331005"));
        assertThat(security6.getCurrencyCode(), is(CurrencyUnit.EUR));

        Security security7 = results.stream().filter(i -> i instanceof SecurityItem).skip(6).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security7.getName(), is("GAMESTOP CORPORATION C"));
        assertThat(security7.getIsin(), is("US36467W1099"));
        assertThat(security7.getCurrencyCode(), is(CurrencyUnit.USD));

        Security security8 = results.stream().filter(i -> i instanceof SecurityItem).skip(7).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security8.getName(), is("AIRBUS SE"));
        assertThat(security8.getIsin(), is("NL0000235190"));
        assertThat(security8.getCurrencyCode(), is(CurrencyUnit.EUR));

        Security security9 = results.stream().filter(i -> i instanceof SecurityItem).skip(8).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security9.getName(), is("RWE AG"));
        assertThat(security9.getIsin(), is("DE0007037129"));
        assertThat(security9.getCurrencyCode(), is(CurrencyUnit.EUR));

        Security security10 = results.stream().filter(i -> i instanceof SecurityItem).skip(9).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security10.getName(), is("ETF ISHARES S&P 500 CHF"));
        assertThat(security10.getIsin(), is("IE00B88DZ566"));
        assertThat(security10.getCurrencyCode(), is("CHF"));

        Security security11 = results.stream().filter(i -> i instanceof SecurityItem).skip(10).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security11.getName(), is("SERVICENOW INC. COMMO"));
        assertThat(security11.getIsin(), is("US81762P1021"));
        assertThat(security11.getCurrencyCode(), is(CurrencyUnit.USD));

        Security security12 = results.stream().filter(i -> i instanceof SecurityItem).skip(11).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security12.getName(), is("ACCENTURE PLC. CLASS A"));
        assertThat(security12.getIsin(), is("IE00B4BNMY34"));
        assertThat(security12.getCurrencyCode(), is(CurrencyUnit.USD));

        Security security13 = results.stream().filter(i -> i instanceof SecurityItem).skip(12).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security13.getName(), is("LYXOR UCITS ETF S&P500"));
        assertThat(security13.getIsin(), is("LU0832435464"));
        assertThat(security13.getCurrencyCode(), is(CurrencyUnit.EUR));

        Security security14 = results.stream().filter(i -> i instanceof SecurityItem).skip(13).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security14.getName(), is("WIRECARD AG"));
        assertThat(security14.getIsin(), is("DE0007472060"));
        assertThat(security14.getCurrencyCode(), is(CurrencyUnit.EUR));

        Security security15 = results.stream().filter(i -> i instanceof SecurityItem).skip(14).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security15.getName(), is("ALLIANZ SE"));
        assertThat(security15.getIsin(), is("DE0008404005"));
        assertThat(security15.getCurrencyCode(), is(CurrencyUnit.EUR));

        Security security16 = results.stream().filter(i -> i instanceof SecurityItem).skip(16).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security16.getName(), is("CSAM ISHARES SPI (CH)"));
        assertThat(security16.getIsin(), is("CH0237935652"));
        assertThat(security16.getCurrencyCode(), is("CHF"));

        Security security17 = results.stream().filter(i -> i instanceof SecurityItem).skip(17).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security17.getName(), is("ISHARES MSCI USA MINIM"));
        assertThat(security17.getIsin(), is("US46429B6974"));
        assertThat(security17.getCurrencyCode(), is(CurrencyUnit.USD));

        // check 1st buy transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList()).get(0).getSubject();

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
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList()).get(1).getSubject();

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
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList()).get(2).getSubject();

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
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList()).get(3).getSubject();

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
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList()).get(4).getSubject();

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
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList()).get(5).getSubject();

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
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList()).get(6).getSubject();

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
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList()).get(7).getSubject();

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
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList()).get(8).getSubject();

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
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList()).get(22).getSubject();

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
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList()).get(28).getSubject();

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
    public void testTransacties01()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transacties01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(6));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security1 = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security1.getName(), is("MCDONALD'S CORPORATION"));
        assertThat(security1.getIsin(), is("US5801351017"));
        assertThat(security1.getCurrencyCode(), is(CurrencyUnit.USD));

        Security security2 = results.stream().filter(i -> i instanceof SecurityItem).skip(1).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security2.getName(), is("VANGUARD FTSE AW"));
        assertThat(security2.getIsin(), is("IE00B3RBWM25"));
        assertThat(security2.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check 1st buy/sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList()).get(0).getSubject();

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
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(1).getSubject();

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
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(2).getSubject();

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
        assertThat(results.size(), is(8));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getName(), is("MACYS INC COMMON STOC"));
        assertThat(security.getIsin(), is("US55616P1049"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check first transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList()).get(0).getSubject();

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
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(1).getSubject();

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
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(2).getSubject();

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
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(3).getSubject();

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
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getName(), is("APPLE INC. - COMMON ST"));
        assertThat(security.getIsin(), is("US0378331005"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check buy transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList()).get(0).getSubject();

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

        // check sell transaction
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(1).getSubject();

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
        assertThat(results.size(), is(11));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getName(), is("LUMINAR TECHNOLOGIES"));
        assertThat(security.getIsin(), is("US5504241051"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check 1st sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList()).get(0).getSubject();

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
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(1).getSubject();

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
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(2).getSubject();

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
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(3).getSubject();

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
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(4).getSubject();

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
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(5).getSubject();

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
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(6).getSubject();

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
    public void testTransactions_english01()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transactions_english01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(24));
        new AssertImportActions().check(results, "CHF");

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getName(), is("NRG ENERGY INC. COMMO"));
        assertThat(security.getIsin(), is("US6293775085"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check 1st buy transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList()).get(0).getSubject();

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

        // check 2nd buy transaction
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(1).getSubject();

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

        // check 3rd buy transaction
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(2).getSubject();

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
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(3).getSubject();

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
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(4).getSubject();

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
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(5).getSubject();

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
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(6).getSubject();

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
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(7).getSubject();

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
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(8).getSubject();

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
    public void testTransakcje01()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transakcje01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(242));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getName(), is("INVESCO EQQQ NASDAQ-100"));
        assertThat(security.getIsin(), is("IE0032077012"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check 1st buy transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList()).get(0).getSubject();

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

        // check 2nd buy/sell transaction
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(12).getSubject();

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

        // check 3rd buy/sell transaction
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(38).getSubject();

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
        assertThat(results.size(), is(818));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getName(), is("APHRIA INC. - COMMON"));
        assertThat(security.getIsin(), is("CA03765K1049"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check 1st buy transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList()).get(0).getSubject();

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

        // check 2nd buy/sell transaction
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(8).getSubject();

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

        // check 3rd buy/sell transaction
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(281).getSubject();

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

        // check 4th buy/sell transaction
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(412).getSubject();

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

        // check 5th buy/sell transaction
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(506).getSubject();

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
        assertThat(results.size(), is(1234));

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getName(), is("AURORA CANNABIS"));
        assertThat(security.getIsin(), is("CA05156X8843"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check 1st buy transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList()).get(0).getSubject();

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

        // check 234th buy/sell transaction
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(234).getSubject();

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

        // check 646th buy/sell transaction
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(646).getSubject();

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

        // check 851th buy/sell transaction
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem).collect(Collectors.toList())
                        .get(851).getSubject();

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
        assertThat(results.size(), is(8));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        AccountTransaction transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(0).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-10-28T08:50")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(200.00))));
        assertThat(transaction.getSource(), is("EstrattoConto01.txt"));
        assertThat(transaction.getNote(), is("Deposito"));

        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(1).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-09-29T08:50")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(200.00))));
        assertThat(transaction.getSource(), is("EstrattoConto01.txt"));
        assertThat(transaction.getNote(), is("Deposito"));

        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(2).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-09-20T08:50")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(300.00))));
        assertThat(transaction.getSource(), is("EstrattoConto01.txt"));
        assertThat(transaction.getNote(), is("Deposito"));

        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(3).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-09-01T02:44")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.01))));
        assertThat(transaction.getSource(), is("EstrattoConto01.txt"));
        assertThat(transaction.getNote(), is("Deposito"));

        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(4).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST_CHARGE));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-10-02T05:50")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.03))));
        assertThat(transaction.getSource(), is("EstrattoConto01.txt"));
        assertThat(transaction.getNote(), is("Flatex Interest"));

        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(5).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-07-02T07:41")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getSource(), is("EstrattoConto01.txt"));
        assertThat(transaction.getNote(), is("Flatex Interest Income"));

        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(6).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-11-01T14:26")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.56))));
        assertThat(transaction.getSource(), is("EstrattoConto01.txt"));
        assertThat(transaction.getNote(), is("DEGIRO Costi di connessione 2021"));

        transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .collect(Collectors.toList()).get(7).getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-10-02T10:46")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.24))));
        assertThat(transaction.getSource(), is("EstrattoConto01.txt"));
        assertThat(transaction.getNote(), is("DEGIRO Costi di connessione 2021"));
    }

    @Test
    public void testOperazioni01()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Operazioni01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getName(), is("VANGUARD FTSE ALL-"));
        assertThat(security.getIsin(), is("IE00BK5BQT80"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check 1st buy transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList()).get(0).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-10-04T09:04")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(2)));
        assertThat(entry.getSource(), is("Operazioni01.txt"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(191.22))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(191.22))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        // check 234th buy/sell transaction
        entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList()).get(1).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-09-24T09:04")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(3)));

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
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(i -> i instanceof SecurityItem).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("GB00B03MLX29"));
        assertThat(security.getName(), is("ROYAL DUTCH SHELLA"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(i -> i instanceof TransactionItem)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-06-22T00:00")));
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
        assertThat(results.size(), is(308));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check 1st security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getName(), is("THE TRADE DESK CL A"));
        assertThat(security.getIsin(), is("US88339J1051"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check 1st buy transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(i -> i instanceof BuySellEntryItem)
                        .collect(Collectors.toList()).get(0).getSubject();

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
    public void testTransakce01()
    {
        DegiroPDFExtractor extractor = new DegiroPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Transakce01.txt"), errors);

        assertThat(errors, empty());
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
        assertThat(security40.getName(), is("SOLARWINDS CORPORATION"));
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
}
