package name.abuchen.portfolio.model.ledger.projection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionLeg;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionKind;
import name.abuchen.portfolio.model.ledger.nativeentry.LedgerNativeEntryAssembler;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class LedgerCorporateActionSecurityInProjectionTest
{
    private static final LocalDateTime DATE = LocalDateTime.of(2026, 1, 2, 0, 0);

    @Test
    public void testStockDividendMaterializesRepeatedInboundProjections()
    {
        var fixture = fixture();
        var entry = LedgerNativeEntryAssembler.corporateAction(fixture.client) //
                        .kind(CorporateActionKind.STOCK_DIVIDEND) //
                        .date(DATE) //
                        .securityContext("context-1", "main", fixture.portfolio, fixture.sourceSecurity) //
                        .securityIn("target-1", fixture.portfolio, fixture.targetSecurity, shares(5)) //
                        .securityIn("target-2", fixture.secondPortfolio, fixture.rightSecurity, shares(3)) //
                        .buildAndAdd().getEntry();

        var descriptors = LedgerProjectionSupport.descriptors(entry);
        var securityDescriptors = descriptors.stream()
                        .filter(descriptor -> descriptor.getRole() == LedgerProjectionRole.NEW_SECURITY_LEG).toList();

        assertThat(securityDescriptors.size(), is(2));
        assertThat(semanticKeys(securityDescriptors), is(Set.of("target-1", "target-2")));
        assertThrows(IllegalArgumentException.class,
                        () -> new LedgerProjectionFactory().createProjection(entry, LedgerProjectionRole.NEW_SECURITY_LEG));
        assertThat(new LedgerProjectionFactory()
                        .createProjection(entry, LedgerProjectionRole.NEW_SECURITY_LEG, "target-2").getUUID(),
                        is(entry.getUUID() + ":NEW_SECURITY_LEG:target-2"));

        LedgerProjectionService.materialize(fixture.client);
        LedgerProjectionService.materialize(fixture.client);

        var first = ledgerBackedPortfolioProjection(fixture.portfolio, LedgerProjectionRole.NEW_SECURITY_LEG,
                        "target-1");
        var second = ledgerBackedPortfolioProjection(fixture.secondPortfolio, LedgerProjectionRole.NEW_SECURITY_LEG,
                        "target-2");

        assertThat(first.getShares(), is(shares(5)));
        assertThat(second.getShares(), is(shares(3)));
        assertSame(fixture.targetSecurity, first.getSecurity());
        assertSame(fixture.rightSecurity, second.getSecurity());
        assertThat(fixture.portfolio.getTransactions().size(), is(1));
        assertThat(fixture.secondPortfolio.getTransactions().size(), is(1));
        assertThat(runtimeProjectionIds(List.of(first, second)),
                        is(securityDescriptors.stream().map(DerivedProjectionDescriptor::getRuntimeProjectionId)
                                        .collect(Collectors.toSet())));
        assertFalse(LedgerProjectionSupport.descriptors(entry).stream()
                        .anyMatch(descriptor -> descriptor.getPrimaryPosting()
                                        .getCorporateActionLeg() == CorporateActionLeg.SECURITY_CONTEXT));
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

    private static Fixture fixture()
    {
        var client = new Client();
        var account = new Account();
        var portfolio = new Portfolio();
        var secondPortfolio = new Portfolio();
        var sourceSecurity = new Security("Source AG", CurrencyUnit.EUR);
        var targetSecurity = new Security("Target AG", CurrencyUnit.EUR);
        var rightSecurity = new Security("Rights", CurrencyUnit.EUR);

        account.setName("Cash");
        account.setCurrencyCode(CurrencyUnit.EUR);
        portfolio.setName("Portfolio");
        secondPortfolio.setName("Second Portfolio");

        client.addAccount(account);
        client.addPortfolio(portfolio);
        client.addPortfolio(secondPortfolio);
        client.addSecurity(sourceSecurity);
        client.addSecurity(targetSecurity);
        client.addSecurity(rightSecurity);

        return new Fixture(client, account, portfolio, secondPortfolio, sourceSecurity, targetSecurity, rightSecurity);
    }

    private record Fixture(Client client, Account account, Portfolio portfolio, Portfolio secondPortfolio,
                    Security sourceSecurity, Security targetSecurity, Security rightSecurity)
    {
    }
}
