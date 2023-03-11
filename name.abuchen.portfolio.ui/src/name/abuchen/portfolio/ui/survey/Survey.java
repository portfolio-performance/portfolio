package name.abuchen.portfolio.ui.survey;

public class Survey
{
    private static boolean isActice = true;

    private Survey()
    {
    }

    public static boolean isActive()
    {
        return isActice;
    }

    /* package */ static void setActive(boolean isActive)
    {
        isActice = isActive;
    }
}
