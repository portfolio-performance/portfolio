package name.abuchen.portfolio.util;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

/* package */ enum TLVHolidayName
{
    // @Formatter:off
    PURIM,
    EREV_PASSOVER,
    PASSOVERI,
    PASSOVERII,
    MEMORIAL_DAY,
    INDEPENDENCE_DAY,
    SAVHUOT_EVE,
    SHAVUOT,
    FAST_DAY,
    NEW_YEAR_EVE,
    NEWYEARI,
    NEWYEARII,
    YOM_KIPUR_EVE,
    YOM_KIPUR,
    SUKKOTH_EVE,
    SUKKOTH,
    SIMCHAT_TORA_EVE,
    SIMCHAT_TORA;
    // @Formatter:on
    
    
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
