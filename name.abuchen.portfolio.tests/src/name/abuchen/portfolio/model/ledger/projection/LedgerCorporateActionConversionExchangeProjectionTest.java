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
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.model.ledger.LedgerMutationContext;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;
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
public class LedgerCorporateActionConversionExchangeProjectionTest
{
    private static final LocalDateTime DATE = LocalDateTime.of(2026, 1, 2, 0, 0);

    @Test
    public void testExchangeMaterializesRepeatedSourceTargetAndCashProjections()
    {
        var fixture = fixture();
        var entry = LedgerNativeEntryAssembler.corporateAction(fixture.client) //
                        .kind(CorporateActionKind.EXCHANGE) //
                        .date(DATE) //
                        .securityContext("context-1", "main", fixture.portfolio, fixture.sourceSecurity) //
                        .securityOut("source-1", "source-1", fixture.portfolio, fixture.sourceSecurity, shares(10)) //
                        .securityOut("source-2", "source-2", fixture.secondPortfolio, fixture.secondSourceSecurity,
                                        shares(7)) //
                        .securityIn("target-1", "target-1", fixture.portfolio, fixture.targetSecurity, shares(6)) //
                        .securityIn("target-2", "target-2", fixture.secondPortfolio, fixture.secondTargetSecurity,
                                        shares(4)) //
                        .cash("cash-1", "cash-1", fixture.account, money(9)) //
                        .cash("cash-2", "cash-2", fixture.secondAccount, money(11)) //
                        .buildAndAdd().getEntry();

        var descriptors = LedgerProjectionSupport.descriptors(entry);
        var sourceDescriptors = descriptors.stream()
                        .filter(descriptor -> descriptor.getRole() == LedgerProjectionRole.DELIVERY_OUTBOUND)
                        .toList();
        var targetDescriptors = descriptors.stream()
                        .filter(descriptor -> descriptor.getRole() == LedgerProjectionRole.NEW_SECURITY_LEG).toList();
        var cashDescriptors = descriptors.stream()
                        .filter(descriptor -> descriptor.getRole() == LedgerProjectionRole.CASH_COMPENSATION).toList();

        assertThat(sourceDescriptors.size(), is(2));
        assertThat(targetDescriptors.size(), is(2));
        assertThat(cashDescriptors.size(), is(2));
        assertThat(semanticKeys(sourceDescriptors), is(Set.of("source-1", "source-2")));
        assertThat(semanticKeys(targetDescriptors), is(Set.of("target-1", "target-2")));
        assertThat(semanticKeys(cashDescriptors), is(Set.of("cash-1", "cash-2")));

        assertThrows(IllegalArgumentException.class,
                        () -> new LedgerProjectionFactory().createProjection(entry, LedgerProjectionRole.DELIVERY_OUTBOUND));
        assertThrows(IllegalArgumentException.class,
                        () -> new LedgerProjectionFactory().createProjection(entry, LedgerProjectionRole.NEW_SECURITY_LEG));
        assertThrows(IllegalArgumentException.class,
                        () -> new LedgerProjectionFactory().createProjection(entry, LedgerProjectionRole.CASH_COMPENSATION));

        LedgerProjectionService.materialize(fixture.client);
        LedgerProjectionService.materialize(fixture.client);

        var firstSource = ledgerBackedPortfolioProjection(fixture.portfolio,
                        LedgerProjectionRole.DELIVERY_OUTBOUND, "source-1");
        var secondSource = ledgerBackedPortfolioProjection(fixture.secondPortfolio,
                        LedgerProjectionRole.DELIVERY_OUTBOUND, "source-2");
        var firstTarget = ledgerBackedPortfolioProjection(fixture.portfolio, LedgerProjectionRole.NEW_SECURITY_LEG,
                        "target-1");
        var secondTarget = ledgerBackedPortfolioProjection(fixture.secondPortfolio, LedgerProjectionRole.NEW_SECURITY_LEG,
                        "target-2");
        var firstCash = ledgerBackedAccountProjection(fixture.account, LedgerProjectionRole.CASH_COMPENSATION,
                        "cash-1");
        var secondCash = ledgerBackedAccountProjection(fixture.secondAccount, LedgerProjectionRole.CASH_COMPENSATION,
                        "cash-2");

        assertThat(firstSource.getShares(), is(shares(10)));
        assertThat(secondSource.getShares(), is(shares(7)));
        assertThat(firstTarget.getShares(), is(shares(6)));
        assertThat(secondTarget.getShares(), is(shares(4)));
        assertThat(firstCash.getAmount(), is(money(9).getAmount()));
        assertThat(secondCash.getAmount(), is(money(11).getAmount()));
        assertThat(fixture.portfolio.getTransactions().size(), is(2));
        assertThat(fixture.secondPortfolio.getTransactions().size(), is(2));
        assertThat(fixture.account.getTransactions().size(), is(1));
        assertThat(fixture.secondAccount.getTransactions().size(), is(1));
        assertThat(runtimeProjectionIds(List.of(firstSource, secondSource)),
                        is(sourceDescriptors.stream().map(DerivedProjectionDescriptor::getRuntimeProjectionId)
                                        .collect(Collectors.toSet())));
        assertThat(runtimeProjectionIds(List.of(firstTarget, secondTarget)),
                        is(targetDescriptors.stream().map(DerivedProjectionDescriptor::getRuntimeProjectionId)
                                        .collect(Collectors.toSet())));
        assertThat(runtimeProjectionIds(List.of(firstCash, secondCash)),
                        is(cashDescriptors.stream().map(DerivedProjectionDescriptor::getRuntimeProjectionId)
                                        .collect(Collectors.toSet())));
    }

    @Test
    public void testConversionAttachesFeeTaxByMatchingGroup()
    {
        var fixture = fixture();
        var entry = LedgerNativeEntryAssembler.corporateAction(fixture.client) //
                        .kind(CorporateActionKind.CONVERSION) //
                        .date(DATE) //
                        .securityContext("context-1", "main", fixture.portfolio, fixture.sourceSecurity) //
                        .securityOut("source-1", "main", fixture.portfolio, fixture.sourceSecurity, shares(10)) //
                        .securityIn("target-1", "main", fixture.portfolio, fixture.targetSecurity, shares(5)) //
                        .cash("cash-1", "cash-1", fixture.account, money(9)) //
                        .fee("fee-1", fixture.account, money(2), "cash-1") //
                        .tax("tax-1", fixture.account, money(1), "cash-1") //
                        .buildAndAdd().getEntry();

        LedgerProjectionService.materialize(fixture.client);

        var cashProjection = ledgerBackedAccountProjection(fixture.account, LedgerProjectionRole.CASH_COMPENSATION,
                        "cash-1");

        assertThat(cashProjection.getUnit(Unit.Type.FEE).orElseThrow().getAmount().getAmount(), is(money(2).getAmount()));
        assertThat(cashProjection.getUnit(Unit.Type.TAX).orElseThrow().getAmount().getAmount(), is(money(1).getAmount()));
        assertThat(LedgerProjectionSupport.descriptor(entry, LedgerProjectionRole.CASH_COMPENSATION, "cash-1")
                        .getUnitPostings().size(), is(2));
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
        var secondSourceSecurity = new Security("Second Source AG", CurrencyUnit.EUR);
        var targetSecurity = new Security("Target AG", CurrencyUnit.EUR);
        var secondTargetSecurity = new Security("Second Target AG", CurrencyUnit.EUR);

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
        client.addSecurity(secondSourceSecurity);
        client.addSecurity(targetSecurity);
        client.addSecurity(secondTargetSecurity);

        return new Fixture(client, account, secondAccount, portfolio, secondPortfolio, sourceSecurity,
                        secondSourceSecurity, targetSecurity, secondTargetSecurity);
    }

    private record Fixture(Client client, Account account, Account secondAccount, Portfolio portfolio,
                    Portfolio secondPortfolio, Security sourceSecurity, Security secondSourceSecurity,
                    Security targetSecurity, Security secondTargetSecurity)
    {
    }
}
