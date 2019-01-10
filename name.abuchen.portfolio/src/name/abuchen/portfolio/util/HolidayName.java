package name.abuchen.portfolio.util;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

/* package */ enum HolidayName
{
    BANK_HOLIDAY,
    BOXING_DAY,
    CHRISTMAS,
    CHRISTMAS_EVE,
    NEW_YEARS_EVE,
    EASTER_MONDAY,
    FIRST_CHRISTMAS_DAY,
    FUNERAL_OF_PRESIDENT_REAGAN,
    GOOD_FRIDAY,
    HURRICANE_SANDY,
    INDEPENDENCE,
    LABOUR_DAY,
    MARTIN_LUTHER_KING,
    MEMORIAL,
    NEW_YEAR,
    REMEMBERANCE_OF_PRESIDENT_FORD,
    SECOND_CHRISTMAS_DAY,
    THANKSGIVING,
    UNIFICATION_GERMANY,
    WASHINGTONS_BIRTHDAY,
    WHIT_MONDAY;

    private static final ResourceBundle RESOURCES = ResourceBundle
                    .getBundle("name.abuchen.portfolio.util.holiday-names"); //$NON-NLS-1$

    @Override
    public String toString()
    {
        try
        {
            return RESOURCES.getString(name());
        }
        catch (MissingResourceException e)
        {
            return name();
        }
    }
}
