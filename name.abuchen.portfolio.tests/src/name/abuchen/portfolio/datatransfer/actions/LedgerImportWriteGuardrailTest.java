package name.abuchen.portfolio.datatransfer.actions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.Test;

import name.abuchen.portfolio.junit.TestCurrencyConverter;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.InvestmentPlan;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransaction.Type;
import name.abuchen.portfolio.model.SaveFlag;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.LedgerParameter;
import name.abuchen.portfolio.model.ledger.LedgerPosting;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRef;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;
import name.abuchen.portfolio.model.ledger.LedgerStructuralValidator;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerDeliveryTransactionCreator;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPostingType;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedTransaction;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

/**
 * Tests import updates for generated ledger-backed transactions.
 * These tests make sure matching imports update existing ledger truth and rejected imports leave the booking unchanged.
 */
@SuppressWarnings("nls")
public class LedgerImportWriteGuardrailTest
{
    /**
     * Verifies that an imported buy updates a plan-generated ledger-backed buy.
     * The existing ledger entry and plan reference must survive while shares, note, source, fees, and taxes change.
     */
    @Test
    public void testInvestmentPlanImportUpdatesLedgerBackedGeneratedBuyThroughLedgerTruth() throws Exception
    {
        var fixture = buyPlanFixture(true);
        var generatedProjection = fixture.generatedProjection();
        LedgerEntry generatedEntry = ((LedgerBackedTransaction) generatedProjection).getLedgerEntry();
        var entryUUID = generatedEntry.getUUID();
        var executionRefs = List.copyOf(fixture.plan().getLedgerExecutionRefs());
        var accountProjection = generatedProjection.getCrossEntry().getCrossTransaction(generatedProjection);
        var accountProjectionUUID = accountProjection.getUUID();
        var portfolioProjectionUUID = generatedProjection.getUUID();
        var imported = importedBuy(fixture.security(), generatedProjection.getDateTime().plusDays(2),
                        generatedProjection.getAmount() + Values.Amount.factorize(1),
                        generatedProjection.getShares() + Values.Share.factorize(0.5));

        InsertAction action = new InsertAction(fixture.client());
        action.setInvestmentPlanItem(true);

        action.process(imported, fixture.otherAccount(), fixture.otherPortfolio());

        assertThat(fixture.client().getLedger().getEntries().size(), is(1));
        assertThat(generatedEntry.getUUID(), is(entryUUID));
        assertThat(fixture.account().getTransactions().size(), is(1));
        assertThat(fixture.portfolio().getTransactions().size(), is(1));
        assertThat(fixture.otherAccount().getTransactions().isEmpty(), is(true));
        assertThat(fixture.otherPortfolio().getTransactions().isEmpty(), is(true));
        assertThat(accountProjection.getUUID(), is(accountProjectionUUID));
        assertThat(generatedProjection.getUUID(), is(portfolioProjectionUUID));
        assertThat(fixture.plan().getLedgerExecutionRefs(), is(executionRefs));
        assertThat(generatedProjection.getDateTime(), is(imported.getPortfolioTransaction().getDateTime()));
        assertThat(generatedProjection.getAmount(), is(imported.getPortfolioTransaction().getAmount()));
        assertThat(generatedProjection.getCurrencyCode(), is(imported.getPortfolioTransaction().getCurrencyCode()));
        assertThat(generatedProjection.getSecurity(), is(fixture.security()));
        assertThat(generatedProjection.getShares(), is(imported.getPortfolioTransaction().getShares()));
        assertThat(generatedProjection.getNote(), is("imported note"));
        assertThat(generatedProjection.getSource(), is("imported source"));
        assertThat(generatedProjection.getUnitSum(Unit.Type.FEE), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2))));
        assertThat(generatedProjection.getUnitSum(Unit.Type.TAX), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1))));
        assertRoundtrip(fixture.client(), entryUUID, portfolioProjectionUUID, executionRefs);
        assertValid(fixture.client());
    }

    /**
     * Verifies that an imported buy can update a plan-generated inbound delivery.
     * The delivery remains the single ledger truth and no new portfolio booking is inserted.
     */
    @Test
    public void testInvestmentPlanImportUpdatesLedgerBackedGeneratedDeliveryThroughLedgerTruth() throws Exception
    {
        var fixture = buyPlanFixture(false);
        var generatedProjection = fixture.generatedProjection();
        LedgerEntry generatedEntry = ((LedgerBackedTransaction) generatedProjection).getLedgerEntry();
        var entryUUID = generatedEntry.getUUID();
        var executionRefs = List.copyOf(fixture.plan().getLedgerExecutionRefs());
        var portfolioProjectionUUID = generatedProjection.getUUID();
        var imported = importedBuy(fixture.security(), generatedProjection.getDateTime().plusDays(1),
                        generatedProjection.getAmount() + Values.Amount.factorize(1),
                        generatedProjection.getShares() + Values.Share.factorize(0.5));

        InsertAction action = new InsertAction(fixture.client());
        action.setInvestmentPlanItem(true);

        action.process(imported, fixture.account(), fixture.otherPortfolio());

        assertThat(fixture.client().getLedger().getEntries().size(), is(1));
        assertThat(generatedEntry.getUUID(), is(entryUUID));
        assertThat(fixture.account().getTransactions().isEmpty(), is(true));
        assertThat(fixture.portfolio().getTransactions().size(), is(1));
        assertThat(fixture.otherPortfolio().getTransactions().isEmpty(), is(true));
        assertThat(generatedProjection.getUUID(), is(portfolioProjectionUUID));
        assertThat(generatedProjection.getType(), is(Type.DELIVERY_INBOUND));
        assertThat(fixture.plan().getLedgerExecutionRefs(), is(executionRefs));
        assertThat(generatedProjection.getDateTime(), is(imported.getPortfolioTransaction().getDateTime()));
        assertThat(generatedProjection.getAmount(), is(imported.getPortfolioTransaction().getAmount()));
        assertThat(generatedProjection.getCurrencyCode(), is(imported.getPortfolioTransaction().getCurrencyCode()));
        assertThat(generatedProjection.getSecurity(), is(fixture.security()));
        assertThat(generatedProjection.getShares(), is(imported.getPortfolioTransaction().getShares()));
        assertThat(generatedProjection.getNote(), is("imported note"));
        assertThat(generatedProjection.getSource(), is("imported source"));
        assertThat(generatedProjection.getUnitSum(Unit.Type.FEE), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2))));
        assertThat(generatedProjection.getUnitSum(Unit.Type.TAX), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1))));
        assertRoundtrip(fixture.client(), entryUUID, portfolioProjectionUUID, executionRefs);
        assertValid(fixture.client());
    }

    /**
     * Verifies that an imported sell is not applied to a generated buy match.
     * The import path must reject before mutation when the generated booking has the wrong direction.
     */
    @Test
    public void testInvestmentPlanImportRejectsSellMatchAgainstGeneratedBuyBeforeMutation() throws Exception
    {
        var fixture = buyPlanFixture(true);
        var generatedProjection = fixture.generatedProjection();
        LedgerEntry generatedEntry = ((LedgerBackedTransaction) generatedProjection).getLedgerEntry();
        var snapshot = LedgerEntrySnapshot.of(generatedEntry);
        var executionRefs = List.copyOf(fixture.plan().getLedgerExecutionRefs());
        var imported = importedBuy(fixture.security(), generatedProjection.getDateTime(), generatedProjection.getAmount(),
                        generatedProjection.getShares());
        imported.setType(Type.SELL);

        InsertAction action = new InsertAction(fixture.client());
        action.setInvestmentPlanItem(true);

        assertThrows(UnsupportedOperationException.class,
                        () -> action.process(imported, fixture.account(), fixture.portfolio()));

        assertThat(fixture.plan().getLedgerExecutionRefs(), is(executionRefs));
        snapshot.assertUnchanged(generatedEntry);
        assertValid(fixture.client());
    }

    /**
     * Verifies that unsupported generated security transaction types are rejected before import update.
     * The plan reference, ledger entry, and owner lists must remain unchanged.
     */
    @Test
    public void testInvestmentPlanImportRejectsUnsupportedLedgerBackedGeneratedSecurityTypeBeforeMutation()
    {
        var fixture = unsupportedOutboundDeliveryFixture();
        var generatedProjection = fixture.generatedProjection();
        LedgerEntry generatedEntry = ((LedgerBackedTransaction) generatedProjection).getLedgerEntry();
        var snapshot = LedgerEntrySnapshot.of(generatedEntry);
        var executionRefs = List.copyOf(fixture.plan().getLedgerExecutionRefs());
        var imported = importedBuy(fixture.security(), generatedProjection.getDateTime(), generatedProjection.getAmount(),
                        generatedProjection.getShares());

        InsertAction action = new InsertAction(fixture.client());
        action.setInvestmentPlanItem(true);

        assertThrows(UnsupportedOperationException.class,
                        () -> action.process(imported, fixture.account(), fixture.portfolio()));

        assertThat(fixture.client().getLedger().getEntries().size(), is(1));
        assertThat(fixture.plan().getLedgerExecutionRefs(), is(executionRefs));
        assertThat(fixture.portfolio().getTransactions(), is(List.of(generatedProjection)));
        assertThat(fixture.account().getTransactions().isEmpty(), is(true));
        snapshot.assertUnchanged(generatedEntry);
        assertValid(fixture.client());
    }

    private InvestmentPlanFixture buyPlanFixture(boolean withAccount) throws Exception
    {
        var client = new Client();
        var account = new Account("Account");
        var otherAccount = new Account("Other Account");
        var portfolio = new Portfolio("Portfolio");
        var otherPortfolio = new Portfolio("Other Portfolio");
        var security = new Security("Security", CurrencyUnit.EUR);
        var updatedAt = Instant.now();

        account.setUpdatedAt(updatedAt);
        otherAccount.setUpdatedAt(updatedAt);
        portfolio.setUpdatedAt(updatedAt);
        otherPortfolio.setUpdatedAt(updatedAt);
        security.setUpdatedAt(updatedAt);
        client.addAccount(account);
        client.addAccount(otherAccount);
        client.addPortfolio(portfolio);
        client.addPortfolio(otherPortfolio);
        client.addSecurity(security);
        portfolio.setReferenceAccount(account);
        otherPortfolio.setReferenceAccount(otherAccount);

        security.addPrice(new SecurityPrice(LocalDate.now(), Values.Quote.factorize(10)));
        InvestmentPlan plan = new InvestmentPlan();
        plan.setName("ledger plan");
        plan.setType(InvestmentPlan.Type.PURCHASE_OR_DELIVERY);
        if (withAccount)
            plan.setAccount(account);
        plan.setPortfolio(portfolio);
        plan.setSecurity(security);
        plan.setStart(LocalDate.now().minusMonths(1));
        plan.setInterval(12);
        plan.setAmount(Values.Amount.factorize(100));
        client.addPlan(plan);

        var generated = plan.generateTransactions(client, new TestCurrencyConverter());
        return new InvestmentPlanFixture(client, account, otherAccount, portfolio, otherPortfolio, security, plan,
                        (PortfolioTransaction) generated.get(0).getTransaction());
    }

    private InvestmentPlanFixture unsupportedOutboundDeliveryFixture()
    {
        var client = new Client();
        var account = new Account("Account");
        var otherAccount = new Account("Other Account");
        var portfolio = new Portfolio("Portfolio");
        var otherPortfolio = new Portfolio("Other Portfolio");
        var security = new Security("Security", CurrencyUnit.EUR);
        var updatedAt = Instant.now();

        account.setUpdatedAt(updatedAt);
        otherAccount.setUpdatedAt(updatedAt);
        portfolio.setUpdatedAt(updatedAt);
        otherPortfolio.setUpdatedAt(updatedAt);
        security.setUpdatedAt(updatedAt);
        portfolio.setReferenceAccount(account);
        otherPortfolio.setReferenceAccount(otherAccount);
        client.addAccount(account);
        client.addAccount(otherAccount);
        client.addPortfolio(portfolio);
        client.addPortfolio(otherPortfolio);
        client.addSecurity(security);

        InvestmentPlan plan = new InvestmentPlan();
        plan.setName("unsupported ledger plan");
        plan.setType(InvestmentPlan.Type.PURCHASE_OR_DELIVERY);
        plan.setPortfolio(portfolio);
        plan.setSecurity(security);
        plan.setStart(LocalDate.now().minusMonths(1));
        plan.setInterval(12);
        client.addPlan(plan);

        var generatedProjection = new LedgerDeliveryTransactionCreator(client).create(portfolio,
                        Type.DELIVERY_OUTBOUND, LocalDateTime.now().minusMonths(1), Values.Amount.factorize(100),
                        CurrencyUnit.EUR, security, Values.Share.factorize(10), null, null, List.of(), null, null);
        plan.addLedgerExecutionRef(InvestmentPlan.LedgerExecutionRef.of((LedgerBackedTransaction) generatedProjection));

        return new InvestmentPlanFixture(client, account, otherAccount, portfolio, otherPortfolio, security, plan,
                        generatedProjection);
    }

    private BuySellEntry importedBuy(Security security, LocalDateTime dateTime, long amount, long shares)
    {
        BuySellEntry imported = new BuySellEntry();
        imported.setType(Type.BUY);
        imported.setDate(dateTime);
        imported.setSecurity(security);
        imported.setShares(shares);
        imported.setCurrencyCode(CurrencyUnit.EUR);
        imported.setAmount(amount);
        imported.setNote("imported note");
        imported.setSource("imported source");
        imported.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE, Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2))));
        imported.getPortfolioTransaction().addUnit(new Unit(Unit.Type.TAX, Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1))));
        return imported;
    }

    private void assertRoundtrip(Client client, String entryUUID, String projectionUUID,
                    List<InvestmentPlan.LedgerExecutionRef> executionRefs) throws Exception
    {
        assertLoadedRoundtrip(loadXml(saveXml(client)), entryUUID, projectionUUID, executionRefs);
        assertLoadedRoundtrip(loadProtobuf(saveProtobuf(client)), entryUUID, projectionUUID, executionRefs);
    }

    private void assertLoadedRoundtrip(Client client, String entryUUID, String projectionUUID,
                    List<InvestmentPlan.LedgerExecutionRef> executionRefs)
    {
        assertThat(client.getLedger().getEntries().stream().filter(entry -> entryUUID.equals(entry.getUUID())).count(),
                        is(1L));
        assertTrue(client.getAllTransactions().stream()
                        .anyMatch(pair -> projectionUUID.equals(pair.getTransaction().getUUID())));
        assertThat(client.getPlans().get(0).getLedgerExecutionRefs().size(), is(executionRefs.size()));
        assertThat(client.getPlans().get(0).getLedgerExecutionRefs().get(0).getLedgerEntryUUID(),
                        is(executionRefs.get(0).getLedgerEntryUUID()));
        assertThat(client.getPlans().get(0).getLedgerExecutionRefs().get(0).getProjectionUUID(),
                        is(executionRefs.get(0).getProjectionUUID()));
        assertThat(client.getPlans().get(0).getLedgerExecutionRefs().get(0).getProjectionRole(),
                        is(executionRefs.get(0).getProjectionRole()));
        assertValid(client);
    }

    private byte[] saveXml(Client client) throws Exception
    {
        File file = Files.createTempFile("ledger-investment-plan-import-update", ".xml").toFile();
        try
        {
            ClientFactory.save(client, file);
            return Files.readAllBytes(file.toPath());
        }
        finally
        {
            Files.deleteIfExists(file.toPath());
        }
    }

    private Client loadXml(byte[] bytes) throws Exception
    {
        return ClientFactory.load(new ByteArrayInputStream(bytes));
    }

    private byte[] saveProtobuf(Client client) throws Exception
    {
        File file = Files.createTempFile("ledger-investment-plan-import-update", ".portfolio").toFile();
        try
        {
            ClientFactory.saveAs(client, file, null, EnumSet.of(SaveFlag.BINARY, SaveFlag.COMPRESSED));
            return Files.readAllBytes(file.toPath());
        }
        finally
        {
            Files.deleteIfExists(file.toPath());
        }
    }

    private Client loadProtobuf(byte[] bytes) throws Exception
    {
        File file = Files.createTempFile("ledger-investment-plan-import-update", ".portfolio").toFile();
        try
        {
            Files.write(file.toPath(), bytes);
            return ClientFactory.load(file, null, new NullProgressMonitor());
        }
        finally
        {
            Files.deleteIfExists(file.toPath());
        }
    }

    private void assertValid(Client client)
    {
        if (!LedgerStructuralValidator.validate(client.getLedger()).isOK())
            throw new AssertionError(LedgerStructuralValidator.validate(client.getLedger()).getIssues().toString());
    }

    private record LedgerEntrySnapshot(LocalDateTime dateTime, String note, String source,
                    List<PostingSnapshot> postings, List<ProjectionSnapshot> projections)
    {
        static LedgerEntrySnapshot of(LedgerEntry entry)
        {
            return new LedgerEntrySnapshot(entry.getDateTime(), entry.getNote(), entry.getSource(),
                            entry.getPostings().stream().map(PostingSnapshot::of).toList(),
                            entry.getProjectionRefs().stream().map(ProjectionSnapshot::of).toList());
        }

        void assertUnchanged(LedgerEntry entry)
        {
            assertThat(entry.getDateTime(), is(dateTime));
            assertThat(entry.getNote(), is(note));
            assertThat(entry.getSource(), is(source));
            assertThat(entry.getPostings().stream().map(PostingSnapshot::of).toList(), is(postings));
            assertThat(entry.getProjectionRefs().stream().map(ProjectionSnapshot::of).toList(), is(projections));
        }
    }

    private record PostingSnapshot(String uuid, LedgerPostingType type, long amount, String currency, Long forexAmount,
                    String forexCurrency, BigDecimal exchangeRate, Security security, long shares, Account account,
                    Portfolio portfolio, List<LedgerParameter<?>> parameters)
    {
        static PostingSnapshot of(LedgerPosting posting)
        {
            return new PostingSnapshot(posting.getUUID(), posting.getType(), posting.getAmount(), posting.getCurrency(),
                            posting.getForexAmount(), posting.getForexCurrency(), posting.getExchangeRate(),
                            posting.getSecurity(), posting.getShares(), posting.getAccount(), posting.getPortfolio(),
                            List.copyOf(posting.getParameters()));
        }
    }

    private record ProjectionSnapshot(String uuid, LedgerProjectionRole role, Account account, Portfolio portfolio,
                    String primaryPostingUUID, String postingGroupUUID)
    {
        static ProjectionSnapshot of(LedgerProjectionRef projection)
        {
            return new ProjectionSnapshot(projection.getUUID(), projection.getRole(), projection.getAccount(),
                            projection.getPortfolio(), projection.getPrimaryPostingUUID(),
                            projection.getPostingGroupUUID());
        }
    }

    private record InvestmentPlanFixture(Client client, Account account, Account otherAccount, Portfolio portfolio,
                    Portfolio otherPortfolio, Security security, InvestmentPlan plan,
                    PortfolioTransaction generatedProjection)
    {
    }
}
