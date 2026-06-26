package name.abuchen.portfolio.ui.wizards.splits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityEvent;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerBuySellTransactionCreator;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerDeliveryTransactionCreator;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerDividendTransactionCreator;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerPortfolioTransferTransactionCreator;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Values;

/**
 * Tests stock split write behavior for ledger-backed share transactions.
 * These tests make sure split adjustments update ledger facts without changing unrelated transactions.
 */
public class LedgerStockSplitWritePathTest
{
    private static final LocalDate EX_DATE = LocalDate.of(2020, 1, 10);
    private static final LocalDateTime TRANSACTION_DATE = LocalDateTime.of(2020, 1, 2, 0, 0);

    /**
     * Verifies that a stock split adjusts both legacy and ledger-backed transactions for the selected security.
     * Unrelated securities and non-share facts must stay unchanged.
     */
    @Test
    public void testApplyChangesAdjustsMixedLegacyAndLedgerBackedTransactions()
    {
        var client = new Client();
        var security = security();
        var otherSecurity = security();
        var account = account();
        var portfolio = portfolio();
        var targetPortfolio = portfolio();

        client.addSecurity(security);
        client.addSecurity(otherSecurity);
        client.addAccount(account);
        client.addPortfolio(portfolio);
        client.addPortfolio(targetPortfolio);

        var legacyBuy = legacyBuy(security);
        portfolio.addTransaction(legacyBuy);
        var unaffectedLegacyBuy = legacyBuy(otherSecurity);
        portfolio.addTransaction(unaffectedLegacyBuy);

        new LedgerBuySellTransactionCreator(client).create(portfolio, account, PortfolioTransaction.Type.BUY,
                        TRANSACTION_DATE, Values.Amount.factorize(100), CurrencyUnit.EUR, security,
                        Values.Share.factorize(10), List.of(), "note", "source");
        new LedgerDividendTransactionCreator(client).create(account, TRANSACTION_DATE, Values.Amount.factorize(5),
                        CurrencyUnit.EUR, security, Values.Share.factorize(10), null, null, null, List.of(), "note",
                        "source");
        new LedgerDeliveryTransactionCreator(client).create(portfolio, PortfolioTransaction.Type.DELIVERY_INBOUND,
                        TRANSACTION_DATE, Values.Amount.factorize(100), CurrencyUnit.EUR, security,
                        Values.Share.factorize(10), null, null, List.of(), "note", "source");
        new LedgerPortfolioTransferTransactionCreator(client).create(portfolio, targetPortfolio, security,
                        TRANSACTION_DATE, Values.Share.factorize(10), Values.Amount.factorize(100), CurrencyUnit.EUR,
                        "note", "source");
        security.addPrice(new SecurityPrice(LocalDate.of(2020, 1, 1), 100L));

        var before = StateBefore.of(client, security);

        splitModel(client, security).applyChanges();

        before.assertOnlySelectedSharesChanged(client, security, EX_DATE, Values.Share.factorize(20));

        assertThat(security.getEvents().size(), is(1));
        assertThat(security.getEvents().get(0).getDate(), is(EX_DATE));
        assertThat(security.getEvents().get(0).getType(), is(SecurityEvent.Type.STOCK_SPLIT));
        assertThat(security.getPrices().get(0).getValue(), is(50L));
        assertThat(legacyBuy.getShares(), is(Values.Share.factorize(20)));
        assertThat(account.getTransactions().get(1).getShares(), is(Values.Share.factorize(20)));
        assertThat(portfolio.getTransactions().get(2).getShares(), is(Values.Share.factorize(20)));
        assertThat(portfolio.getTransactions().get(3).getShares(), is(Values.Share.factorize(20)));
        assertThat(targetPortfolio.getTransactions().get(0).getShares(), is(Values.Share.factorize(20)));
        assertThat(unaffectedLegacyBuy.getShares(), is(Values.Share.factorize(10)));
    }

    /**
     * Verifies that an invalid ledger share adjustment is rejected before live mutation.
     * The split must not add a security event or partially change legacy or ledger-backed shares.
     */
    @Test
    public void testApplyChangesRejectsInvalidLedgerAdjustmentBeforeLiveMutation()
    {
        var client = new Client();
        var security = security();
        var account = account();
        var portfolio = portfolio();

        client.addSecurity(security);
        client.addAccount(account);
        client.addPortfolio(portfolio);

        var legacyBuy = legacyBuy(security);
        portfolio.addTransaction(legacyBuy);

        new LedgerBuySellTransactionCreator(client).create(portfolio, account, PortfolioTransaction.Type.BUY,
                        TRANSACTION_DATE, Values.Amount.factorize(100), CurrencyUnit.EUR, security,
                        Values.Share.factorize(10), List.of(), "note", "source");

        var before = StateBefore.of(client, security);

        var model = splitModel(client, security);
        model.setNewShares(BigDecimal.valueOf(-1));

        assertThrows(IllegalArgumentException.class, model::applyChanges);

        before.assertOnlySelectedSharesChanged(client, security, EX_DATE, Values.Share.factorize(10));
        assertThat(security.getEvents().size(), is(0));
        assertThat(legacyBuy.getShares(), is(Values.Share.factorize(10)));
    }

    private StockSplitModel splitModel(Client client, Security security)
    {
        var model = new StockSplitModel(client, security);
        model.setExDate(EX_DATE);
        model.setNewShares(BigDecimal.valueOf(2));
        model.setOldShares(BigDecimal.ONE);
        return model;
    }

    private PortfolioTransaction legacyBuy(Security security)
    {
        var transaction = new PortfolioTransaction();
        transaction.setType(PortfolioTransaction.Type.BUY);
        transaction.setDateTime(TRANSACTION_DATE);
        transaction.setSecurity(security);
        transaction.setShares(Values.Share.factorize(10));
        transaction.setAmount(Values.Amount.factorize(100));
        transaction.setCurrencyCode(security.getCurrencyCode());
        return transaction;
    }

    private Account account()
    {
        return new Account();
    }

    private Portfolio portfolio()
    {
        return new Portfolio();
    }

    private Security security()
    {
        return new Security("Security", CurrencyUnit.EUR);
    }

    private record StateBefore(List<TransactionState> allTransactions, List<OwnerState> accounts,
                    List<OwnerState> portfolios)
    {
        static StateBefore of(Client client, Security security)
        {
            return new StateBefore(client.getAllTransactions().stream()
                            .map(pair -> TransactionState.of(pair.getTransaction()))
                                            .toList(),
                            client.getAccounts().stream().map(OwnerState::of).toList(),
                            client.getPortfolios().stream().map(OwnerState::of).toList());
        }

        void assertOnlySelectedSharesChanged(Client client, Security security, LocalDate exDate, long adjustedShares)
        {
            assertThat(client.getAccounts().stream().map(OwnerState::of).toList(), is(accounts));
            assertThat(client.getPortfolios().stream().map(OwnerState::of).toList(), is(portfolios));

            var current = client.getAllTransactions().stream().map(pair -> TransactionState.of(pair.getTransaction()))
                            .toList();
            assertThat(current.size(), is(allTransactions.size()));

            for (var before : allTransactions)
            {
                var after = current.stream().filter(snapshot -> snapshot.uuid().equals(before.uuid())).findFirst()
                                .orElseThrow();

                assertThat(after.dateTime(), is(before.dateTime()));
                assertThat(after.security(), is(before.security()));
                assertThat(after.amount(), is(before.amount()));
                assertThat(after.currencyCode(), is(before.currencyCode()));
                assertThat(after.note(), is(before.note()));
                assertThat(after.source(), is(before.source()));

                if (before.security() == security && before.dateTime().toLocalDate().isBefore(exDate))
                    assertThat(after.shares(), is(adjustedShares));
                else
                    assertThat(after.shares(), is(before.shares()));
            }
        }
    }

    private record OwnerState(String uuid, List<String> transactionUUIDs)
    {
        static OwnerState of(Account account)
        {
            return new OwnerState(account.getUUID(),
                            account.getTransactions().stream().map(Transaction::getUUID).toList());
        }

        static OwnerState of(Portfolio portfolio)
        {
            return new OwnerState(portfolio.getUUID(),
                            portfolio.getTransactions().stream().map(Transaction::getUUID).toList());
        }
    }

    private record TransactionState(String uuid, LocalDateTime dateTime, Security security, long shares, long amount,
                    String currencyCode, String note, String source)
    {
        static TransactionState of(Transaction transaction)
        {
            return new TransactionState(transaction.getUUID(), transaction.getDateTime(), transaction.getSecurity(),
                            transaction.getShares(), transaction.getAmount(), transaction.getCurrencyCode(),
                            transaction.getNote(), transaction.getSource());
        }
    }
}
