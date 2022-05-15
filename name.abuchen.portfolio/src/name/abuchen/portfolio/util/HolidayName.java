package name.abuchen.portfolio.util;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

/* package */ enum HolidayName
{
    ASCENSION_DAY,
    ASSUMPTION_DAY,
    BERCHTOLDSTAG,
    BOXING_DAY,
    CHRISTMAS,
    CHRISTMAS_EVE,
    CHRISTMAS_EVE_RUSSIA,
    CIVIC_DAY,
    CORPUS_CHRISTI,
    NEW_YEARS_EVE,
    DEFENDER_OF_THE_FATHERLAND_DAY,
    EARLY_MAY_BANK_HOLIDAY,
    EASTER_MONDAY,
    FAMILY_DAY,
    FIRST_CHRISTMAS_DAY,
    FUNERAL_OF_PRESIDENT_NIXON,
    FUNERAL_OF_PRESIDENT_REAGAN,
    GOOD_FRIDAY,
    HURRICANE_SANDY,
    INDEPENDENCE,
    INTERNATION_WOMENS_DAY,
    JUNETEENTH,
    LABOUR_DAY,
    MARTIN_LUTHER_KING,
    MEMORIAL,
    MILLENNIUM,
    NEW_YEAR,
    NEW_YEAR_HOLIDAY,
    NATION_DAY,
    REFORMATION_DAY,
    REMEMBERANCE_OF_PRESIDENT_FORD,
    REPENTANCE_AND_PRAYER,
    ROYAL_JUBILEE,
    ROYAL_WEDDING,
    SAINT_STEPHEN,
    SECOND_CHRISTMAS_DAY,
    TERRORIST_ATTACKS,
    THANKSGIVING,
    SPRING_MAY_BANK_HOLIDAY,
    SUMMER_BANK_HOLIDAY,
    UNIFICATION_GERMANY,
    UNITY_DAY,
    VICTORIA_DAY,
    VICTORY_DAY,
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
