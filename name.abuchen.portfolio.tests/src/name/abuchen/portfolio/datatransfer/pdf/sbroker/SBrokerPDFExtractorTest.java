package name.abuchen.portfolio.datatransfer.pdf.sbroker;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.check;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.dividend;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.fee;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.feeRefund;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasCurrencyCode;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasFees;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasForexGrossValue;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasGrossValue;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasIsin;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasName;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasNote;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasShares;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasSource;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasTaxes;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasTicker;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasWkn;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.interest;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.interestCharge;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.purchase;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.removal;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.sale;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.security;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.taxRefund;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.taxes;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.withFailureMessage;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countAccountTransactions;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countBuySell;
import static name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.countSecurities;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertNull;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.Extractor.BuySellEntryItem;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.datatransfer.ImportAction.Status;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.datatransfer.actions.CheckCurrenciesAction;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.datatransfer.pdf.SBrokerPDFExtractor;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class SBrokerPDFExtractorTest
{
    @Test
    public void testWertpapierKauf01()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000A0H0785"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("iS.EO G.B.C.1.5-10.5y.U.ETF DE Inhaber-Anteile"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2014-09-29T20:35")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(16)));
        assertThat(entry.getSource(), is("Kauf01.txt"));
        assertThat(entry.getNote(), is("Abrechnungs-Nr. 10000000"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1930.17))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1926.40))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3.77))));
    }

    @Test
    public void testWertpapierKauf02()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US5801351017"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("McDonald's Corp. Registered Shares DL-,01"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2011-11-11T09:02")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(18)));
        assertThat(entry.getSource(), is("Kauf02.txt"));
        assertThat(entry.getNote(), is("Abrechnungs-Nr. 28116496"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1249.30))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1238.40))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10.90))));
    }

    @Test
    public void testWertpapierKauf03()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf03.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("LU0171310443"));
        assertThat(security.getWkn(), is("A0BMAN"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("BGF - WORLD TECHNOLOGY FUND ACT. NOM. CLASSE A2 EUR O.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-02-27T01:31:42")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(7.1535)));
        assertThat(entry.getSource(), is("Kauf03.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(500.00))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(485.44))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(14.56))));
    }

    @Test
    public void testWertpapierKauf04()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf04.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000ETFL508"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Deka MSCI World UCITS ETF Inhaber-Anteile"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-05-05T09:04")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(19.916)));
        assertThat(entry.getSource(), is("Kauf04.txt"));
        assertThat(entry.getNote(), is("Abrechnungs-Nr. 65091167"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(498.20))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(498.20))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierKauf05()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf05.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000ETFL342"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Deka MSCI Em. Mkts. UCITS ETF Inhaber-Anteile"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-04-12T10:06")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(66)));
        assertThat(entry.getSource(), is("Kauf05.txt"));
        assertThat(entry.getNote(), is("Abrechnungs-Nr. 54229911"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3186.41))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3171.30))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(14.40 + 0.71))));
    }

    @Test
    public void testWertpapierKauf06()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf06.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US45781V1017"));
        assertThat(security.getWkn(), is("A2DGXH"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("INNOVATIVE INDL PROPERTIES REGISTERED SHARES DL -,001"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-01-10T20:56:38")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(30)));
        assertThat(entry.getSource(), is("Kauf06.txt"));
        assertThat(entry.getNote(), is("Limit 189,40 EUR"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5683.48))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5682.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.48))));
    }

    @Test
    public void testWertpapierKauf07()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf07.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US74144T1088"));
        assertThat(security.getWkn(), is("870967"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("T. ROWE PRICE GROUP INC. REGISTERED SHARES DL -,20"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-02-01T21:03:07")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(25)));
        assertThat(entry.getSource(), is("Kauf07.txt"));
        assertThat(entry.getNote(), is("Limit 138,75 EUR"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3469.73))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3468.75))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.98))));
    }

    @Test
    public void testWertpapierKauf08()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf08.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000A2YN900"));
        assertThat(security.getWkn(), is("A2YN90"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("TEAMVIEWER AG INHABER-AKTIEN O.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-10-14T09:00:12")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(20)));
        assertThat(entry.getSource(), is("Kauf08.txt"));
        assertThat(entry.getNote(), is("Limit billigst"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(298.12))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(282.40))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(15.00 + 0.60 + 0.12))));
    }

    @Test
    public void testWertpapierKauf09()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf09.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("LU0552385295"));
        assertThat(security.getWkn(), is("A1H6XK"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("MOR.ST.INV.-GLOBAL OPPORTUNITY ACTIONS NOMINATIVES A USD O.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-07-26T15:18:11")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(125)));
        assertThat(entry.getSource(), is("Kauf09.txt"));
        assertThat(entry.getNote(), is("Auftragsnummer 999999/99.99"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(15181.54))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(14757.27))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(424.27))));

        Unit grossValueUnit = entry.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE)
                        .orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(17357.50))));
    }

    @Test
    public void testWertpapierKauf09WithSecurityInEUR()
    {
        Security security = new Security("MOR.ST.INV.-GLOBAL OPPORTUNITY ACTIONS NOMINATIVES A USD O.N.", CurrencyUnit.EUR);
        security.setIsin("LU0552385295");
        security.setWkn("A1H6XK");

        Client client = new Client();
        client.addSecurity(security);

        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf09.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-07-26T15:18:11")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(125)));
        assertThat(entry.getSource(), is("Kauf09.txt"));
        assertThat(entry.getNote(), is("Auftragsnummer 999999/99.99"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(15181.54))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(14757.27))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(424.27))));

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        account.setCurrencyCode(CurrencyUnit.EUR);
        Status s = c.process(entry, account, entry.getPortfolio());
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testWertpapierKauf10()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf10.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US1941621039"));
        assertThat(security.getWkn(), is("850667"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("COLGATE-PALMOLIVE CO. SHARES REGISTERED SHARES DL 1"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2022-09-05T12:07:49")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.6304)));
        assertThat(entry.getSource(), is("Kauf10.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(51.25))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(50.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.25))));
    }

    @Test
    public void testWertpapierKauf11()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf11.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000A0D8Q49"));
        assertThat(security.getWkn(), is("A0D8Q4"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("ISH.DJ U.S.SELECT DIV.U.ETF DE INHABER-ANTEILE"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-11-05T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.6798)));
        assertThat(entry.getSource(), is("Kauf11.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(51.30))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(50.05))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.25))));
    }

    @Test
    public void testWertpapierKauf12()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf12.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000A0D8Q49"));
        assertThat(security.getWkn(), is("A0D8Q4"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("ISH.DJ U.S.SELECT DIV.U.ETF DE INHABER-ANTEILE"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.BUY));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-11-05T00:00")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.7769)));
        assertThat(entry.getSource(), is("Kauf12.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(58.63))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(57.20))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.43))));
    }

    @Test
    public void testWertpapierKauf13()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf13.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0292096186"), hasWkn(null), hasTicker(null), //
                        hasName("Xtr.Stoxx Gbl Sel.Div.100 Swap Inhaber-Anteile 1D o.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2020-05-05T09:06"), hasShares(21.413), //
                        hasSource("Kauf13.txt"), hasNote("Abrechnungs-Nr. 15651559"), //
                        hasAmount("EUR", 460.91), hasGrossValue("EUR", 460.91), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testWertpapierKauf14()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf14.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US4781601046"), hasWkn(null), hasTicker(null), //
                        hasName("Johnson & Johnson Registered Shares DL 1"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-04-08T21:20"), hasShares(8.000), //
                        hasSource("Kauf14.txt"), //
                        hasNote("Abrechnungs-Nr. 21461152"), //
                        hasAmount("EUR", 1106.31), hasGrossValue("EUR", 1095.84), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 10.47))));
    }

    @Test
    public void testWertpapierKauf15()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf15.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("GB00B24CGK77"), hasWkn(null), hasTicker(null), //
                        hasName("Reckitt Benckiser Group Registered Shares LS -,10"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-08-03T09:04"), hasShares(15.000), //
                        hasSource("Kauf15.txt"), //
                        hasNote("Abrechnungs-Nr. 53941243"), //
                        hasAmount("EUR", 1007.47), hasGrossValue("EUR", 997.50), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 9.97))));
    }

    @Test
    public void testWertpapierVerkauf01()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000A0H0785"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("iS.EO G.B.C.1.5-10.5y.U.ETF DE Inhaber-Anteile"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2015-06-02T08:05")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(47)));
        assertThat(entry.getSource(), is("Verkauf01.txt"));
        assertThat(entry.getNote(), is("Abrechnungs-Nr. 10000000"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5648.24))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5656.45))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(8.21))));

        // check tax refund transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.TAX_REFUND));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2015-06-03T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(47)));
        assertThat(transaction.getSource(), is("Verkauf01.txt"));
        assertThat(entry.getNote(), is("Abrechnungs-Nr. 10000000"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(11.48))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(11.48))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierVerkauf02()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000ETFL110"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Deka iB.EO L.Sov.D.1-10 U.ETF Inhaber-Anteile"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-04-26T14:10")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(0.836)));
        assertThat(entry.getSource(), is("Verkauf02.txt"));
        assertThat(entry.getNote(), is("Abrechnungs-Nr. 94703363"));

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(98.87))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(99.58))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.71))));
    }

    @Test
    public void testWertpapierVerkauf03()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf03.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("FR0013495298"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Gaussin S.A. Actions au Port. EO 1"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check buy sell transaction
        BuySellEntry entry = (BuySellEntry) results.stream().filter(BuySellEntryItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(entry.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.SELL));
        assertThat(entry.getAccountTransaction().getType(), is(AccountTransaction.Type.SELL));

        assertThat(entry.getPortfolioTransaction().getDateTime(), is(LocalDateTime.parse("2021-05-11T18:08")));
        assertThat(entry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(200)));
        assertThat(entry.getSource(), is("Verkauf03.txt"));
        assertNull(entry.getNote());

        assertThat(entry.getPortfolioTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1757.25))));
        assertThat(entry.getPortfolioTransaction().getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1767.20))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(entry.getPortfolioTransaction().getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(9.95))));

        // check tax refund transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.TAX_REFUND));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-05-11T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(200)));
        assertThat(transaction.getSource(), is("Verkauf03.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(74.02))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(74.02))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testWertpapierVerkauf04()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf04.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("BMG667211046"), hasWkn("A1KBL8"), hasTicker(null), //
                        hasName("NORWEGIAN CRUISE LINE HOLDINGS REGISTERED SHARES O.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2023-01-03T12:15:16"), hasShares(2), //
                        hasSource("Verkauf04.txt"), //
                        hasNote("Auftragsnummer 123456/38.00 | Limit bestens"), //
                        hasAmount("EUR", 13.69), hasGrossValue("EUR", 23.79), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 10.10))));
    }

    @Test
    public void testWertpapierVerkauf05()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Verkauf05.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(1L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000ENER6Y0"), hasWkn("ENER6Y"), hasTicker(null), //
                        hasName("SIEMENS ENERGY AG NAMENS-AKTIEN O.N."), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(sale( //
                        hasDate("2024-05-10T17:37:13"), hasShares(1000.00), //
                        hasSource("Verkauf05.txt"), //
                        hasNote("Auftragsnummer 188320/63.00 | Limit 24,27 EUR"), //
                        hasAmount("EUR", 24060.40), hasGrossValue("EUR", 24270.00), //
                        hasTaxes("EUR", 192.09 + 10.56), hasFees("EUR", 6.95))));
    }

    @Test
    public void testFondsparplan01()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Fondsparplan01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(4L));
        assertThat(countAccountTransactions(results), is(0L));
        assertThat(results.size(), is(5));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000DK0ECU8"), hasWkn("DK0ECU"), hasTicker(null), //
                        hasName("DEKA-GLOBALCHAMPIONS"), //
                        hasCurrencyCode("EUR"))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-07-02T00:00"), hasShares(0.5269), //
                        hasSource("Fondsparplan01.txt"), //
                        hasNote("Auftragsnummer 172520/34.00"), //
                        hasAmount("EUR", 148.76), hasGrossValue("EUR", 145.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.76))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-08-03T00:00"), hasShares(0.5277), //
                        hasSource("Fondsparplan01.txt"), //
                        hasNote("Auftragsnummer 209798/72.00"), //
                        hasAmount("EUR", 148.76), hasGrossValue("EUR", 145.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.76))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-09-02T00:00"), hasShares(0.5108), //
                        hasSource("Fondsparplan01.txt"), //
                        hasNote("Auftragsnummer 242199/94.00"), //
                        hasAmount("EUR", 148.76), hasGrossValue("EUR", 145.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.76))));

        // check buy sell transaction
        assertThat(results, hasItem(purchase( //
                        hasDate("2021-10-04T00:00"), hasShares(0.5241), //
                        hasSource("Fondsparplan01.txt"), //
                        hasNote("Auftragsnummer 285851/23.00"), //
                        hasAmount("EUR", 148.76), hasGrossValue("EUR", 145.00), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 3.76))));
    }

    @Test
    public void testDividende01()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000A0H0785"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("iS.EO G.B.C.1.5-10.5y.U.ETF DE Inhaber-Anteile"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2014-11-17T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(16)));
        assertThat(transaction.getSource(), is("Dividende01.txt"));
        assertThat(transaction.getNote(), is("Ertrag für 2014/15 (12,70 EUR)"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(12.70))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(12.70))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende02()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US5801351017"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("McDonald's Corp. Registered Shares DL-,01"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2014-12-15T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(103)));
        assertThat(transaction.getSource(), is("Dividende02.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(52.36))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(70.32))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize((13.13 / 1.24495) + 7.03 + 0.38))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(87.54))));
    }

    @Test
    public void testDividende02WithSecurityInEUR()
    {
        Security security = new Security("McDonald's Corp. Registered Shares DL-,01", CurrencyUnit.EUR);
        security.setIsin("US5801351017");

        Client client = new Client();
        client.addSecurity(security);

        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2014-12-15T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(103)));
        assertThat(transaction.getSource(), is("Dividende02.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(52.36))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(70.32))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize((13.13 / 1.24495) + 7.03 + 0.38))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        account.setCurrencyCode(CurrencyUnit.EUR);
        Status s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testDividende03()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende03.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US7427181091"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("Procter & Gamble Co., The Registered Shares o.N."));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-05-17T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(25)));
        assertThat(transaction.getSource(), is("Dividende03.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(13.39))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(17.99))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.70 + 1.80 + 0.10))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(21.75))));
    }

    @Test
    public void testDividende03WithSecurityInEUR()
    {
        Security security = new Security("Procter & Gamble Co., The Registered Shares o.N.", CurrencyUnit.EUR);
        security.setIsin("US7427181091");

        Client client = new Client();
        client.addSecurity(security);

        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende03.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-05-17T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(25)));
        assertThat(transaction.getSource(), is("Dividende03.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(13.39))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(17.99))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.70 + 1.80 + 0.10))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        account.setCurrencyCode(CurrencyUnit.EUR);
        Status s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testDividende04()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende04.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US3765361080"));
        assertThat(security.getWkn(), is("260884"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("GLADSTONE COMMERCIAL CORP. REGISTERED SHARES DL -,01"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-12-31T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(250)));
        assertThat(transaction.getSource(), is("Dividende04.txt"));
        assertThat(transaction.getNote(), is("Abrechnungsnr."));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(20.31))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(27.48))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize((4.70 / 1.1396) + 2.70 + 0.14 + 0.21))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(31.32))));
    }

    @Test
    public void testDividende04WithSecurityInEUR()
    {
        Security security = new Security("GLADSTONE COMMERCIAL CORP. REGISTERED SHARES DL -,01", CurrencyUnit.EUR);
        security.setIsin("US3765361080");
        security.setWkn("260884");

        Client client = new Client();
        client.addSecurity(security);

        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende04.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-12-31T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(250)));
        assertThat(transaction.getSource(), is("Dividende04.txt"));
        assertThat(transaction.getNote(), is("Abrechnungsnr."));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(20.31))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(27.48))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize((4.70 / 1.1396) + 2.70 + 0.14 + 0.21))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        account.setCurrencyCode(CurrencyUnit.EUR);
        Status s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testDividende05()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende05.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US5021751020"));
        assertThat(security.getWkn(), is("884625"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("LTC PROPERTIES INC. REGISTERED SHARES DL -,01"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-12-31T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(150)));
        assertThat(transaction.getSource(), is("Dividende05.txt"));
        assertThat(transaction.getNote(), is("Abrechnungsnr."));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(18.50))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(25.01))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3.75 + 2.44 + 0.13 + 0.19))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(28.50))));
    }

    @Test
    public void testDividende05WithSecurityInEUR()
    {
        Security security = new Security("LTC PROPERTIES INC. REGISTERED SHARES DL -,01", CurrencyUnit.EUR);
        security.setIsin("US5021751020");
        security.setWkn("884625");

        Client client = new Client();
        client.addSecurity(security);

        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende05.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-12-31T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(150)));
        assertThat(transaction.getSource(), is("Dividende05.txt"));
        assertThat(transaction.getNote(), is("Abrechnungsnr."));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(18.50))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(25.01))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3.75 + 2.44 + 0.13 + 0.19))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        account.setCurrencyCode(CurrencyUnit.EUR);
        Status s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testDividende06()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende06.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US83012A1097"));
        assertThat(security.getWkn(), is("A2P60W"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("SIXTH STREET SPECIALITY LEND. REGISTERED SHARES DL -,01"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-12-31T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(250)));
        assertThat(transaction.getSource(), is("Dividende06.txt"));
        assertThat(transaction.getNote(), is("Abrechnungsnr."));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(11.36))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(15.36))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.30 + 1.50 + 0.08 + 0.12))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(17.50))));
    }

    @Test
    public void testDividende06WithSecurityInEUR()
    {
        Security security = new Security("SIXTH STREET SPECIALITY LEND. REGISTERED SHARES DL -,01", CurrencyUnit.EUR);
        security.setIsin("US83012A1097");
        security.setWkn("A2P60W");

        Client client = new Client();
        client.addSecurity(security);

        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende06.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-12-31T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(250)));
        assertThat(transaction.getSource(), is("Dividende06.txt"));
        assertThat(transaction.getNote(), is("Abrechnungsnr."));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(11.36))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(15.36))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.30 + 1.50 + 0.08 + 0.12))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        account.setCurrencyCode(CurrencyUnit.EUR);
        Status s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testDividende07()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende07.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("US02209S1033"));
        assertThat(security.getWkn(), is("200417"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("ALTRIA GROUP INC. REGISTERED SHARES DL -,333"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-01-10T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(100)));
        assertThat(transaction.getSource(), is("Dividende07.txt"));
        assertThat(transaction.getNote(), is("Abrechnungsnr. | Quartalsdividende"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(58.37))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(79.01))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(11.85 + 7.75 + 0.42 + 0.62))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(90.00))));
    }

    @Test
    public void testDividende07WithSecurityInEUR()
    {
        Security security = new Security("ALTRIA GROUP INC. REGISTERED SHARES DL -,333", CurrencyUnit.EUR);
        security.setIsin("US02209S1033");
        security.setWkn("200417");

        Client client = new Client();
        client.addSecurity(security);

        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende07.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-01-10T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(100)));
        assertThat(transaction.getSource(), is("Dividende07.txt"));
        assertThat(transaction.getNote(), is("Abrechnungsnr. | Quartalsdividende"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(58.37))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(79.01))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(11.85 + 7.75 + 0.42 + 0.62))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        account.setCurrencyCode(CurrencyUnit.EUR);
        Status s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testDividende08()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende08.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("BMG9156K1018"));
        assertThat(security.getWkn(), is("A2PNW9"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("2020 BULKERS LTD. REGISTERED SHARES DL 1"));
        assertThat(security.getCurrencyCode(), is("NOK"));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-01-31T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(57)));
        assertThat(transaction.getSource(), is("Dividende08.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.67))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.67))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of("NOK", Values.Amount.factorize(50.77))));
    }

    @Test
    public void testDividende08WithSecurityInEUR()
    {
        Security security = new Security("2020 BULKERS LTD. REGISTERED SHARES DL 1", CurrencyUnit.EUR);
        security.setIsin("BMG9156K1018");
        security.setWkn("A2PNW9");

        Client client = new Client();
        client.addSecurity(security);

        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende08.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.DIVIDENDS));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2023-01-31T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(57)));
        assertThat(transaction.getSource(), is("Dividende08.txt"));
        assertNull(transaction.getNote());

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.67))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.67))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        account.setCurrencyCode(CurrencyUnit.EUR);
        Status s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testDividende09()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende09.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000A0D8Q49"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("iSh.DJ U.S.Select Div.U.ETF DE Inhaber-Anteile"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.USD));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.TAXES));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-01-15T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(62.489)));
        assertThat(transaction.getSource(), is("Dividende09.txt"));
        assertThat(transaction.getNote(), is("Abrechnungs-Nr. 70314707 | Ertragsthesaurierung für 2017 (54,16 USD)"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.65))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.65))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        Unit grossValueUnit = transaction.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(IllegalArgumentException::new);
        assertThat(grossValueUnit.getForex(), is(Money.of(CurrencyUnit.USD, Values.Amount.factorize(0.78))));
    }

    @Test
    public void testDividende09WithSecurityInEUR()
    {
        Security security = new Security("iSh.DJ U.S.Select Div.U.ETF DE Inhaber-Anteile", CurrencyUnit.EUR);
        security.setIsin("DE000A0D8Q49");

        Client client = new Client();
        client.addSecurity(security);

        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende09.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.TAXES));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-01-15T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(62.489)));
        assertThat(transaction.getSource(), is("Dividende09.txt"));
        assertThat(transaction.getNote(), is("Abrechnungs-Nr. 70314707 | Ertragsthesaurierung für 2017 (54,16 USD)"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.65))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.65))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));

        CheckCurrenciesAction c = new CheckCurrenciesAction();
        Account account = new Account();
        account.setCurrencyCode(CurrencyUnit.EUR);
        Status s = c.process(transaction, account);
        assertThat(s, is(Status.OK_STATUS));
    }

    @Test
    public void testDividende10()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende10.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE0005933923"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("iShares MDAX UCITS ETF DE Inhaber-Anteile"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check dividends transaction
        AccountTransaction transaction = (AccountTransaction) results.stream().filter(TransactionItem.class::isInstance)
                        .findFirst().orElseThrow(IllegalArgumentException::new).getSubject();

        assertThat(transaction.getType(), is(AccountTransaction.Type.TAXES));

        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2018-01-15T00:00")));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0.254)));
        assertThat(transaction.getSource(), is("Dividende10.txt"));
        assertThat(transaction.getNote(), is("Abrechnungs-Nr. 61314054 | Ertragsthesaurierung für 2017 (0,68 EUR)"));

        assertThat(transaction.getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.01))));
        assertThat(transaction.getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.01))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividende11()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende11.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("FI0009000459"), hasWkn("870740"), hasTicker(null), //
                        hasName("HUHTAMAEKI OYJ REGISTERED SHARES O.N."), //
                        hasCurrencyCode("EUR"))));

        // check dividende transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-05-09T00:00"), hasShares(28), //
                        hasSource("Dividende11.txt"), //
                        hasNote("Abrechnungsnr. 12345678 | Zwischendividende"), //
                        hasAmount("EUR", 9.10), hasGrossValue("EUR", 14.00), //
                        hasTaxes("EUR", 4.90), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende12()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende12.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00B14X4T88"), hasWkn(null), hasTicker(null), //
                        hasName("iShs-Asia Pacific Div.U.ETF Registered Shares USD (Dist)oN"), //
                        hasCurrencyCode("USD"))));

        // check cancellation (Amount = 0,00) transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        taxes( //
                                        hasDate("2017-12-31"), hasShares(6.064), //
                                        hasSource("Dividende12.txt"), //
                                        hasNote("Thesaurierung (0,52 EUR)"), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasForexGrossValue("USD", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testDividende13()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende13.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US4781601046"), hasWkn(null), hasTicker(null), //
                        hasName("Johnson & Johnson Registered Shares DL 1"), //
                        hasCurrencyCode("USD"))));

        // check dividende transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2021-09-07T00:00"), hasShares(8.000), //
                        hasSource("Dividende13.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 5.30), hasGrossValue("EUR", 7.12), //
                        hasForexGrossValue("USD", 8.47), //
                        hasTaxes("EUR", 1.07 + 0.71 + 0.04), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende14()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende14.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US9884981013"), hasWkn("909190"), hasTicker(null), //
                        hasName("YUM! BRANDS, INC. REGISTERED SHARES O.N."), //
                        hasCurrencyCode("USD"))));

        // check dividende transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-12-08T00:00"), hasShares(105.000), //
                        hasSource("Dividende14.txt"), //
                        hasNote("Abrechnungsnr. 84528768080 | Quartalsdividende"), //
                        hasAmount("EUR", 43.51), hasGrossValue("EUR", 58.89), //
                        hasForexGrossValue("USD", 63.53), //
                        hasTaxes("EUR", 8.83 + 5.78 + 0.31 + 0.46), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende14WithSecurityInEUR()
    {
        Security security = new Security("YUM! BRANDS, INC. REGISTERED SHARES O.N.", CurrencyUnit.EUR);
        security.setIsin("US9884981013");
        security.setWkn("909190");

        Client client = new Client();
        client.addSecurity(security);

        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende14.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-12-08T00:00"), hasShares(105.000), //
                        hasSource("Dividende14.txt"), //
                        hasNote("Abrechnungsnr. 84528768080 | Quartalsdividende"), //
                        hasAmount("EUR", 43.51), hasGrossValue("EUR", 58.89), //
                        hasTaxes("EUR", 8.83 + 5.78 + 0.31 + 0.46), hasFees("EUR", 0.00), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Account account = new Account();
                            account.setCurrencyCode(CurrencyUnit.EUR);
                            Status s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende15()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende15.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("US5949181045"), hasWkn("870747"), hasTicker(null), //
                        hasName("MICROSOFT CORP. REGISTERED SHARES DL-,00000625"), //
                        hasCurrencyCode("USD"))));

        // check dividende transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-12-14T00:00"), hasShares(47.00), //
                        hasSource("Dividende15.txt"), //
                        hasNote("Abrechnungsnr. 84953682750 | Quartalsdividende"), //
                        hasAmount("EUR", 23.64), hasGrossValue("EUR", 31.99), //
                        hasForexGrossValue("USD", 35.25), //
                        hasTaxes("EUR", 4.80 + 3.13 + 0.17 + 0.25), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende15WithSecurityInEUR()
    {
        Security security = new Security("MICROSOFT CORP. REGISTERED SHARES DL-,00000625", CurrencyUnit.EUR);
        security.setIsin("US5949181045");
        security.setWkn("870747");

        Client client = new Client();
        client.addSecurity(security);

        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(client);

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende15.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check dividends transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2023-12-14T00:00"), hasShares(47.00), //
                        hasSource("Dividende15.txt"), //
                        hasNote("Abrechnungsnr. 84953682750 | Quartalsdividende"), //
                        hasAmount("EUR", 23.64), hasGrossValue("EUR", 31.99), //
                        hasTaxes("EUR", 4.80 + 3.13 + 0.17 + 0.25), hasFees("EUR", 0.00), //
                        check(tx -> {
                            CheckCurrenciesAction c = new CheckCurrenciesAction();
                            Account account = new Account();
                            account.setCurrencyCode(CurrencyUnit.EUR);
                            Status s = c.process((AccountTransaction) tx, account);
                            assertThat(s, is(Status.OK_STATUS));
                        }))));
    }

    @Test
    public void testDividende16()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende16.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000A0H0785"), hasWkn(null), hasTicker(null), //
                        hasName("iS.EO G.B.C.1.5-10.5y.U.ETF DE Inhaber-Anteile"), //
                        hasCurrencyCode("EUR"))));

        // check dividende transaction
        assertThat(results, hasItem(dividend( //
                        hasDate("2016-11-15T00:00"), hasShares(105.00), //
                        hasSource("Dividende16.txt"), //
                        hasNote("Ertrag für 2016/17 (57,56 EUR)"), //
                        hasAmount("EUR", 41.44), hasGrossValue("EUR", 57.56), //
                        hasTaxes("EUR", 14.08 + 0.77 + 1.27), hasFees("EUR", 0.00))));
    }

    @Test
    public void testDividende17()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Dividende17.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE000ETFL235"), hasWkn(null), hasTicker(null), //
                        hasName("Deka DAXplus Maximum Div.U.ETF Inhaber-Anteile"), //
                        hasCurrencyCode("EUR"))));

        // check dividende transaction
        assertThat(results, hasItem(taxRefund( //
                        hasDate("2018-01-15T00:00"), hasShares(165.00), //
                        hasSource("Dividende17.txt"), //
                        hasNote("Abrechnungs-Nr. 13753098 | Ertragsthesaurierung für 2017 (0,94 EUR)"), //
                        hasAmount("EUR", 4.69), hasGrossValue("EUR", 4.69), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testVorabpauschale01()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Vorabpauschale01.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("IE00BYZK4552"), hasWkn("A2ANH0"), hasTicker(null), //
                        hasName("ISHSIV-AUTOMATION&ROBOT.U.ETF REGISTERED SHARES O.N."), //
                        hasCurrencyCode("EUR"))));

        // check taxes transaction
        assertThat(results, hasItem(taxes( //
                        hasDate("2024-01-02T00:00"), hasShares(155), //
                        hasSource("Vorabpauschale01.txt"), //
                        hasNote("Abrechnungsnr. 51274930950"), //
                        hasAmount("EUR", 4.86), hasGrossValue("EUR", 4.86), //
                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
    }

    @Test
    public void testVorabpauschale02()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Vorabpauschale02.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("LU0629459743"), hasWkn("A1JA1R"), hasTicker(null), //
                        hasName("UBS(L)FS-MSCI WLD SOC.RSP.UETF NAMENS-ANTEILE (USD) A-DIS O.N"), //
                        hasCurrencyCode("EUR"))));

        // check taxes transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        taxes( //
                                        hasDate("2024-01-02T00:00"), hasShares(8.781), //
                                        hasSource("Vorabpauschale02.txt"), //
                                        hasNote("Abrechnungsnr. 51265850630"), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testVorabpauschale03()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Vorabpauschale03.txt"), errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(1L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        assertThat(results, hasItem(security( //
                        hasIsin("DE0005933923"), hasWkn("593392"), hasTicker(null), //
                        hasName("ISHARES MDAX UCITS ETF DE"), //
                        hasCurrencyCode("EUR"))));

        // check taxes transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        taxes( //
                                        hasDate("2024-01-02T00:00"), hasShares(12.2335), //
                                        hasSource("Vorabpauschale03.txt"), //
                                        hasNote("Abrechnungsnr. 51299965770"), //
                                        hasAmount("EUR", 0.00), hasGrossValue("EUR", 0.00), //
                                        hasTaxes("EUR", 0.00), hasFees("EUR", 0.00)))));
    }

    @Test
    public void testDividendeStorno01()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DividendeStorno01.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE000A0D8QZ7"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("iSh.ST.Euro.Small 200 U.ETF DE Inhaber-Anteile"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check cancellation (Storno) transaction
        TransactionItem cancellation = (TransactionItem) results.stream() //
                        .filter(i -> i.isFailure()) //
                        .filter(TransactionItem.class::isInstance) //
                        .findFirst().orElseThrow(IllegalArgumentException::new);

        assertThat(((AccountTransaction) cancellation.getSubject()).getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(cancellation.getFailureMessage(), is(Messages.MsgErrorOrderCancellationUnsupported));

        assertThat(((Transaction) cancellation.getSubject()).getDateTime(), is(LocalDateTime.parse("2016-06-15T00:00")));
        assertThat(((Transaction) cancellation.getSubject()).getShares(), is(Values.Share.factorize(84.092)));
        assertThat(((Transaction) cancellation.getSubject()).getSource(), is("DividendeStorno01.txt"));
        assertThat(((Transaction) cancellation.getSubject()).getNote(), is("Abrechnungs-Nr. 60667425 | Ertrag für 2015/16 (20,24 EUR)"));

        assertThat(((Transaction) cancellation.getSubject()).getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(20.24))));
        assertThat(((Transaction) cancellation.getSubject()).getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(20.28))));
        assertThat(((Transaction) cancellation.getSubject()).getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.04))));
        assertThat(((Transaction) cancellation.getSubject()).getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testDividendeStorno02()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "DividendeStorno02.txt"), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check security
        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();
        assertThat(security.getIsin(), is("DE0002635281"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("iSh.EO ST.Sel.Div.30 U.ETF DE Inhaber-Anteile"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));

        // check cancellation (Storno) transaction
        TransactionItem cancellation = (TransactionItem) results.stream() //
                        .filter(i -> i.isFailure()) //
                        .filter(TransactionItem.class::isInstance) //
                        .findFirst().orElseThrow(IllegalArgumentException::new);

        assertThat(((AccountTransaction) cancellation.getSubject()).getType(), is(AccountTransaction.Type.TAX_REFUND));
        assertThat(cancellation.getFailureMessage(), is(Messages.MsgErrorOrderCancellationUnsupported));

        assertThat(((Transaction) cancellation.getSubject()).getDateTime(), is(LocalDateTime.parse("2018-01-15T00:00")));
        assertThat(((Transaction) cancellation.getSubject()).getShares(), is(Values.Share.factorize(195.419)));
        assertThat(((Transaction) cancellation.getSubject()).getSource(), is("DividendeStorno02.txt"));
        assertThat(((Transaction) cancellation.getSubject()).getNote(), is("Abrechnungs-Nr. 26495157 | Ertragsthesaurierung für 2017 (20,73 EUR)"));

        assertThat(((Transaction) cancellation.getSubject()).getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.05))));
        assertThat(((Transaction) cancellation.getSubject()).getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.05))));
        assertThat(((Transaction) cancellation.getSubject()).getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(((Transaction) cancellation.getSubject()).getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testGiroKontoauszug01()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug01.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(20L));
        assertThat(results.size(), is(20));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-03-01"), hasAmount("EUR", 1.00), //
                        hasSource("GiroKontoauszug01.txt"), hasNote("Überweisung online"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-03-01"), hasAmount("EUR", 1.50), //
                        hasSource("GiroKontoauszug01.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-03-03"), hasAmount("EUR", 1.80), //
                        hasSource("GiroKontoauszug01.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-03-06"), hasAmount("EUR", 2.00), //
                        hasSource("GiroKontoauszug01.txt"), hasNote("Überweisung online"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-03-06"), hasAmount("EUR", 3.00), //
                        hasSource("GiroKontoauszug01.txt"), hasNote("Überweisung online"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-03-08"), hasAmount("EUR", 4.00), //
                        hasSource("GiroKontoauszug01.txt"), hasNote("Überweisung online"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-03-08"), hasAmount("EUR", 3.00), //
                        hasSource("GiroKontoauszug01.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-03-14"), hasAmount("EUR", 10.00), //
                        hasSource("GiroKontoauszug01.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-03-14"), hasAmount("EUR", 8.60), //
                        hasSource("GiroKontoauszug01.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-03-20"), hasAmount("EUR", 224.00), //
                        hasSource("GiroKontoauszug01.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-03-20"), hasAmount("EUR", 189.90), //
                        hasSource("GiroKontoauszug01.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-03-21"), hasAmount("EUR", 7.87), //
                        hasSource("GiroKontoauszug01.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-03-23"), hasAmount("EUR", 90.18), //
                        hasSource("GiroKontoauszug01.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-03-23"), hasAmount("EUR", 189.90), //
                        hasSource("GiroKontoauszug01.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-03-27"), hasAmount("EUR", 191.00), //
                        hasSource("GiroKontoauszug01.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-03-28"), hasAmount("EUR", 8.60), //
                        hasSource("GiroKontoauszug01.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-03-30"), hasAmount("EUR", 1000.00), //
                        hasSource("GiroKontoauszug01.txt"), hasNote("Dauerauftrag"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2023-03-30"), hasAmount("EUR", 25.63), //
                        hasSource("GiroKontoauszug01.txt"), hasNote("Lohn, Gehalt, Rente"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2023-03-31"), hasAmount("EUR", 5.40), //
                        hasSource("GiroKontoauszug01.txt"), hasNote("Entgelte vom 01.03.2023 bis 31.03.2023"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2023-03-31"), hasAmount("EUR", 0.00), //
                        hasSource("GiroKontoauszug01.txt"), hasNote("Abrechnungszeitraum vom 01.01.2023 bis 31.03.2023"))));

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        interest(hasDate("2023-03-31"), hasAmount("EUR", 0.00), //
                        hasSource("GiroKontoauszug01.txt"), hasNote("Abrechnungszeitraum vom 01.01.2023 bis 31.03.2023")))));
    }

    @Test
    public void testGiroKontoauszug02()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug02.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(16L));
        assertThat(results.size(), is(16));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check transaction
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(16L));

        Item item = iter.next();

        // assert transaction
        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-03-02T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.00))));
        assertThat(transaction.getSource(), is("GiroKontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Lastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-03-02T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.00))));
        assertThat(transaction.getSource(), is("GiroKontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Überweisung online"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-03-02T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3.50))));
        assertThat(transaction.getSource(), is("GiroKontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Lastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-03-04T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(8.60))));
        assertThat(transaction.getSource(), is("GiroKontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Lastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-03-09T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10.00))));
        assertThat(transaction.getSource(), is("GiroKontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Überweisung online"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-03-09T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.00))));
        assertThat(transaction.getSource(), is("GiroKontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Überweisung online"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-03-09T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5.96))));
        assertThat(transaction.getSource(), is("GiroKontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Kartenzahlung"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-03-12T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5.00))));
        assertThat(transaction.getSource(), is("GiroKontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Überweisung online"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-03-19T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.00))));
        assertThat(transaction.getSource(), is("GiroKontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Lastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-03-19T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(8.60))));
        assertThat(transaction.getSource(), is("GiroKontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Lastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-03-30T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3.15))));
        assertThat(transaction.getSource(), is("GiroKontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Lastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-03-30T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(9999.37))));
        assertThat(transaction.getSource(), is("GiroKontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Lohn, Gehalt, Rente"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-03-31T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.00))));
        assertThat(transaction.getSource(), is("GiroKontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Überweisung online"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-03-31T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(6.99))));
        assertThat(transaction.getSource(), is("GiroKontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Lastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-03-31T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.70))));
        assertThat(transaction.getSource(), is("GiroKontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Entgelte vom 29.02.2020 bis 31.03.2020"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST_CHARGE));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-03-31T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.66))));
        assertThat(transaction.getSource(), is("GiroKontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Abrechnungszeitraum vom 01.01.2020 bis 31.03.2020"));
    }

    @Test
    public void testGiroKontoauszug03()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug03.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(8L));
        assertThat(results.size(), is(8));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check transaction
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(8L));

        Item item = iter.next();

        // assert transaction
        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-09-15T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(39.52))));
        assertThat(transaction.getSource(), is("GiroKontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Überweisung online"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-09-21T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(28.68))));
        assertThat(transaction.getSource(), is("GiroKontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Überweisung online"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-09-25T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(9.75))));
        assertThat(transaction.getSource(), is("GiroKontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Lastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-09-28T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3830.16))));
        assertThat(transaction.getSource(), is("GiroKontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Überweisung online"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-09-29T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4343.22))));
        assertThat(transaction.getSource(), is("GiroKontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Lohn, Gehalt, Rente"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-09-30T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(500.00))));
        assertThat(transaction.getSource(), is("GiroKontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Dauerauftrag"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-09-30T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3.20))));
        assertThat(transaction.getSource(), is("GiroKontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Entgelte vom 01.09.2020 bis 30.09.2020"));

        item = iter.next();

        // assert transaction is cancellation
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.INTEREST));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2020-09-30T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(transaction.getSource(), is("GiroKontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Abrechnungszeitraum vom 01.07.2020 bis 30.09.2020"));

        // check cancellation (Amount = 0,00) transaction
        TransactionItem cancellation = (TransactionItem) results.stream() //
                        .filter(i -> i.isFailure()) //
                        .filter(TransactionItem.class::isInstance) //
                        .findFirst().orElseThrow(IllegalArgumentException::new);

        assertThat(((AccountTransaction) cancellation.getSubject()).getType(), is(AccountTransaction.Type.INTEREST));
        assertThat(cancellation.getFailureMessage(), is(Messages.MsgErrorTransactionTypeNotSupported));

        assertThat(((Transaction) cancellation.getSubject()).getDateTime(), is(LocalDateTime.parse("2020-09-30T00:00")));
        assertThat(((Transaction) cancellation.getSubject()).getShares(), is(Values.Share.factorize(0)));
        assertThat(((Transaction) cancellation.getSubject()).getSource(), is("GiroKontoauszug03.txt"));
        assertThat(transaction.getNote(), is("Abrechnungszeitraum vom 01.07.2020 bis 30.09.2020"));

        assertThat(((Transaction) cancellation.getSubject()).getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(((Transaction) cancellation.getSubject()).getGrossValue(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(((Transaction) cancellation.getSubject()).getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
        assertThat(((Transaction) cancellation.getSubject()).getUnitSum(Unit.Type.FEE),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.00))));
    }

    @Test
    public void testGiroKontoauszug04()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug04.txt"),
                        errors);

//        assertThat(errors, empty());
//        assertThat(countSecurities(results), is(0L));
//        assertThat(countBuySell(results), is(0L));
//        assertThat(countAccountTransactions(results), is(12L));
//        assertThat(results.size(), is(12));
//        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check transaction
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
//        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(12L));

        Item item = iter.next();

        // assert transaction
        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-08-05T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10.49))));
        assertThat(transaction.getSource(), is("GiroKontoauszug04.txt"));
        assertThat(transaction.getNote(), is("Überweisung online"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-08-06T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(40.99))));
        assertThat(transaction.getSource(), is("GiroKontoauszug04.txt"));
        assertThat(transaction.getNote(), is("Lastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-08-07T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(8.60))));
        assertThat(transaction.getSource(), is("GiroKontoauszug04.txt"));
        assertThat(transaction.getNote(), is("Lastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-08-09T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(180.99))));
        assertThat(transaction.getSource(), is("GiroKontoauszug04.txt"));
        assertThat(transaction.getNote(), is("Lastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-08-09T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(14.38))));
        assertThat(transaction.getSource(), is("GiroKontoauszug04.txt"));
        assertThat(transaction.getNote(), is("Kartenzahlung"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-08-14T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(13.20))));
        assertThat(transaction.getSource(), is("GiroKontoauszug04.txt"));
        assertThat(transaction.getNote(), is("Lastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-08-16T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(12.37))));
        assertThat(transaction.getSource(), is("GiroKontoauszug04.txt"));
        assertThat(transaction.getNote(), is("Lastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-08-19T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.60))));
        assertThat(transaction.getSource(), is("GiroKontoauszug04.txt"));
        assertThat(transaction.getNote(), is("Lastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-08-21T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2000.00))));
        assertThat(transaction.getSource(), is("GiroKontoauszug04.txt"));
        assertThat(transaction.getNote(), is("Überweisung online"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-08-27T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3.60))));
        assertThat(transaction.getSource(), is("GiroKontoauszug04.txt"));
        assertThat(transaction.getNote(), is("Lastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-08-29T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4684.55))));
        assertThat(transaction.getSource(), is("GiroKontoauszug04.txt"));
        assertThat(transaction.getNote(), is("Lohn, Gehalt, Rente"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2019-08-30T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5.60))));
        assertThat(transaction.getSource(), is("GiroKontoauszug04.txt"));
        assertThat(transaction.getNote(), is("Entgelte vom 01.08.2019 bis 30.08.2019"));
    }

    @Test
    public void testGiroKontoauszug05()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug05.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check transaction
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(3L));

        Item item = iter.next();

        // assert transaction
        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2016-03-01T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(119.00))));
        assertThat(transaction.getSource(), is("GiroKontoauszug05.txt"));
        assertThat(transaction.getNote(), is("Basis-Lastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2016-03-01T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(130.00))));
        assertThat(transaction.getSource(), is("GiroKontoauszug05.txt"));
        assertThat(transaction.getNote(), is("Zahlungseingang"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2016-02-29T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.40))));
        assertThat(transaction.getSource(), is("GiroKontoauszug05.txt"));
        assertThat(transaction.getNote(), is("Entgelte vom 30.01.2016 bis 29.02.2016"));
    }

    @Test
    public void testGiroKontoauszug06()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug06.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // check transaction
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(2L));

        Item item = iter.next();

        // assert transaction
        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-04-06T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3000.00))));
        assertThat(transaction.getSource(), is("GiroKontoauszug06.txt"));
        assertThat(transaction.getNote(), is("Überweisung"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2017-03-31T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.40))));
        assertThat(transaction.getSource(), is("GiroKontoauszug06.txt"));
        assertThat(transaction.getNote(), is("Entgelte vom 01.03.2017 bis 31.03.2017"));
    }

    @Test
    public void testGiroKontoauszug07()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug07.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2016-11-22"), hasAmount("EUR", 17.95), //
                        hasSource("GiroKontoauszug07.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2016-11-29"), hasAmount("EUR", 896.91), //
                        hasSource("GiroKontoauszug07.txt"), hasNote("Lohn, Gehalt, Rente"))));
    }

    @Test
    public void testGiroKontoauszug08()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug08.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(5L));
        assertThat(results.size(), is(5));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2018-12-04"), hasAmount("EUR", 33.40), //
                        hasSource("GiroKontoauszug08.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2018-12-06"), hasAmount("EUR", 330.37), //
                        hasSource("GiroKontoauszug08.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2018-12-27"), hasAmount("EUR", 1111.11), //
                        hasSource("GiroKontoauszug08.txt"), hasNote("Lohn, Gehalt, Rente"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2018-12-28"), hasAmount("EUR", 4.40), //
                        hasSource("GiroKontoauszug08.txt"), hasNote("Entgelte vom 01.12.2018 bis 28.12.2018"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2018-12-31"), hasAmount("EUR", 0.00), //
                        hasSource("GiroKontoauszug08.txt"), hasNote("Abrechnungszeitraum vom 01.10.2018 bis 31.12.2018"))));

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        interest(hasDate("2018-12-31"), hasAmount("EUR", 0.00), //
                        hasSource("GiroKontoauszug08.txt"), hasNote("Abrechnungszeitraum vom 01.10.2018 bis 31.12.2018")))));
    }

    @Test
    public void testGiroKontoauszug09()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug09.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(3L));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2019-12-27"), hasAmount("EUR", 1111.11), //
                        hasSource("GiroKontoauszug09.txt"), hasNote("Lohn, Gehalt, Rente"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2019-12-30"), hasAmount("EUR", 6.20), //
                        hasSource("GiroKontoauszug09.txt"), hasNote("Entgelte vom 30.11.2019 bis 30.12.2019"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2019-12-31"), hasAmount("EUR", 0.00), //
                        hasSource("GiroKontoauszug09.txt"), hasNote("Abrechnungszeitraum vom 01.10.2019 bis 31.12.2019"))));

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        interest(hasDate("2019-12-31"), hasAmount("EUR", 0.00), //
                        hasSource("GiroKontoauszug09.txt"), hasNote("Abrechnungszeitraum vom 01.10.2019 bis 31.12.2019")))));
    }

    @Test
    public void testGiroKontoauszug10()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug10.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(7L));
        assertThat(results.size(), is(7));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2014-06-30"), hasAmount("EUR", 14.99), //
                        hasSource("GiroKontoauszug10.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2014-06-30"), hasAmount("EUR", 400.00), //
                        hasSource("GiroKontoauszug10.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2014-07-01"), hasAmount("EUR", 119.00), //
                        hasSource("GiroKontoauszug10.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2014-07-01"), hasAmount("EUR", 28.50), //
                        hasSource("GiroKontoauszug10.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2014-07-07"), hasAmount("EUR", 500.00), //
                        hasSource("GiroKontoauszug10.txt"), hasNote("Geldautomat"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2014-06-30"), hasAmount("EUR", 4.05), //
                        hasSource("GiroKontoauszug10.txt"), hasNote("Entgelte vom 31.05.2014 bis 30.06.2014"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2014-06-30"), hasAmount("EUR", 0.00), //
                        hasSource("GiroKontoauszug10.txt"), hasNote("Abrechnungszeitraum vom 01.04.2014 bis 30.06.2014"))));

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        interest(hasDate("2014-06-30"), hasAmount("EUR", 0.00), //
                        hasSource("GiroKontoauszug10.txt"), hasNote("Abrechnungszeitraum vom 01.04.2014 bis 30.06.2014")))));
    }

    @Test
    public void testGiroKontoauszug11()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug11.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2015-06-23"), hasAmount("EUR", 500.00), //
                        hasSource("GiroKontoauszug11.txt"), hasNote("Barumsatz"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2015-06-30"), hasAmount("EUR", 3.55), //
                        hasSource("GiroKontoauszug11.txt"), hasNote("Entgelte vom 30.05.2015 bis 30.06.2015"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2015-06-30"), hasAmount("EUR", 0.00), //
                        hasSource("GiroKontoauszug11.txt"), hasNote("Abrechnungszeitraum vom 01.04.2015 bis 30.06.2015"))));

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        interest(hasDate("2015-06-30"), hasAmount("EUR", 0.00), //
                        hasSource("GiroKontoauszug11.txt"), hasNote("Abrechnungszeitraum vom 01.04.2015 bis 30.06.2015")))));
    }

    @Test
    public void testGiroKontoauszug12()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug12.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(9L));
        assertThat(results.size(), is(9));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2018-04-24"), hasAmount("EUR", 87.59), //
                        hasSource("GiroKontoauszug12.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2018-04-27"), hasAmount("EUR", 1111.11), //
                        hasSource("GiroKontoauszug12.txt"), hasNote("Lohn, Gehalt, Rente"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2018-04-30"), hasAmount("EUR", 400.00), //
                        hasSource("GiroKontoauszug12.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2018-04-30"), hasAmount("EUR", 91.35), //
                        hasSource("GiroKontoauszug12.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2018-05-02"), hasAmount("EUR", 260.00), //
                        hasSource("GiroKontoauszug12.txt"), hasNote("Basislastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2018-05-02"), hasAmount("EUR", 33.48), //
                        hasSource("GiroKontoauszug12.txt"), hasNote("Basislastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2018-05-03"), hasAmount("EUR", 4000.00), //
                        hasSource("GiroKontoauszug12.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2018-05-03"), hasAmount("EUR", 11.90), //
                        hasSource("GiroKontoauszug12.txt"), hasNote("Basislastschrift"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2018-04-30"), hasAmount("EUR", 4.40), //
                        hasSource("GiroKontoauszug12.txt"), hasNote("Entgelte vom 30.03.2018 bis 30.04.2018"))));
    }

    @Test
    public void testGiroKontoauszug13()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug13.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2017-01-23"), hasAmount("EUR", 303.70), //
                        hasSource("GiroKontoauszug13.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2017-02-01"), hasAmount("EUR", 5.00), //
                        hasSource("GiroKontoauszug13.txt"), hasNote("sonstige Entgelte"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2017-01-31"), hasAmount("EUR", 4.10), //
                        hasSource("GiroKontoauszug13.txt"), hasNote("Entgelte vom 31.12.2016 bis 31.01.2017"))));
    }

    @Test
    public void testGiroKontoauszug14()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug14.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(7L));
        assertThat(results.size(), is(7));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2016-01-14"), hasAmount("EUR", 5.71), //
                        hasSource("GiroKontoauszug14.txt"), hasNote("Basis-Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2016-01-14"), hasAmount("EUR", 7.90), //
                        hasSource("GiroKontoauszug14.txt"), hasNote("Basis-Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2016-01-15"), hasAmount("EUR", 29.70), //
                        hasSource("GiroKontoauszug14.txt"), hasNote("Basis-Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2016-01-22"), hasAmount("EUR", 483.99), //
                        hasSource("GiroKontoauszug14.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2016-01-25"), hasAmount("EUR", 304.02), //
                        hasSource("GiroKontoauszug14.txt"), hasNote("sonstige Buchung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2016-01-28"), hasAmount("EUR", 1981.40), //
                        hasSource("GiroKontoauszug14.txt"), hasNote("Lohn, Gehalt, Rente"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2016-01-28"), hasAmount("EUR", 420.25), //
                        hasSource("GiroKontoauszug14.txt"), hasNote("Zahlungseingang"))));
    }

    @Test
    public void testGiroKontoauszug15()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug15.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(9L));
        assertThat(results.size(), is(9));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2019-03-04"), hasAmount("EUR", 3000.00), //
                        hasSource("GiroKontoauszug15.txt"), hasNote("Überweisung online"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2019-03-05"), hasAmount("EUR", 2200.00), //
                        hasSource("GiroKontoauszug15.txt"), hasNote("Überweisung online"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2019-03-18"), hasAmount("EUR", 750.00), //
                        hasSource("GiroKontoauszug15.txt"), hasNote("Bargeldauszahlung (Debitkarte)"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2019-03-18"), hasAmount("EUR", 64.71), //
                        hasSource("GiroKontoauszug15.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2019-03-18"), hasAmount("EUR", 25.75), //
                        hasSource("GiroKontoauszug15.txt"), hasNote("Überweisung online"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2019-03-22"), hasAmount("EUR", 2092.00), //
                        hasSource("GiroKontoauszug15.txt"), hasNote("Bargeldeinzahlung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2019-03-28"), hasAmount("EUR", 25.75), //
                        hasSource("GiroKontoauszug15.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2019-03-29"), hasAmount("EUR", 5.00), //
                        hasSource("GiroKontoauszug15.txt"), hasNote("Entgelte vom 01.03.2019 bis 29.03.2019"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2019-03-31"), hasAmount("EUR", 0.00), //
                        hasSource("GiroKontoauszug15.txt"), hasNote("Abrechnungszeitraum vom 01.01.2019 bis 31.03.2019"))));

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        interest(hasDate("2019-03-31"), hasAmount("EUR", 0.00), //
                        hasSource("GiroKontoauszug15.txt"), hasNote("Abrechnungszeitraum vom 01.01.2019 bis 31.03.2019")))));
    }

    @Test
    public void testGiroKontoauszug16()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug16.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(15L));
        assertThat(results.size(), is(15));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-11-02"), hasAmount("EUR", 600.00), //
                        hasSource("GiroKontoauszug16.txt"), hasNote("Dauerauftrag"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-11-02"), hasAmount("EUR", 34.50), //
                        hasSource("GiroKontoauszug16.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-11-08"), hasAmount("EUR", 1000.00), //
                        hasSource("GiroKontoauszug16.txt"), hasNote("Überweisung online"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-11-16"), hasAmount("EUR", 57.30), //
                        hasSource("GiroKontoauszug16.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-11-16"), hasAmount("EUR", 8.30), //
                        hasSource("GiroKontoauszug16.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-11-23"), hasAmount("EUR", 86.39), //
                        hasSource("GiroKontoauszug16.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-11-23"), hasAmount("EUR", 12.34), //
                        hasSource("GiroKontoauszug16.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-11-24"), hasAmount("EUR", 73.73), //
                        hasSource("GiroKontoauszug16.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-11-29"), hasAmount("EUR", 250.00), //
                        hasSource("GiroKontoauszug16.txt"), hasNote("Überweisung online"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-11-29"), hasAmount("EUR", 10.00), //
                        hasSource("GiroKontoauszug16.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-11-29"), hasAmount("EUR", 1111.11), //
                        hasSource("GiroKontoauszug16.txt"), hasNote("Lohn, Gehalt, Rente"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-11-30"), hasAmount("EUR", 1000.00), //
                        hasSource("GiroKontoauszug16.txt"), hasNote("Überweisung online"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-11-30"), hasAmount("EUR", 600.00), //
                        hasSource("GiroKontoauszug16.txt"), hasNote("Dauerauftrag"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-11-30"), hasAmount("EUR", 10.96), //
                        hasSource("GiroKontoauszug16.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2021-11-30"), hasAmount("EUR", 5.10), //
                        hasSource("GiroKontoauszug16.txt"), hasNote("Entgelte vom 30.10.2021 bis 30.11.2021"))));
    }

    @Test
    public void testGiroKontoauszug17()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug17.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(9L));
        assertThat(results.size(), is(9));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2022-12-09"), hasAmount("EUR", 10.00), //
                        hasSource("GiroKontoauszug17.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2022-12-12"), hasAmount("EUR", 98.07), //
                        hasSource("GiroKontoauszug17.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2022-12-27"), hasAmount("EUR", 250.00), //
                        hasSource("GiroKontoauszug17.txt"), hasNote("Überweisung online"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2022-12-28"), hasAmount("EUR", 10.00), //
                        hasSource("GiroKontoauszug17.txt"), hasNote("Rechnung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2022-12-28"), hasAmount("EUR", 23.35), //
                        hasSource("GiroKontoauszug17.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2022-12-29"), hasAmount("EUR", 1111.11), //
                        hasSource("GiroKontoauszug17.txt"), hasNote("Lohn, Gehalt, Rente"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2022-12-30"), hasAmount("EUR", 600.00), //
                        hasSource("GiroKontoauszug17.txt"), hasNote("Dauerauftrag"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2022-12-30"), hasAmount("EUR", 4.60), //
                        hasSource("GiroKontoauszug17.txt"), hasNote("Entgelte vom 01.12.2022 bis 30.12.2022"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2022-12-31"), hasAmount("EUR", 0.00), //
                        hasSource("GiroKontoauszug17.txt"), hasNote("Abrechnungszeitraum vom 01.10.2022 bis 31.12.2022"))));

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        interest(hasDate("2022-12-31"), hasAmount("EUR", 0.00), //
                        hasSource("GiroKontoauszug17.txt"), hasNote("Abrechnungszeitraum vom 01.10.2022 bis 31.12.2022")))));
    }

    @Test
    public void testGiroKontoauszug18()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug18.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2008-07-08"), hasAmount("EUR", 103.75), //
                        hasSource("GiroKontoauszug18.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2008-07-08"), hasAmount("EUR", 100.00), //
                        hasSource("GiroKontoauszug18.txt"), hasNote("Zahlungseingang"))));
    }

    @Test
    public void testGiroKontoauszug19()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug19.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(11L));
        assertThat(results.size(), is(11));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2008-09-24"), hasAmount("EUR", 72.00), //
                        hasSource("GiroKontoauszug19.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2008-09-25"), hasAmount("EUR", 259.85), //
                        hasSource("GiroKontoauszug19.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2008-09-25"), hasAmount("EUR", 21.98), //
                        hasSource("GiroKontoauszug19.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2008-09-29"), hasAmount("EUR", 418.16), //
                        hasSource("GiroKontoauszug19.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2008-09-29"), hasAmount("EUR", 227.21), //
                        hasSource("GiroKontoauszug19.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2008-09-30"), hasAmount("EUR", 12.00), //
                        hasSource("GiroKontoauszug19.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2008-09-30"), hasAmount("EUR", 1111.06), //
                        hasSource("GiroKontoauszug19.txt"), hasNote("Lohn, Gehalt, Rente"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2008-10-02"), hasAmount("EUR", 39.00), //
                        hasSource("GiroKontoauszug19.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2008-10-02"), hasAmount("EUR", 13.28), //
                        hasSource("GiroKontoauszug19.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2008-10-06"), hasAmount("EUR", 356.25), //
                        hasSource("GiroKontoauszug19.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(interestCharge(hasDate("2008-09-30"), hasAmount("EUR", 0.15), //
                        hasSource("GiroKontoauszug19.txt"), hasNote("Abrechnungszeitraum vom 01.07.2008 bis 30.09.2008"))));
    }

    @Test
    public void testGiroKontoauszug20()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug20.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(4L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2015-08-31"), hasAmount("EUR", 54.69), //
                        hasSource("GiroKontoauszug20.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2015-09-01"), hasAmount("EUR", 29.50), //
                        hasSource("GiroKontoauszug20.txt"), hasNote("Basis-Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2015-09-01"), hasAmount("EUR", 200.00), //
                        hasSource("GiroKontoauszug20.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2015-08-31"), hasAmount("EUR", 6.20), //
                        hasSource("GiroKontoauszug20.txt"), hasNote("Entgelte vom 01.08.2015 bis 31.08.2015"))));
    }

    @Test
    public void testGiroKontoauszug21()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug21.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(4L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2014-12-01"), hasAmount("EUR", 400.00), //
                        hasSource("GiroKontoauszug21.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2014-12-01"), hasAmount("EUR", 119.00), //
                        hasSource("GiroKontoauszug21.txt"), hasNote("Basis-Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2014-12-01"), hasAmount("EUR", 28.50), //
                        hasSource("GiroKontoauszug21.txt"), hasNote("Basis-Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2014-11-28"), hasAmount("EUR", 3.55 - 1.80), //
                        hasSource("GiroKontoauszug21.txt"), hasNote("Entgelte vom 01.11.2014 bis 28.11.2014"))));
    }

    @Test
    public void testGiroKontoauszug22()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug22.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2012-11-26"), hasAmount("EUR", 440.00), //
                        hasSource("GiroKontoauszug22.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2012-11-30"), hasAmount("EUR", 1111.110), //
                        hasSource("GiroKontoauszug22.txt"), hasNote("Lohn, Gehalt, Rente"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2012-11-30"), hasAmount("EUR", 4.30 - 1.80), //
                        hasSource("GiroKontoauszug22.txt"), hasNote("Entgelte vom 01.11.2012 bis 30.11.2012"))));
    }

    @Test
    public void testGiroKontoauszug23()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug23.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2013-12-27"), hasAmount("EUR", 187.20), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2013-12-30"), hasAmount("EUR", 5.30), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Entgelte vom 30.11.2013 bis 30.12.2013"))));

        // assert transaction
        assertThat(results, hasItem(interestCharge(hasDate("2013-12-31"), hasAmount("EUR", 0.74), //
                        hasSource("GiroKontoauszug23.txt"), hasNote("Abrechnungszeitraum vom 01.10.2013 bis 31.12.2013"))));
    }

    @Test
    public void testGiroKontoauszug24()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug24.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2012-12-17"), hasAmount("EUR", 750.00), //
                        hasSource("GiroKontoauszug24.txt"), hasNote("Geldautomat"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2012-12-28"), hasAmount("EUR", 4.30 - 1.80), //
                        hasSource("GiroKontoauszug24.txt"), hasNote("Entgelte vom 01.12.2012 bis 28.12.2012"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2012-12-31"), hasAmount("EUR", 0.00), //
                        hasSource("GiroKontoauszug24.txt"), hasNote("Abrechnungszeitraum vom 01.10.2012 bis 31.12.2012"))));

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        interest(hasDate("2012-12-31"), hasAmount("EUR", 0.00), //
                        hasSource("GiroKontoauszug24.txt"), hasNote("Abrechnungszeitraum vom 01.10.2012 bis 31.12.2012")))));
    }

    @Test
    public void testGiroKontoauszug25()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug25.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(10L));
        assertThat(results.size(), is(10));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2011-06-24"), hasAmount("EUR", 35.00), //
                        hasSource("GiroKontoauszug25.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2011-06-27"), hasAmount("EUR", 223.32), //
                        hasSource("GiroKontoauszug25.txt"), hasNote("Eigene Kreditkartenabrechnung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2011-06-27"), hasAmount("EUR", 44.00), //
                        hasSource("GiroKontoauszug25.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2011-06-30"), hasAmount("EUR", 1111.11), //
                        hasSource("GiroKontoauszug25.txt"), hasNote("Lohn, Gehalt, Rente"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2011-06-30"), hasAmount("EUR", 254.00), //
                        hasSource("GiroKontoauszug25.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2011-07-01"), hasAmount("EUR", 400.00), //
                        hasSource("GiroKontoauszug25.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2011-07-01"), hasAmount("EUR", 26.00), //
                        hasSource("GiroKontoauszug25.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2011-07-04"), hasAmount("EUR", 2100.00), //
                        hasSource("GiroKontoauszug25.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2011-06-30"), hasAmount("EUR", 4.05), //
                        hasSource("GiroKontoauszug25.txt"), hasNote("Entgelte vom 01.06.2011 bis 30.06.2011"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2011-06-30"), hasAmount("EUR", 0.00), //
                        hasSource("GiroKontoauszug25.txt"), hasNote("Abrechnungszeitraum vom 01.04.2011 bis 30.06.2011"))));

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        interest(hasDate("2011-06-30"), hasAmount("EUR", 0.00), //
                        hasSource("GiroKontoauszug25.txt"), hasNote("Abrechnungszeitraum vom 01.04.2011 bis 30.06.2011")))));
    }

    @Test
    public void testGiroKontoauszug26()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug26.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(5L));
        assertThat(results.size(), is(5));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2011-06-15"), hasAmount("EUR", 33.43), //
                        hasSource("GiroKontoauszug26.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2011-06-16"), hasAmount("EUR", 260.95), //
                        hasSource("GiroKontoauszug26.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2011-06-20"), hasAmount("EUR", 40.20), //
                        hasSource("GiroKontoauszug26.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2011-06-20"), hasAmount("EUR", 9.85), //
                        hasSource("GiroKontoauszug26.txt"), hasNote("entgeltfreie Buchung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2011-06-21"), hasAmount("EUR", 65.28), //
                        hasSource("GiroKontoauszug26.txt"), hasNote("Kartenzahlung"))));
    }

    @Test
    public void testGiroKontoauszug27()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug27.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2012-09-18"), hasAmount("EUR", 200.00), //
                        hasSource("GiroKontoauszug27.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2012-09-24"), hasAmount("EUR", 119.73), //
                        hasSource("GiroKontoauszug27.txt"), hasNote("Scheckeinzug"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2012-08-31"), hasAmount("EUR", 4.55), //
                        hasSource("GiroKontoauszug27.txt"), hasNote("Entgelte vom 01.08.2012 bis 31.08.2012"))));
    }

    @Test
    public void testGiroKontoauszug28()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug28.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(1L));
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2011-05-10"), hasAmount("EUR", 35.00), //
                        hasSource("GiroKontoauszug28.txt"), hasNote("SEPA Gutschrift"))));
    }

    @Test
    public void testGiroKontoauszug29()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug29.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(28L));
        assertThat(results.size(), is(28));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-05-03"), hasAmount("EUR", 300.00), //
                        hasSource("GiroKontoauszug29.txt"), hasNote("Dauerauftrag"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-05-03"), hasAmount("EUR", 198.33), //
                        hasSource("GiroKontoauszug29.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-05-03"), hasAmount("EUR", 25.00), //
                        hasSource("GiroKontoauszug29.txt"), hasNote("Dauerauftrag"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-05-03"), hasAmount("EUR", 242.49), //
                        hasSource("GiroKontoauszug29.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-05-06"), hasAmount("EUR", 330.14), //
                        hasSource("GiroKontoauszug29.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-05-07"), hasAmount("EUR", 7.00), //
                        hasSource("GiroKontoauszug29.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-05-07"), hasAmount("EUR", 77.00), //
                        hasSource("GiroKontoauszug29.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-05-07"), hasAmount("EUR", 197.35), //
                        hasSource("GiroKontoauszug29.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-05-10"), hasAmount("EUR", 42.00), //
                        hasSource("GiroKontoauszug29.txt"), hasNote("Dauerauftrag"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-05-10"), hasAmount("EUR", 43.48), //
                        hasSource("GiroKontoauszug29.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-05-10"), hasAmount("EUR", 90.00), //
                        hasSource("GiroKontoauszug29.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-05-10"), hasAmount("EUR", 1701.89), //
                        hasSource("GiroKontoauszug29.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-05-09"), hasAmount("EUR", 2000.00), //
                        hasSource("GiroKontoauszug29.txt"), hasNote("Bargeldeinzahlung SB"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-05-11"), hasAmount("EUR", 136.14), //
                        hasSource("GiroKontoauszug29.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-05-11"), hasAmount("EUR", 121.65), //
                        hasSource("GiroKontoauszug29.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-05-11"), hasAmount("EUR", 16.60), //
                        hasSource("GiroKontoauszug29.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-05-17"), hasAmount("EUR", 59.97), //
                        hasSource("GiroKontoauszug29.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-05-17"), hasAmount("EUR", 15.00), //
                        hasSource("GiroKontoauszug29.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-05-20"), hasAmount("EUR", 99.04), //
                        hasSource("GiroKontoauszug29.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-05-20"), hasAmount("EUR", 216.06), //
                        hasSource("GiroKontoauszug29.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-05-26"), hasAmount("EUR", 216.06), //
                        hasSource("GiroKontoauszug29.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-05-28"), hasAmount("EUR", 55.00), //
                        hasSource("GiroKontoauszug29.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-05-31"), hasAmount("EUR", 26.69), //
                        hasSource("GiroKontoauszug29.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-05-31"), hasAmount("EUR", 25.20), //
                        hasSource("GiroKontoauszug29.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-05-31"), hasAmount("EUR", 125.00), //
                        hasSource("GiroKontoauszug29.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2021-05-31"), hasAmount("EUR", 8.50), //
                        hasSource("GiroKontoauszug29.txt"), hasNote("Entgelte vom 01.05.2021 bis 31.05.2021"))));
    }

    @Test
    public void testGiroKontoauszug30()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug30.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(40L));
        assertThat(results.size(), is(40));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-04-01"), hasAmount("EUR", 400.00), //
                        hasSource("GiroKontoauszug30.txt"), hasNote("Dauerauftrag"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-04-01"), hasAmount("EUR", 300.00), //
                        hasSource("GiroKontoauszug30.txt"), hasNote("Dauerauftrag"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-04-01"), hasAmount("EUR", 72.00), //
                        hasSource("GiroKontoauszug30.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-04-01"), hasAmount("EUR", 67.00), //
                        hasSource("GiroKontoauszug30.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-04-01"), hasAmount("EUR", 41.03), //
                        hasSource("GiroKontoauszug30.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-04-01"), hasAmount("EUR", 25.00), //
                        hasSource("GiroKontoauszug30.txt"), hasNote("Dauerauftrag"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-04-01"), hasAmount("EUR", 9.99), //
                        hasSource("GiroKontoauszug30.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-04-01"), hasAmount("EUR", 100.00), //
                        hasSource("GiroKontoauszug30.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-04-01"), hasAmount("EUR", 100.00), //
                        hasSource("GiroKontoauszug30.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-04-01"), hasAmount("EUR", 242.49), //
                        hasSource("GiroKontoauszug30.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-04-06"), hasAmount("EUR", 104.99), //
                        hasSource("GiroKontoauszug30.txt"), hasNote("Überweisung online"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-04-06"), hasAmount("EUR", 43.82), //
                        hasSource("GiroKontoauszug30.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-04-06"), hasAmount("EUR", 50.00), //
                        hasSource("GiroKontoauszug30.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-04-06"), hasAmount("EUR", 78.21), //
                        hasSource("GiroKontoauszug30.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-04-07"), hasAmount("EUR", 212.90), //
                        hasSource("GiroKontoauszug30.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-04-08"), hasAmount("EUR", 1651.64), //
                        hasSource("GiroKontoauszug30.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-04-09"), hasAmount("EUR", 57.99), //
                        hasSource("GiroKontoauszug30.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-04-09"), hasAmount("EUR", 150.00), //
                        hasSource("GiroKontoauszug30.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-04-09"), hasAmount("EUR", 1726.81), //
                        hasSource("GiroKontoauszug30.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-04-12"), hasAmount("EUR", 42.00), //
                        hasSource("GiroKontoauszug30.txt"), hasNote("Dauerauftrag"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-04-12"), hasAmount("EUR", 50.70), //
                        hasSource("GiroKontoauszug30.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-04-12"), hasAmount("EUR", 212.90), //
                        hasSource("GiroKontoauszug30.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-04-16"), hasAmount("EUR", 59.97), //
                        hasSource("GiroKontoauszug30.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-04-19"), hasAmount("EUR", 15.00), //
                        hasSource("GiroKontoauszug30.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-04-19"), hasAmount("EUR", 6000.00), //
                        hasSource("GiroKontoauszug30.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-04-20"), hasAmount("EUR", 24.00), //
                        hasSource("GiroKontoauszug30.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-04-20"), hasAmount("EUR", 15.44), //
                        hasSource("GiroKontoauszug30.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-04-21"), hasAmount("EUR", 10668.97), //
                        hasSource("GiroKontoauszug30.txt"), hasNote("Überweisung online"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-04-21"), hasAmount("EUR", 40.00), //
                        hasSource("GiroKontoauszug30.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-04-22"), hasAmount("EUR", 3.05), //
                        hasSource("GiroKontoauszug30.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-04-23"), hasAmount("EUR", 135.00), //
                        hasSource("GiroKontoauszug30.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-04-23"), hasAmount("EUR", 30.00), //
                        hasSource("GiroKontoauszug30.txt"), hasNote("Bargeldauszahlung (Debitkarte & Fremd-Geldautomat)"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-04-26"), hasAmount("EUR", 20.00), //
                        hasSource("GiroKontoauszug30.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-04-27"), hasAmount("EUR", 88.12), //
                        hasSource("GiroKontoauszug30.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2021-04-28"), hasAmount("EUR", 55.00), //
                        hasSource("GiroKontoauszug30.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-04-29"), hasAmount("EUR", 39.00), //
                        hasSource("GiroKontoauszug30.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2021-04-30"), hasAmount("EUR", 136.14), //
                        hasSource("GiroKontoauszug30.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2021-04-30"), hasAmount("EUR", 8.50), //
                        hasSource("GiroKontoauszug30.txt"), hasNote("Entgelte vom 01.04.2021 bis 30.04.2021"))));
    }

    @Test
    public void testGiroKontoauszug31()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug31.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2019-03-30"), hasAmount("EUR", 500.00), //
                        hasSource("GiroKontoauszug31.txt"), hasNote("Bargeldeinzahlung SB"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2019-04-30"), hasAmount("EUR", 8.50), //
                        hasSource("GiroKontoauszug31.txt"), hasNote("Entgelte vom 30.03.2019 bis 30.04.2019"))));
    }

    @Test
    public void testGiroKontoauszug32()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug32.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2019-06-13"), hasAmount("EUR", 800.00), //
                        hasSource("GiroKontoauszug32.txt"), hasNote("Überweisung Vordruck"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2019-06-28"), hasAmount("EUR", 8.50), //
                        hasSource("GiroKontoauszug32.txt"), hasNote("Entgelte vom 01.06.2019 bis 28.06.2019"))));
    }

    @Test
    public void testGiroKontoauszug33()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug33.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2017-01-20"), hasAmount("EUR", 831.01), //
                        hasSource("GiroKontoauszug33.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2017-01-20"), hasAmount("EUR", 50.00), //
                        hasSource("GiroKontoauszug33.txt"), hasNote("Überweisung"))));
    }

    @Test
    public void testGiroKontoauszug34()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug34.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(4L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2019-03-21"), hasAmount("EUR", 500.00), //
                        hasSource("GiroKontoauszug34.txt"), hasNote("Bargeldauszahlung (Debitkarte & Fremd-Geldautomat)"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2019-03-22"), hasAmount("EUR", 1608.00), //
                        hasSource("GiroKontoauszug34.txt"), hasNote("Bargeldeinzahlung"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2019-03-29"), hasAmount("EUR", 8.50), //
                        hasSource("GiroKontoauszug34.txt"), hasNote("Entgelte vom 01.03.2019 bis 29.03.2019"))));

        // assert transaction
        assertThat(results, hasItem(interest(hasDate("2019-03-31"), hasAmount("EUR", 0.00), //
                        hasSource("GiroKontoauszug34.txt"), hasNote("Abrechnungszeitraum vom 01.01.2019 bis 31.03.2019"))));

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        interest(hasDate("2019-03-31"), hasAmount("EUR", 0.00), //
                        hasSource("GiroKontoauszug34.txt"), hasNote("Abrechnungszeitraum vom 01.01.2019 bis 31.03.2019")))));
    }

    @Test
    public void testGiroKontoauszug35()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug35.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(39L));
        assertThat(results.size(), is(39));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-10-01"), hasAmount("EUR", 200.00), //
                        hasSource("GiroKontoauszug35.txt"), hasNote("Dauerauftrag"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-10-01"), hasAmount("EUR", 25.00), //
                        hasSource("GiroKontoauszug35.txt"), hasNote("Dauerauftrag"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-10-01"), hasAmount("EUR", 44.01), //
                        hasSource("GiroKontoauszug35.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-10-01"), hasAmount("EUR", 70.54), //
                        hasSource("GiroKontoauszug35.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-10-05"), hasAmount("EUR", 17.31), //
                        hasSource("GiroKontoauszug35.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-10-05"), hasAmount("EUR", 51.99), //
                        hasSource("GiroKontoauszug35.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-10-05"), hasAmount("EUR", 98.99), //
                        hasSource("GiroKontoauszug35.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-10-06"), hasAmount("EUR", 1276.87), //
                        hasSource("GiroKontoauszug35.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-10-06"), hasAmount("EUR", 71.78), //
                        hasSource("GiroKontoauszug35.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-10-08"), hasAmount("EUR", 1664.54), //
                        hasSource("GiroKontoauszug35.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-10-09"), hasAmount("EUR", 104.20), //
                        hasSource("GiroKontoauszug35.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-10-12"), hasAmount("EUR", 42.00), //
                        hasSource("GiroKontoauszug35.txt"), hasNote("Dauerauftrag"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-10-12"), hasAmount("EUR", 10.00), //
                        hasSource("GiroKontoauszug35.txt"), hasNote("Überweisung online"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-10-13"), hasAmount("EUR", 34.91), //
                        hasSource("GiroKontoauszug35.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-10-13"), hasAmount("EUR", 103.98), //
                        hasSource("GiroKontoauszug35.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-10-14"), hasAmount("EUR", 0.01), //
                        hasSource("GiroKontoauszug35.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-10-14"), hasAmount("EUR", 10.00), //
                        hasSource("GiroKontoauszug35.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-10-14"), hasAmount("EUR", 41.20), //
                        hasSource("GiroKontoauszug35.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-10-14"), hasAmount("EUR", 51.20), //
                        hasSource("GiroKontoauszug35.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-10-14"), hasAmount("EUR", 55.00), //
                        hasSource("GiroKontoauszug35.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-10-15"), hasAmount("EUR", 19.49), //
                        hasSource("GiroKontoauszug35.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-10-15"), hasAmount("EUR", 52.90), //
                        hasSource("GiroKontoauszug35.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-10-15"), hasAmount("EUR", 11.54), //
                        hasSource("GiroKontoauszug35.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-10-15"), hasAmount("EUR", 9.24), //
                        hasSource("GiroKontoauszug35.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-10-16"), hasAmount("EUR", 24.00), //
                        hasSource("GiroKontoauszug35.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-10-20"), hasAmount("EUR", 22.49), //
                        hasSource("GiroKontoauszug35.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-10-21"), hasAmount("EUR", 34.98), //
                        hasSource("GiroKontoauszug35.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-10-23"), hasAmount("EUR", 11.00), //
                        hasSource("GiroKontoauszug35.txt"), hasNote("Überweisung online"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-10-26"), hasAmount("EUR", 7.00), //
                        hasSource("GiroKontoauszug35.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-10-27"), hasAmount("EUR", 34.10), //
                        hasSource("GiroKontoauszug35.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-10-27"), hasAmount("EUR", 12.99), //
                        hasSource("GiroKontoauszug35.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-10-27"), hasAmount("EUR", 171.75), //
                        hasSource("GiroKontoauszug35.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-10-27"), hasAmount("EUR", 350.00), //
                        hasSource("GiroKontoauszug35.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-10-28"), hasAmount("EUR", 9.35), //
                        hasSource("GiroKontoauszug35.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-10-28"), hasAmount("EUR", 8.87), //
                        hasSource("GiroKontoauszug35.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-10-28"), hasAmount("EUR", 4.60), //
                        hasSource("GiroKontoauszug35.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-10-28"), hasAmount("EUR", 5.00), //
                        hasSource("GiroKontoauszug35.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-10-29"), hasAmount("EUR", 127.19), //
                        hasSource("GiroKontoauszug35.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2020-10-30"), hasAmount("EUR", 8.50), //
                        hasSource("GiroKontoauszug35.txt"), hasNote("Entgelte vom 01.10.2020 bis 30.10.2020"))));
    }

    @Test
    public void testGiroKontoauszug36()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug36.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(4L));
        assertThat(results.size(), is(4));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-07-27"), hasAmount("EUR", 888.00), //
                        hasSource("GiroKontoauszug36.txt"), hasNote("Bargeldauszahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-07-28"), hasAmount("EUR", 3333.00), //
                        hasSource("GiroKontoauszug36.txt"), hasNote("Überweisung online"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2023-07-31"), hasAmount("EUR", 3.50), //
                        hasSource("GiroKontoauszug36.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2023-07-31"), hasAmount("EUR", 8.50), //
                        hasSource("GiroKontoauszug36.txt"), hasNote("Entgelte vom 01.07.2023 bis 31.07.2023"))));
    }

    @Test
    public void testGiroKontoauszug37()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug37.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(3L));
        assertThat(results.size(), is(3));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-02-13"), hasAmount("EUR", 60.00), //
                        hasSource("GiroKontoauszug37.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-02-15"), hasAmount("EUR", 60.00), //
                        hasSource("GiroKontoauszug37.txt"), hasNote("Überweisung online"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2024-02-22"), hasAmount("EUR", 161.59), //
                        hasSource("GiroKontoauszug37.txt"), hasNote("Gutschrift (Überweisung)"))));
    }

    @Test
    public void testGiroKontoauszug38()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug38.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(30L));
        assertThat(results.size(), is(30));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2017-11-02"), hasAmount("EUR", 200.00), //
                        hasSource("GiroKontoauszug38.txt"), hasNote("Dauerauftrag"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2017-11-02"), hasAmount("EUR", 100.00), //
                        hasSource("GiroKontoauszug38.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2017-11-06"), hasAmount("EUR", 200.00), //
                        hasSource("GiroKontoauszug38.txt"), hasNote("Geldautomat"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2017-11-06"), hasAmount("EUR", 20.00), //
                        hasSource("GiroKontoauszug38.txt"), hasNote("Basis-Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2017-11-07"), hasAmount("EUR", 35.00), //
                        hasSource("GiroKontoauszug38.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2017-11-08"), hasAmount("EUR", 1506.42), //
                        hasSource("GiroKontoauszug38.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2017-11-09"), hasAmount("EUR", 16.01), //
                        hasSource("GiroKontoauszug38.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2017-11-09"), hasAmount("EUR", 20.00), //
                        hasSource("GiroKontoauszug38.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2017-11-13"), hasAmount("EUR", 289.00), //
                        hasSource("GiroKontoauszug38.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2017-11-14"), hasAmount("EUR", 149.95), //
                        hasSource("GiroKontoauszug38.txt"), hasNote("Basis-Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2017-11-14"), hasAmount("EUR", 113.57), //
                        hasSource("GiroKontoauszug38.txt"), hasNote("Basis-Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2017-11-14"), hasAmount("EUR", 98.73), //
                        hasSource("GiroKontoauszug38.txt"), hasNote("Basis-Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2017-11-14"), hasAmount("EUR", 5.99), //
                        hasSource("GiroKontoauszug38.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2017-11-14"), hasAmount("EUR", 10.01), //
                        hasSource("GiroKontoauszug38.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2017-11-14"), hasAmount("EUR", 110.00), //
                        hasSource("GiroKontoauszug38.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2017-11-16"), hasAmount("EUR", 16.67), //
                        hasSource("GiroKontoauszug38.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2017-11-16"), hasAmount("EUR", 50.00), //
                        hasSource("GiroKontoauszug38.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2017-11-17"), hasAmount("EUR", 24.99), //
                        hasSource("GiroKontoauszug38.txt"), hasNote("Basis-Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2017-11-17"), hasAmount("EUR", 5.01), //
                        hasSource("GiroKontoauszug38.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2017-11-20"), hasAmount("EUR", 165.00), //
                        hasSource("GiroKontoauszug38.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2017-11-20"), hasAmount("EUR", 16.49), //
                        hasSource("GiroKontoauszug38.txt"), hasNote("Basis-Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2017-11-21"), hasAmount("EUR", 6.50), //
                        hasSource("GiroKontoauszug38.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2017-11-21"), hasAmount("EUR", 15.00), //
                        hasSource("GiroKontoauszug38.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2017-11-21"), hasAmount("EUR", 25.00), //
                        hasSource("GiroKontoauszug38.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2017-11-22"), hasAmount("EUR", 990.18), //
                        hasSource("GiroKontoauszug38.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2017-11-23"), hasAmount("EUR", 50.00), //
                        hasSource("GiroKontoauszug38.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2017-11-24"), hasAmount("EUR", 11.00), //
                        hasSource("GiroKontoauszug38.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2017-11-27"), hasAmount("EUR", 23.09), //
                        hasSource("GiroKontoauszug38.txt"), hasNote("Basis-Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2017-11-30"), hasAmount("EUR", 8.50), //
                        hasSource("GiroKontoauszug38.txt"), hasNote("Entgelte vom 31.10.2017 bis 30.11.2017"))));
    }

    @Test
    public void testGiroKontoauszug39()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug39.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(15L));
        assertThat(results.size(), is(15));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2019-12-02"), hasAmount("EUR", 260.00), //
                        hasSource("GiroKontoauszug39.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2019-12-02"), hasAmount("EUR", 34.50), //
                        hasSource("GiroKontoauszug39.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2019-12-05"), hasAmount("EUR", 2000.00), //
                        hasSource("GiroKontoauszug39.txt"), hasNote("Überweisung online"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2019-12-05"), hasAmount("EUR", 5.60), //
                        hasSource("GiroKontoauszug39.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2019-12-10"), hasAmount("EUR", 33.99), //
                        hasSource("GiroKontoauszug39.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2019-12-10"), hasAmount("EUR", 22.95), //
                        hasSource("GiroKontoauszug39.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2019-12-12"), hasAmount("EUR", 5.60), //
                        hasSource("GiroKontoauszug39.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2019-12-12"), hasAmount("EUR", 33.99), //
                        hasSource("GiroKontoauszug39.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2019-12-13"), hasAmount("EUR", 29.99), //
                        hasSource("GiroKontoauszug39.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2019-12-18"), hasAmount("EUR", 30.00), //
                        hasSource("GiroKontoauszug39.txt"), hasNote("Überweisung online"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2019-12-20"), hasAmount("EUR", 6.60), //
                        hasSource("GiroKontoauszug39.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2019-12-27"), hasAmount("EUR", 6.60), //
                        hasSource("GiroKontoauszug39.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2019-12-27"), hasAmount("EUR", 291.48), //
                        hasSource("GiroKontoauszug39.txt"), hasNote("Lohn, Gehalt, Rente"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2019-12-30"), hasAmount("EUR", 6.20), //
                        hasSource("GiroKontoauszug39.txt"), hasNote("Entgelte vom 30.11.2019 bis 30.12.2019"))));

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        interest(hasDate("2019-12-31"), hasAmount("EUR", 0.00), //
                        hasSource("GiroKontoauszug39.txt"), hasNote("Abrechnungszeitraum vom 01.10.2019 bis 31.12.2019")))));
    }

    @Test
    public void testGiroKontoauszug40()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug40.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(31L));
        assertThat(results.size(), is(31));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-06-01"), hasAmount("EUR", 200.00), //
                        hasSource("GiroKontoauszug40.txt"), hasNote("Dauerauftrag"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-06-01"), hasAmount("EUR", 25.00), //
                        hasSource("GiroKontoauszug40.txt"), hasNote("Dauerauftrag"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-06-02"), hasAmount("EUR", 8.49), //
                        hasSource("GiroKontoauszug40.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-06-02"), hasAmount("EUR", 10.00), //
                        hasSource("GiroKontoauszug40.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-06-05"), hasAmount("EUR", 1194.77), //
                        hasSource("GiroKontoauszug40.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-06-08"), hasAmount("EUR", 64.00), //
                        hasSource("GiroKontoauszug40.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-06-08"), hasAmount("EUR", 48.27), //
                        hasSource("GiroKontoauszug40.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-06-08"), hasAmount("EUR", 25.00), //
                        hasSource("GiroKontoauszug40.txt"), hasNote("Überweisung online"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-06-08"), hasAmount("EUR", 15.00), //
                        hasSource("GiroKontoauszug40.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-06-09"), hasAmount("EUR", 140.00), //
                        hasSource("GiroKontoauszug40.txt"), hasNote("Überweisung online"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-06-09"), hasAmount("EUR", 4.34), //
                        hasSource("GiroKontoauszug40.txt"), hasNote("Überweisung online"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-06-09"), hasAmount("EUR", 652.56), //
                        hasSource("GiroKontoauszug40.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-06-10"), hasAmount("EUR", 42.00), //
                        hasSource("GiroKontoauszug40.txt"), hasNote("Dauerauftrag"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-06-10"), hasAmount("EUR", 620.57), //
                        hasSource("GiroKontoauszug40.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-06-15"), hasAmount("EUR", 50.00), //
                        hasSource("GiroKontoauszug40.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-06-16"), hasAmount("EUR", 19.99), //
                        hasSource("GiroKontoauszug40.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-06-17"), hasAmount("EUR", 96.27), //
                        hasSource("GiroKontoauszug40.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-06-18"), hasAmount("EUR", 10.00), //
                        hasSource("GiroKontoauszug40.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-06-18"), hasAmount("EUR", 12.00), //
                        hasSource("GiroKontoauszug40.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-06-22"), hasAmount("EUR", 22.49), //
                        hasSource("GiroKontoauszug40.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-06-24"), hasAmount("EUR", 37.24), //
                        hasSource("GiroKontoauszug40.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-06-25"), hasAmount("EUR", 5.59), //
                        hasSource("GiroKontoauszug40.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-06-26"), hasAmount("EUR", 811.19), //
                        hasSource("GiroKontoauszug40.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-06-26"), hasAmount("EUR", 34.98), //
                        hasSource("GiroKontoauszug40.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-06-26"), hasAmount("EUR", 5.85), //
                        hasSource("GiroKontoauszug40.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-06-29"), hasAmount("EUR", 351.26), //
                        hasSource("GiroKontoauszug40.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-06-30"), hasAmount("EUR", 3000.00), //
                        hasSource("GiroKontoauszug40.txt"), hasNote("Überweisung online"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-06-30"), hasAmount("EUR", 990.00), //
                        hasSource("GiroKontoauszug40.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2020-06-30"), hasAmount("EUR", 8.50), //
                        hasSource("GiroKontoauszug40.txt"), hasNote("Entgelte vom 30.05.2020 bis 30.06.2020"))));

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        interest(hasDate("2020-06-30"), hasAmount("EUR", 0.00), //
                        hasSource("GiroKontoauszug40.txt"), hasNote("Abrechnungszeitraum vom 01.04.2020 bis 30.06.2020")))));
    }

    @Test
    public void testGiroKontoauszug41()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug41.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2024-04-04"), hasAmount("EUR", 0.07), //
                        hasSource("GiroKontoauszug41.txt"), hasNote("Überweisung online"))));

        // assert transaction
        assertThat(results, hasItem(taxRefund(hasDate("2024-04-08"), hasAmount("EUR", 0.02), //
                        hasSource("GiroKontoauszug41.txt"), hasNote("Buchung beleglos"))));

    }

    @Test
    public void testGiroKontoauszug42()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug42.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(44L));
        assertThat(results.size(), is(44));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2016-06-01"), hasAmount("EUR", 1950.00), //
                        hasSource("GiroKontoauszug42.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2016-06-01"), hasAmount("EUR", 800.00), //
                        hasSource("GiroKontoauszug42.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2016-06-01"), hasAmount("EUR", 19.99), //
                        hasSource("GiroKontoauszug42.txt"), hasNote("Basis-Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2016-06-01"), hasAmount("EUR", 3890.58), //
                        hasSource("GiroKontoauszug42.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2016-06-02"), hasAmount("EUR", 95.00), //
                        hasSource("GiroKontoauszug42.txt"), hasNote("Basis-Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2016-06-02"), hasAmount("EUR", 220.00), //
                        hasSource("GiroKontoauszug42.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2016-06-03"), hasAmount("EUR", 46.11), //
                        hasSource("GiroKontoauszug42.txt"), hasNote("Basis-Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2016-06-03"), hasAmount("EUR", 22.71), //
                        hasSource("GiroKontoauszug42.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2016-06-06"), hasAmount("EUR", 89.00), //
                        hasSource("GiroKontoauszug42.txt"), hasNote("Basis-Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2016-06-06"), hasAmount("EUR", 54.15), //
                        hasSource("GiroKontoauszug42.txt"), hasNote("Basis-Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2016-06-07"), hasAmount("EUR", 42.00), //
                        hasSource("GiroKontoauszug42.txt"), hasNote("Basis-Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2016-06-07"), hasAmount("EUR", 7.99), //
                        hasSource("GiroKontoauszug42.txt"), hasNote("Basis-Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2016-06-07"), hasAmount("EUR", 20.00), //
                        hasSource("GiroKontoauszug42.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2016-06-08"), hasAmount("EUR", 16.49), //
                        hasSource("GiroKontoauszug42.txt"), hasNote("Basis-Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2016-06-09"), hasAmount("EUR", 64.02), //
                        hasSource("GiroKontoauszug42.txt"), hasNote("Basis-Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2016-06-09"), hasAmount("EUR", 100.00), //
                        hasSource("GiroKontoauszug42.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2016-06-10"), hasAmount("EUR", 165.32), //
                        hasSource("GiroKontoauszug42.txt"), hasNote("Basis-Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2016-06-10"), hasAmount("EUR", 48.00), //
                        hasSource("GiroKontoauszug42.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2016-06-13"), hasAmount("EUR", 161.00), //
                        hasSource("GiroKontoauszug42.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2016-06-13"), hasAmount("EUR", 20.52), //
                        hasSource("GiroKontoauszug42.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2016-06-14"), hasAmount("EUR", 21.78), //
                        hasSource("GiroKontoauszug42.txt"), hasNote("Basis-Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2016-06-15"), hasAmount("EUR", 83.10), //
                        hasSource("GiroKontoauszug42.txt"), hasNote("Basis-Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2016-06-16"), hasAmount("EUR", 50.00), //
                        hasSource("GiroKontoauszug42.txt"), hasNote("Basis-Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2016-06-17"), hasAmount("EUR", 12.95), //
                        hasSource("GiroKontoauszug42.txt"), hasNote("Basis-Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2016-06-20"), hasAmount("EUR", 113.09), //
                        hasSource("GiroKontoauszug42.txt"), hasNote("Basis-Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2016-06-20"), hasAmount("EUR", 374.92), //
                        hasSource("GiroKontoauszug42.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2016-06-21"), hasAmount("EUR", 300.00), //
                        hasSource("GiroKontoauszug42.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2016-06-21"), hasAmount("EUR", 18.81), //
                        hasSource("GiroKontoauszug42.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2016-06-21"), hasAmount("EUR", 796.47), //
                        hasSource("GiroKontoauszug42.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2016-06-22"), hasAmount("EUR", 3.61), //
                        hasSource("GiroKontoauszug42.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2016-06-23"), hasAmount("EUR", 450.00), //
                        hasSource("GiroKontoauszug42.txt"), hasNote("Geldautomat"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2016-06-23"), hasAmount("EUR", 235.00), //
                        hasSource("GiroKontoauszug42.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2016-06-27"), hasAmount("EUR", 901.72), //
                        hasSource("GiroKontoauszug42.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2016-06-27"), hasAmount("EUR", 400.00), //
                        hasSource("GiroKontoauszug42.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2016-06-27"), hasAmount("EUR", 300.00), //
                        hasSource("GiroKontoauszug42.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2016-06-27"), hasAmount("EUR", 1140.00), //
                        hasSource("GiroKontoauszug42.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2016-06-29"), hasAmount("EUR", 4.22), //
                        hasSource("GiroKontoauszug42.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2016-06-30"), hasAmount("EUR", 116.12), //
                        hasSource("GiroKontoauszug42.txt"), hasNote("Basis-Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2016-05-31"), hasAmount("EUR", 0.50), //
                        hasSource("GiroKontoauszug42.txt"), hasNote("Entgelte vom 30.04.2016 bis 31.05.2016"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2016-06-30"), hasAmount("EUR", 8.50), //
                        hasSource("GiroKontoauszug42.txt"), hasNote("Entgelte vom 01.06.2016 bis 30.06.2016"))));

        // assert transaction
        assertThat(results, hasItem(withFailureMessage( //
                        Messages.MsgErrorTransactionTypeNotSupported, //
                        interest(hasDate("2016-06-30"), hasAmount("EUR", 0.00), //
                        hasSource("GiroKontoauszug42.txt"), hasNote("Abrechnungszeitraum vom 01.04.2016 bis 30.06.2016")))));
    }

    @Test
    public void testGiroKontoauszug43()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug43.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(24L));
        assertThat(results.size(), is(24));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-01-02"), hasAmount("EUR", 200.00), //
                        hasSource("GiroKontoauszug43.txt"), hasNote("Dauerauftrag"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-01-02"), hasAmount("EUR", 144.47), //
                        hasSource("GiroKontoauszug43.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-01-02"), hasAmount("EUR", 25.00), //
                        hasSource("GiroKontoauszug43.txt"), hasNote("Dauerauftrag"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-01-02"), hasAmount("EUR", 4.60), //
                        hasSource("GiroKontoauszug43.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-01-03"), hasAmount("EUR", 12.00), //
                        hasSource("GiroKontoauszug43.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-01-07"), hasAmount("EUR", 260.78), //
                        hasSource("GiroKontoauszug43.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-01-07"), hasAmount("EUR", 10.50), //
                        hasSource("GiroKontoauszug43.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-01-08"), hasAmount("EUR", 64.53), //
                        hasSource("GiroKontoauszug43.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-01-10"), hasAmount("EUR", 15.00), //
                        hasSource("GiroKontoauszug43.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-01-10"), hasAmount("EUR", 1641.18), //
                        hasSource("GiroKontoauszug43.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-01-13"), hasAmount("EUR", 10.00), //
                        hasSource("GiroKontoauszug43.txt"), hasNote("Überweisung online"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-01-13"), hasAmount("EUR", 48.00), //
                        hasSource("GiroKontoauszug43.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-01-14"), hasAmount("EUR", 5000.00), //
                        hasSource("GiroKontoauszug43.txt"), hasNote("Überweisung online"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-01-15"), hasAmount("EUR", 110.88), //
                        hasSource("GiroKontoauszug43.txt"), hasNote("Überweisung online"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-01-16"), hasAmount("EUR", 19.99), //
                        hasSource("GiroKontoauszug43.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-01-20"), hasAmount("EUR", 22.00), //
                        hasSource("GiroKontoauszug43.txt"), hasNote("Überweisung online"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-01-20"), hasAmount("EUR", 10.00), //
                        hasSource("GiroKontoauszug43.txt"), hasNote("Überweisung online"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-01-20"), hasAmount("EUR", 22.49), //
                        hasSource("GiroKontoauszug43.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-01-21"), hasAmount("EUR", 2.01), //
                        hasSource("GiroKontoauszug43.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-01-27"), hasAmount("EUR", 34.98), //
                        hasSource("GiroKontoauszug43.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-01-30"), hasAmount("EUR", 1442.17), //
                        hasSource("GiroKontoauszug43.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2020-01-31"), hasAmount("EUR", 8.50), //
                        hasSource("GiroKontoauszug43.txt"), hasNote("Entgelte vom 31.12.2019 bis 31.01.2020"))));
    }

    @Test
    public void testGiroKontoauszug44()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug44.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(34L));
        assertThat(results.size(), is(34));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-07-01"), hasAmount("EUR", 1013.71), //
                        hasSource("GiroKontoauszug44.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-07-01"), hasAmount("EUR", 1000.00), //
                        hasSource("GiroKontoauszug44.txt"), hasNote("Überweisung online"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-07-01"), hasAmount("EUR", 200.00), //
                        hasSource("GiroKontoauszug44.txt"), hasNote("Dauerauftrag"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-07-01"), hasAmount("EUR", 25.00), //
                        hasSource("GiroKontoauszug44.txt"), hasNote("Dauerauftrag"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-07-02"), hasAmount("EUR", 612.50), //
                        hasSource("GiroKontoauszug44.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-07-03"), hasAmount("EUR", 55.25), //
                        hasSource("GiroKontoauszug44.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-07-06"), hasAmount("EUR", 1024.57), //
                        hasSource("GiroKontoauszug44.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-07-06"), hasAmount("EUR", 57.06), //
                        hasSource("GiroKontoauszug44.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-07-06"), hasAmount("EUR", 65.00), //
                        hasSource("GiroKontoauszug44.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-07-06"), hasAmount("EUR", 393.50), //
                        hasSource("GiroKontoauszug44.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-07-08"), hasAmount("EUR", 24.41), //
                        hasSource("GiroKontoauszug44.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-07-08"), hasAmount("EUR", 1652.56), //
                        hasSource("GiroKontoauszug44.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-07-10"), hasAmount("EUR", 42.00), //
                        hasSource("GiroKontoauszug44.txt"), hasNote("Dauerauftrag"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-07-13"), hasAmount("EUR", 181.53), //
                        hasSource("GiroKontoauszug44.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-07-13"), hasAmount("EUR", 7.52), //
                        hasSource("GiroKontoauszug44.txt"), hasNote("Überweisung online"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-07-14"), hasAmount("EUR", 181.36), //
                        hasSource("GiroKontoauszug44.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-07-14"), hasAmount("EUR", 1560.00), //
                        hasSource("GiroKontoauszug44.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-07-15"), hasAmount("EUR", 19.49), //
                        hasSource("GiroKontoauszug44.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-07-15"), hasAmount("EUR", 4.98), //
                        hasSource("GiroKontoauszug44.txt"), hasNote("Überweisung online"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-07-16"), hasAmount("EUR", 100.00), //
                        hasSource("GiroKontoauszug44.txt"), hasNote("Überweisung online"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-07-20"), hasAmount("EUR", 7.95), //
                        hasSource("GiroKontoauszug44.txt"), hasNote("Überweisung online"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-07-20"), hasAmount("EUR", 4.80), //
                        hasSource("GiroKontoauszug44.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-07-20"), hasAmount("EUR", 22.49), //
                        hasSource("GiroKontoauszug44.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-07-20"), hasAmount("EUR", 229.00), //
                        hasSource("GiroKontoauszug44.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2020-07-21"), hasAmount("EUR", 120.00), //
                        hasSource("GiroKontoauszug44.txt"), hasNote("Gutschrift (Überweisung)"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-07-24"), hasAmount("EUR", 176.20), //
                        hasSource("GiroKontoauszug44.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-07-27"), hasAmount("EUR", 34.10), //
                        hasSource("GiroKontoauszug44.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-07-27"), hasAmount("EUR", 7.80), //
                        hasSource("GiroKontoauszug44.txt"), hasNote("Überweisung online"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-07-28"), hasAmount("EUR", 40.00), //
                        hasSource("GiroKontoauszug44.txt"), hasNote("Überweisung online"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-07-28"), hasAmount("EUR", 5.46), //
                        hasSource("GiroKontoauszug44.txt"), hasNote("Überweisung online"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-07-30"), hasAmount("EUR", 34.12), //
                        hasSource("GiroKontoauszug44.txt"), hasNote("Überweisung online"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-07-31"), hasAmount("EUR", 17.60), //
                        hasSource("GiroKontoauszug44.txt"), hasNote("Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2020-07-31"), hasAmount("EUR", 8.50), //
                        hasSource("GiroKontoauszug44.txt"), hasNote("Entgelte vom 01.07.2020 bis 31.07.2020"))));
    }

    @Test
    public void testGiroKontoauszug45()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "GiroKontoauszug45.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(38L));
        assertThat(results.size(), is(38));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2016-05-02"), hasAmount("EUR", 350.00), //
                        hasSource("GiroKontoauszug45.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2016-05-02"), hasAmount("EUR", 19.99), //
                        hasSource("GiroKontoauszug45.txt"), hasNote("Basis-Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2016-05-02"), hasAmount("EUR", 13.45), //
                        hasSource("GiroKontoauszug45.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2016-05-02"), hasAmount("EUR", 13.45), //
                        hasSource("GiroKontoauszug45.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2016-05-03"), hasAmount("EUR", 12.00), //
                        hasSource("GiroKontoauszug45.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2016-05-03"), hasAmount("EUR", 13.08), //
                        hasSource("GiroKontoauszug45.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2016-05-03"), hasAmount("EUR", 13.45), //
                        hasSource("GiroKontoauszug45.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2016-05-03"), hasAmount("EUR", 130.00), //
                        hasSource("GiroKontoauszug45.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2016-05-04"), hasAmount("EUR", 83.36), //
                        hasSource("GiroKontoauszug45.txt"), hasNote("Basis-Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2016-05-04"), hasAmount("EUR", 13.45), //
                        hasSource("GiroKontoauszug45.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2016-05-06"), hasAmount("EUR", 1.60), //
                        hasSource("GiroKontoauszug45.txt"), hasNote("Basis-Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2016-05-09"), hasAmount("EUR", 55.00), //
                        hasSource("GiroKontoauszug45.txt"), hasNote("Kartenzahlung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2016-05-10"), hasAmount("EUR", 24.80), //
                        hasSource("GiroKontoauszug45.txt"), hasNote("Basis-Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2016-05-10"), hasAmount("EUR", 6.00), //
                        hasSource("GiroKontoauszug45.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2016-05-11"), hasAmount("EUR", 58.31), //
                        hasSource("GiroKontoauszug45.txt"), hasNote("Basis-Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2016-05-11"), hasAmount("EUR", 5.99), //
                        hasSource("GiroKontoauszug45.txt"), hasNote("Basis-Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2016-05-17"), hasAmount("EUR", 89.98), //
                        hasSource("GiroKontoauszug45.txt"), hasNote("Überweisung"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2016-05-17"), hasAmount("EUR", 83.10), //
                        hasSource("GiroKontoauszug45.txt"), hasNote("Basis-Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2016-05-20"), hasAmount("EUR", 50.00), //
                        hasSource("GiroKontoauszug45.txt"), hasNote("Geldautomat"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2016-05-20"), hasAmount("EUR", 5.99), //
                        hasSource("GiroKontoauszug45.txt"), hasNote("Basis-Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2016-05-23"), hasAmount("EUR", 13.45), //
                        hasSource("GiroKontoauszug45.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2016-05-23"), hasAmount("EUR", 70.00), //
                        hasSource("GiroKontoauszug45.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2016-05-24"), hasAmount("EUR", 4.99), //
                        hasSource("GiroKontoauszug45.txt"), hasNote("Basis-Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2016-05-24"), hasAmount("EUR", 12.00), //
                        hasSource("GiroKontoauszug45.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2016-05-25"), hasAmount("EUR", 12.00), //
                        hasSource("GiroKontoauszug45.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2016-05-25"), hasAmount("EUR", 30.48), //
                        hasSource("GiroKontoauszug45.txt"), hasNote("Basis-Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2016-05-25"), hasAmount("EUR", 5.99), //
                        hasSource("GiroKontoauszug45.txt"), hasNote("Basis-Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2016-05-25"), hasAmount("EUR", 82.50), //
                        hasSource("GiroKontoauszug45.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2016-05-27"), hasAmount("EUR", 99.00), //
                        hasSource("GiroKontoauszug45.txt"), hasNote("Basis-Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2016-05-26"), hasAmount("EUR", 12.00), //
                        hasSource("GiroKontoauszug45.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2016-05-30"), hasAmount("EUR", 51.89), //
                        hasSource("GiroKontoauszug45.txt"), hasNote("Basis-Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2016-05-30"), hasAmount("EUR", 2.79), //
                        hasSource("GiroKontoauszug45.txt"), hasNote("Basis-Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2016-05-30"), hasAmount("EUR", 26.78), //
                        hasSource("GiroKontoauszug45.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2016-05-30"), hasAmount("EUR", 64.82), //
                        hasSource("GiroKontoauszug45.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2016-05-31"), hasAmount("EUR", 75.68), //
                        hasSource("GiroKontoauszug45.txt"), hasNote("Basis-Lastschrift"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2016-05-31"), hasAmount("EUR", 28.01), //
                        hasSource("GiroKontoauszug45.txt"), hasNote("Zahlungseingang"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2016-06-01"), hasAmount("EUR", 81.78), //
                        hasSource("GiroKontoauszug45.txt"), hasNote("Scheckeinzug"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2016-05-31"), hasAmount("EUR", 106.00), //
                        hasSource("GiroKontoauszug45.txt"), hasNote("Zahlungseingang"))));
    }

    @Test
    public void testKreditKontoauszug01()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KreditKontoauszug01.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(23));

        // check transaction
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(23L));

        Item item = iter.next();

        // assert transaction
        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-09-03T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1345.61))));
        assertThat(transaction.getSource(), is("KreditKontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Lastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-09-05T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3.00))));
        assertThat(transaction.getSource(), is("KreditKontoauszug01.txt"));
        assertThat(transaction.getNote(), is("PAYPAL *LUKASMATHY, 38888899999"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-09-05T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(7.62))));
        assertThat(transaction.getSource(), is("KreditKontoauszug01.txt"));
        assertThat(transaction.getNote(), is("PAYPAL *AUTOTEILEGI, 38888899999"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-09-05T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(6.54))));
        assertThat(transaction.getSource(), is("KreditKontoauszug01.txt"));
        assertThat(transaction.getNote(), is("MGP*Vinted 40530371895, L-1125"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-09-06T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(12.90))));
        assertThat(transaction.getSource(), is("KreditKontoauszug01.txt"));
        assertThat(transaction.getNote(), is("PAYPAL *ANNA.JAEGER97"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-09-08T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.99))));
        assertThat(transaction.getSource(), is("KreditKontoauszug01.txt"));
        assertThat(transaction.getNote(), is("PAYPAL *NETFLIX.COM, 38888899999"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-09-12T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5.00))));
        assertThat(transaction.getSource(), is("KreditKontoauszug01.txt"));
        assertThat(transaction.getNote(), is("PAYPAL *BEYMARVIN2001"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-09-12T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(0.75))));
        assertThat(transaction.getSource(), is("KreditKontoauszug01.txt"));
        assertThat(transaction.getNote(), is("PAYPAL *F_KLUGE, 38888899999"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-09-12T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.50))));
        assertThat(transaction.getSource(), is("KreditKontoauszug01.txt"));
        assertThat(transaction.getNote(), is("PAYPAL *POSTCODELOT, 38888899999"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-09-12T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.40))));
        assertThat(transaction.getSource(), is("KreditKontoauszug01.txt"));
        assertThat(transaction.getNote(), is("Gutmann am Dutzendteich, Nuernberg"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-09-12T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.08))));
        assertThat(transaction.getSource(), is("KreditKontoauszug01.txt"));
        assertThat(transaction.getNote(), is("HEM Tankstelle, Ebersdorf"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-09-16T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5.50))));
        assertThat(transaction.getSource(), is("KreditKontoauszug01.txt"));
        assertThat(transaction.getNote(), is("PAYPAL *MEDPEXVERSA, 38888899999"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-09-16T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(15.00))));
        assertThat(transaction.getSource(), is("KreditKontoauszug01.txt"));
        assertThat(transaction.getNote(), is("PAYPAL *LWA24, 38888899999"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-09-19T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(9.59))));
        assertThat(transaction.getSource(), is("KreditKontoauszug01.txt"));
        assertThat(transaction.getNote(), is("PAYPAL *EBAY DE, 38888899999"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-09-03T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(20.29))));
        assertThat(transaction.getSource(), is("KreditKontoauszug01.txt"));
        assertThat(transaction.getNote(), is("PAYPAL *EBAY DE, 38888899999"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-09-21T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(39.98))));
        assertThat(transaction.getSource(), is("KreditKontoauszug01.txt"));
        assertThat(transaction.getNote(), is("PAYPAL *EBAY DE, 38888899999"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-09-22T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.00))));
        assertThat(transaction.getSource(), is("KreditKontoauszug01.txt"));
        assertThat(transaction.getNote(), is("PAYPAL *LWA24, 38888899999"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-09-23T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10.00))));
        assertThat(transaction.getSource(), is("KreditKontoauszug01.txt"));
        assertThat(transaction.getNote(), is("PAYPAL *BRITTAWENDLAND"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-09-26T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(95.21))));
        assertThat(transaction.getSource(), is("KreditKontoauszug01.txt"));
        assertThat(transaction.getNote(), is("PAYPAL *AUTOTEILEGI, 38888899999"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-09-27T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(7.99))));
        assertThat(transaction.getSource(), is("KreditKontoauszug01.txt"));
        assertThat(transaction.getNote(), is("PAYPAL *EBAY DE, 38888899999"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-09-27T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5.89))));
        assertThat(transaction.getSource(), is("KreditKontoauszug01.txt"));
        assertThat(transaction.getNote(), is("PAYPAL *EBAY DE, 38888899999"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-09-28T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5.49))));
        assertThat(transaction.getSource(), is("KreditKontoauszug01.txt"));
        assertThat(transaction.getNote(), is("PAYPAL *DHL OL, 38888899999"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2022-09-29T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(6.35))));
        assertThat(transaction.getSource(), is("KreditKontoauszug01.txt"));
        assertThat(transaction.getNote(), is("PAYPAL *EBAY DE, 38888899999"));
    }

    @Test
    public void testKreditKontoauszug02()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KreditKontoauszug02.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(23));

        // check transaction
        // get transactions
        Iterator<Extractor.Item> iter = results.stream().filter(TransactionItem.class::isInstance).iterator();
        assertThat(results.stream().filter(TransactionItem.class::isInstance).count(), is(23L));

        Item item = iter.next();

        // assert transaction
        AccountTransaction transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-01-06T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(862.96))));
        assertThat(transaction.getSource(), is("KreditKontoauszug02.txt"));
        assertThat(transaction.getNote(), is("Lastschrift"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-01-06T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(6.99))));
        assertThat(transaction.getSource(), is("KreditKontoauszug02.txt"));
        assertThat(transaction.getNote(), is("PAYPAL *JESSICAWILDE, 35314369001"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-01-06T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(19.42))));
        assertThat(transaction.getSource(), is("KreditKontoauszug02.txt"));
        assertThat(transaction.getNote(), is("PAYPAL *BATTERIUM BATT"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-01-08T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.99))));
        assertThat(transaction.getSource(), is("KreditKontoauszug02.txt"));
        assertThat(transaction.getNote(), is("PAYPAL *NETFLIX.COM, 35314369001"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-01-06T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3.89))));
        assertThat(transaction.getSource(), is("KreditKontoauszug02.txt"));
        assertThat(transaction.getNote(), is("AMZN Mktp US, Amzn.com/bill"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-01-12T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.90))));
        assertThat(transaction.getSource(), is("KreditKontoauszug02.txt"));
        assertThat(transaction.getNote(), is("PAYPAL *EBAY DE, 35314369001"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-01-12T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(60.80))));
        assertThat(transaction.getSource(), is("KreditKontoauszug02.txt"));
        assertThat(transaction.getNote(), is("WWW.ALIEXPRESS.COM"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-01-12T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(40.22))));
        assertThat(transaction.getSource(), is("KreditKontoauszug02.txt"));
        assertThat(transaction.getNote(), is("SHELL 1708, LICHTENFELS"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-01-14T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.40))));
        assertThat(transaction.getSource(), is("KreditKontoauszug02.txt"));
        assertThat(transaction.getNote(), is("PAYPAL *ATU EBAY ATU, 35314369001"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-01-14T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.28))));
        assertThat(transaction.getSource(), is("KreditKontoauszug02.txt"));
        assertThat(transaction.getNote(), is("PAYPAL *Q PARTS24 EBAY"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-01-18T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(6.99))));
        assertThat(transaction.getSource(), is("KreditKontoauszug02.txt"));
        assertThat(transaction.getNote(), is("PAYPAL *MMSECOMMERC EB"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-01-18T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(43.52))));
        assertThat(transaction.getSource(), is("KreditKontoauszug02.txt"));
        assertThat(transaction.getNote(), is("ALIEXPRESS.COM, Luxembourg"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-01-21T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(9.68))));
        assertThat(transaction.getSource(), is("KreditKontoauszug02.txt"));
        assertThat(transaction.getNote(), is("WWW.ALIEXPRESS.COM"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-01-22T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.00))));
        assertThat(transaction.getSource(), is("KreditKontoauszug02.txt"));
        assertThat(transaction.getNote(), is("PAYPAL *EBAY DE, 35314369001"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-01-25T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(7.95))));
        assertThat(transaction.getSource(), is("KreditKontoauszug02.txt"));
        assertThat(transaction.getNote(), is("PAYPAL *UVISION EBAY U"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-01-25T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.29))));
        assertThat(transaction.getSource(), is("KreditKontoauszug02.txt"));
        assertThat(transaction.getNote(), is("PAYPAL *ZHANGSHAZHI EB"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-01-25T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(148.46))));
        assertThat(transaction.getSource(), is("KreditKontoauszug02.txt"));
        assertThat(transaction.getNote(), is("EVERDRIVE.ME, KRAKOW"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-01-25T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.96))));
        assertThat(transaction.getSource(), is("KreditKontoauszug02.txt"));
        assertThat(transaction.getNote(), is("PAYPAL *HAIBEILIKEJ EB, 4029357733"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-01-06T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5.40))));
        assertThat(transaction.getSource(), is("KreditKontoauszug02.txt"));
        assertThat(transaction.getNote(), is("PAYPAL *ATU EBAY ATU, 35314369001"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-02-01T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(24.71))));
        assertThat(transaction.getSource(), is("KreditKontoauszug02.txt"));
        assertThat(transaction.getNote(), is("PAYPAL *AUTOTEILEGI AU"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-01-06T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(6.99))));
        assertThat(transaction.getSource(), is("KreditKontoauszug02.txt"));
        assertThat(transaction.getNote(), is("PAYPAL *MMSECOMMERC EB"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-02-01T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1.99))));
        assertThat(transaction.getSource(), is("KreditKontoauszug02.txt"));
        assertThat(transaction.getNote(), is("PAYPAL *FHUAUTOWALD EB"));

        item = iter.next();

        // assert transaction
        transaction = (AccountTransaction) item.getSubject();
        assertThat(transaction.getType(), is(AccountTransaction.Type.FEES));
        assertThat(transaction.getDateTime(), is(LocalDateTime.parse("2021-01-25T00:00")));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2.97))));
        assertThat(transaction.getSource(), is("KreditKontoauszug02.txt"));
        assertThat(transaction.getNote(), is("2% für Währungsumrechnung"));
    }

    @Test
    public void testKreditKontoauszug03()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KreditKontoauszug03.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(2L));
        assertThat(results.size(), is(2));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2020-02-24"), hasAmount("EUR", 33.10), //
                        hasSource("KreditKontoauszug03.txt"), hasNote("ALIEXPRESS.COM, Luxembourg"))));

        // assert transaction
        assertThat(results, hasItem(fee(hasDate("2020-02-24"), hasAmount("EUR", 0.58), //
                        hasSource("KreditKontoauszug03.txt"), hasNote("1,75% für Einsatz der Karte im Ausland"))));
    }

    @Test
    public void testKreditKontoauszug04()
    {
        SBrokerPDFExtractor extractor = new SBrokerPDFExtractor(new Client());

        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "KreditKontoauszug04.txt"),
                        errors);

        assertThat(errors, empty());
        assertThat(countSecurities(results), is(0L));
        assertThat(countBuySell(results), is(0L));
        assertThat(countAccountTransactions(results), is(5L));
        assertThat(results.size(), is(5));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2019-11-13"), hasAmount("EUR", 25.28), //
                        hasSource("KreditKontoauszug04.txt"), hasNote("ALIEXPRESS.COM, London"))));

        // assert transaction
        assertThat(results, hasItem(removal(hasDate("2019-11-19"), hasAmount("EUR", 94.90), //
                        hasSource("KreditKontoauszug04.txt"), hasNote("PAYPAL *GERDMARQUAR, 35314369001"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2019-11-05"), hasAmount("EUR", 28.55), //
                        hasSource("KreditKontoauszug04.txt"), hasNote("WWW.ALIEXPRESS.COM, LONDON"))));

        // assert transaction
        assertThat(results, hasItem(feeRefund(hasDate("2019-11-05"), hasAmount("EUR", 0.50), //
                        hasSource("KreditKontoauszug04.txt"), hasNote("1,75% für Einsatz der Karte im Ausland"))));

        // assert transaction
        assertThat(results, hasItem(deposit(hasDate("2019-11-05"), hasAmount("EUR", 302.93), //
                        hasSource("KreditKontoauszug04.txt"), hasNote("PAYPAL *OTTO GMBH, 35314369001"))));

    }
}
