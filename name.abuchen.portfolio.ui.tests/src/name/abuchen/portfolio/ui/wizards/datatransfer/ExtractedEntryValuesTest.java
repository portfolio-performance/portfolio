package name.abuchen.portfolio.ui.wizards.datatransfer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class ExtractedEntryValuesTest
{
    private static final LocalDateTime DATE = LocalDateTime.parse("2024-01-02T10:00");

    private Security security;

    @Before
    public void setup()
    {
        security = new SecurityBuilder(CurrencyUnit.EUR).addTo(new Client());
    }

    private static Money eur(double amount)
    {
        return Money.of(CurrencyUnit.EUR, Values.Amount.factorize(amount));
    }

    private Extractor.Item buy()
    {
        var entry = new BuySellEntry();
        entry.setType(PortfolioTransaction.Type.BUY);
        entry.setSecurity(security);
        entry.setDate(DATE);
        entry.setCurrencyCode(CurrencyUnit.EUR);
        entry.setAmount(Values.Amount.factorize(1012));
        entry.setShares(Values.Share.factorize(10));
        entry.setNote("Ord.-Nr. 4711");
        entry.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE, eur(10)));
        entry.getPortfolioTransaction().addUnit(new Unit(Unit.Type.TAX, eur(2)));
        return new Extractor.BuySellEntryItem(entry);
    }

    private Extractor.Item account(AccountTransaction.Type type)
    {
        var transaction = new AccountTransaction(DATE, CurrencyUnit.EUR, Values.Amount.factorize(100),
                        type == AccountTransaction.Type.DIVIDENDS ? security : null, type);
        return new Extractor.TransactionItem(transaction);
    }

    private static List<ExtractedEntry> entries(Extractor.Item... items)
    {
        return List.of(items).stream().map(ExtractedEntry::new).toList();
    }

    // -- values of the optional columns

    @Test
    public void testTaxesFeesAndNoteOfBuy()
    {
        var buy = buy();

        assertThat(ExtractedEntryValues.getTaxes(buy), is(eur(2)));
        assertThat(ExtractedEntryValues.getFees(buy), is(eur(10)));
        assertThat(ExtractedEntryValues.getNote(buy), is("Ord.-Nr. 4711"));
    }

    @Test
    public void testTaxesOfDividend()
    {
        var dividend = account(AccountTransaction.Type.DIVIDENDS);
        ((AccountTransaction) dividend.getSubject()).addUnit(new Unit(Unit.Type.TAX, eur(1.77)));

        assertThat(ExtractedEntryValues.getTaxes(dividend), is(eur(1.77)));
        assertThat(ExtractedEntryValues.getFees(dividend), is(nullValue()));
    }

    @Test
    public void testNoValuesWithoutTaxesFeesAndNote()
    {
        var deposit = account(AccountTransaction.Type.DEPOSIT);

        assertThat(ExtractedEntryValues.getTaxes(deposit), is(nullValue()));
        assertThat(ExtractedEntryValues.getFees(deposit), is(nullValue()));
        assertThat(ExtractedEntryValues.getNote(deposit), is(nullValue()));
    }

    @Test
    public void testFeesOfAccountTransferAreTakenFromTheSourceTransaction()
    {
        var entry = new AccountTransferEntry();
        entry.setDate(DATE);
        entry.setCurrencyCode(CurrencyUnit.EUR);
        entry.setAmount(Values.Amount.factorize(500));
        entry.getSourceTransaction().addUnit(new Unit(Unit.Type.FEE, eur(1.50)));

        assertThat(ExtractedEntryValues.getFees(new Extractor.AccountTransferItem(entry, true)), is(eur(1.50)));
    }

    @Test
    public void testNoValuesForSecurityItem()
    {
        var item = new Extractor.SecurityItem(security);

        assertThat(ExtractedEntryValues.getTaxes(item), is(nullValue()));
        assertThat(ExtractedEntryValues.getFees(item), is(nullValue()));
    }

    // -- which import options apply

    @Test
    public void testOptionsForBuyAndDividend()
    {
        var entries = entries(buy(), account(AccountTransaction.Type.DIVIDENDS), new Extractor.SecurityItem(security));

        assertThat(ExtractedEntryValues.hasBuySell(entries), is(true));
        assertThat(ExtractedEntryValues.hasDividends(entries), is(true));
    }

    @Test
    public void testNoOptionsForAccountStatement()
    {
        var entries = entries(account(AccountTransaction.Type.DEPOSIT), account(AccountTransaction.Type.INTEREST));

        assertThat(ExtractedEntryValues.hasBuySell(entries), is(false));
        assertThat(ExtractedEntryValues.hasDividends(entries), is(false));
    }

    @Test
    public void testOptionsForEmptyList()
    {
        assertThat(ExtractedEntryValues.hasBuySell(List.of()), is(false));
        assertThat(ExtractedEntryValues.hasDividends(List.of()), is(false));
    }
}
