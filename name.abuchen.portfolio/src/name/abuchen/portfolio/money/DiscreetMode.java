package name.abuchen.portfolio.money;

public class DiscreetMode
{
    public static final String HIDDEN_AMOUNT = "***"; //$NON-NLS-1$

    /**
     * Indicates whether the discreet mode is active, i.e., no absolute amounts
     * are displayed directly. Set and read only from main thread.
     */
    private static boolean isActive = false;

    private DiscreetMode()
    {
    }

    public static boolean isActive()
    {
        return isActive;
    }

    public static void setActive(boolean active)
    {
        isActive = active;
    }
}
