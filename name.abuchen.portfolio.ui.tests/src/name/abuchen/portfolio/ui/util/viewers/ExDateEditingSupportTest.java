package name.abuchen.portfolio.ui.util.viewers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.TransactionPair;

/**
 * Tests inline ex-date value handling used by legacy and ledger-aware update paths.
 * These tests make sure clearing and parsing ex-date values produces the expected model value.
 */
@SuppressWarnings("nls")
public class ExDateEditingSupportTest
{
    /**
     * Verifies that a blank ex-date cell clears the field and notifies listeners with a null value.
     * Ledger-aware callers rely on the same UI signal to clear the persisted ex-date fact safely.
     */
    @Test
    public void blankValueClearsExDateAndNotifiesNullAsNewValue()
    {
        var support = new ExDateEditingSupport();

        var tx = new AccountTransaction();
        tx.setType(AccountTransaction.Type.DIVIDENDS);
        tx.setExDate(LocalDateTime.of(2026, 2, 21, 10, 15));

        var element = new TransactionPair<>(new Account("Test Account"), tx);

        AtomicBoolean notified = new AtomicBoolean(false);
        AtomicReference<Object> newValue = new AtomicReference<>();
        AtomicReference<Object> oldValue = new AtomicReference<>();

        support.addListener((e, n, o) -> {
            notified.set(true);
            newValue.set(n);
            oldValue.set(o);
        });

        support.setValue(element, "   ");

        assertThat(tx.getExDate(), is(nullValue()));
        assertThat(notified.get(), is(true));
        assertThat(newValue.get(), is(nullValue()));
        assertThat(oldValue.get(), is(LocalDateTime.of(2026, 2, 21, 10, 15)));
        assertThat(support.getValue(element), is(""));
    }

    /**
     * Verifies that a date-only ex-date input is stored at the start of that day.
     * The inline editor must provide the same value shape to legacy and ledger-aware update paths.
     */
    @Test
    public void parsedLocalDateIsStoredAsLocalDateTimeAtStartOfDay()
    {
        var support = new ExDateEditingSupport();

        var tx = new AccountTransaction();
        tx.setType(AccountTransaction.Type.DIVIDENDS);

        var element = new TransactionPair<>(new Account("Test Account"), tx);

        support.setValue(element, "2026-02-22");

        assertThat(tx.getExDate(), is(LocalDate.of(2026, 2, 22).atStartOfDay()));
    }
}
