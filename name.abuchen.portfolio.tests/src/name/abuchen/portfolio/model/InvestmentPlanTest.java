package name.abuchen.portfolio.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.junit.TestCurrencyConverter;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;
import name.abuchen.portfolio.model.ledger.LedgerStructuralValidator;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedTransaction;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.util.TradeCalendar;
import name.abuchen.portfolio.util.TradeCalendarManager;

/**
 * Tests investment plan behavior, including generated ledger-backed bookings.
 * These tests make sure plans can still resolve generated transactions after ledger-aware updates and conversions.
 */
@SuppressWarnings("nls")
public class InvestmentPlanTest
{
    Client client;
    Security security;
    Account account;
    Portfolio portfolio;
    InvestmentPlan investmentPlan;

    @Before
    public void setup()
    {
        this.client = new Client();
        this.security = new SecurityBuilder().addPrice("2015-01-01", Values.Quote.factorize(10)).addTo(client);
        this.account = new AccountBuilder().addTo(client);
        this.portfolio = new PortfolioBuilder(account).addTo(client);
        this.investmentPlan = new InvestmentPlan();
        this.investmentPlan.setAmount(Values.Amount.factorize(100));
        this.investmentPlan.setInterval(1);
    }

    /**
     * Checks the generated booking scenario: generation of buy transaction.
     * The plan reference must resolve to the intended transaction after the operation.
     * This prevents stale or ambiguous generated booking references.
     */
    @Test
    public void testGenerationOfBuyTransaction() throws IOException
    {
        investmentPlan.setType(InvestmentPlan.Type.PURCHASE_OR_DELIVERY);

        investmentPlan.setAccount(account); // set both account and portfolio
        investmentPlan.setPortfolio(portfolio); // causes securities to be
                                                // bought
        investmentPlan.setSecurity(security);
        investmentPlan.setStart(LocalDateTime.parse("2016-01-31T00:00:00"));

        investmentPlan.generateTransactions(new TestCurrencyConverter());

        List<Transaction> tx = investmentPlan.getTransactions().stream()
                        .filter(t -> t.getDateTime().isBefore(LocalDateTime.parse("2017-04-10T00:00")))
                        .collect(Collectors.toList());

        assertThat(tx.size(), is(15));

        tx = investmentPlan.getTransactions().stream()
                        .filter(t -> t.getDateTime().getYear() == 2016 && t.getDateTime().getMonth() == Month.MAY)
                        .collect(Collectors.toList());

        assertThat(investmentPlan.getPlanType(), is(InvestmentPlan.Type.PURCHASE_OR_DELIVERY));

        // May 2016 should contain two transactions:
        // one "spilled over" from April as 30 April is a Saturday
        // and the regular one from 31 May

        assertThat(tx.size(), is(2));
        assertThat(tx.get(0), instanceOf(PortfolioTransaction.class));
        assertThat(tx.get(1), instanceOf(PortfolioTransaction.class));

        assertThat(tx.get(0).getDateTime(), is(LocalDateTime.parse("2016-05-02T00:00")));
        assertThat(((PortfolioTransaction) tx.get(0)).getType(), is(PortfolioTransaction.Type.BUY));

        assertThat(tx.get(1).getDateTime(), is(LocalDateTime.parse("2016-05-31T00:00")));
        assertThat(((PortfolioTransaction) tx.get(1)).getType(), is(PortfolioTransaction.Type.BUY));

        // check that delta generation of transactions also takes into account
        // the transaction "spilled over" into the next month

        investmentPlan.getTransactions().stream()
                        .filter(t -> t.getDateTime().isAfter(LocalDateTime.parse("2016-05-10T00:00")))
                        .collect(Collectors.toList())
                        .forEach(t -> investmentPlan.removeTransaction((PortfolioTransaction) t));

        List<TransactionPair<?>> newlyGenerated = investmentPlan.generateTransactions(new TestCurrencyConverter());
        assertThat(newlyGenerated.isEmpty(), is(false));
        assertThat(newlyGenerated.get(0).getTransaction(), instanceOf(PortfolioTransaction.class));
        assertThat(newlyGenerated.get(0).getTransaction().getDateTime(), is(LocalDateTime.parse("2016-05-31T00:00")));
    }

    /**
     * Checks the generated booking scenario: generation of delivery transaction.
     * The plan reference must resolve to the intended transaction after the operation.
     * This prevents stale or ambiguous generated booking references.
     */
    @Test
    public void testGenerationOfDeliveryTransaction() throws IOException
    {
        investmentPlan.setType(InvestmentPlan.Type.PURCHASE_OR_DELIVERY);

        // set portfolio only causes securities to be delivered in
        investmentPlan.setPortfolio(portfolio);

        investmentPlan.setSecurity(security);
        investmentPlan.setStart(LocalDateTime.parse("2016-01-31T00:00"));

        investmentPlan.generateTransactions(new TestCurrencyConverter());

        List<Transaction> tx = investmentPlan.getTransactions().stream()
                        .filter(t -> t.getDateTime().isBefore(LocalDateTime.parse("2017-04-10T00:00")))
                        .collect(Collectors.toList());

        assertThat(tx.size(), is(15));

        tx = investmentPlan.getTransactions().stream()
                        .filter(t -> t.getDateTime().getYear() == 2016 && t.getDateTime().getMonth() == Month.MAY)
                        .collect(Collectors.toList());

        assertThat(investmentPlan.getPlanType(), is(InvestmentPlan.Type.PURCHASE_OR_DELIVERY));

        // May 2016 should contain two transactions:
        // one "spilled over" from April as 30 April is a Saturday
        // and the regular one from 31 May

        assertThat(tx.size(), is(2));
        assertThat(tx.get(0), instanceOf(PortfolioTransaction.class));
        assertThat(tx.get(0).getDateTime(), is(LocalDateTime.parse("2016-05-02T00:00")));
        assertThat(((PortfolioTransaction) tx.get(0)).getType(), is(PortfolioTransaction.Type.DELIVERY_INBOUND));
        assertThat(tx.get(1), instanceOf(PortfolioTransaction.class));
        assertThat(tx.get(1).getDateTime(), is(LocalDateTime.parse("2016-05-31T00:00")));
        assertThat(((PortfolioTransaction) tx.get(1)).getType(), is(PortfolioTransaction.Type.DELIVERY_INBOUND));
    }

    /**
     * Checks the generated booking scenario: generation of deposit transaction.
     * The plan reference must resolve to the intended transaction after the operation.
     * This prevents stale or ambiguous generated booking references.
     */
    @Test
    public void testGenerationOfDepositTransaction() throws IOException
    {
        investmentPlan.setType(InvestmentPlan.Type.DEPOSIT);

        investmentPlan.setAccount(account);
        investmentPlan.setStart(LocalDateTime.parse("2016-01-31T00:00"));

        investmentPlan.generateTransactions(new TestCurrencyConverter());

        List<Transaction> tx = investmentPlan.getTransactions().stream()
                        .filter(t -> t.getDateTime().isBefore(LocalDateTime.parse("2017-04-10T00:00")))
                        .collect(Collectors.toList());

        assertThat(tx.size(), is(15));

        tx = investmentPlan.getTransactions().stream()
                        .filter(t -> t.getDateTime().getYear() == 2016 && t.getDateTime().getMonth() == Month.MAY)
                        .collect(Collectors.toList());

        assertThat(investmentPlan.getPlanType(), is(InvestmentPlan.Type.DEPOSIT));

        // May 2016 should contain two transactions:
        // one "spilled over" from April as 30 April is a Saturday
        // and the regular one from 31 May

        assertThat(tx.isEmpty(), is(false));
        assertThat(tx.size(), is(2));

        assertThat(tx.get(0), instanceOf(AccountTransaction.class));
        assertThat(tx.get(0).getDateTime(), is(LocalDateTime.parse("2016-05-02T00:00")));
        assertThat(((AccountTransaction) tx.get(0)).getType(), is(AccountTransaction.Type.DEPOSIT));

        assertThat(tx.get(1), instanceOf(AccountTransaction.class));
        assertThat(tx.get(1).getDateTime(), is(LocalDateTime.parse("2016-05-31T00:00")));
        assertThat(((AccountTransaction) tx.get(1)).getType(), is(AccountTransaction.Type.DEPOSIT));
    }

    /**
     * Checks the generated booking scenario: generation of removal transaction.
     * The plan reference must resolve to the intended transaction after the operation.
     * This prevents stale or ambiguous generated booking references.
     */
    @Test
    public void testGenerationOfRemovalTransaction() throws IOException
    {
        investmentPlan.setType(InvestmentPlan.Type.REMOVAL);

        investmentPlan.setAmount(Values.Amount.factorize(100));

        investmentPlan.setAccount(account);
        investmentPlan.setStart(LocalDateTime.parse("2022-03-29T00:00"));

        investmentPlan.generateTransactions(new TestCurrencyConverter());

        List<Transaction> tx = investmentPlan.getTransactions().stream()
                        .filter(t -> t.getDateTime().getYear() == 2022 && t.getDateTime().getMonth() == Month.MARCH)
                        .collect(Collectors.toList());

        assertThat(investmentPlan.getPlanType(), is(InvestmentPlan.Type.REMOVAL));

        assertThat(tx.isEmpty(), is(false));
        assertThat(tx.size(), is(1));

        assertThat(tx.get(0), instanceOf(AccountTransaction.class));
        assertThat(tx.get(0).getDateTime(), is(LocalDateTime.parse("2022-03-29T00:00")));
        assertThat(((AccountTransaction) tx.get(0)).getType(), is(AccountTransaction.Type.REMOVAL));
        assertThat(((AccountTransaction) tx.get(0)).getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(100))));
    }

    /**
     * Checks the generated booking scenario: generation of interest transaction.
     * The plan reference must resolve to the intended transaction after the operation.
     * This prevents stale or ambiguous generated booking references.
     */
    @Test
    public void testGenerationOfInterestTransaction() throws IOException
    {
        investmentPlan.setType(InvestmentPlan.Type.INTEREST);

        investmentPlan.setAmount(Values.Amount.factorize(200));
        investmentPlan.setTaxes(Values.Amount.factorize(10));

        investmentPlan.setAccount(account);
        investmentPlan.setStart(LocalDateTime.parse("2022-03-29T00:00"));

        investmentPlan.generateTransactions(new TestCurrencyConverter());

        List<Transaction> tx = investmentPlan.getTransactions().stream()
                        .filter(t -> t.getDateTime().getYear() == 2022 && t.getDateTime().getMonth() == Month.MARCH)
                        .collect(Collectors.toList());

        assertThat(investmentPlan.getPlanType(), is(InvestmentPlan.Type.INTEREST));

        assertThat(tx.isEmpty(), is(false));
        assertThat(tx.size(), is(1));

        assertThat(tx.get(0), instanceOf(AccountTransaction.class));
        assertThat(tx.get(0).getDateTime(), is(LocalDateTime.parse("2022-03-29T00:00")));
        assertThat(((AccountTransaction) tx.get(0)).getType(), is(AccountTransaction.Type.INTEREST));
        assertThat(((AccountTransaction) tx.get(0)).getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(200))));
        assertThat(((AccountTransaction) tx.get(0)).getUnitSum(Unit.Type.TAX),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10))));
    }

    /**
     * Checks the generated booking scenario: no generation with start in future.
     * The plan reference must resolve to the intended transaction after the operation.
     * This prevents stale or ambiguous generated booking references.
     */
    @Test
    public void testNoGenerationWithStartInFuture() throws IOException
    {
        investmentPlan.setType(InvestmentPlan.Type.DEPOSIT);

        investmentPlan.setAccount(account);
        investmentPlan.setStart(LocalDate.now().minusMonths(6));
        investmentPlan.setInterval(12);

        investmentPlan.generateTransactions(new TestCurrencyConverter());
        int previousTransactionCount = investmentPlan.getTransactions().size();

        // given is an investment plan with existing transactions,
        // the user changes the start date to be in the future

        investmentPlan.setStart(LocalDate.now().plusMonths(12));
        investmentPlan.setInterval(1);
        investmentPlan.generateTransactions(new TestCurrencyConverter());

        // no new transactions should be created until this date
        assertThat(investmentPlan.getTransactions(), hasSize(previousTransactionCount));

        // generation resumes at start date
        LocalDate resumeDate = LocalDate.now().minusMonths(1).minusDays(10);
        investmentPlan.setStart(resumeDate);
        investmentPlan.generateTransactions(new TestCurrencyConverter());
        assertThat(investmentPlan.getTransactions(), hasSize(previousTransactionCount + 2));

        LocalDate firstNewDate = investmentPlan.getTransactions().get(previousTransactionCount).getDateTime()
                        .toLocalDate();

        // if resume date is on a bank holiday, move the resume date to the next
        // non-holiday
        TradeCalendar tradeCalendar = TradeCalendarManager.getDefaultInstance();
        while (tradeCalendar.isHoliday(resumeDate))
            resumeDate = resumeDate.plusDays(1);

        assertThat(firstNewDate, is(resumeDate));
    }

    @Test(expected = IOException.class)
    public void testErrorMessageWhenNoQuotesExist() throws IOException
    {
        security.removeAllPrices();

        investmentPlan.setType(InvestmentPlan.Type.PURCHASE_OR_DELIVERY);

        investmentPlan.setAccount(account);
        investmentPlan.setPortfolio(portfolio);
        investmentPlan.setSecurity(security);
        investmentPlan.setStart(LocalDateTime.parse("2016-01-31T00:00:00"));

        investmentPlan.generateTransactions(new TestCurrencyConverter());
    }

    /**
     * Checks the generated booking scenario: generation of weekly buy transaction.
     * The plan reference must resolve to the intended transaction after the operation.
     * This prevents stale or ambiguous generated booking references.
     */
    @Test
    public void testGenerationOfWeeklyBuyTransaction() throws IOException
    {
        investmentPlan.setType(InvestmentPlan.Type.PURCHASE_OR_DELIVERY);

        investmentPlan.setAccount(account); // set both account and portfolio
        investmentPlan.setPortfolio(portfolio); // causes securities to be
                                                // bought
        investmentPlan.setSecurity(security);
        investmentPlan.setStart(LocalDateTime.parse("2016-01-01T00:00:00"));

        investmentPlan.setInterval(101); // 101 is weekly, 201 bi-weekly

        investmentPlan.generateTransactions(new TestCurrencyConverter());

        var june2016 = LocalDateTime.parse("2016-06-01T00:00");
        assertThat(investmentPlan.getTransactions().stream().filter(t -> t.getDateTime().isBefore(june2016)).count(),
                        is(22L));

        // Friday 1st January 2016 is a holiday : all transaction shall be on
        // Fridays except on holiday.
        // The real first transaction is Monday 4 January 2016
        var tx = investmentPlan.getTransactions().stream().toList();
        assertThat(tx.get(0), instanceOf(PortfolioTransaction.class));
        assertThat(tx.get(0).getDateTime(), is(LocalDateTime.parse("2016-01-04T00:00")));
        assertThat(((PortfolioTransaction) tx.get(0)).getType(), is(PortfolioTransaction.Type.BUY));

        // check that the second date is reverted back to Friday
        assertThat(tx.get(1).getDateTime(), is(LocalDateTime.parse("2016-01-08T00:00")));

        assertThat(investmentPlan.getPlanType(), is(InvestmentPlan.Type.PURCHASE_OR_DELIVERY));

        // March 2016 has Friday 25 and Monday 28 as holidays :
        // the March 25th transaction should happen on March 29th
        var txMarch = investmentPlan.getTransactions().stream()
                        .filter(t -> t.getDateTime().getYear() == 2016 && t.getDateTime().getMonth() == Month.MARCH)
                        .toList();
        assertThat(txMarch.size(), is(4));
        assertThat(txMarch.get(0).getDateTime(), is(LocalDateTime.parse("2016-03-04T00:00")));
        assertThat(txMarch.get(1).getDateTime(), is(LocalDateTime.parse("2016-03-11T00:00")));
        assertThat(txMarch.get(2).getDateTime(), is(LocalDateTime.parse("2016-03-18T00:00")));
        assertThat(txMarch.get(3).getDateTime(), is(LocalDateTime.parse("2016-03-29T00:00")));

        // There are 5 Fridays in April 2016 : April should have 5 transaction.
        // Check that the weekly plan is back on Friday 1st April 2016 after the
        // Tuesday 29 March 2016 transaction.
        var txApril = investmentPlan.getTransactions().stream()
                        .filter(t -> t.getDateTime().getYear() == 2016 && t.getDateTime().getMonth() == Month.APRIL)
                        .toList();
        assertThat(txApril.size(), is(5));
        assertThat(txApril.get(0).getDateTime(), is(LocalDateTime.parse("2016-04-01T00:00")));
        assertThat(txApril.get(1).getDateTime(), is(LocalDateTime.parse("2016-04-08T00:00")));
        assertThat(txApril.get(2).getDateTime(), is(LocalDateTime.parse("2016-04-15T00:00")));
        assertThat(txApril.get(3).getDateTime(), is(LocalDateTime.parse("2016-04-22T00:00")));
        assertThat(txApril.get(4).getDateTime(), is(LocalDateTime.parse("2016-04-29T00:00")));

        // check that generation of transactions also takes into account
        // the Calendar even when regenerated
        investmentPlan.getTransactions().stream()
                        .filter(t -> t.getDateTime().isAfter(LocalDateTime.parse("2016-03-20T00:00"))).toList()
                        .forEach(t -> investmentPlan.removeTransaction((PortfolioTransaction) t));

        List<TransactionPair<?>> newlyGenerated = investmentPlan.generateTransactions(new TestCurrencyConverter());
        assertThat(newlyGenerated.isEmpty(), is(false));
        assertThat(newlyGenerated.get(0).getTransaction().getDateTime(), is(LocalDateTime.parse("2016-03-29T00:00")));
        assertThat(newlyGenerated.get(1).getTransaction().getDateTime(), is(LocalDateTime.parse("2016-04-01T00:00")));
        assertThat(newlyGenerated.get(2).getTransaction().getDateTime(), is(LocalDateTime.parse("2016-04-08T00:00")));
    }

    /**
     * Checks the generated booking scenario: weekly from18 march2024.
     * The plan reference must resolve to the intended transaction after the operation.
     * This prevents stale or ambiguous generated booking references.
     */
    @Test
    public void testWeeklyFrom18March2024() throws IOException
    {
        investmentPlan.setType(InvestmentPlan.Type.PURCHASE_OR_DELIVERY);

        investmentPlan.setAccount(account);
        investmentPlan.setPortfolio(portfolio);
        investmentPlan.setSecurity(security);
        investmentPlan.setStart(LocalDateTime.parse("2024-03-18T00:00:00"));

        investmentPlan.setInterval(101); // 101 is weekly, 201 bi-weekly

        investmentPlan.generateTransactions(new TestCurrencyConverter());

        var tx = investmentPlan.getTransactions().stream().toList();
        assertThat(tx.get(0).getDateTime(), is(LocalDateTime.parse("2024-03-18T00:00")));
        assertThat(tx.get(1).getDateTime(), is(LocalDateTime.parse("2024-03-25T00:00")));
        assertThat(tx.get(2).getDateTime(), is(LocalDateTime.parse("2024-04-02T00:00")));
        assertThat(tx.get(3).getDateTime(), is(LocalDateTime.parse("2024-04-08T00:00")));
    }

    /**
     * Checks the generated booking scenario: ledger generation of buy stores portfolio execution ref.
     * The plan reference must resolve to the intended transaction after the operation.
     * This prevents stale or ambiguous generated booking references.
     */
    @Test
    public void testLedgerGenerationOfBuyStoresPortfolioExecutionRef() throws IOException
    {
        investmentPlan.setName("Buy Plan");
        investmentPlan.setType(InvestmentPlan.Type.PURCHASE_OR_DELIVERY);
        investmentPlan.setAccount(account);
        investmentPlan.setPortfolio(portfolio);
        investmentPlan.setSecurity(security);
        investmentPlan.setFees(Values.Amount.factorize(1));
        investmentPlan.setTaxes(Values.Amount.factorize(2));
        investmentPlan.setStart(LocalDate.now().minusMonths(1));
        investmentPlan.setInterval(12);

        List<TransactionPair<?>> generated = investmentPlan.generateTransactions(client, new TestCurrencyConverter());

        assertThat(generated, hasSize(1));
        assertThat(investmentPlan.getTransactions(), hasSize(0));
        assertThat(investmentPlan.getLedgerExecutionRefs(), hasSize(1));
        assertThat(client.getLedger().getEntries(), hasSize(1));
        assertThat(account.getTransactions(), hasSize(1));
        assertThat(portfolio.getTransactions(), hasSize(1));
        assertThat(client.getAllTransactions(), hasSize(1));

        var transaction = generated.get(0).getTransaction();
        assertThat(transaction, instanceOf(LedgerBackedTransaction.class));
        assertThat(transaction, instanceOf(PortfolioTransaction.class));
        assertThat(((PortfolioTransaction) transaction).getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(transaction.getUnitSum(Unit.Type.FEE), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2))));

        var ledgerBacked = (LedgerBackedTransaction) transaction;
        var ref = investmentPlan.getLedgerExecutionRefs().get(0);
        assertThat(ref.getLedgerEntryUUID(), is(ledgerBacked.getLedgerEntry().getUUID()));
        assertThat(ref.getProjectionUUID(), is(ledgerBacked.getLedgerProjectionRef().getUUID()));
        assertThat(ref.getProjectionRole(), is(LedgerProjectionRole.PORTFOLIO));
        assertThat(investmentPlan.getTransactions(client).get(0).getTransaction(), is(transaction));
        assertValidLedger();
    }

    /**
     * Checks the generated booking scenario: ledger generation of delivery stores portfolio execution ref.
     * The plan reference must resolve to the intended transaction after the operation.
     * This prevents stale or ambiguous generated booking references.
     */
    @Test
    public void testLedgerGenerationOfDeliveryStoresPortfolioExecutionRef() throws IOException
    {
        investmentPlan.setName("Delivery Plan");
        investmentPlan.setType(InvestmentPlan.Type.PURCHASE_OR_DELIVERY);
        investmentPlan.setPortfolio(portfolio);
        investmentPlan.setSecurity(security);
        investmentPlan.setStart(LocalDate.now().minusMonths(1));
        investmentPlan.setInterval(12);

        List<TransactionPair<?>> generated = investmentPlan.generateTransactions(client, new TestCurrencyConverter());

        assertThat(generated, hasSize(1));
        assertThat(investmentPlan.getTransactions(), hasSize(0));
        assertThat(investmentPlan.getLedgerExecutionRefs(), hasSize(1));
        assertThat(client.getLedger().getEntries(), hasSize(1));
        assertThat(portfolio.getTransactions(), hasSize(1));
        assertThat(client.getAllTransactions(), hasSize(1));

        var transaction = generated.get(0).getTransaction();
        assertThat(transaction, instanceOf(LedgerBackedTransaction.class));
        assertThat(transaction, instanceOf(PortfolioTransaction.class));
        assertThat(((PortfolioTransaction) transaction).getType(), is(PortfolioTransaction.Type.DELIVERY_INBOUND));

        var ledgerBacked = (LedgerBackedTransaction) transaction;
        var ref = investmentPlan.getLedgerExecutionRefs().get(0);
        assertThat(ref.getLedgerEntryUUID(), is(ledgerBacked.getLedgerEntry().getUUID()));
        assertThat(ref.getProjectionUUID(), is(ledgerBacked.getLedgerProjectionRef().getUUID()));
        assertThat(ref.getProjectionRole(), is(LedgerProjectionRole.DELIVERY_INBOUND));
        assertThat(investmentPlan.getTransactions(client).get(0).getTransaction(), is(transaction));
        assertValidLedger();
    }

    /**
     * Checks the generated booking scenario: ledger generation of account only stores account execution ref.
     * The plan reference must resolve to the intended transaction after the operation.
     * This prevents stale or ambiguous generated booking references.
     */
    @Test
    public void testLedgerGenerationOfAccountOnlyStoresAccountExecutionRef() throws IOException
    {
        investmentPlan.setName("Interest Plan");
        investmentPlan.setType(InvestmentPlan.Type.INTEREST);
        investmentPlan.setAccount(account);
        investmentPlan.setAmount(Values.Amount.factorize(200));
        investmentPlan.setTaxes(Values.Amount.factorize(10));
        investmentPlan.setStart(LocalDate.now().minusMonths(1));
        investmentPlan.setInterval(12);

        List<TransactionPair<?>> generated = investmentPlan.generateTransactions(client, new TestCurrencyConverter());

        assertThat(generated, hasSize(1));
        assertThat(investmentPlan.getTransactions(), hasSize(0));
        assertThat(investmentPlan.getLedgerExecutionRefs(), hasSize(1));
        assertThat(client.getLedger().getEntries(), hasSize(1));
        assertThat(account.getTransactions(), hasSize(1));
        assertThat(client.getAllTransactions(), hasSize(1));

        var transaction = generated.get(0).getTransaction();
        assertThat(transaction, instanceOf(LedgerBackedTransaction.class));
        assertThat(transaction, instanceOf(AccountTransaction.class));
        assertThat(((AccountTransaction) transaction).getType(), is(AccountTransaction.Type.INTEREST));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(200))));
        assertThat(transaction.getUnitSum(Unit.Type.TAX), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10))));

        var ledgerBacked = (LedgerBackedTransaction) transaction;
        var ref = investmentPlan.getLedgerExecutionRefs().get(0);
        assertThat(ref.getLedgerEntryUUID(), is(ledgerBacked.getLedgerEntry().getUUID()));
        assertThat(ref.getProjectionUUID(), is(ledgerBacked.getLedgerProjectionRef().getUUID()));
        assertThat(ref.getProjectionRole(), is(LedgerProjectionRole.ACCOUNT));
        assertThat(investmentPlan.getTransactions(client).get(0).getTransaction(), is(transaction));
        assertValidLedger();
    }

    /**
     * Checks the generated booking scenario: ledger generation of deposit without units stores account execution ref.
     * The plan reference must resolve to the intended transaction after the operation.
     * This prevents stale or ambiguous generated booking references.
     */
    @Test
    public void testLedgerGenerationOfDepositWithoutUnitsStoresAccountExecutionRef() throws IOException
    {
        investmentPlan.setName("Deposit Plan");
        investmentPlan.setType(InvestmentPlan.Type.DEPOSIT);
        investmentPlan.setAccount(account);
        investmentPlan.setAmount(Values.Amount.factorize(200));
        investmentPlan.setStart(LocalDate.now().minusMonths(1));
        investmentPlan.setInterval(12);

        List<TransactionPair<?>> generated = investmentPlan.generateTransactions(client, new TestCurrencyConverter());

        assertThat(generated, hasSize(1));
        assertThat(investmentPlan.getTransactions(), hasSize(0));
        assertThat(investmentPlan.getLedgerExecutionRefs(), hasSize(1));
        assertThat(client.getLedger().getEntries(), hasSize(1));
        assertThat(account.getTransactions(), hasSize(1));
        assertThat(client.getAllTransactions(), hasSize(1));

        var transaction = generated.get(0).getTransaction();
        assertThat(transaction, instanceOf(LedgerBackedTransaction.class));
        assertThat(transaction, instanceOf(AccountTransaction.class));
        assertThat(((AccountTransaction) transaction).getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(transaction.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(200))));

        var ledgerBacked = (LedgerBackedTransaction) transaction;
        var ref = investmentPlan.getLedgerExecutionRefs().get(0);
        assertThat(ref.getLedgerEntryUUID(), is(ledgerBacked.getLedgerEntry().getUUID()));
        assertThat(ref.getProjectionUUID(), is(ledgerBacked.getLedgerProjectionRef().getUUID()));
        assertThat(ref.getProjectionRole(), is(LedgerProjectionRole.ACCOUNT));
        assertValidLedger();
    }

    /**
     * Checks the generated booking scenario: ledger generation rejects unsupported deposit units before mutation.
     * The plan reference must resolve to the intended transaction after the operation.
     * This prevents stale or ambiguous generated booking references.
     */
    @Test
    public void testLedgerGenerationRejectsUnsupportedDepositUnitsBeforeMutation()
    {
        assertUnsupportedAccountOnlyUnitsRejectedBeforeMutation(InvestmentPlan.Type.DEPOSIT);
    }

    /**
     * Checks the generated booking scenario: ledger generation rejects unsupported removal units before mutation.
     * The plan reference must resolve to the intended transaction after the operation.
     * This prevents stale or ambiguous generated booking references.
     */
    @Test
    public void testLedgerGenerationRejectsUnsupportedRemovalUnitsBeforeMutation()
    {
        assertUnsupportedAccountOnlyUnitsRejectedBeforeMutation(InvestmentPlan.Type.REMOVAL);
    }

    /**
     * Checks the generated booking scenario: ledger generation is idempotent and removal clears execution ref.
     * The plan reference must resolve to the intended transaction after the operation.
     * This prevents stale or ambiguous generated booking references.
     */
    @Test
    public void testLedgerGenerationIsIdempotentAndRemovalClearsExecutionRef() throws IOException
    {
        investmentPlan.setName("Idempotent Plan");
        investmentPlan.setType(InvestmentPlan.Type.PURCHASE_OR_DELIVERY);
        investmentPlan.setAccount(account);
        investmentPlan.setPortfolio(portfolio);
        investmentPlan.setSecurity(security);
        investmentPlan.setStart(LocalDate.now().minusMonths(1));
        investmentPlan.setInterval(12);

        List<TransactionPair<?>> generated = investmentPlan.generateTransactions(client, new TestCurrencyConverter());
        investmentPlan.generateTransactions(client, new TestCurrencyConverter());
        var generatedDate = generated.get(0).getTransaction().getDateTime().toLocalDate();

        assertThat(client.getLedger().getEntries(), hasSize(1));
        assertThat(account.getTransactions(), hasSize(1));
        assertThat(portfolio.getTransactions(), hasSize(1));
        assertThat(investmentPlan.getLedgerExecutionRefs(), hasSize(1));
        assertTrue(investmentPlan.getLastDate().isEmpty());
        assertThat(investmentPlan.getLastDate(client).orElseThrow(), is(generatedDate));
        assertTrue(investmentPlan.getDateOfNextTransactionToBeGenerated(client).isAfter(generatedDate));

        investmentPlan.removeTransaction((PortfolioTransaction) generated.get(0).getTransaction());

        assertThat(investmentPlan.getLedgerExecutionRefs(), hasSize(0));
        assertThat(client.getLedger().getEntries(), hasSize(1));
        assertThat(portfolio.getTransactions(), hasSize(1));
    }

    /**
     * Checks the generated booking scenario: ledger deletion clears execution refs before protobuf roundtrip.
     * The plan reference must resolve to the intended transaction after the operation.
     * This prevents stale or ambiguous generated booking references.
     */
    @Test
    public void testLedgerDeletionClearsExecutionRefsBeforeProtobufRoundtrip() throws IOException
    {
        investmentPlan.setName("Deleted Ledger Plan");
        investmentPlan.setType(InvestmentPlan.Type.PURCHASE_OR_DELIVERY);
        investmentPlan.setAccount(account);
        investmentPlan.setPortfolio(portfolio);
        investmentPlan.setSecurity(security);
        investmentPlan.setStart(LocalDate.now().minusMonths(1));
        investmentPlan.setInterval(12);
        client.addPlan(investmentPlan);

        InvestmentPlan unrelatedPlan = new InvestmentPlan();
        unrelatedPlan.setName("Unrelated Ledger Plan");
        unrelatedPlan.setType(InvestmentPlan.Type.PURCHASE_OR_DELIVERY);
        unrelatedPlan.setAccount(account);
        unrelatedPlan.setPortfolio(portfolio);
        unrelatedPlan.setSecurity(security);
        unrelatedPlan.setStart(LocalDate.now().minusMonths(1));
        unrelatedPlan.setInterval(12);
        unrelatedPlan.setAmount(Values.Amount.factorize(100));
        client.addPlan(unrelatedPlan);

        var generated = investmentPlan.generateTransactions(client, new TestCurrencyConverter());
        var unrelatedGenerated = unrelatedPlan.generateTransactions(client, new TestCurrencyConverter());
        var deletedPortfolioProjection = (PortfolioTransaction) generated.get(0).getTransaction();
        var deletedLedgerProjection = (LedgerBackedTransaction) deletedPortfolioProjection;
        var deletedEntryUUID = deletedLedgerProjection.getLedgerEntry().getUUID();
        var accountProjection = (AccountTransaction) deletedPortfolioProjection.getCrossEntry()
                        .getCrossTransaction(deletedPortfolioProjection);
        var unrelatedEntryUUID = ((LedgerBackedTransaction) unrelatedGenerated.get(0).getTransaction()).getLedgerEntry()
                        .getUUID();

        account.deleteTransaction(accountProjection, client);

        assertFalse(client.getLedger().getEntries().stream().anyMatch(entry -> entry.getUUID().equals(deletedEntryUUID)));
        assertThat(client.getLedger().getEntries().stream().filter(entry -> entry.getUUID().equals(unrelatedEntryUUID))
                        .count(), is(1L));
        assertThat(account.getTransactions(), hasSize(1));
        assertThat(portfolio.getTransactions(), hasSize(1));
        assertFalse(account.getTransactions().stream()
                        .anyMatch(transaction -> transaction.getUUID().equals(accountProjection.getUUID())));
        assertFalse(portfolio.getTransactions().stream()
                        .anyMatch(transaction -> transaction.getUUID().equals(deletedPortfolioProjection.getUUID())));
        assertThat(investmentPlan.getLedgerExecutionRefs(), hasSize(0));
        assertThat(investmentPlan.getTransactions(client), hasSize(0));
        assertThat(unrelatedPlan.getLedgerExecutionRefs(), hasSize(1));
        assertThat(unrelatedPlan.getTransactions(client), hasSize(1));

        String xml = ClientTestUtilities.toString(client);
        assertFalse(xml.contains(deletedEntryUUID));
        Client xmlLoaded = ClientFactory.load(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        assertThat(xmlLoaded.getPlans().get(0).getLedgerExecutionRefs(), hasSize(0));
        assertThat(xmlLoaded.getPlans().get(0).getTransactions(xmlLoaded), hasSize(0));
        assertThat(xmlLoaded.getPlans().get(1).getLedgerExecutionRefs(), hasSize(1));
        assertFalse(xmlLoaded.getAccounts().get(0).getTransactions().stream()
                        .anyMatch(transaction -> transaction.getUUID().equals(accountProjection.getUUID())));
        assertFalse(xmlLoaded.getPortfolios().get(0).getTransactions().stream()
                        .anyMatch(transaction -> transaction.getUUID().equals(deletedPortfolioProjection.getUUID())));
        assertFalse(xmlLoaded.getLedger().getEntries().stream().anyMatch(entry -> entry.getUUID().equals(deletedEntryUUID)));

        Client loaded = loadProtobuf(client);
        assertThat(loaded.getPlans().get(0).getLedgerExecutionRefs(), hasSize(0));
        assertThat(loaded.getPlans().get(0).getTransactions(loaded), hasSize(0));
        assertThat(loaded.getPlans().get(1).getLedgerExecutionRefs(), hasSize(1));
        assertFalse(loaded.getLedger().getEntries().stream().anyMatch(entry -> entry.getUUID().equals(deletedEntryUUID)));
    }

    /**
     * Checks the generated booking scenario: owner delete ledger backed account only transaction removes ledger truth before xml roundtrip.
     * The plan reference must resolve to the intended transaction after the operation.
     * This prevents stale or ambiguous generated booking references.
     */
    @Test
    public void testOwnerDeleteLedgerBackedAccountOnlyTransactionRemovesLedgerTruthBeforeXmlRoundtrip() throws IOException
    {
        investmentPlan.setName("Deleted Account Plan");
        investmentPlan.setType(InvestmentPlan.Type.INTEREST);
        investmentPlan.setAccount(account);
        investmentPlan.setAmount(Values.Amount.factorize(200));
        investmentPlan.setTaxes(Values.Amount.factorize(10));
        investmentPlan.setStart(LocalDate.now().minusMonths(1));
        investmentPlan.setInterval(12);
        client.addPlan(investmentPlan);

        var generated = investmentPlan.generateTransactions(client, new TestCurrencyConverter());
        var deletedProjection = (AccountTransaction) generated.get(0).getTransaction();
        var deletedEntryUUID = ((LedgerBackedTransaction) deletedProjection).getLedgerEntry().getUUID();

        account.deleteTransaction(deletedProjection, client);

        assertThat(account.getTransactions(), hasSize(0));
        assertThat(client.getLedger().getEntries(), hasSize(0));
        assertThat(investmentPlan.getLedgerExecutionRefs(), hasSize(0));
        assertThat(investmentPlan.getTransactions(client), hasSize(0));

        String xml = ClientTestUtilities.toString(client);
        assertFalse(xml.contains(deletedEntryUUID));

        Client loaded = ClientFactory.load(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

        assertThat(loaded.getLedger().getEntries(), hasSize(0));
        assertThat(loaded.getAccounts().get(0).getTransactions(), hasSize(0));
        assertThat(loaded.getPlans().get(0).getLedgerExecutionRefs(), hasSize(0));
        assertThat(loaded.getPlans().get(0).getTransactions(loaded), hasSize(0));
    }

    /**
     * Checks the generated booking scenario: generated ledger execution refs survive xml and protobuf roundtrip.
     * The plan reference must resolve to the intended transaction after the operation.
     * This prevents stale or ambiguous generated booking references.
     */
    @Test
    public void testGeneratedLedgerExecutionRefsSurviveXmlAndProtobufRoundtrip() throws IOException
    {
        investmentPlan.setName("Roundtrip Plan");
        investmentPlan.setType(InvestmentPlan.Type.PURCHASE_OR_DELIVERY);
        investmentPlan.setAccount(account);
        investmentPlan.setPortfolio(portfolio);
        investmentPlan.setSecurity(security);
        investmentPlan.setStart(LocalDate.now().minusMonths(1));
        investmentPlan.setInterval(12);
        client.addPlan(investmentPlan);

        var generated = investmentPlan.generateTransactions(client, new TestCurrencyConverter());
        var expected = (LedgerBackedTransaction) generated.get(0).getTransaction();

        String xml = ClientTestUtilities.toString(client);
        assertThat(xml, containsString("<ledger-execution-ref>"));
        assertFalse(xml.contains("<account-transaction"));
        assertFalse(xml.contains("<portfolio-transaction"));

        var xmlLoaded = ClientFactory.load(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        assertGeneratedPlanRefRoundtrip(xmlLoaded, expected);

        var protobufLoaded = loadProtobuf(client);
        assertGeneratedPlanRefRoundtrip(protobufLoaded, expected);
    }

    /**
     * Checks the generated booking scenario: ambiguous ledger execution ref without projection identity is rejected.
     * The plan reference must resolve to the intended transaction after the operation.
     * This prevents stale or ambiguous generated booking references.
     */
    @Test
    public void testAmbiguousLedgerExecutionRefWithoutProjectionIdentityIsRejected() throws IOException
    {
        investmentPlan.setName("Ambiguous Plan");
        investmentPlan.setType(InvestmentPlan.Type.PURCHASE_OR_DELIVERY);
        investmentPlan.setAccount(account);
        investmentPlan.setPortfolio(portfolio);
        investmentPlan.setSecurity(security);
        investmentPlan.setStart(LocalDate.now().minusMonths(1));
        investmentPlan.setInterval(12);

        var generated = investmentPlan.generateTransactions(client, new TestCurrencyConverter());
        var ledgerBacked = (LedgerBackedTransaction) generated.get(0).getTransaction();
        investmentPlan.getLedgerExecutionRefs().clear();
        investmentPlan.addLedgerExecutionRef(
                        new InvestmentPlan.LedgerExecutionRef(ledgerBacked.getLedgerEntry().getUUID(), null, null));

        assertThrows(IllegalArgumentException.class, () -> investmentPlan.getTransactions(client));
    }

    private void assertGeneratedPlanRefRoundtrip(Client loaded, LedgerBackedTransaction expected)
    {
        var loadedPlan = loaded.getPlans().get(0);
        assertThat(loadedPlan.getTransactions(), hasSize(0));
        assertThat(loadedPlan.getLedgerExecutionRefs(), hasSize(1));

        var ref = loadedPlan.getLedgerExecutionRefs().get(0);
        assertThat(ref.getLedgerEntryUUID(), is(expected.getLedgerEntry().getUUID()));
        assertThat(ref.getProjectionUUID(), is(expected.getLedgerProjectionRef().getUUID()));
        assertThat(ref.getProjectionRole(), is(LedgerProjectionRole.PORTFOLIO));

        var resolved = loadedPlan.getTransactions(loaded).get(0).getTransaction();
        assertThat(resolved, instanceOf(LedgerBackedTransaction.class));
        assertThat(resolved.getUUID(), is(expected.getLedgerProjectionRef().getUUID()));
        assertThat(((PortfolioTransaction) resolved).getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(loaded.getLedger().getEntries(), hasSize(1));
        assertThat(loaded.getAllTransactions(), hasSize(1));
        assertTrue(LedgerStructuralValidator.validate(loaded.getLedger()).isOK());
    }

    private void assertUnsupportedAccountOnlyUnitsRejectedBeforeMutation(InvestmentPlan.Type type)
    {
        investmentPlan.setName("Unsupported Account Plan");
        investmentPlan.setType(type);
        investmentPlan.setAccount(account);
        investmentPlan.setTaxes(Values.Amount.factorize(10));
        investmentPlan.setStart(LocalDate.now().minusMonths(1));
        investmentPlan.setInterval(12);

        assertThrows(InvestmentPlan.UnsupportedLedgerGenerationException.class,
                        () -> investmentPlan.generateTransactions(client, new TestCurrencyConverter()));
        assertThat(client.getLedger().getEntries(), hasSize(0));
        assertThat(account.getTransactions(), hasSize(0));
        assertThat(investmentPlan.getLedgerExecutionRefs(), hasSize(0));
    }

    private Client loadProtobuf(Client source) throws IOException
    {
        var stream = new ByteArrayOutputStream();
        new ProtobufWriter().save(source, stream);
        return new ProtobufWriter().load(new ByteArrayInputStream(stream.toByteArray()));
    }

    private void assertValidLedger()
    {
        var result = LedgerStructuralValidator.validate(client.getLedger());
        assertTrue(result.getIssues().toString(), result.isOK());
    }

}
