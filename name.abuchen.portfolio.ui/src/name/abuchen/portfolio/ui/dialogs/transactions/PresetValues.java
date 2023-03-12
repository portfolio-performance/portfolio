package name.abuchen.portfolio.ui.dialogs.transactions;

import java.time.LocalTime;

public class PresetValues
{
    public enum TimePreset
    {
        MIDNIGHT, NOW
    }

    private static TimePreset timePreset = TimePreset.MIDNIGHT;

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
}
