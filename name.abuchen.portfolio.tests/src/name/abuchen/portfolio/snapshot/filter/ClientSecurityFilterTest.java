package name.abuchen.portfolio.snapshot.filter;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.junit.TestCurrencyConverter;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.FundTransferEntry;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.util.Interval;

@SuppressWarnings("nls")
public class ClientSecurityFilterTest
{
    private Client client;
    private Security securityUSD;
    private Security securityEUR;
    private Account accountEUR;
    private Account accountUSD;

    @Before
    public void setupClient()
    {
        client = new Client();

        securityUSD = new SecurityBuilder() //
                        .addPrice("2016-06-01", Values.Quote.factorize(100)) //
                        .addTo(client);
        securityEUR = new SecurityBuilder(CurrencyUnit.USD) //
                        .addPrice("2016-06-01", Values.Quote.factorize(100)) //
                        .addTo(client);

        accountEUR = new AccountBuilder(CurrencyUnit.EUR) //
                        .dividend("2017-01-01", Values.Amount.factorize(20), securityEUR) //
                        .addTo(client);

        accountUSD = new AccountBuilder(CurrencyUnit.USD) //
                        .dividend("2017-01-01", Values.Amount.factorize(20), securityUSD) //
                        .addTo(client);

        new PortfolioBuilder(accountEUR) //
                        .inbound_delivery(securityEUR, "2016-06-01", Values.Share.factorize(20),
                                        Values.Amount.factorize(2000)) //
                        .inbound_delivery(securityUSD, "2016-06-01", Values.Share.factorize(20),
                                        Values.Amount.factorize(2000)) //
                        .addTo(client);
    }

    @Test
    public void testThatForexTransactionsAreAddedToTheCorrectAccount()
    {
        Client filtered = new ClientSecurityFilter(securityUSD).filter(client);

        assertThat(filtered.getAccounts(), hasSize(2));
        assertThat(filtered.getPortfolios(), hasSize(1));

        // no transactions in EUR account (but exists b/c it is reference
        // account for the portfolio)
        assertThat(filtered.getAccounts().stream() //
                        .filter(a -> accountEUR.getName().equals(a.getName())).findAny()
                        .orElseThrow(IllegalArgumentException::new).getTransactions(), empty());

        // only one transaction on portfolio
        assertThat(filtered.getPortfolios().get(0).getTransactions(), hasSize(1));

    }

    @Test
    public void testThatDividendsAreOnlyAddedToTheReferenceAccount()
    {
        Client filtered = new ClientSecurityFilter(securityEUR).filter(client);

        assertThat(filtered.getAccounts(), hasSize(1));
        assertThat(filtered.getPortfolios(), hasSize(1));

        // check that USD account is not included
        assertThat(filtered.getAccounts().stream().filter(a -> accountUSD.getName().equals(a.getName()))
                        .collect(Collectors.toList()), empty());
    }

    @Test
    public void testThatNotesArePreservedOnCopiedTransactions()
    {
        accountEUR.getTransactions().forEach(t -> t.setNote("dividend note"));
        client.getPortfolios().get(0).getTransactions().stream().filter(t -> t.getSecurity() == securityEUR)
                        .forEach(t -> t.setNote("delivery note"));

        Client filtered = new ClientSecurityFilter(securityEUR).filter(client);

        Account account = filtered.getAccounts().get(0);
        List<AccountTransaction> accountTx = account.getTransactions();

        // the delivery is converted to a pseudo transaction, keeping the note
        assertThat(filtered.getPortfolios().get(0).getTransactions().stream().map(Transaction::getNote)
                        .collect(Collectors.toList()), is(Arrays.asList("delivery note")));

        assertThat(notesOf(accountTx, AccountTransaction.Type.DIVIDENDS), is(Arrays.asList("dividend note")));

        // the balancing entry is created by the filter, not copied from a user
        // transaction, and therefore must not carry a note
        assertThat(notesOf(accountTx, AccountTransaction.Type.REMOVAL), is(Collections.singletonList(null)));
    }

    private List<String> notesOf(List<AccountTransaction> transactions, AccountTransaction.Type type)
    {
        return transactions.stream().filter(t -> t.getType() == type).map(Transaction::getNote)
                        .collect(Collectors.toList());
    }

    @Test
    public void testThatAllSecuritiesHaveIdendicalPerformanceToClient()
    {
        Client filtered = new ClientSecurityFilter(securityEUR, securityUSD).filter(client);

        assertThat(filtered.getAccounts(), hasSize(2));
        assertThat(filtered.getPortfolios(), hasSize(1));

        List<Exception> warnings = new ArrayList<>();
        TestCurrencyConverter converter = new TestCurrencyConverter();
        Interval interval = Interval.of(LocalDate.parse("2015-12-31"), LocalDate.parse("2017-01-31"));

        PerformanceIndex all = PerformanceIndex.forClient(client, converter, interval, warnings);
        assertThat(warnings, empty());

        PerformanceIndex filteredAll = PerformanceIndex.forClient(filtered, converter, interval, warnings);
        assertThat(warnings, empty());

        assertThat(all.getFinalAccumulatedPercentage(), is(filteredAll.getFinalAccumulatedPercentage()));
        assertThat(all.getDeltaPercentage(), is(filteredAll.getDeltaPercentage()));
    }

    @Test
    public void testFundTransfersAreKeptWhenFilteringSecurities()
    {
        Client client = new Client();
        Security sourceFund = new SecurityBuilder().addTo(client);
        Security targetFund = new SecurityBuilder().addTo(client);
        Account sourceAccount = new AccountBuilder().addTo(client);
        Account targetAccount = new AccountBuilder().addTo(client);

        Portfolio sourcePortfolio = new PortfolioBuilder(sourceAccount) //
                        .buy(sourceFund, "2020-01-01", Values.Share.factorize(10),
                                        Values.Amount.factorize(1000)) //
                        .addTo(client);
        Portfolio targetPortfolio = new PortfolioBuilder(targetAccount).addTo(client);

        PortfolioTransaction sourceBuy = sourcePortfolio.getTransactions().get(0);

        FundTransferEntry entry = new FundTransferEntry(sourcePortfolio, targetPortfolio);
        entry.setDate(LocalDateTime.parse("2020-06-01T00:00"));
        entry.setSourceSecurity(sourceFund);
        entry.setTargetSecurity(targetFund);
        entry.setSourceShares(Values.Share.factorize(3));
        entry.setTargetShares(Values.Share.factorize(5));
        entry.setSourceMonetaryAmount(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(600)));
        entry.setTargetMonetaryAmount(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(600)));
        entry.addCarriedLot(new FundTransferEntry.CarriedLot(LocalDate.parse("2020-01-01"),
                        Values.Share.factorize(3), Values.Share.factorize(5),
                        Money.of(CurrencyUnit.EUR, Values.Amount.factorize(300)), sourceBuy.getUUID()));
        entry.insert();

        Client targetOnly = new ClientSecurityFilter(targetFund).filter(client);
        PortfolioTransaction targetOnlyTransaction = findTransaction(targetOnly.getPortfolios().get(0),
                        PortfolioTransaction.Type.FUND_TRANSFER_IN);
        assertThat(targetOnlyTransaction.getCrossEntry().getOwner(targetOnlyTransaction),
                        is(targetOnly.getPortfolios().get(0)));

        Client sourceOnly = new ClientSecurityFilter(sourceFund).filter(client);
        PortfolioTransaction sourceOnlyTransaction = findTransaction(sourceOnly.getPortfolios().get(0),
                        PortfolioTransaction.Type.FUND_TRANSFER_OUT);
        assertThat(sourceOnlyTransaction.getCrossEntry().getOwner(sourceOnlyTransaction),
                        is(sourceOnly.getPortfolios().get(0)));

        Client both = new ClientSecurityFilter(sourceFund, targetFund).filter(client);
        PortfolioTransaction sourceTransaction = findTransaction(both.getPortfolios().get(0),
                        PortfolioTransaction.Type.FUND_TRANSFER_OUT);
        PortfolioTransaction targetTransaction = findTransaction(both.getPortfolios().get(1),
                        PortfolioTransaction.Type.FUND_TRANSFER_IN);

        assertThat(sourceTransaction.getCrossEntry(), is(targetTransaction.getCrossEntry()));
    }

    private PortfolioTransaction findTransaction(Portfolio portfolio, PortfolioTransaction.Type type)
    {
        return portfolio.getTransactions().stream().filter(t -> t.getType() == type).findFirst()
                        .orElseThrow(IllegalArgumentException::new);
    }
}
