package name.abuchen.portfolio.model;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.model.ledger.configuration.CorporateActionKind;
import name.abuchen.portfolio.model.ledger.nativeentry.LedgerNativeEntryAssembler;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedAccountTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedPortfolioTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerProjectionService;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class TransactionDetailsTest
{
    private static final Instant UPDATED_AT = Instant.parse("2026-01-02T00:00:00Z");
    private static final LocalDateTime DATE = LocalDateTime.of(2026, 1, 2, 0, 0);

    @Test
    public void testLegacyAccountTransactionDetails()
    {
        var fixture = fixture();
        var transaction = legacyDeposit();
        fixture.account.addTransaction(transaction);

        var details = TransactionDetails.of(fixture.client, transaction);

        assertThat(details.source(), is(TransactionDetailsSource.LEGACY));
        assertTrue(details.isLegacy());
        assertFalse(details.isLedgerProjection());
        assertSame(transaction, details.transaction());
        assertSame(transaction, details.accountTransaction().orElseThrow());
        assertTrue(details.portfolioTransaction().isEmpty());
        assertSame(fixture.account, details.account().orElseThrow());
        assertTrue(details.portfolio().isEmpty());
        assertThat(details.dateTime(), is(DATE));
        assertThat(details.amount().orElseThrow(), is(money(10)));
        assertThat(details.visibleType().orElseThrow(), is(VisibleTransactionKind.ACCOUNT_DEPOSIT));
        assertTrue(details.ledgerEntry().isEmpty());
        assertTrue(details.projectionDescriptor().isEmpty());
        assertThat(details.ledgerPostings().count(), is(0L));
    }

    @Test
    public void testLegacyPortfolioTransactionDetails()
    {
        var fixture = fixture();
        var transaction = legacyDelivery(fixture.sourceSecurity);
        fixture.portfolio.addTransaction(transaction);

        var details = TransactionDetails.of(fixture.client, transaction);

        assertThat(details.source(), is(TransactionDetailsSource.LEGACY));
        assertTrue(details.isLegacy());
        assertFalse(details.isLedgerProjection());
        assertSame(transaction, details.portfolioTransaction().orElseThrow());
        assertTrue(details.accountTransaction().isEmpty());
        assertSame(fixture.portfolio, details.portfolio().orElseThrow());
        assertTrue(details.account().isEmpty());
        assertSame(fixture.sourceSecurity, details.security().orElseThrow());
        assertThat(details.shares().orElseThrow(), is(shares(5)));
        assertThat(details.visibleType().orElseThrow(), is(VisibleTransactionKind.PORTFOLIO_DELIVERY_INBOUND));
        assertTrue(details.ledgerEntry().isEmpty());
    }

    @Test
    public void testLedgerBackedAccountProjectionDetails()
    {
        var fixture = fixture();
        var entry = addCashDistribution(fixture);
        LedgerProjectionService.materialize(fixture.client);
        var transaction = fixture.account.getTransactions().stream()
                        .filter(LedgerBackedAccountTransaction.class::isInstance).findFirst().orElseThrow();

        var details = TransactionDetails.of(fixture.client, transaction);

        assertThat(details.source(), is(TransactionDetailsSource.LEDGER_PROJECTION));
        assertFalse(details.isLegacy());
        assertTrue(details.isLedgerProjection());
        assertSame(fixture.account, details.account().orElseThrow());
        assertSame(fixture.sourceSecurity, details.security().orElseThrow());
        assertThat(details.amount().orElseThrow(), is(money(10)));
        assertThat(details.visibleType().orElseThrow(), is(VisibleTransactionKind.ACCOUNT_DIVIDENDS));
        assertSame(entry, details.ledgerEntry().orElseThrow());
        assertTrue(details.projectionDescriptor().isPresent());
        assertTrue(details.primaryPosting().isPresent());
        assertThat(details.ledgerPostings().count(), is(2L));
        assertThat(details.localKey().orElseThrow(), is("cash-1"));
        assertThat(details.groupKey().orElseThrow(), is("cash-1"));
    }

    @Test
    public void testLedgerBackedPortfolioProjectionDetails()
    {
        var fixture = fixture();
        var entry = addStockDividend(fixture);
        LedgerProjectionService.materialize(fixture.client);
        var transaction = fixture.portfolio.getTransactions().stream()
                        .filter(LedgerBackedPortfolioTransaction.class::isInstance).findFirst().orElseThrow();

        var details = TransactionDetails.of(fixture.client, transaction);

        assertThat(details.source(), is(TransactionDetailsSource.LEDGER_PROJECTION));
        assertTrue(details.isLedgerProjection());
        assertSame(fixture.portfolio, details.portfolio().orElseThrow());
        assertSame(fixture.targetSecurity, details.security().orElseThrow());
        assertThat(details.shares().orElseThrow(), is(shares(3)));
        assertThat(details.visibleType().orElseThrow(), is(VisibleTransactionKind.PORTFOLIO_DELIVERY_INBOUND));
        assertSame(entry, details.ledgerEntry().orElseThrow());
        assertTrue(details.projectionDescriptor().isPresent());
        assertThat(details.unitPostings().count(), is(0L));
    }

    @Test
    public void testStreamContainsLegacyAndLedgerProjectionsWithoutDuplicates()
    {
        var fixture = fixture();
        fixture.account.addTransaction(legacyDeposit());
        addCashDistribution(fixture);
        LedgerProjectionService.materialize(fixture.client);

        var before = fixture.account.getTransactions().size();
        var details = TransactionDetails.stream(fixture.client).toList();
        var after = fixture.account.getTransactions().size();

        assertThat(before, is(after));
        assertThat(details.size(), is(2));
        assertThat(details.stream().filter(TransactionDetails::isLegacy).count(), is(1L));
        assertThat(details.stream().filter(TransactionDetails::isLedgerProjection).count(), is(1L));
        assertThat(details.stream().map(TransactionDetails::transaction).distinct().count(), is(2L));
    }

    @Test
    public void testQueryFilters()
    {
        var fixture = fixture();
        fixture.account.addTransaction(legacyDeposit());
        fixture.portfolio.addTransaction(legacyDelivery(fixture.targetSecurity));
        addCashDistribution(fixture);
        LedgerProjectionService.materialize(fixture.client);

        assertThat(TransactionDetails.query(fixture.client).legacyOnly().stream().count(), is(2L));
        assertThat(TransactionDetails.query(fixture.client).ledgerOnly().stream().count(), is(1L));
        assertThat(TransactionDetails.query(fixture.client).withAccount(fixture.account).stream().count(), is(2L));
        assertThat(TransactionDetails.query(fixture.client).withPortfolio(fixture.portfolio).stream().count(), is(1L));
        assertThat(TransactionDetails.query(fixture.client).withSecurity(fixture.sourceSecurity).stream().count(),
                        is(1L));
        assertThat(TransactionDetails.query(fixture.client).withType(VisibleTransactionKind.ACCOUNT_DIVIDENDS).stream()
                        .count(), is(1L));
        assertThat(TransactionDetails.query(fixture.client).between(LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 1, 3)).stream().count(), is(3L));
    }

    @Test
    public void testStreamCanBeCalledRepeatedlyWithoutMutation()
    {
        var fixture = fixture();
        addCashDistribution(fixture);
        LedgerProjectionService.materialize(fixture.client);

        var accountTransactions = fixture.account.getTransactions().size();
        var first = TransactionDetails.stream(fixture.client).toList();
        var second = TransactionDetails.stream(fixture.client).toList();

        assertThat(first.size(), is(1));
        assertThat(second.size(), is(1));
        assertThat(fixture.account.getTransactions().size(), is(accountTransactions));
    }

    @Test
    public void testContributorStyleSecurityStream()
    {
        var fixture = fixture();
        fixture.portfolio.addTransaction(legacyDelivery(fixture.targetSecurity));
        addCashDistribution(fixture);
        LedgerProjectionService.materialize(fixture.client);

        var securities = TransactionDetails.stream(fixture.client).flatMap(details -> details.security().stream())
                        .toList();

        assertThat(securities, hasItem(fixture.sourceSecurity));
        assertThat(securities, hasItem(fixture.targetSecurity));
    }

    private static name.abuchen.portfolio.model.ledger.LedgerEntry addCashDistribution(Fixture fixture)
    {
        return LedgerNativeEntryAssembler.corporateAction(fixture.client) //
                        .kind(CorporateActionKind.CASH_DISTRIBUTION) //
                        .date(DATE) //
                        .securityContext("context-1", "cash-1", fixture.portfolio, fixture.sourceSecurity) //
                        .cash("cash-1", "cash-1", fixture.account, money(10)) //
                        .buildAndAdd().getEntry();
    }

    private static name.abuchen.portfolio.model.ledger.LedgerEntry addStockDividend(Fixture fixture)
    {
        return LedgerNativeEntryAssembler.corporateAction(fixture.client) //
                        .kind(CorporateActionKind.STOCK_DIVIDEND) //
                        .date(DATE) //
                        .securityContext("context-1", "target-1", fixture.portfolio, fixture.sourceSecurity) //
                        .securityIn("target-1", fixture.portfolio, fixture.targetSecurity, shares(3)) //
                        .buildAndAdd().getEntry();
    }

    private static Client client(Account account, Portfolio portfolio, Security sourceSecurity, Security targetSecurity)
    {
        var client = new Client();
        client.addAccount(account);
        client.addPortfolio(portfolio);
        client.addSecurity(sourceSecurity);
        client.addSecurity(targetSecurity);
        return client;
    }

    private static Fixture fixture()
    {
        var account = new Account();
        var portfolio = new Portfolio();
        var sourceSecurity = new Security("Source AG", CurrencyUnit.EUR);
        var targetSecurity = new Security("Target AG", CurrencyUnit.EUR);

        account.setName("Account");
        account.setCurrencyCode(CurrencyUnit.EUR);
        account.setUpdatedAt(UPDATED_AT);
        portfolio.setName("Portfolio");
        portfolio.setReferenceAccount(account);
        portfolio.setUpdatedAt(UPDATED_AT);
        sourceSecurity.setUpdatedAt(UPDATED_AT);
        targetSecurity.setUpdatedAt(UPDATED_AT);

        return new Fixture(client(account, portfolio, sourceSecurity, targetSecurity), account, portfolio,
                        sourceSecurity, targetSecurity);
    }

    private static AccountTransaction legacyDeposit()
    {
        var transaction = new AccountTransaction(AccountTransaction.Type.DEPOSIT);

        transaction.setDateTime(DATE);
        transaction.setUpdatedAt(UPDATED_AT);
        transaction.setCurrencyCode(CurrencyUnit.EUR);
        transaction.setAmount(Values.Amount.factorize(10));

        return transaction;
    }

    private static PortfolioTransaction legacyDelivery(Security security)
    {
        var transaction = new PortfolioTransaction(PortfolioTransaction.Type.DELIVERY_INBOUND);

        transaction.setDateTime(DATE);
        transaction.setUpdatedAt(UPDATED_AT);
        transaction.setSecurity(security);
        transaction.setCurrencyCode(CurrencyUnit.EUR);
        transaction.setAmount(Values.Amount.factorize(10));
        transaction.setShares(shares(5));

        return transaction;
    }

    private static Money money(long amount)
    {
        return Money.of(CurrencyUnit.EUR, Values.Amount.factorize(amount));
    }

    private static long shares(long shares)
    {
        return Values.Share.factorize(shares);
    }

    private record Fixture(Client client, Account account, Portfolio portfolio, Security sourceSecurity,
                    Security targetSecurity)
    {
    }
}
