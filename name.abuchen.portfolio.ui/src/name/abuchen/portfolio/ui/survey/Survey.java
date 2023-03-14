package name.abuchen.portfolio.ui.survey;

public class Survey
{
    private static boolean isActive = true;

    private Survey()
    {
    }

    public static boolean isActive()
    {
        return isActive;
    }

    /* package */ static void setActive(boolean isActive)
    {
        Survey.isActive = isActive;
    }
}
