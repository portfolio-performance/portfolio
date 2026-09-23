package name.abuchen.portfolio.ui.wizards.datatransfer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.time.LocalDateTime;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.ImportAction.Status;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class ExtractedEntryTest
{
    private static final LocalDateTime DATE = LocalDateTime.parse("2024-01-02T00:00");

    private static ExtractedEntry entry(AccountTransaction.Type type)
    {
        var transaction = new AccountTransaction(DATE, CurrencyUnit.EUR, Values.Amount.factorize(10), null, type);
        return new ExtractedEntry(new Extractor.TransactionItem(transaction));
    }

    @Test
    public void testCopyWithKeepsChoiceOfUserAndSecurityDependency()
    {
        var security = new SecurityBuilder().addTo(new Client());
        var dependency = new ExtractedEntry(new Extractor.SecurityItem(security));

        var original = entry(AccountTransaction.Type.DEPOSIT);
        original.setImported(false);
        original.setSecurityDependency(dependency);
        original.setSecurityOverride(security);

        var copy = original.copyWith(entry(AccountTransaction.Type.REMOVAL).getItem());

        assertThat(copy.getItem().getTypeInformation(), is(AccountTransaction.Type.REMOVAL.toString()));
        assertThat(copy.isImported(), is(false));
        assertThat(copy.getSecurityDependency(), is(dependency));
        assertThat(copy.getSecurityOverride(), is(security));
    }

    @Test
    public void testCopyWithDoesNotTakeOverTheOwner()
    {
        var owner = entry(AccountTransaction.Type.DEPOSIT);
        owner.setImported(false);

        var original = entry(AccountTransaction.Type.FEES);
        original.setOwner(owner);

        var copy = original.copyWith(entry(AccountTransaction.Type.TAXES).getItem());

        assertThat(original.isImported(), is(false));
        assertThat(copy.isImported(), is(true));
    }

    @Test
    public void testEntryIsImportedTogetherWithItsOwner()
    {
        var owner = entry(AccountTransaction.Type.DEPOSIT);
        var fee = entry(AccountTransaction.Type.FEES);
        fee.setOwner(owner);

        assertThat(fee.isImported(), is(true));

        owner.setImported(false);
        assertThat(fee.isImported(), is(false));

        owner.setImported(true);
        assertThat(fee.isImported(), is(true));
    }

    @Test
    public void testEntryIsNotImportedIfItsOwnerHasAnError()
    {
        var owner = entry(AccountTransaction.Type.DEPOSIT);
        owner.addStatus(new Status(Status.Code.ERROR, "error"));

        var fee = entry(AccountTransaction.Type.FEES);
        fee.setOwner(owner);
        fee.setImported(true);

        assertThat(fee.isImported(), is(false));
    }

    @Test
    public void testExplicitExclusionOfOwnedEntryIsKept()
    {
        var owner = entry(AccountTransaction.Type.DEPOSIT);
        var fee = entry(AccountTransaction.Type.FEES);
        fee.setOwner(owner);
        fee.setImported(false);

        assertThat(owner.isImported(), is(true));
        assertThat(fee.isImported(), is(false));
    }

    @Test
    public void testEntryWithoutOwner()
    {
        var entry = entry(AccountTransaction.Type.DEPOSIT);

        assertThat(entry.getSecurityDependency(), is(nullValue()));
        assertThat(entry.isImported(), is(true));
    }
}
