package name.abuchen.portfolio.ui.views.panes;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.selection.SecuritySelection;

@SuppressWarnings("nls")
public class TransactionsPaneMultiSelectionTest
{
    @Test
    public void testResolveMultipleSecuritiesCollectsAllTransactions()
    {
        var client = new Client();

        var securityA = new SecurityBuilder()
                        .addPrice("2024-01-01", Values.Quote.factorize(100))
                        .addTo(client);

        var securityB = new SecurityBuilder()
                        .addPrice("2024-01-01", Values.Quote.factorize(50))
                        .addTo(client);

        new AccountBuilder()
                        .dividend("2024-03-01", Values.Amount.factorize(10), securityA)
                        .dividend("2024-06-01", Values.Amount.factorize(5), securityB)
                        .addTo(client);

        var account = client.getAccounts().get(0);

        new PortfolioBuilder(account)
                        .buy(securityA, "2024-01-15", Values.Share.factorize(10), Values.Amount.factorize(1000))
                        .buy(securityB, "2024-02-01", Values.Share.factorize(20), Values.Amount.factorize(1000))
                        .sell(securityA, "2024-04-01", Values.Share.factorize(5), Values.Amount.factorize(600))
                        .addTo(client);

        var selection = new SecuritySelection(client, Arrays.asList(securityA, securityB));
        var resolved = TransactionPaneInput.resolve(selection, client);

        assertThat(resolved.getTransactions(), hasSize(5)); // buyA + divA + sellA + buyB + divB
        assertThat(resolved.getSource(), is(selection));
    }

    @Test
    public void testResolveSingleSecuritySelection()
    {
        var client = new Client();

        var securityA = new SecurityBuilder()
                        .addPrice("2024-01-01", Values.Quote.factorize(100))
                        .addTo(client);

        var securityB = new SecurityBuilder()
                        .addPrice("2024-01-01", Values.Quote.factorize(50))
                        .addTo(client);

        new AccountBuilder()
                        .dividend("2024-03-01", Values.Amount.factorize(10), securityA)
                        .dividend("2024-06-01", Values.Amount.factorize(5), securityB)
                        .addTo(client);

        var account = client.getAccounts().get(0);

        new PortfolioBuilder(account)
                        .buy(securityA, "2024-01-15", Values.Share.factorize(10), Values.Amount.factorize(1000))
                        .addTo(client);

        var selection = new SecuritySelection(client, securityA);
        var resolved = TransactionPaneInput.resolve(selection, client);

        assertThat(resolved.getTransactions(), hasSize(2)); // buy + dividend
    }

    @Test
    public void testResolveEmptySelectionReturnsNoTransactions()
    {
        var client = new Client();

        var selection = new SecuritySelection(client, List.of());
        var resolved = TransactionPaneInput.resolve(selection, client);

        assertThat(resolved.getTransactions(), hasSize(0));
        assertThat(resolved.getExportLabel(), is(""));
    }

    @Test
    public void testResolveBareSecurityInput()
    {
        var client = new Client();

        var security = new SecurityBuilder()
                        .addPrice("2024-01-01", Values.Quote.factorize(100))
                        .addTo(client);

        var account = new AccountBuilder()
                        .dividend("2024-03-01", Values.Amount.factorize(10), security)
                        .addTo(client);

        new PortfolioBuilder(account)
                        .buy(security, "2024-01-15", Values.Share.factorize(10), Values.Amount.factorize(1000))
                        .addTo(client);

        var resolved = TransactionPaneInput.resolve(security, client);

        assertThat(resolved.getTransactions(), hasSize(2)); // buy + dividend
        assertThat(resolved.getSource(), is(security));
    }

    @Test
    public void testExportLabelForMultipleSecurities()
    {
        var client = new Client();
        var securityA = new SecurityBuilder().addTo(client);
        var securityB = new SecurityBuilder().addTo(client);

        var selection = new SecuritySelection(client, Arrays.asList(securityA, securityB));
        var resolved = TransactionPaneInput.resolve(selection, client);

        assertThat(resolved.getExportLabel(), is("2 Securities"));
    }

    @Test
    public void testExportLabelForSingleSecuritySelection()
    {
        var client = new Client();
        var security = new SecurityBuilder().addTo(client);

        var selection = new SecuritySelection(client, security);
        var resolved = TransactionPaneInput.resolve(selection, client);

        assertThat(resolved.getExportLabel(), is(security.getName()));
    }

    @Test
    public void testExportLabelForBareSecurity()
    {
        var client = new Client();
        var security = new SecurityBuilder().addTo(client);

        var resolved = TransactionPaneInput.resolve(security, client);

        assertThat(resolved.getExportLabel(), is(security.getName()));
    }
}
