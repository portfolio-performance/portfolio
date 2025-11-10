package name.abuchen.portfolio.ui.dialogs.transactions;

import java.time.LocalDate;
import java.time.LocalTime;

public class PresetValues
{
    public enum TimePreset
    {
        MIDNIGHT, NOW
    }

    private static TimePreset timePreset = TimePreset.MIDNIGHT;
    private static LocalDate lastTransactionDate = null;

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
     * transaction dialogs. If the stored date is more than 1 year old, returns
     * the current date instead. This 1-year guard prevents accidental entry of
     * multiple transactions way in the past.
     */
    public static LocalDate getLastTransactionDate()
    {
        if (lastTransactionDate != null && !lastTransactionDate.isBefore(LocalDate.now().minusYears(1)))
            return lastTransactionDate;
        return LocalDate.now();
    }

    public static void setLastTransactionDate(LocalDate date)
    {
        PresetValues.lastTransactionDate = date;
    }

    public static void resetLastTransactionDate()
    {
        PresetValues.lastTransactionDate = null;
    }
}
