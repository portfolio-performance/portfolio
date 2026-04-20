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

public class ClientMergeServiceSecurityMatchingTest
{
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @SuppressWarnings("deprecation")
    @Test
    public void shouldReuseSecurityWhenIsinWknCurrencyMatchesUniquely() throws Exception
    {
        Client base = new Client();
        Client source = new Client();

        Security baseSecurity = createSecurity("Security A", "DE0001234567", "123456", "EUR"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        Security sourceSecurity = createSecurity("Security A Copy", "DE0001234567", "123456", "EUR"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

        base.addSecurity(baseSecurity);
        source.addSecurity(sourceSecurity);

        ClientMergeService service = new ClientMergeService();
        ClientMergeResult result = service.mergeClients(base, Arrays.asList(source));

        assertThat(result.getClient().getSecurities().size(), is(1));
        assertThat(result.getSecuritiesReused(), is(1));
        assertThat(result.getSecuritiesAdded(), is(0));
        assertSame(baseSecurity, result.getClient().getSecurities().get(0));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void shouldAddSecurityWhenNoMatchExists() throws Exception
    {
        Client base = new Client();
        Client source = new Client();

        base.addSecurity(createSecurity("Security A", "DE0001111111", "111111", "EUR")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        source.addSecurity(createSecurity("Security B", "DE0002222222", "222222", "EUR")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

        ClientMergeService service = new ClientMergeService();
        ClientMergeResult result = service.mergeClients(base, Arrays.asList(source));

        assertThat(result.getClient().getSecurities().size(), is(2));
        assertThat(result.getSecuritiesReused(), is(0));
        assertThat(result.getSecuritiesAdded(), is(1));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void shouldNotForceMergeWhenIsinWknCurrencyIsAmbiguousInTarget() throws Exception
    {
        Client base = new Client();
        Client source = new Client();

        base.addSecurity(createSecurity("Security A1", "DE0001234567", "123456", "EUR")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        base.addSecurity(createSecurity("Security A2", "DE0001234567", "123456", "EUR")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

        source.addSecurity(createSecurity("Security A Copy", "DE0001234567", "123456", "EUR")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

        ClientMergeService service = new ClientMergeService();
        ClientMergeResult result = service.mergeClients(base, Arrays.asList(source));

        assertThat(result.getSecuritiesReused(), is(0));
        assertThat(result.getSecuritiesAdded(), is(1));
        assertThat(result.getClient().getSecurities().size(), is(3));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void shouldUseWknOnlyFallbackWhenUniqueOnBothSides() throws Exception
    {
        Client base = new Client();
        Client source = new Client();

        Security baseSecurity = createSecurity("Security A", null, "123456", null); //$NON-NLS-1$ //$NON-NLS-2$
        Security sourceSecurity = createSecurity("Security A Copy", null, "123456", null); //$NON-NLS-1$ //$NON-NLS-2$

        base.addSecurity(baseSecurity);
        source.addSecurity(sourceSecurity);

        ClientMergeService service = new ClientMergeService();
        ClientMergeResult result = service.mergeClients(base, Arrays.asList(source));

        assertThat(result.getClient().getSecurities().size(), is(1));
        assertThat(result.getSecuritiesReused(), is(1));
        assertThat(result.getSecuritiesAdded(), is(0));
        assertSame(baseSecurity, result.getClient().getSecurities().get(0));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void shouldNotUseWknOnlyFallbackWhenAmbiguousInTarget() throws Exception
    {
        Client base = new Client();
        Client source = new Client();

        base.addSecurity(createSecurity("Security A1", null, "123456", null)); //$NON-NLS-1$ //$NON-NLS-2$
        base.addSecurity(createSecurity("Security A2", null, "123456", null)); //$NON-NLS-1$ //$NON-NLS-2$

        source.addSecurity(createSecurity("Security A Copy", null, "123456", null)); //$NON-NLS-1$ //$NON-NLS-2$

        ClientMergeService service = new ClientMergeService();
        ClientMergeResult result = service.mergeClients(base, Arrays.asList(source));

        assertThat(result.getSecuritiesReused(), is(0));
        assertThat(result.getSecuritiesAdded(), is(1));
        assertThat(result.getClient().getSecurities().size(), is(3));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void shouldPreferStrongerMatchLevelBeforeFallingBackToWeakerOne() throws Exception
    {
        Client base = new Client();
        Client source = new Client();

        Security strongMatch = createSecurity("Preferred Match", "DE0001234567", "123456", "EUR"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        Security weakerCandidate = createSecurity("Weaker Candidate", null, "123456", null); //$NON-NLS-1$ //$NON-NLS-2$

        Security sourceSecurity = createSecurity("Source Security", "DE0001234567", "123456", "EUR"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

        base.addSecurity(strongMatch);
        base.addSecurity(weakerCandidate);
        source.addSecurity(sourceSecurity);

        ClientMergeService service = new ClientMergeService();
        ClientMergeResult result = service.mergeClients(base, Arrays.asList(source));

        assertThat(result.getClient().getSecurities().size(), is(2));
        assertThat(result.getSecuritiesReused(), is(1));
        assertThat(result.getSecuritiesAdded(), is(0));
        assertSame(strongMatch, result.getClient().getSecurities().get(0));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void shouldRemapAccountTransactionToReusedSecurity() throws Exception
    {
        Client base = new Client();
        Client source = new Client();

        Security baseSecurity = createSecurity("Security A", "DE0001234567", "123456", "EUR"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        Security sourceSecurity = createSecurity("Security A Copy", "DE0001234567", "123456", "EUR"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

        base.addSecurity(baseSecurity);
        source.addSecurity(sourceSecurity);

        Account sourceAccount = createAccount("Source Account"); //$NON-NLS-1$
        sourceAccount.setCurrencyCode(CurrencyUnit.EUR);
        AccountTransaction tx = new AccountTransaction();
        tx.setType(AccountTransaction.Type.DIVIDENDS);
        tx.setDateTime(LocalDateTime.of(2024, 1, 15, 12, 0));
        tx.setSecurity(sourceSecurity);
        tx.setAmount(10000);
        tx.setCurrencyCode(CurrencyUnit.EUR);
        sourceAccount.addTransaction(tx);
        source.addAccount(sourceAccount);

        ClientMergeService service = new ClientMergeService();
        ClientMergeResult result = service.mergeClients(base, Arrays.asList(source));

        Account mergedAccount = result.getClient().getAccounts().get(0);
        AccountTransaction mergedTransaction = mergedAccount.getTransactions().get(0);

        assertSame(baseSecurity, mergedTransaction.getSecurity());
        assertThat(result.getSecuritiesReused(), is(1));
        assertThat(result.getSecuritiesAdded(), is(0));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void shouldRegenerateImportedAccountUuidEvenWhenSecurityIsReused() throws Exception
    {
        Client base = new Client();
        Client source = new Client();

        Security baseSecurity = createSecurity("Security A", "DE0001234567", "123456", "EUR"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        Security sourceSecurity = createSecurity("Security A Copy", "DE0001234567", "123456", "EUR"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

        base.addSecurity(baseSecurity);
        source.addSecurity(sourceSecurity);

        Account sourceAccount = createAccount("Imported Account"); //$NON-NLS-1$
        String originalUuid = sourceAccount.getUUID();

        AccountTransaction tx = new AccountTransaction();
        tx.setType(AccountTransaction.Type.DIVIDENDS);
        tx.setDateTime(LocalDateTime.of(2024, 1, 15, 12, 0));
        tx.setSecurity(sourceSecurity);
        tx.setAmount(10000);
        tx.setCurrencyCode(CurrencyUnit.EUR);
        sourceAccount.addTransaction(tx);
        source.addAccount(sourceAccount);

        ClientMergeService service = new ClientMergeService();
        ClientMergeResult result = service.mergeClients(base, Arrays.asList(source));

        Account mergedAccount = result.getClient().getAccounts().get(0);

        assertThat(mergedAccount.getUUID(), is(not(equalTo(originalUuid))));
        assertSame(baseSecurity, mergedAccount.getTransactions().get(0).getSecurity());
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
}
