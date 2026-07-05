package name.abuchen.portfolio.model.ledger.nativeentry;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;

import java.time.LocalDateTime;

import org.junit.Test;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionKind;
import name.abuchen.portfolio.model.ledger.configuration.LedgerLegRole;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class LedgerCorporateActionViewTest
{
    private static final LocalDateTime DATE = LocalDateTime.of(2026, 1, 2, 0, 0);

    @Test
    public void testSingleFailsOnNoMatch()
    {
        var fixture = fixture();
        var entry = stockDividend(fixture);

        assertThrows(IllegalArgumentException.class,
                        () -> LedgerCorporateActionView.of(entry).cash().withAccount(fixture.secondAccount).single());
    }

    @Test
    public void testSingleFailsOnMultipleMatches()
    {
        var fixture = fixture();
        var entry = stockDividend(fixture);

        assertThrows(IllegalArgumentException.class, () -> LedgerCorporateActionView.of(entry).securityIn().single());
    }

    @Test
    public void testPortfolioSecurityFiltersDisambiguateRepeatedTargetLegs()
    {
        var fixture = fixture();
        var entry = stockDividend(fixture);

        var handle = LedgerCorporateActionView.of(entry).securityIn().withPortfolio(fixture.secondPortfolio)
                        .withSecurity(fixture.secondTarget).single();

        assertThat(handle.role(), is(LedgerLegRole.TARGET_SECURITY_LEG));
        assertThat(handle.localKey(), is("target-2"));
        assertThat(handle.groupKey(), is("main"));
    }

    @Test
    public void testAccountFilterDisambiguatesRepeatedCashLegs()
    {
        var fixture = fixture();
        var entry = LedgerNativeEntryAssembler.corporateAction(fixture.client) //
                        .kind(CorporateActionKind.CASH_DISTRIBUTION) //
                        .date(DATE) //
                        .securityContext("context-1", "cash-1", fixture.portfolio, fixture.source) //
                        .cash("cash-1", "cash-1", fixture.account, money(10)) //
                        .cash("cash-2", "cash-2", fixture.secondAccount, money(20)) //
                        .buildAndAdd().getEntry();

        var handle = LedgerCorporateActionView.of(entry).cash().withAccount(fixture.secondAccount).single();

        assertThat(handle.role(), is(LedgerLegRole.CASH_LEG));
        assertThat(handle.localKey(), is("cash-2"));
        assertThat(handle.groupKey(), is("cash-2"));
    }

    @Test
    public void testHandleExposesSemanticKeyWithoutUuidSelector()
    {
        var fixture = fixture();
        var entry = stockDividend(fixture);

        var handle = LedgerCorporateActionView.of(entry).securityIn().withPortfolio(fixture.portfolio)
                        .withSecurity(fixture.target).single();

        assertThat(handle.toSemanticKey().role(), is(LedgerLegRole.TARGET_SECURITY_LEG));
        assertThat(handle.toSemanticKey().localKey(), is("target-1"));
        assertThat(handle.toSemanticKey().groupKey(), is("main"));
        assertFalse(handle.toSemanticKey().localKey().equals(handle.posting().getUUID()));
    }

    private static name.abuchen.portfolio.model.ledger.LedgerEntry stockDividend(Fixture fixture)
    {
        return LedgerNativeEntryAssembler.corporateAction(fixture.client) //
                        .kind(CorporateActionKind.STOCK_DIVIDEND) //
                        .date(DATE) //
                        .securityContext("context-1", "main", fixture.portfolio, fixture.source) //
                        .securityIn("target-1", "main", fixture.portfolio, fixture.target, Values.Share.factorize(5)) //
                        .securityIn("target-2", "main", fixture.secondPortfolio, fixture.secondTarget,
                                        Values.Share.factorize(3)) //
                        .cash("cash-1", "cash-1", fixture.account, money(1)) //
                        .buildAndAdd().getEntry();
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
        var source = new Security("Source AG", CurrencyUnit.EUR);
        var target = new Security("Target AG", CurrencyUnit.EUR);
        var secondTarget = new Security("Second Target AG", CurrencyUnit.EUR);

        account.setName("Cash Account");
        account.setCurrencyCode(CurrencyUnit.EUR);
        secondAccount.setName("Second Cash Account");
        secondAccount.setCurrencyCode(CurrencyUnit.EUR);
        portfolio.setName("Portfolio");
        portfolio.setReferenceAccount(account);
        secondPortfolio.setName("Second Portfolio");
        secondPortfolio.setReferenceAccount(secondAccount);

        client.addAccount(account);
        client.addAccount(secondAccount);
        client.addPortfolio(portfolio);
        client.addPortfolio(secondPortfolio);
        client.addSecurity(source);
        client.addSecurity(target);
        client.addSecurity(secondTarget);

        return new Fixture(client, account, secondAccount, portfolio, secondPortfolio, source, target, secondTarget);
    }

    private record Fixture(Client client, Account account, Account secondAccount, Portfolio portfolio,
                    Portfolio secondPortfolio, Security source, Security target, Security secondTarget)
    {
    }
}
