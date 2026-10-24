package name.abuchen.portfolio.cli;

/**
 * The wording that tells an agent that an instrument's currency can be a minor
 * currency unit. Core models such a currency as its own, with a fixed rate to
 * the major one, so the figures are right but a hundred times the major-unit
 * figure. Every field that can be in an instrument's currency appends this to
 * its description.
 */
final class MinorUnits
{
    private static final String CAUTION = " The currency may be a minor currency unit, such as ILA (1/100 ILS) or GBX (1/100 GBP); the figure is then in that unit.";

    /** The value in the reporting currency is the <code>converted</code> member next to it. */
    static final String CONVERTED_NEXT_TO_IT = CAUTION + " The value in the reporting currency is in converted.";

    /** The value in the currency asked for is <code>converted</code>, which only <code>--currency</code> adds. */
    static final String CONVERTED_WITH_CURRENCY_OPTION = CAUTION
                    + " The value in the currency asked for with --currency is in converted.";

    /** The command converts nothing; holdings does. */
    static final String CONVERTED_BY_HOLDINGS = CAUTION
                    + " This command does not convert: holdings has the market value in the reporting currency in converted.";

    private MinorUnits()
    {
    }
}
