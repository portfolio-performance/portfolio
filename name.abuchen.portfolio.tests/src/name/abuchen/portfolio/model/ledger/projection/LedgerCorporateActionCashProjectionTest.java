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
import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionLeg;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionKind;
import name.abuchen.portfolio.model.ledger.nativeentry.LedgerNativeEntryAssembler;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class LedgerCorporateActionCashProjectionTest
{
    private static final LocalDateTime DATE = LocalDateTime.of(2026, 1, 2, 0, 0);

    @Test
    public void testCashDistributionMaterializesRepeatedCashProjections()
    {
        var fixture = fixture();
        var entry = LedgerNativeEntryAssembler.corporateAction(fixture.client) //
                        .kind(CorporateActionKind.CASH_DISTRIBUTION) //
                        .date(DATE) //
                        .securityContext("context-1", "main", fixture.portfolio, fixture.security) //
                        .cash("cash-1", fixture.account, money(15)) //
                        .cash("cash-2", fixture.secondAccount, money(7)) //
                        .buildAndAdd().getEntry();

        var descriptors = LedgerProjectionSupport.descriptors(entry);
        var accountDescriptors = descriptors.stream()
                        .filter(descriptor -> descriptor.getRole() == LedgerProjectionRole.ACCOUNT).toList();

        assertThat(accountDescriptors.size(), is(2));
        assertThat(semanticKeys(accountDescriptors), is(Set.of("cash-1", "cash-2")));
        assertThrows(IllegalArgumentException.class,
                        () -> new LedgerProjectionFactory().createProjection(entry, LedgerProjectionRole.ACCOUNT));
        assertThat(new LedgerProjectionFactory().createProjection(entry, LedgerProjectionRole.ACCOUNT, "cash-1")
                        .getUUID(), is(entry.getUUID() + ":ACCOUNT:cash-1"));

        LedgerProjectionService.materialize(fixture.client);
        LedgerProjectionService.materialize(fixture.client);

        var first = ledgerBackedAccountProjection(fixture.account, LedgerProjectionRole.ACCOUNT, "cash-1");
        var second = ledgerBackedAccountProjection(fixture.secondAccount, LedgerProjectionRole.ACCOUNT, "cash-2");

        assertThat(first.getAmount(), is(money(15).getAmount()));
        assertThat(second.getAmount(), is(money(7).getAmount()));
        assertThat(fixture.account.getTransactions().size(), is(1));
        assertThat(fixture.secondAccount.getTransactions().size(), is(1));
        assertThat(runtimeProjectionIds(List.of(first, second)),
                        is(accountDescriptors.stream().map(DerivedProjectionDescriptor::getRuntimeProjectionId)
                                        .collect(Collectors.toSet())));
        assertFalse(LedgerProjectionSupport.descriptors(entry).stream()
                        .anyMatch(descriptor -> descriptor.getPrimaryPosting()
                                        .getCorporateActionLeg() == CorporateActionLeg.SECURITY_CONTEXT));
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
        var security = new Security("Coupon AG", CurrencyUnit.EUR);

        account.setName("Cash");
        account.setCurrencyCode(CurrencyUnit.EUR);
        secondAccount.setName("Second Cash");
        secondAccount.setCurrencyCode(CurrencyUnit.EUR);
        portfolio.setName("Portfolio");

        client.addAccount(account);
        client.addAccount(secondAccount);
        client.addPortfolio(portfolio);
        client.addSecurity(security);

        return new Fixture(client, account, secondAccount, portfolio, security);
    }

    private record Fixture(Client client, Account account, Account secondAccount, Portfolio portfolio,
                    Security security)
    {
    }
}
