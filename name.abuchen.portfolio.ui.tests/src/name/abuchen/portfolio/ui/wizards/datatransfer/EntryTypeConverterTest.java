package name.abuchen.portfolio.ui.wizards.datatransfer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.ImportAction;
import name.abuchen.portfolio.datatransfer.ImportAction.Status;
import name.abuchen.portfolio.datatransfer.actions.CheckCurrenciesAction;
import name.abuchen.portfolio.datatransfer.actions.InsertAction;
import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.wizards.datatransfer.EntryTypeConverter.TypeOption;

@SuppressWarnings("nls")
public class EntryTypeConverterTest
{
    private static final LocalDateTime DATE = LocalDateTime.parse("2024-01-02T10:00");

    private Client client;
    private Account account;
    private Account otherAccount;
    private Portfolio portfolio;
    private Portfolio otherPortfolio;
    private Security security;

    private List<ExtractedEntry> entries;
    private EntryTypeConverter converter;

    @Before
    public void setup()
    {
        client = new Client();
        account = new AccountBuilder(CurrencyUnit.EUR).addTo(client);
        otherAccount = new AccountBuilder(CurrencyUnit.EUR).addTo(client);
        portfolio = new PortfolioBuilder(account).addTo(client);
        otherPortfolio = new PortfolioBuilder(account).addTo(client);
        security = new SecurityBuilder(CurrencyUnit.EUR).addTo(client);

        entries = new ArrayList<>();
        converter = new EntryTypeConverter(entries);
    }

    /** buy or sell of 10 shares with a market value of EUR 1,000 */
    private ExtractedEntry buySell(PortfolioTransaction.Type type, long fee, long tax)
    {
        var entry = new BuySellEntry();
        entry.setType(type);
        entry.setSecurity(security);
        entry.setDate(DATE);
        entry.setCurrencyCode(CurrencyUnit.EUR);
        entry.setShares(Values.Share.factorize(10));
        entry.setNote("note");
        entry.setSource("statement.pdf");

        var costs = fee + tax;
        entry.setAmount(Values.Amount.factorize(1000) + (type == PortfolioTransaction.Type.BUY ? costs : -costs));
        if (fee != 0)
            entry.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE, Money.of(CurrencyUnit.EUR, fee)));
        if (tax != 0)
            entry.getPortfolioTransaction().addUnit(new Unit(Unit.Type.TAX, Money.of(CurrencyUnit.EUR, tax)));

        return add(new Extractor.BuySellEntryItem(entry));
    }

    private ExtractedEntry deposit(AccountTransaction.Type type)
    {
        return add(new Extractor.TransactionItem(
                        new AccountTransaction(DATE, CurrencyUnit.EUR, Values.Amount.factorize(500), null, type)));
    }

    private ExtractedEntry delivery(PortfolioTransaction.Type type)
    {
        var transaction = new PortfolioTransaction();
        transaction.setType(type);
        transaction.setSecurity(security);
        transaction.setDateTime(DATE);
        transaction.setCurrencyCode(CurrencyUnit.EUR);
        transaction.setAmount(Values.Amount.factorize(300));
        transaction.setShares(Values.Share.factorize(3));
        return add(new Extractor.TransactionItem(transaction));
    }

    private ExtractedEntry add(Extractor.Item item)
    {
        var entry = new ExtractedEntry(item);
        entries.add(entry);
        return entry;
    }

    private final ImportAction.Context context = new ImportAction.Context()
    {
        @Override
        public Account getAccount(String currencyCode)
        {
            return account;
        }

        @Override
        public Account getSecondaryAccount(String currencyCode)
        {
            return otherAccount;
        }

        @Override
        public Portfolio getPortfolio()
        {
            return portfolio;
        }

        @Override
        public Portfolio getSecondaryPortfolio()
        {
            return otherPortfolio;
        }
    };

    private static long shares(Portfolio p)
    {
        return p.getTransactions().stream()
                        .mapToLong(t -> t.getType().isPurchase() ? t.getShares() : -t.getShares()).sum();
    }

    private static long balance(Account a)
    {
        return a.getTransactions().stream().mapToLong(t -> t.getType().isCredit() ? t.getAmount() : -t.getAmount())
                        .sum();
    }

    // -- offered types

    @Test
    public void testTypeOptions()
    {
        var buy = buySell(PortfolioTransaction.Type.BUY, 0, 0);
        var delivery = delivery(PortfolioTransaction.Type.DELIVERY_INBOUND);
        var deposit = deposit(AccountTransaction.Type.DEPOSIT);
        var removal = deposit(AccountTransaction.Type.REMOVAL);

        assertThat(converter.getTypeOptions(buy),
                        contains(TypeOption.ORIGINAL, TypeOption.DELIVERY, TypeOption.PORTFOLIO_TRANSFER));
        assertThat(converter.getTypeOptions(delivery), contains(TypeOption.ORIGINAL, TypeOption.PORTFOLIO_TRANSFER));
        assertThat(converter.getTypeOptions(deposit), contains(TypeOption.ORIGINAL, TypeOption.ACCOUNT_TRANSFER));
        assertThat(converter.getTypeOptions(removal), contains(TypeOption.ORIGINAL, TypeOption.ACCOUNT_TRANSFER));
    }

    @Test
    public void testNoPortfolioTransferForTransactionInForeignCurrency()
    {
        var usdSecurity = new SecurityBuilder(CurrencyUnit.USD).addTo(client);

        var entry = new BuySellEntry();
        entry.setType(PortfolioTransaction.Type.BUY);
        entry.setSecurity(usdSecurity);
        entry.setDate(DATE);
        entry.setCurrencyCode(CurrencyUnit.EUR);
        entry.setAmount(Values.Amount.factorize(100));
        entry.setShares(Values.Share.factorize(1));
        entry.getPortfolioTransaction()
                        .addUnit(new Unit(Unit.Type.GROSS_VALUE, Money.of(CurrencyUnit.EUR, Values.Amount.factorize(100)),
                                        Money.of(CurrencyUnit.USD, Values.Amount.factorize(110)),
                                        BigDecimal.valueOf(100).divide(BigDecimal.valueOf(110), 10,
                                                        java.math.RoundingMode.HALF_DOWN)));

        var buy = add(new Extractor.BuySellEntryItem(entry));

        assertThat(converter.getTypeOptions(buy), contains(TypeOption.ORIGINAL, TypeOption.DELIVERY));
    }

    @Test
    public void testNoTypeOptionsForSecurityAndFailedItems()
    {
        var securityEntry = add(new Extractor.SecurityItem(security));

        var failed = new Extractor.TransactionItem(
                        new AccountTransaction(DATE, CurrencyUnit.EUR, 100, null, AccountTransaction.Type.DEPOSIT));
        failed.setFailureMessage("cancellation");
        var failedEntry = add(failed);

        assertThat(converter.getTypeOptions(securityEntry), is(empty()));
        assertThat(converter.getTypeOptions(failedEntry), is(empty()));
    }

    @Test
    public void testTypeLabels()
    {
        var buy = buySell(PortfolioTransaction.Type.BUY, 0, 0).getItem();
        var sell = buySell(PortfolioTransaction.Type.SELL, 0, 0).getItem();

        assertThat(converter.getTypeLabel(buy, TypeOption.ORIGINAL), is(buy.getTypeInformation()));
        assertThat(converter.getTypeLabel(buy, TypeOption.DELIVERY),
                        is(PortfolioTransaction.Type.DELIVERY_INBOUND.toString()));
        assertThat(converter.getTypeLabel(buy, TypeOption.PORTFOLIO_TRANSFER),
                        is(PortfolioTransaction.Type.TRANSFER_IN.toString()));
        assertThat(converter.getTypeLabel(sell, TypeOption.DELIVERY),
                        is(PortfolioTransaction.Type.DELIVERY_OUTBOUND.toString()));
        assertThat(converter.getTypeLabel(sell, TypeOption.PORTFOLIO_TRANSFER),
                        is(PortfolioTransaction.Type.TRANSFER_OUT.toString()));
    }

    // -- conversions

    @Test
    public void testBuyToPortfolioTransferCreatesFeeAndTaxEntries()
    {
        var buy = buySell(PortfolioTransaction.Type.BUY, Values.Amount.factorize(10), Values.Amount.factorize(2));
        var original = buy.getItem();
        original.setAccountPrimary(otherAccount);

        converter.changeType(buy, TypeOption.PORTFOLIO_TRANSFER);

        assertThat(entries, hasSize(3));

        var main = entries.get(0);
        assertThat(main.getItem(), instanceOf(Extractor.PortfolioTransferItem.class));
        assertThat(main.getItem().getTypeInformation(), is(PortfolioTransaction.Type.TRANSFER_IN.toString()));
        assertThat(converter.getCurrentType(main), is(TypeOption.PORTFOLIO_TRANSFER));
        assertThat(converter.getOriginalItem(main), is(original));

        var transfer = (PortfolioTransferEntry) main.getItem().getSubject();
        assertThat(transfer.getSourceTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1000))));
        assertThat(transfer.getSourceTransaction().getShares(), is(Values.Share.factorize(10)));
        assertThat(transfer.getSourceTransaction().getNote(), is("note"));
        assertThat(transfer.getSourceTransaction().getSource(), is("statement.pdf"));

        var fee = (AccountTransaction) entries.get(1).getItem().getSubject();
        assertThat(fee.getType(), is(AccountTransaction.Type.FEES));
        assertThat(fee.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10))));
        assertThat(fee.getSecurity(), is(security));

        var tax = (AccountTransaction) entries.get(2).getItem().getSubject();
        assertThat(tax.getType(), is(AccountTransaction.Type.TAXES));
        assertThat(tax.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2))));

        // the explicitly chosen account is kept for fees and taxes
        assertThat(entries.get(1).getItem().getAccountPrimary(), is(otherAccount));
        assertThat(converter.getAdditionalEntries(main), contains(entries.get(1), entries.get(2)));

        // additional entries cannot be changed and follow their transfer
        assertThat(converter.getTypeOptions(entries.get(1)), is(empty()));
        main.setImported(false);
        assertThat(entries.get(1).isImported(), is(false));
        assertThat(entries.get(2).isImported(), is(false));
    }

    @Test
    public void testChangingBackRestoresTheOriginalItem()
    {
        var buy = buySell(PortfolioTransaction.Type.BUY, Values.Amount.factorize(10), 0);
        var original = buy.getItem();

        converter.changeType(buy, TypeOption.PORTFOLIO_TRANSFER);
        var main = entries.get(0);
        main.setImported(false);

        converter.changeType(main, TypeOption.ORIGINAL);

        assertThat(entries, hasSize(1));
        assertThat(entries.get(0).getItem(), is(original));
        assertThat(entries.get(0).isImported(), is(false));
        assertThat(converter.getCurrentType(entries.get(0)), is(TypeOption.ORIGINAL));
        assertThat(converter.getAdditionalEntries(entries.get(0)), is(empty()));
    }

    @Test
    public void testSwitchingBetweenConvertedTypesKeepsOneSetOfAdditionalEntries()
    {
        var sell = buySell(PortfolioTransaction.Type.SELL, Values.Amount.factorize(5), Values.Amount.factorize(20));

        converter.changeType(sell, TypeOption.DELIVERY);
        assertThat(entries, hasSize(1));
        assertThat(entries.get(0).getItem().getTypeInformation(),
                        is(PortfolioTransaction.Type.DELIVERY_OUTBOUND.toString()));

        converter.changeType(entries.get(0), TypeOption.PORTFOLIO_TRANSFER);
        assertThat(entries, hasSize(3));
        assertThat(entries.get(0).getItem().getTypeInformation(),
                        is(PortfolioTransaction.Type.TRANSFER_OUT.toString()));

        converter.changeType(entries.get(0), TypeOption.DELIVERY);
        assertThat(entries, hasSize(1));
    }

    @Test
    public void testBuyToDeliveryKeepsFeesAndTaxes()
    {
        var buy = buySell(PortfolioTransaction.Type.BUY, Values.Amount.factorize(10), Values.Amount.factorize(2));
        buy.getItem().setPortfolioPrimary(otherPortfolio);

        converter.changeType(buy, TypeOption.DELIVERY);

        assertThat(entries, hasSize(1));
        var delivery = (PortfolioTransaction) entries.get(0).getItem().getSubject();
        assertThat(delivery.getType(), is(PortfolioTransaction.Type.DELIVERY_INBOUND));
        assertThat(delivery.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1012))));
        assertThat(delivery.getUnitSum(Unit.Type.FEE), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10))));
        assertThat(delivery.getUnitSum(Unit.Type.TAX), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2))));
        assertThat(entries.get(0).getItem().getPortfolioPrimary(), is(otherPortfolio));
    }

    @Test
    public void testDepositToInboundAccountTransfer()
    {
        var deposit = deposit(AccountTransaction.Type.DEPOSIT);

        converter.changeType(deposit, TypeOption.ACCOUNT_TRANSFER);

        var item = entries.get(0).getItem();
        assertThat(item, instanceOf(Extractor.AccountTransferItem.class));
        assertThat(((AccountTransferEntry) item.getSubject()).getTargetTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(500))));

        assertThat(item.apply(new InsertAction(client), context).getCode(), is(Status.Code.OK));

        // the money arrives on the account of the deposit
        assertThat(balance(account), is(Values.Amount.factorize(500)));
        assertThat(balance(otherAccount), is(-Values.Amount.factorize(500)));
    }

    @Test
    public void testImportOfConvertedEntries()
    {
        var buy = buySell(PortfolioTransaction.Type.BUY, Values.Amount.factorize(10), Values.Amount.factorize(2));
        var sell = buySell(PortfolioTransaction.Type.SELL, Values.Amount.factorize(5), Values.Amount.factorize(20));
        var inbound = delivery(PortfolioTransaction.Type.DELIVERY_INBOUND);

        converter.changeType(buy, TypeOption.PORTFOLIO_TRANSFER);
        converter.changeType(sell, TypeOption.PORTFOLIO_TRANSFER);
        converter.changeType(inbound, TypeOption.PORTFOLIO_TRANSFER);

        var check = new CheckCurrenciesAction();
        for (var entry : entries)
            assertThat(entry.getItem().apply(check, context).getCode(), is(Status.Code.OK));

        var insert = new InsertAction(client);
        for (var entry : entries)
            assertThat(entry.getItem().apply(insert, context).getCode(), is(Status.Code.OK));

        // buy (+10) and inbound delivery (+3) are transferred into the
        // portfolio, the sale (-10) out of it
        assertThat(shares(portfolio), is(Values.Share.factorize(3)));
        assertThat(shares(otherPortfolio), is(-Values.Share.factorize(3)));

        // only fees and taxes are booked on the account
        assertThat(balance(account), is(-Values.Amount.factorize(10 + 2 + 5 + 20)));
    }

    @Test
    public void testForgetRemovedEntries()
    {
        var buy = buySell(PortfolioTransaction.Type.BUY, 0, 0);
        converter.changeType(buy, TypeOption.DELIVERY);
        var main = entries.get(0);

        entries.clear();
        converter.forgetRemovedEntries();

        assertThat(converter.getCurrentType(main), is(TypeOption.ORIGINAL));
    }

    @Test
    public void testConversionKeepsSecurityDependency()
    {
        var securityEntry = new ExtractedEntry(new Extractor.SecurityItem(security));
        var buy = buySell(PortfolioTransaction.Type.BUY, Values.Amount.factorize(10), 0);
        buy.setSecurityDependency(securityEntry);

        converter.changeType(buy, TypeOption.PORTFOLIO_TRANSFER);

        assertThat(entries.get(0).getSecurityDependency(), is(securityEntry));
        assertThat(entries.get(1).getSecurityDependency(), is(securityEntry));
    }
}
