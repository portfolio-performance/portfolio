package name.abuchen.portfolio.ui.wizards.datatransfer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.datatransfer.ImportAction.Status;
import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;

@SuppressWarnings("nls")
public class CheckReferenceAccountCurrencyActionTest
{
    private static final LocalDateTime DATE = LocalDateTime.parse("2024-01-02T00:00");

    private Client client;
    private Account eurAccount;
    private Account usdAccount;
    private Portfolio eurPortfolio;
    private Portfolio usdPortfolio;
    private Portfolio portfolioWithoutReferenceAccount;
    private Security security;

    /** accounts selected per currency on the wizard page */
    private final Map<String, Account> selected = new HashMap<>();
    private boolean convertToDelivery;

    private final CheckReferenceAccountCurrencyAction action = new CheckReferenceAccountCurrencyAction(
                    () -> convertToDelivery, selected::get);

    @Before
    public void setup()
    {
        client = new Client();
        eurAccount = new AccountBuilder(CurrencyUnit.EUR).addTo(client);
        usdAccount = new AccountBuilder(CurrencyUnit.USD).addTo(client);
        eurPortfolio = new PortfolioBuilder(eurAccount).addTo(client);
        usdPortfolio = new PortfolioBuilder(usdAccount).addTo(client);

        portfolioWithoutReferenceAccount = new Portfolio();
        portfolioWithoutReferenceAccount.setName("without reference account");
        client.addPortfolio(portfolioWithoutReferenceAccount);

        security = new SecurityBuilder(CurrencyUnit.USD).addTo(client);
    }

    private BuySellEntry buy(String currencyCode)
    {
        var entry = new BuySellEntry();
        entry.setType(PortfolioTransaction.Type.BUY);
        entry.setSecurity(security);
        entry.setDate(DATE);
        entry.setCurrencyCode(currencyCode);
        entry.setAmount(Values.Amount.factorize(100));
        entry.setShares(Values.Share.factorize(1));
        return entry;
    }

    private PortfolioTransaction delivery(String currencyCode)
    {
        var transaction = new PortfolioTransaction();
        transaction.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
        transaction.setSecurity(security);
        transaction.setDateTime(DATE);
        transaction.setCurrencyCode(currencyCode);
        transaction.setAmount(Values.Amount.factorize(100));
        transaction.setShares(Values.Share.factorize(1));
        return transaction;
    }

    private PortfolioTransferEntry transfer(String currencyCode)
    {
        var entry = new PortfolioTransferEntry();
        entry.setSecurity(security);
        entry.setDate(DATE);
        entry.setCurrencyCode(currencyCode);
        entry.setAmount(Values.Amount.factorize(100));
        entry.setShares(Values.Share.factorize(1));
        return entry;
    }

    private static String mismatch(String currencyCode, Account referenceAccount)
    {
        return MessageFormat.format(Messages.MsgErrorConvertToBuySellCurrencyMismatch, currencyCode,
                        referenceAccount.getCurrencyCode(), referenceAccount.getName());
    }

    // -- buy/sell: the chosen account counts, not the reference account

    @Test
    public void testBuyFromForeignCurrencyAccountIsValid()
    {
        var status = action.process(buy(CurrencyUnit.USD), usdAccount, eurPortfolio);

        assertThat(status.getCode(), is(Status.Code.OK));
    }

    @Test
    public void testBuyIntoPortfolioWithoutReferenceAccountIsAnError()
    {
        var status = action.process(buy(CurrencyUnit.EUR), eurAccount, portfolioWithoutReferenceAccount);

        assertThat(status.getCode(), is(Status.Code.ERROR));
        assertThat(status.getMessage(), is(Messages.MsgMissingReferenceAccount));
    }

    @Test
    public void testBuyImportedAsDeliveryIsCheckedLikeADelivery()
    {
        convertToDelivery = true;

        var status = action.process(buy(CurrencyUnit.USD), usdAccount, eurPortfolio);
        assertThat(status.getCode(), is(Status.Code.ERROR));
        assertThat(status.getMessage(), is(mismatch(CurrencyUnit.USD, eurAccount)));

        selected.put(CurrencyUnit.USD, usdAccount);
        assertThat(action.process(buy(CurrencyUnit.USD), usdAccount, eurPortfolio).getCode(), is(Status.Code.OK));
    }

    // -- deliveries: reference account or selected account of the currency

    @Test
    public void testDeliveryMatchingTheReferenceAccountIsValid()
    {
        assertThat(action.process(delivery(CurrencyUnit.EUR), eurPortfolio).getCode(), is(Status.Code.OK));
    }

    @Test
    public void testDeliveryIsValidIfAnAccountInItsCurrencyIsSelected()
    {
        selected.put(CurrencyUnit.USD, usdAccount);

        assertThat(action.process(delivery(CurrencyUnit.USD), eurPortfolio).getCode(), is(Status.Code.OK));
    }

    @Test
    public void testDeliveryIsAnErrorWithoutMatchingAccount()
    {
        var status = action.process(delivery(CurrencyUnit.USD), eurPortfolio);

        assertThat(status.getCode(), is(Status.Code.ERROR));
        assertThat(status.getMessage(), is(mismatch(CurrencyUnit.USD, eurAccount)));
    }

    @Test
    public void testSelectedAccountInAnotherCurrencyDoesNotCount()
    {
        selected.put(CurrencyUnit.USD, eurAccount);

        assertThat(action.process(delivery(CurrencyUnit.USD), eurPortfolio).getCode(), is(Status.Code.ERROR));
    }

    @Test
    public void testDeliveryIntoPortfolioWithoutReferenceAccountIsAnError()
    {
        selected.put(CurrencyUnit.EUR, eurAccount);

        var status = action.process(delivery(CurrencyUnit.EUR), portfolioWithoutReferenceAccount);

        assertThat(status.getCode(), is(Status.Code.ERROR));
        assertThat(status.getMessage(), is(Messages.MsgMissingReferenceAccount));
    }

    // -- portfolio transfers: source and target portfolio

    @Test
    public void testPortfolioTransferChecksSourceAndTarget()
    {
        assertThat(action.process(transfer(CurrencyUnit.EUR), eurPortfolio, eurPortfolio).getCode(),
                        is(Status.Code.OK));

        var targetMismatch = action.process(transfer(CurrencyUnit.EUR), eurPortfolio, usdPortfolio);
        assertThat(targetMismatch.getCode(), is(Status.Code.ERROR));
        assertThat(targetMismatch.getMessage(), is(mismatch(CurrencyUnit.EUR, usdAccount)));

        var sourceMismatch = action.process(transfer(CurrencyUnit.EUR), usdPortfolio, eurPortfolio);
        assertThat(sourceMismatch.getCode(), is(Status.Code.ERROR));
        assertThat(sourceMismatch.getMessage(), is(mismatch(CurrencyUnit.EUR, usdAccount)));

        selected.put(CurrencyUnit.EUR, eurAccount);
        assertThat(action.process(transfer(CurrencyUnit.EUR), eurPortfolio, usdPortfolio).getCode(),
                        is(Status.Code.OK));
    }
}
