package name.abuchen.portfolio.util;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

/* package */ enum HolidayName
{
    ALL_SAINTS_DAY,
    ALL_SOULS_DAY,
    ANZAC_DAY,
    ASCENSION_DAY,
    ASSUMPTION_DAY,
    AUSTRALIA_DAY,
    BERCHTOLDSTAG,
    BOXING_DAY,
    CARNIVAL,
    CHILE,
    CHILE_ARMY,
    CHILE_EXTRA,
    CHILE_NAVY_DAY,
    CHRISTMAS,
    CHRISTMAS_EVE,
    CHRISTMAS_EVE_RUSSIA,
    CIVIC_DAY,
    COLUMBUS_DAY,
    CORONATION,
    CORPUS_CHRISTI,
    DEFENDER_OF_THE_FATHERLAND_DAY,
    EARLY_MAY_BANK_HOLIDAY,
    EASTER_MONDAY,
    EXTRA_HOLIDAY,
    FAMILY_DAY,
    FIRST_CHRISTMAS_DAY,
    GOOD_FRIDAY,
    HURRICANE_SANDY,
    INDEPENDENCE,
    INDIGENOUS_PEOPLE,
    INMACULATE_CONCEPTION,
    INTERNATION_WOMENS_DAY,
    JUNETEENTH,
    KINGS_BIRTHDAY,
    LABOUR_DAY,
    MARTIN_LUTHER_KING,
    MEMORIAL,
    MILLENNIUM,
    NATION_DAY,
    NEW_YEAR,
    NEW_YEARS_EVE,
    NEW_YEAR_HOLIDAY,
    PATRON_DAY,
    REFORMATION_DAY,
    REPENTANCE_AND_PRAYER,
    REPUBLIC_PROCLAMATION_DAY,
    ROYAL_JUBILEE,
    ROYAL_WEDDING,
    SAINT_STEPHEN,
    SAINT_PETER_PAUL,
    SECOND_CHRISTMAS_DAY,
    SPRING_MAY_BANK_HOLIDAY,
    STATE_FUNERAL,
    SUMMER_BANK_HOLIDAY,
    TERRORIST_ATTACKS,
    THANKSGIVING,
    TIRADENTES_DAY,
    UNIFICATION_GERMANY,
    UNITY_DAY,
    VICTORIA_DAY,
    VICTORY_DAY,
    VIRGIN_OF_CARMEN,
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
