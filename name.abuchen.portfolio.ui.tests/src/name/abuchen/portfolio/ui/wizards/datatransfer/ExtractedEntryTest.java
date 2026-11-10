package name.abuchen.portfolio.ui.wizards.datatransfer;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.LocalDateTime;

import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.ImportAction;
import name.abuchen.portfolio.datatransfer.ImportAction.Status;
import name.abuchen.portfolio.datatransfer.actions.DetectDuplicatesAction;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class ExtractedEntryTest
{
    private Client client;
    private Account account;
    private Portfolio portfolio;
    private Security existingSecurity;
    private ImportAction.Context context;

    @Before
    public void setup()
    {
        client = new Client();

        account = new Account("Account");
        account.setCurrencyCode(CurrencyUnit.EUR);
        client.addAccount(account);

        portfolio = new Portfolio("Portfolio");
        portfolio.setReferenceAccount(account);
        client.addPortfolio(portfolio);

        existingSecurity = new Security("AVENIR RENDEMENT (PART I)", CurrencyUnit.EUR);
        client.addSecurity(existingSecurity);

        // purchase which has already been imported into the existing security
        var existingEntry = createPurchase(existingSecurity);
        existingEntry.setPortfolio(portfolio);
        existingEntry.setAccount(account);
        existingEntry.insert();

        context = new ImportAction.Context()
        {
            @Override
            public Account getAccount(String currencyCode)
            {
                return account;
            }

            @Override
            public Portfolio getPortfolio()
            {
                return portfolio;
            }

            @Override
            public Account getSecondaryAccount(String currencyCode)
            {
                return account;
            }

            @Override
            public Portfolio getSecondaryPortfolio()
            {
                return portfolio;
            }
        };
    }

    private BuySellEntry createPurchase(Security security)
    {
        var entry = new BuySellEntry(PortfolioTransaction.Type.BUY);
        entry.setDate(LocalDateTime.parse("2025-03-20T00:00"));
        entry.setSecurity(security);
        entry.setShares(Values.Share.factorize(15.4030));
        entry.setCurrencyCode(CurrencyUnit.EUR);
        entry.setAmount(Values.Amount.factorize(680.87));
        return entry;
    }

    @Test
    public void testDuplicateIsDetectedWithSecurityOverride()
    {
        // the import creates a new security with a different name
        var newSecurity = new Security("AVENIR RENDEMENT (PART 555I)", CurrencyUnit.EUR);
        var entry = new ExtractedEntry(new Extractor.BuySellEntryItem(createPurchase(newSecurity)));

        // the user chooses the existing security instead
        entry.setSecurityOverride(existingSecurity);

        var status = entry.apply(new DetectDuplicatesAction(client), context);

        assertThat(status.getCode(), is(Status.Code.WARNING));
        assertThat(status.getMessage(), is(Messages.LabelPotentialDuplicate));
    }

    @Test
    public void testDuplicateIsNotDetectedWithoutSecurityOverride()
    {
        var newSecurity = new Security("AVENIR RENDEMENT (PART 555I)", CurrencyUnit.EUR);
        var entry = new ExtractedEntry(new Extractor.BuySellEntryItem(createPurchase(newSecurity)));

        var status = entry.apply(new DetectDuplicatesAction(client), context);

        assertThat(status.getCode(), is(Status.Code.OK));
    }

    @Test
    public void testOriginalSecurityIsRestoredAfterCheck()
    {
        var newSecurity = new Security("AVENIR RENDEMENT (PART 555I)", CurrencyUnit.EUR);
        var entry = new ExtractedEntry(new Extractor.BuySellEntryItem(createPurchase(newSecurity)));
        entry.setSecurityOverride(existingSecurity);

        entry.apply(new DetectDuplicatesAction(client), context);

        // the override is only applied permanently upon import
        assertThat(entry.getItem().getSecurity(), sameInstance(newSecurity));
        assertThat(entry.getSecurityOverride(), sameInstance(existingSecurity));
    }
}
