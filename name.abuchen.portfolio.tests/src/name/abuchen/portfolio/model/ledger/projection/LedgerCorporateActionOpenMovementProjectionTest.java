package name.abuchen.portfolio.model.ledger.projection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
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
import name.abuchen.portfolio.model.ledger.LedgerMutationContext;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionLeg;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionKind;
import name.abuchen.portfolio.model.ledger.configuration.LedgerLegRole;
import name.abuchen.portfolio.model.ledger.nativeentry.LedgerCorporateActionEditSupport;
import name.abuchen.portfolio.model.ledger.nativeentry.LedgerNativeEntryAssembler;
import name.abuchen.portfolio.model.ledger.nativeentry.LedgerNativeEntryAssemblyException;
import name.abuchen.portfolio.model.ledger.nativeentry.LedgerNativeEntryAssemblyIssue;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class LedgerCorporateActionOpenMovementProjectionTest
{
    private static final LocalDateTime DATE = LocalDateTime.of(2026, 1, 2, 0, 0);

    @Test
    public void testDefaultedInterestMaterializesStandaloneFeeMovement()
    {
        var fixture = fixture();

        LedgerNativeEntryAssembler.corporateAction(fixture.client) //
                        .kind(CorporateActionKind.DEFAULTED_INTEREST) //
                        .date(DATE) //
                        .securityContext("context-1", "main", fixture.portfolio, fixture.sourceSecurity) //
                        .fee("fee-1", fixture.account, money(2), null) //
                        .buildAndAdd();

        LedgerProjectionService.materialize(fixture.client);

        var projection = ledgerBackedAccountProjection(fixture.account, LedgerProjectionRole.ACCOUNT, "fee-1");

        assertThat(projection.getType(), is(AccountTransaction.Type.FEES));
        assertThat(projection.getAmount(), is(money(2).getAmount()));
        assertTrue(projection.getUnits().findAny().isEmpty());
    }

    @Test
    public void testRestructuringMaterializesMixedMovements()
    {
        var fixture = fixture();
        var entry = LedgerNativeEntryAssembler.corporateAction(fixture.client) //
                        .kind(CorporateActionKind.RESTRUCTURING) //
                        .date(DATE) //
                        .securityContext("context-1", "main", fixture.portfolio, fixture.sourceSecurity) //
                        .securityOut("source-1", "main", fixture.portfolio, fixture.sourceSecurity, shares(10)) //
                        .securityIn("target-1", "main", fixture.portfolio, fixture.targetSecurity, shares(5)) //
                        .cash("cash-1", "main", fixture.account, money(9)) //
                        .principal("principal-1", "main", fixture.account, money(9)) //
                        .accruedInterest("interest-1", "main", fixture.account, money(1)) //
                        .buildAndAdd().getEntry();

        LedgerProjectionService.materialize(fixture.client);

        assertThat(ledgerBackedPortfolioProjection(fixture.portfolio, LedgerProjectionRole.DELIVERY_OUTBOUND,
                        "source-1").getType(), is(PortfolioTransaction.Type.DELIVERY_OUTBOUND));
        assertThat(ledgerBackedPortfolioProjection(fixture.portfolio, LedgerProjectionRole.NEW_SECURITY_LEG,
                        "target-1").getType(), is(PortfolioTransaction.Type.DELIVERY_INBOUND));
        assertThat(ledgerBackedAccountProjection(fixture.account, LedgerProjectionRole.ACCOUNT, "cash-1").getType(),
                        is(AccountTransaction.Type.DEPOSIT));
        assertThat(LedgerProjectionSupport.descriptors(entry).size(), is(3));
        assertFalse(hasPrimaryDescriptor(entry, CorporateActionLeg.PRINCIPAL));
        assertFalse(hasPrimaryDescriptor(entry, CorporateActionLeg.ACCRUED_INTEREST));
        assertThat(LedgerCorporateActionEditSupport.postingBySemanticKey(entry,
                        LedgerLegRole.PRINCIPAL_REDEMPTION_LEG, "principal-1", "main").getAmount(),
                        is(money(9).getAmount()));
    }

    @Test
    public void testDefaultMaterializesRepeatedCashAndSecurityMovements()
    {
        var fixture = fixture();
        var entry = LedgerNativeEntryAssembler.corporateAction(fixture.client) //
                        .kind(CorporateActionKind.DEFAULT) //
                        .date(DATE) //
                        .securityContext("context-1", "main", fixture.portfolio, fixture.sourceSecurity) //
                        .securityIn("claim-1", "claim-1", fixture.portfolio, fixture.claimSecurity, shares(3)) //
                        .securityIn("claim-2", "claim-2", fixture.secondPortfolio, fixture.secondClaimSecurity,
                                        shares(4)) //
                        .cash("cash-1", "cash-1", fixture.account, money(5)) //
                        .cash("cash-2", "cash-2", fixture.secondAccount, money(7)) //
                        .buildAndAdd().getEntry();

        var descriptors = LedgerProjectionSupport.descriptors(entry);
        var securityDescriptors = descriptors.stream()
                        .filter(descriptor -> descriptor.getRole() == LedgerProjectionRole.NEW_SECURITY_LEG).toList();
        var accountDescriptors = descriptors.stream()
                        .filter(descriptor -> descriptor.getRole() == LedgerProjectionRole.ACCOUNT).toList();

        assertThat(securityDescriptors.size(), is(2));
        assertThat(accountDescriptors.size(), is(2));
        assertThat(semanticKeys(securityDescriptors), is(Set.of("claim-1", "claim-2")));
        assertThat(semanticKeys(accountDescriptors), is(Set.of("cash-1", "cash-2")));
        assertThrows(IllegalArgumentException.class,
                        () -> new LedgerProjectionFactory().createProjection(entry, LedgerProjectionRole.NEW_SECURITY_LEG));
        assertThrows(IllegalArgumentException.class,
                        () -> new LedgerProjectionFactory().createProjection(entry, LedgerProjectionRole.ACCOUNT));

        LedgerProjectionService.materialize(fixture.client);
        LedgerProjectionService.materialize(fixture.client);

        var firstSecurity = ledgerBackedPortfolioProjection(fixture.portfolio, LedgerProjectionRole.NEW_SECURITY_LEG,
                        "claim-1");
        var secondSecurity = ledgerBackedPortfolioProjection(fixture.secondPortfolio,
                        LedgerProjectionRole.NEW_SECURITY_LEG, "claim-2");
        var firstAccount = ledgerBackedAccountProjection(fixture.account, LedgerProjectionRole.ACCOUNT, "cash-1");
        var secondAccount = ledgerBackedAccountProjection(fixture.secondAccount, LedgerProjectionRole.ACCOUNT,
                        "cash-2");

        assertThat(firstSecurity.getShares(), is(shares(3)));
        assertThat(secondSecurity.getShares(), is(shares(4)));
        assertThat(firstAccount.getAmount(), is(money(5).getAmount()));
        assertThat(secondAccount.getAmount(), is(money(7).getAmount()));
        assertThat(runtimeProjectionIds(List.of(firstSecurity, secondSecurity)),
                        is(securityDescriptors.stream().map(DerivedProjectionDescriptor::getRuntimeProjectionId)
                                        .collect(Collectors.toSet())));
        assertThat(runtimeProjectionIds(List.of(firstAccount, secondAccount)),
                        is(accountDescriptors.stream().map(DerivedProjectionDescriptor::getRuntimeProjectionId)
                                        .collect(Collectors.toSet())));
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
        var sourceSecurity = new Security("Source AG", CurrencyUnit.EUR);
        var targetSecurity = new Security("Target AG", CurrencyUnit.EUR);
        var claimSecurity = new Security("Claim AG", CurrencyUnit.EUR);
        var secondClaimSecurity = new Security("Second Claim AG", CurrencyUnit.EUR);

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
        client.addSecurity(sourceSecurity);
        client.addSecurity(targetSecurity);
        client.addSecurity(claimSecurity);
        client.addSecurity(secondClaimSecurity);

        return new Fixture(client, account, secondAccount, portfolio, secondPortfolio, sourceSecurity, targetSecurity,
                        claimSecurity, secondClaimSecurity);
    }

    private record Fixture(Client client, Account account, Account secondAccount, Portfolio portfolio,
                    Portfolio secondPortfolio, Security sourceSecurity, Security targetSecurity, Security claimSecurity,
                    Security secondClaimSecurity)
    {
    }
}
