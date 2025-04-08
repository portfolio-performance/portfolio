package name.abuchen.portfolio.ui.views.payments;

import java.time.LocalDate;
import java.util.Optional;

import org.eclipse.jface.preference.IPreferenceStore;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.ui.util.ClientFilterMenu;
import name.abuchen.portfolio.ui.views.payments.PaymentsViewModel.Mode;

public class PaymentsViewInput
{
    private static final String KEY_TAB = PaymentsView.class.getSimpleName() + "-tab"; //$NON-NLS-1$
    private static final String KEY_YEAR = PaymentsView.class.getSimpleName() + "-year"; //$NON-NLS-1$
    private static final String KEY_MODE = PaymentsView.class.getSimpleName() + "-mode"; //$NON-NLS-1$
    private static final String KEY_USE_GROSS_VALUE = PaymentsView.class.getSimpleName() + "-use-gross-value"; //$NON-NLS-1$
    private static final String KEY_USE_CONSOLIDATE_RETIRED = PaymentsView.class.getSimpleName()
                    + "-use-consolidate-retired"; //$NON-NLS-1$
    // for legacy reasons, the key is stored with the name PaymentsViewModel
    private static final String KEY_USED_FILTER = PaymentsViewModel.class.getSimpleName();

    /**
     * Preference constant to hide the summary row at the top.
     */
    public static final String TOP = PaymentsView.class.getSimpleName() + "@top"; //$NON-NLS-1$
    /**
     * Preference constant to hide the summary row at the bottom.
     */
    public static final String BOTTOM = PaymentsView.class.getSimpleName() + "@bottom"; //$NON-NLS-1$

    private int tab;
    private int year;
    private Optional<String> clientFilterId;
    private PaymentsViewModel.Mode mode;
    private boolean useGrossValue;
    private boolean useConsolidateRetired;

    public PaymentsViewInput(int tab, int year, Optional<String> clientFilterId, Mode mode, boolean useGrossValue,
                    boolean useConsolidateRetired)
    {
        this.tab = tab;
        this.year = year;
        this.clientFilterId = clientFilterId;
        this.mode = mode;
        this.useGrossValue = useGrossValue;
        this.useConsolidateRetired = useConsolidateRetired;
    }

    public int getTab()
    {
        return tab;
    }

    public void setTab(int tab)
    {
        this.tab = tab;
    }

    public int getYear()
    {
        return year;
    }

    public void setYear(int year)
    {
        this.year = year;
    }

    public Optional<String> getClientFilterId()
    {
        return clientFilterId;
    }

    public void setClientFilterId(String clientFilterId)
    {
        this.clientFilterId = Optional.ofNullable(clientFilterId);
    }

    public PaymentsViewModel.Mode getMode()
    {
        return mode;
    }

    public void setMode(PaymentsViewModel.Mode mode)
    {
        this.mode = mode;
    }

    public boolean isUseGrossValue()
    {
        return useGrossValue;
    }

    public void setUseGrossValue(boolean useGrossValue)
    {
        this.useGrossValue = useGrossValue;
    }

    public boolean isUseConsolidateRetired()
    {
        return useConsolidateRetired;
    }

    public void setUseConsolidateRetired(boolean useConsolidateRetired)
    {
        this.useConsolidateRetired = useConsolidateRetired;
    }

    public static PaymentsViewInput fromPreferences(IPreferenceStore preferences, Client client)
    {
        int tab = preferences.getInt(KEY_TAB);

        int year = preferences.getInt(KEY_YEAR);
        LocalDate now = LocalDate.now();
        if (year < 1900 || year > now.getYear())
            year = now.getYear() - 2;

        Optional<String> clientFilterId = ClientFilterMenu.getSelectedFilterId(client, KEY_USED_FILTER);

        PaymentsViewModel.Mode mode = PaymentsViewModel.Mode.ALL;
        String prefMode = preferences.getString(KEY_MODE);

        if (prefMode != null && !prefMode.isEmpty())
        {
            try
            {
                mode = PaymentsViewModel.Mode.valueOf(prefMode);
            }
            catch (Exception ignore)
            {
                // use default mode
            }
        }

        boolean useGrossValue = preferences.getBoolean(KEY_USE_GROSS_VALUE);
        boolean useConsolidateRetired = preferences.getBoolean(KEY_USE_CONSOLIDATE_RETIRED);

        return new PaymentsViewInput(tab, year, clientFilterId, mode, useGrossValue, useConsolidateRetired);
    }

    public void writeToPreferences(IPreferenceStore preferences, Client client)
    {
        preferences.setValue(KEY_TAB, tab);
        preferences.setValue(KEY_YEAR, year);
        preferences.setValue(KEY_MODE, mode.name());
        preferences.setValue(KEY_USE_GROSS_VALUE, useGrossValue);
        preferences.setValue(KEY_USE_CONSOLIDATE_RETIRED, useConsolidateRetired);

        ClientFilterMenu.saveSelectedFilter(client, KEY_USED_FILTER, clientFilterId.orElse("")); //$NON-NLS-1$
    }
}
