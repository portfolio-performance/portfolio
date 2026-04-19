package name.abuchen.portfolio.model;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;

import java.time.LocalDateTime;
import java.util.Arrays;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import name.abuchen.portfolio.money.CurrencyUnit;

public class ClientMergeServiceUuidTest
{
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @SuppressWarnings("deprecation")
    @Test
    public void shouldRegenerateImportedAccountUuid() throws Exception
    {
        Client base = new Client();
        Client source = new Client();

        Account sourceAccount = createAccount("Imported Account"); //$NON-NLS-1$
        String originalUuid = sourceAccount.getUUID();

        source.addAccount(sourceAccount);

        ClientMergeService service = new ClientMergeService();
        ClientMergeResult result = service.mergeClients(base, Arrays.asList(source));

        Account mergedAccount = result.getClient().getAccounts().get(0);

        assertThat(mergedAccount.getUUID(), is(not(equalTo(originalUuid))));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void shouldRegenerateImportedPortfolioUuid() throws Exception
    {
        Client base = new Client();
        Client source = new Client();

        Portfolio sourcePortfolio = createPortfolio("Imported Portfolio"); //$NON-NLS-1$
        String originalUuid = sourcePortfolio.getUUID();

        source.addPortfolio(sourcePortfolio);

        ClientMergeService service = new ClientMergeService();
        ClientMergeResult result = service.mergeClients(base, Arrays.asList(source));

        Portfolio mergedPortfolio = result.getClient().getPortfolios().get(0);

        assertThat(mergedPortfolio.getUUID(), is(not(equalTo(originalUuid))));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void shouldRegenerateImportedAccountTransactionUuid() throws Exception
    {
        Client base = new Client();
        Client source = new Client();

        Account sourceAccount = createAccount("Imported Account"); //$NON-NLS-1$
        AccountTransaction sourceTransaction = new AccountTransaction();
        sourceTransaction.setType(AccountTransaction.Type.DEPOSIT);
        sourceTransaction.setDateTime(LocalDateTime.of(2024, 1, 15, 12, 0));
        sourceTransaction.setAmount(10000);
        sourceTransaction.setCurrencyCode(CurrencyUnit.EUR);
        String originalUuid = sourceTransaction.getUUID();

        sourceAccount.addTransaction(sourceTransaction);
        source.addAccount(sourceAccount);

        ClientMergeService service = new ClientMergeService();
        ClientMergeResult result = service.mergeClients(base, Arrays.asList(source));

        Account mergedAccount = result.getClient().getAccounts().get(0);
        AccountTransaction mergedTransaction = mergedAccount.getTransactions().get(0);

        assertThat(mergedTransaction.getUUID(), is(not(equalTo(originalUuid))));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void shouldRegenerateImportedPortfolioTransactionUuid() throws Exception
    {
        Client base = new Client();
        Client source = new Client();

        Portfolio sourcePortfolio = createPortfolio("Imported Portfolio"); //$NON-NLS-1$
        PortfolioTransaction sourceTransaction = new PortfolioTransaction();
        sourceTransaction.setType(PortfolioTransaction.Type.BUY);
        String originalUuid = sourceTransaction.getUUID();

        sourcePortfolio.addTransaction(sourceTransaction);
        source.addPortfolio(sourcePortfolio);

        ClientMergeService service = new ClientMergeService();
        ClientMergeResult result = service.mergeClients(base, Arrays.asList(source));

        Portfolio mergedPortfolio = result.getClient().getPortfolios().get(0);
        PortfolioTransaction mergedTransaction = mergedPortfolio.getTransactions().get(0);

        assertThat(mergedTransaction.getUUID(), is(not(equalTo(originalUuid))));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void shouldKeepBaseAccountUuidUnchanged() throws Exception
    {
        Client base = new Client();
        Client source = new Client();

        Account baseAccount = createAccount("Base Account"); //$NON-NLS-1$
        String baseUuid = baseAccount.getUUID();

        base.addAccount(baseAccount);

        ClientMergeService service = new ClientMergeService();
        ClientMergeResult result = service.mergeClients(base, Arrays.asList(source));

        Account mergedAccount = result.getClient().getAccounts().get(0);

        assertThat(mergedAccount.getUUID(), is(equalTo(baseUuid)));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void shouldKeepBasePortfolioUuidUnchanged() throws Exception
    {
        Client base = new Client();
        Client source = new Client();

        Portfolio basePortfolio = createPortfolio("Base Portfolio"); //$NON-NLS-1$
        String baseUuid = basePortfolio.getUUID();

        base.addPortfolio(basePortfolio);

        ClientMergeService service = new ClientMergeService();
        ClientMergeResult result = service.mergeClients(base, Arrays.asList(source));

        Portfolio mergedPortfolio = result.getClient().getPortfolios().get(0);

        assertThat(mergedPortfolio.getUUID(), is(equalTo(baseUuid)));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void shouldRemapSecurityBeforeRegeneratingImportedTransactionUuid() throws Exception
    {
        Client base = new Client();
        Client source = new Client();

        Security baseSecurity = createSecurity("Security A", "DE0001234567", "123456", "EUR"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        Security sourceSecurity = createSecurity("Security A Copy", "DE0001234567", "123456", "EUR"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

        base.addSecurity(baseSecurity);
        source.addSecurity(sourceSecurity);

        Account sourceAccount = createAccount("Imported Account"); //$NON-NLS-1$
        AccountTransaction sourceTransaction = new AccountTransaction();
        sourceTransaction.setType(AccountTransaction.Type.DIVIDENDS);
        sourceTransaction.setDateTime(LocalDateTime.of(2024, 1, 15, 12, 0));
        sourceTransaction.setSecurity(sourceSecurity);
        sourceTransaction.setAmount(10000);
        sourceTransaction.setCurrencyCode(CurrencyUnit.EUR);
        String originalUuid = sourceTransaction.getUUID();

        sourceAccount.addTransaction(sourceTransaction);
        source.addAccount(sourceAccount);

        ClientMergeService service = new ClientMergeService();
        ClientMergeResult result = service.mergeClients(base, Arrays.asList(source));

        Account mergedAccount = result.getClient().getAccounts().get(0);
        AccountTransaction mergedTransaction = mergedAccount.getTransactions().get(0);

        assertThat(mergedTransaction.getUUID(), is(not(equalTo(originalUuid))));
        assertSame(baseSecurity, mergedTransaction.getSecurity());
    }

    @SuppressWarnings("deprecation")
    @Test
    public void shouldRegenerateImportedAccountAndTransactionUuidsTogether() throws Exception
    {
        Client base = new Client();
        Client source = new Client();

        Account sourceAccount = createAccount("Imported Account"); //$NON-NLS-1$
        String originalAccountUuid = sourceAccount.getUUID();

        AccountTransaction sourceTransaction = new AccountTransaction();
        sourceTransaction.setType(AccountTransaction.Type.REMOVAL);
        sourceTransaction.setDateTime(LocalDateTime.of(2024, 1, 15, 12, 0));
        sourceTransaction.setAmount(10000);
        sourceTransaction.setCurrencyCode(CurrencyUnit.EUR);

        String originalTransactionUuid = sourceTransaction.getUUID();

        sourceAccount.addTransaction(sourceTransaction);
        source.addAccount(sourceAccount);

        ClientMergeService service = new ClientMergeService();
        ClientMergeResult result = service.mergeClients(base, Arrays.asList(source));

        Account mergedAccount = result.getClient().getAccounts().get(0);
        AccountTransaction mergedTransaction = mergedAccount.getTransactions().get(0);

        assertThat(mergedAccount.getUUID(), is(not(equalTo(originalAccountUuid))));
        assertThat(mergedTransaction.getUUID(), is(not(equalTo(originalTransactionUuid))));
    }

    private Security createSecurity(String name, String isin, String wkn, String currency)
    {
        Security security = new Security();
        security.setName(name);
        security.setIsin(isin);
        security.setWkn(wkn);
        security.setCurrencyCode(currency);
        return security;
    }

    private Account createAccount(String name)
    {
        Account account = new Account();
        account.setName(name);
        return account;
    }

    private Portfolio createPortfolio(String name)
    {
        Portfolio portfolio = new Portfolio();
        portfolio.setName(name);
        return portfolio;
    }
}
