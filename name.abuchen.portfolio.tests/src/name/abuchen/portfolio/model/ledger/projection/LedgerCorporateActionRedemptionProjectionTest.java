package name.abuchen.portfolio.model.ledger.projection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.model.ledger.LedgerMutationContext;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionBasisStatus;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionLeg;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionKind;
import name.abuchen.portfolio.model.ledger.configuration.LedgerLegRole;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPostingType;
import name.abuchen.portfolio.model.ledger.nativeentry.LedgerCorporateActionEditSupport;
import name.abuchen.portfolio.model.ledger.nativeentry.LedgerNativeEntryAssembler;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class LedgerCorporateActionRedemptionProjectionTest
{
    private static final LocalDateTime DATE = LocalDateTime.of(2026, 1, 2, 0, 0);

    @Test
    public void testMaturityMaterializesSecurityOutAndCashProjection()
    {
        var fixture = fixture();
        var entry = LedgerNativeEntryAssembler.corporateAction(fixture.client) //
                        .kind(CorporateActionKind.MATURITY) //
                        .date(DATE) //
                        .note("bond maturity") //
                        .securityContext("context-1", "redemption-1", fixture.portfolio, fixture.bond) //
                        .securityOut("source-1", "redemption-1", fixture.portfolio, fixture.bond, shares(10)) //
                        .cash("cash-1", "redemption-1", fixture.account, money(100)) //
                        .principal("principal-1", "redemption-1", fixture.account, money(100)) //
                        .basis(CorporateActionBasisStatus.UNKNOWN) //
                        .buildAndAdd().getEntry();

        LedgerProjectionService.materialize(fixture.client);

        var portfolioProjection = ledgerBackedPortfolioProjection(fixture.portfolio,
                        LedgerProjectionRole.DELIVERY_OUTBOUND, "source-1");
        var accountProjection = ledgerBackedAccountProjection(fixture.account, LedgerProjectionRole.ACCOUNT, "cash-1");

        assertThat(portfolioProjection.getType(), is(PortfolioTransaction.Type.DELIVERY_OUTBOUND));
        assertThat(portfolioProjection.getDateTime(), is(DATE));
        assertThat(portfolioProjection.getNote(), is("bond maturity"));
        assertSame(fixture.bond, portfolioProjection.getSecurity());
        assertThat(portfolioProjection.getShares(), is(shares(10)));

        assertThat(accountProjection.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(accountProjection.getAmount(), is(money(100).getAmount()));
        assertSame(fixture.bond, accountProjection.getSecurity());

        assertThat(LedgerCorporateActionEditSupport.postingBySemanticKey(entry,
                        LedgerLegRole.PRINCIPAL_REDEMPTION_LEG, "principal-1", "redemption-1").getAmount(),
                        is(money(100).getAmount()));
        assertThat(LedgerProjectionSupport.descriptors(entry).size(), is(2));
        assertFalse(hasPrimaryDescriptor(entry, CorporateActionLeg.SECURITY_CONTEXT));
        assertFalse(hasPrimaryDescriptor(entry, CorporateActionLeg.PRINCIPAL));
    }

    @Test
    public void testPartialRedemptionMaterializesSecurityOutAndCashProjection()
    {
        var fixture = fixture();

        LedgerNativeEntryAssembler.corporateAction(fixture.client) //
                        .kind(CorporateActionKind.PARTIAL_REDEMPTION) //
                        .date(DATE) //
                        .securityContext("context-1", "redemption-1", fixture.portfolio, fixture.bond) //
                        .securityOut("source-1", "redemption-1", fixture.portfolio, fixture.bond, shares(4)) //
                        .cash("cash-1", "redemption-1", fixture.account, money(40)) //
                        .principal("principal-1", "redemption-1", fixture.account, money(40)) //
                        .basis(CorporateActionBasisStatus.UNKNOWN) //
                        .buildAndAdd();

        LedgerProjectionService.materialize(fixture.client);

        var portfolioProjection = ledgerBackedPortfolioProjection(fixture.portfolio,
                        LedgerProjectionRole.DELIVERY_OUTBOUND, "source-1");
        var accountProjection = ledgerBackedAccountProjection(fixture.account, LedgerProjectionRole.ACCOUNT, "cash-1");

        assertThat(portfolioProjection.getType(), is(PortfolioTransaction.Type.DELIVERY_OUTBOUND));
        assertThat(portfolioProjection.getShares(), is(shares(4)));
        assertThat(accountProjection.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(accountProjection.getAmount(), is(money(40).getAmount()));
    }

    @Test
    public void testCallMaterializesSecurityOutAndCashProjection()
    {
        var fixture = fixture();

        LedgerNativeEntryAssembler.corporateAction(fixture.client) //
                        .kind(CorporateActionKind.CALL) //
                        .date(DATE) //
                        .securityContext("context-1", "call-1", fixture.portfolio, fixture.bond) //
                        .securityOut("source-1", "call-1", fixture.portfolio, fixture.bond, shares(8)) //
                        .cash("cash-1", "call-1", fixture.account, money(80)) //
                        .principal("principal-1", "call-1", fixture.account, money(80)) //
                        .buildAndAdd();

        LedgerProjectionService.materialize(fixture.client);

        assertThat(ledgerBackedPortfolioProjection(fixture.portfolio, LedgerProjectionRole.DELIVERY_OUTBOUND,
                        "source-1").getShares(), is(shares(8)));
        assertThat(ledgerBackedAccountProjection(fixture.account, LedgerProjectionRole.ACCOUNT, "cash-1").getAmount(),
                        is(money(80).getAmount()));
    }

    @Test
    public void testPutMaterializesSecurityOutAndCashProjection()
    {
        var fixture = fixture();

        LedgerNativeEntryAssembler.corporateAction(fixture.client) //
                        .kind(CorporateActionKind.PUT) //
                        .date(DATE) //
                        .securityContext("context-1", "put-1", fixture.portfolio, fixture.bond) //
                        .securityOut("source-1", "put-1", fixture.portfolio, fixture.bond, shares(3)) //
                        .cash("cash-1", "put-1", fixture.account, money(30)) //
                        .principal("principal-1", "put-1", fixture.account, money(30)) //
                        .buildAndAdd();

        LedgerProjectionService.materialize(fixture.client);

        assertThat(ledgerBackedPortfolioProjection(fixture.portfolio, LedgerProjectionRole.DELIVERY_OUTBOUND,
                        "source-1").getType(), is(PortfolioTransaction.Type.DELIVERY_OUTBOUND));
        assertThat(ledgerBackedAccountProjection(fixture.account, LedgerProjectionRole.ACCOUNT, "cash-1").getType(),
                        is(AccountTransaction.Type.DEPOSIT));
    }

    @Test
    public void testMaturityMaterializesRepeatedSecurityOutAndCashProjections()
    {
        var fixture = fixture();
        var entry = LedgerNativeEntryAssembler.corporateAction(fixture.client) //
                        .kind(CorporateActionKind.MATURITY) //
                        .date(DATE) //
                        .securityContext("context-1", "main", fixture.portfolio, fixture.bond) //
                        .securityOut("source-1", "redemption-1", fixture.portfolio, fixture.bond, shares(5)) //
                        .securityOut("source-2", "redemption-2", fixture.secondPortfolio, fixture.secondBond,
                                        shares(7)) //
                        .cash("cash-1", "redemption-1", fixture.account, money(50)) //
                        .cash("cash-2", "redemption-2", fixture.secondAccount, money(70)) //
                        .principal("principal-1", "redemption-1", fixture.account, money(50)) //
                        .principal("principal-2", "redemption-2", fixture.secondAccount, money(70)) //
                        .buildAndAdd().getEntry();

        var descriptors = LedgerProjectionSupport.descriptors(entry);
        var portfolioDescriptors = descriptors.stream()
                        .filter(descriptor -> descriptor.getRole() == LedgerProjectionRole.DELIVERY_OUTBOUND)
                        .toList();
        var accountDescriptors = descriptors.stream()
                        .filter(descriptor -> descriptor.getRole() == LedgerProjectionRole.ACCOUNT).toList();

        assertThat(portfolioDescriptors.size(), is(2));
        assertThat(accountDescriptors.size(), is(2));
        assertThat(semanticKeys(portfolioDescriptors), is(Set.of("source-1", "source-2")));
        assertThat(semanticKeys(accountDescriptors), is(Set.of("cash-1", "cash-2")));
        assertThrows(IllegalArgumentException.class,
                        () -> new LedgerProjectionFactory().createProjection(entry, LedgerProjectionRole.DELIVERY_OUTBOUND));
        assertThrows(IllegalArgumentException.class,
                        () -> new LedgerProjectionFactory().createProjection(entry, LedgerProjectionRole.ACCOUNT));
        assertThat(new LedgerProjectionFactory().createProjection(entry, LedgerProjectionRole.DELIVERY_OUTBOUND,
                        "source-2").getUUID(), is(entry.getUUID() + ":DELIVERY_OUTBOUND:source-2"));
        assertThat(new LedgerProjectionFactory().createProjection(entry, LedgerProjectionRole.ACCOUNT,
                        "cash-2").getUUID(), is(entry.getUUID() + ":ACCOUNT:cash-2"));

        LedgerProjectionService.materialize(fixture.client);
        LedgerProjectionService.materialize(fixture.client);

        var firstPortfolio = ledgerBackedPortfolioProjection(fixture.portfolio,
                        LedgerProjectionRole.DELIVERY_OUTBOUND, "source-1");
        var secondPortfolio = ledgerBackedPortfolioProjection(fixture.secondPortfolio,
                        LedgerProjectionRole.DELIVERY_OUTBOUND, "source-2");
        var firstAccount = ledgerBackedAccountProjection(fixture.account, LedgerProjectionRole.ACCOUNT, "cash-1");
        var secondAccount = ledgerBackedAccountProjection(fixture.secondAccount, LedgerProjectionRole.ACCOUNT,
                        "cash-2");

        assertThat(firstPortfolio.getShares(), is(shares(5)));
        assertThat(secondPortfolio.getShares(), is(shares(7)));
        assertThat(firstAccount.getAmount(), is(money(50).getAmount()));
        assertThat(secondAccount.getAmount(), is(money(70).getAmount()));
        assertThat(fixture.portfolio.getTransactions().size(), is(1));
        assertThat(fixture.secondPortfolio.getTransactions().size(), is(1));
        assertThat(fixture.account.getTransactions().size(), is(1));
        assertThat(fixture.secondAccount.getTransactions().size(), is(1));
        assertThat(runtimeProjectionIds(List.of(firstPortfolio, secondPortfolio)),
                        is(portfolioDescriptors.stream().map(DerivedProjectionDescriptor::getRuntimeProjectionId)
                                        .collect(Collectors.toSet())));
        assertThat(runtimeProjectionIds(List.of(firstAccount, secondAccount)),
                        is(accountDescriptors.stream().map(DerivedProjectionDescriptor::getRuntimeProjectionId)
                                        .collect(Collectors.toSet())));
    }

    @Test
    public void testMaturityKeepsOptionalAccruedInterestAsNonProjectingDetail()
    {
        var fixture = fixture();
        var entry = LedgerNativeEntryAssembler.corporateAction(fixture.client) //
                        .kind(CorporateActionKind.MATURITY) //
                        .date(DATE) //
                        .securityContext("context-1", "redemption-1", fixture.portfolio, fixture.bond) //
                        .securityOut("source-1", "redemption-1", fixture.portfolio, fixture.bond, shares(10)) //
                        .cash("cash-1", "redemption-1", fixture.account, money(100)) //
                        .principal("principal-1", "redemption-1", fixture.account, money(100)) //
                        .accruedInterest("interest-1", "redemption-1", fixture.account, money(5)) //
                        .fee("fee-1", fixture.account, money(2), "redemption-1") //
                        .tax("tax-1", fixture.account, money(1), "redemption-1") //
                        .buildAndAdd().getEntry();

        LedgerProjectionService.materialize(fixture.client);

        var accountProjection = ledgerBackedAccountProjection(fixture.account, LedgerProjectionRole.ACCOUNT, "cash-1");

        assertThat(accountProjection.getUnit(Unit.Type.FEE).orElseThrow().getAmount().getAmount(),
                        is(money(2).getAmount()));
        assertThat(accountProjection.getUnit(Unit.Type.TAX).orElseThrow().getAmount().getAmount(),
                        is(money(1).getAmount()));
        assertThat(LedgerProjectionSupport.descriptors(entry).size(), is(2));
        assertFalse(hasPrimaryDescriptor(entry, CorporateActionLeg.ACCRUED_INTEREST));
        assertThat(LedgerCorporateActionEditSupport.postingBySemanticKey(entry, LedgerLegRole.ACCRUED_INTEREST_LEG,
                        "interest-1", "redemption-1").getAmount(), is(money(5).getAmount()));
    }

    @Test
    public void testMaturityDoesNotAttachMismatchedFeeTaxOrAccruedInterest()
    {
        var fixture = fixture();
        var entry = LedgerNativeEntryAssembler.corporateAction(fixture.client) //
                        .kind(CorporateActionKind.MATURITY) //
                        .date(DATE) //
                        .securityContext("context-1", "redemption-1", fixture.portfolio, fixture.bond) //
                        .securityOut("source-1", "redemption-1", fixture.portfolio, fixture.bond, shares(10)) //
                        .cash("cash-1", "redemption-1", fixture.account, money(100)) //
                        .principal("principal-1", "redemption-1", fixture.account, money(100)) //
                        .accruedInterest("interest-1", "other", fixture.account, money(5)) //
                        .fee("fee-1", fixture.account, money(2), "other") //
                        .tax("tax-1", fixture.account, money(1), "other") //
                        .buildAndAdd().getEntry();

        LedgerProjectionService.materialize(fixture.client);

        var accountProjection = ledgerBackedAccountProjection(fixture.account, LedgerProjectionRole.ACCOUNT, "cash-1");

        assertTrue(accountProjection.getUnit(Unit.Type.FEE).isEmpty());
        assertTrue(accountProjection.getUnit(Unit.Type.TAX).isEmpty());
        assertThat(LedgerProjectionSupport.descriptor(entry, LedgerProjectionRole.ACCOUNT, "cash-1")
                        .getUnitPostings().size(), is(0));
        assertFalse(hasPrimaryDescriptor(entry, CorporateActionLeg.ACCRUED_INTEREST));
    }

    @Test
    public void testDeletingMaturityRemovesRedemptionProjections()
    {
        var fixture = fixture();
        var entry = LedgerNativeEntryAssembler.corporateAction(fixture.client) //
                        .kind(CorporateActionKind.MATURITY) //
                        .date(DATE) //
                        .securityContext("context-1", "redemption-1", fixture.portfolio, fixture.bond) //
                        .securityOut("source-1", "redemption-1", fixture.portfolio, fixture.bond, shares(10)) //
                        .cash("cash-1", "redemption-1", fixture.account, money(100)) //
                        .principal("principal-1", "redemption-1", fixture.account, money(100)) //
                        .buildAndAdd().getEntry();

        LedgerProjectionService.materialize(fixture.client);
        LedgerProjectionService.materialize(fixture.client);

        assertThat(fixture.portfolio.getTransactions().size(), is(1));
        assertThat(fixture.account.getTransactions().size(), is(1));

        new LedgerMutationContext(fixture.client).removeEntry(entry);

        assertTrue(fixture.portfolio.getTransactions().isEmpty());
        assertTrue(fixture.account.getTransactions().isEmpty());
    }

    private boolean hasPrimaryDescriptor(name.abuchen.portfolio.model.ledger.LedgerEntry entry, CorporateActionLeg leg)
    {
        return LedgerProjectionSupport.descriptors(entry).stream().map(DerivedProjectionDescriptor::getPrimaryPosting)
                        .anyMatch(posting -> posting.getCorporateActionLeg() == leg);
    }

    private LedgerBackedPortfolioTransaction ledgerBackedPortfolioProjection(Portfolio portfolio,
                    LedgerProjectionRole role, String semanticInstanceKey)
    {
        return portfolio.getTransactions().stream() //
                        .filter(LedgerBackedPortfolioTransaction.class::isInstance) //
                        .map(LedgerBackedPortfolioTransaction.class::cast) //
                        .filter(transaction -> transaction.getLedgerProjectionRole() == role) //
                        .filter(transaction -> transaction.getLedgerProjectionDescriptor().getSemanticInstanceKey()
                                        .filter(semanticInstanceKey::equals).isPresent()) //
                        .findFirst().orElseThrow();
    }

    private LedgerBackedAccountTransaction ledgerBackedAccountProjection(Account account, LedgerProjectionRole role,
                    String semanticInstanceKey)
    {
        return account.getTransactions().stream() //
                        .filter(LedgerBackedAccountTransaction.class::isInstance) //
                        .map(LedgerBackedAccountTransaction.class::cast) //
                        .filter(transaction -> transaction.getLedgerProjectionRole() == role) //
                        .filter(transaction -> transaction.getLedgerProjectionDescriptor().getSemanticInstanceKey()
                                        .filter(semanticInstanceKey::equals).isPresent()) //
                        .findFirst().orElseThrow();
    }

    private Set<String> semanticKeys(List<DerivedProjectionDescriptor> descriptors)
    {
        return descriptors.stream().map(descriptor -> descriptor.getSemanticInstanceKey().orElseThrow())
                        .collect(Collectors.toSet());
    }

    private Set<String> runtimeProjectionIds(List<? extends LedgerBackedTransaction> transactions)
    {
        return transactions.stream().map(LedgerBackedTransaction::getRuntimeProjectionId).collect(Collectors.toSet());
    }

    private static long shares(long shares)
    {
        return Values.Share.factorize(shares);
    }

    private static Money money(long amount)
    {
        return Money.of(CurrencyUnit.EUR, Values.Amount.factorize(amount));
    }

    private static Fixture fixture()
    {
        var client = new Client();
        var account = new Account();
        var secondAccount = new Account();
        var portfolio = new Portfolio();
        var secondPortfolio = new Portfolio();
        var bond = new Security("Bond AG", CurrencyUnit.EUR);
        var secondBond = new Security("Second Bond AG", CurrencyUnit.EUR);

        account.setName("Cash");
        account.setCurrencyCode(CurrencyUnit.EUR);
        secondAccount.setName("Second Cash");
        secondAccount.setCurrencyCode(CurrencyUnit.EUR);
        portfolio.setName("Portfolio");
        secondPortfolio.setName("Second Portfolio");

        client.addAccount(account);
        client.addAccount(secondAccount);
        client.addPortfolio(portfolio);
        client.addPortfolio(secondPortfolio);
        client.addSecurity(bond);
        client.addSecurity(secondBond);

        return new Fixture(client, account, secondAccount, portfolio, secondPortfolio, bond, secondBond);
    }

    private record Fixture(Client client, Account account, Account secondAccount, Portfolio portfolio,
                    Portfolio secondPortfolio, Security bond, Security secondBond)
    {
    }
}
