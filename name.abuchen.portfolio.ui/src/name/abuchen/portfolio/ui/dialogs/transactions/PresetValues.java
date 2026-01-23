package name.abuchen.portfolio.ui.dialogs.transactions;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicReference;
import com.google.common.annotations.VisibleForTesting;

public class PresetValues
{
    public enum TimePreset
    {
        MIDNIGHT, NOW
    }

    private static TimePreset timePreset = TimePreset.MIDNIGHT;
    private static final AtomicReference<LocalDate> lastTransactionDate = new AtomicReference<>();

    public static void setTimePreset(String timePresetValue)
    {
        PresetValues.timePreset = TimePreset.valueOf(timePresetValue);
    }

    /**
     * Preset time value initially shown in dialogs for new transactions.
     */
    public static LocalTime getTime()
    {
        if (timePreset == TimePreset.NOW)
            return LocalTime.now();
        return LocalTime.MIDNIGHT;
    }

    /**
     * Returns the last transaction date to be used as default for new
     * transaction dialogs.
     */
    public static LocalDate getLastTransactionDate()
    {
        LocalDate date = lastTransactionDate.get();
        return date != null ? date : LocalDate.now();
    }

    public static void setLastTransactionDate(LocalDate date)
    {
        lastTransactionDate.set(date);
    }

    @VisibleForTesting
    /* package */ static void resetLastTransactionDate()
    {
        lastTransactionDate.set(null);
    }
}
