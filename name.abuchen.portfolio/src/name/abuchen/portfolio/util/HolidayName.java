package name.abuchen.portfolio.util;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

/* package */ enum HolidayName
{
    ALL_SOULS_DAY,
    ASCENSION_DAY,
    ASSUMPTION_DAY,
    BERCHTOLDSTAG,
    BOXING_DAY,
    CARNIVAL,
    CHRISTMAS,
    CHRISTMAS_EVE,
    CHRISTMAS_EVE_RUSSIA,
    CIVIC_DAY,
    CORONATION,
    CORPUS_CHRISTI,
    NEW_YEARS_EVE,
    DEFENDER_OF_THE_FATHERLAND_DAY,
    EARLY_MAY_BANK_HOLIDAY,
    EASTER_MONDAY,
    EXTRA_HOLIDAY,
    FAMILY_DAY,
    FIRST_CHRISTMAS_DAY,
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
    PATRON_DAY,
    REFORMATION_DAY,
    REPENTANCE_AND_PRAYER,
    REPUBLIC_PROCLAMATION_DAY,
    ROYAL_JUBILEE,
    ROYAL_WEDDING,
    SAINT_STEPHEN,
    SECOND_CHRISTMAS_DAY,
    TERRORIST_ATTACKS,
    THANKSGIVING,
    TIRADENTES_DAY,
    SPRING_MAY_BANK_HOLIDAY,
    STATE_FUNERAL,
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
