package name.abuchen.portfolio.cli.masterdata;

import static name.abuchen.portfolio.cli.masterdata.CashAccounts.CASH_ACCOUNTS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThrows;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import name.abuchen.portfolio.cli.command.CliException;
import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;

public class CashAccountsTest
{
    private final Client client = new Client();

    private Account account(String currency, String name)
    {
        var account = new AccountBuilder(currency).addTo(client);
        account.setName(name);
        return account;
    }

    private Account resolve(String value) throws CliException
    {
        return CASH_ACCOUNTS.reference().resolve(client, "cash-account", value);
    }

    @Test
    public void rendersEveryMemberInDeclarationOrder()
    {
        var account = account("USD", "Brokerage cash");
        account.setNote("for dividends");
        account.setRetired(true);
        account.setUpdatedAt(Instant.parse("2026-10-24T15:04:57.900Z"));

        var item = CASH_ACCOUNTS.renderer(client).apply(account);

        assertThat(item.keySet().toString(), is("[uuid, name, currencyCode, note, retired, updatedAt]"));
        assertThat(item.get("uuid"), is(account.getUUID()));
        assertThat(item.get("name"), is("Brokerage cash"));
        assertThat(item.get("currencyCode"), is("USD"));
        assertThat(item.get("note"), is("for dividends"));
        assertThat(item.get("retired"), is(true));
        assertThat(item.get("updatedAt"), is("2026-10-24T15:04:57Z"));
    }

    @Test
    public void theCurrencyIsAlwaysRenderedAndABlankNoteIsNot()
    {
        var account = new Account();
        client.addAccount(account);
        account.setNote("   ");
        account.setUpdatedAt(null);

        var item = CASH_ACCOUNTS.renderer(client).apply(account);

        assertThat(item.keySet().toString(), is("[uuid, name, currencyCode, retired]"));
        assertThat(item.get("name"), is(""));
        assertThat(item.get("retired"), is(false));
    }

    @Test
    public void resolvesByUuidAndNameSearchingOnlyCashAccounts() throws CliException
    {
        var cash = account("EUR", "Main");
        var depot = new PortfolioBuilder(cash).addTo(client);
        depot.setName("Broker");
        new SecurityBuilder().addTo(client).setName("Stock");

        assertThat(resolve(cash.getUUID()), is(sameInstance(cash)));
        assertThat(resolve("main"), is(sameInstance(cash)));

        assertThat(assertThrows(CliException.class, () -> resolve(depot.getUUID())).getCode(),
                        is("reference-not-found"));
        assertThat(assertThrows(CliException.class, () -> resolve("Stock")).getCode(), is("reference-not-found"));
    }

    @Test
    public void ambiguousCashAccountsListTheirCurrencyAndRetiredFlag()
    {
        var first = account("USD", "Main");
        var second = account("EUR", "MAIN");
        second.setRetired(true);

        var e = assertThrows(CliException.class, () -> resolve("Main"));

        assertThat(e.getCode(), is("reference-ambiguous"));
        assertThat(e.getMessage(), containsString("cash accounts"));
        @SuppressWarnings("unchecked")
        var candidates = (List<Map<String, Object>>) e.toProblem().get("candidates");
        var candidate = candidates.stream().filter(c -> c.get("uuid").equals(first.getUUID())).findFirst()
                        .orElseThrow();
        assertThat(candidate.keySet().toString(), is("[uuid, name, retired, matchedBy, currencyCode]"));
        assertThat(candidate.get("currencyCode"), is("USD"));
        assertThat(candidate.get("matchedBy"), is(List.of("name")));
        var retired = candidates.stream().filter(c -> c.get("uuid").equals(second.getUUID())).findFirst()
                        .orElseThrow();
        assertThat(retired.get("retired"), is(true));
    }
}
