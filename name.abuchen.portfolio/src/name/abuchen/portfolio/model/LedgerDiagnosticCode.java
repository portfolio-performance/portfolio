package name.abuchen.portfolio.model;

import java.util.Locale;

/**
 * Stable diagnostic identifiers for Ledger messages.
 * Codes are technical identifiers and are not translated.
 */
public enum LedgerDiagnosticCode
{
    LEDGER_CORE_001("CORE", 1), //$NON-NLS-1$
    LEDGER_CORE_002("CORE", 2), //$NON-NLS-1$
    LEDGER_CORE_003("CORE", 3), //$NON-NLS-1$
    LEDGER_CORE_004("CORE", 4), //$NON-NLS-1$
    LEDGER_CORE_005("CORE", 5), //$NON-NLS-1$
    LEDGER_CORE_006("CORE", 6), //$NON-NLS-1$
    LEDGER_CORE_007("CORE", 7), //$NON-NLS-1$
    LEDGER_CORE_008("CORE", 8), //$NON-NLS-1$
    LEDGER_CORE_009("CORE", 9), //$NON-NLS-1$
    LEDGER_CORE_010("CORE", 10), //$NON-NLS-1$
    LEDGER_CORE_011("CORE", 11), //$NON-NLS-1$
    LEDGER_CORE_012("CORE", 12), //$NON-NLS-1$
    LEDGER_CORE_013("CORE", 13), //$NON-NLS-1$
    LEDGER_CORE_014("CORE", 14), //$NON-NLS-1$
    LEDGER_CORE_015("CORE", 15), //$NON-NLS-1$
    LEDGER_CORE_016("CORE", 16), //$NON-NLS-1$
    LEDGER_CORE_017("CORE", 17), //$NON-NLS-1$
    LEDGER_CORE_018("CORE", 18), //$NON-NLS-1$
    LEDGER_CORE_019("CORE", 19), //$NON-NLS-1$
    LEDGER_CORE_020("CORE", 20), //$NON-NLS-1$
    LEDGER_CORE_021("CORE", 21), //$NON-NLS-1$
    LEDGER_CORE_022("CORE", 22), //$NON-NLS-1$
    LEDGER_CORE_023("CORE", 23), //$NON-NLS-1$
    LEDGER_CORE_024("CORE", 24), //$NON-NLS-1$
    LEDGER_CORE_025("CORE", 25), //$NON-NLS-1$
    LEDGER_STRUCT_001("STRUCT", 1), //$NON-NLS-1$
    LEDGER_STRUCT_002("STRUCT", 2), //$NON-NLS-1$
    LEDGER_STRUCT_003("STRUCT", 3), //$NON-NLS-1$
    LEDGER_STRUCT_004("STRUCT", 4), //$NON-NLS-1$
    LEDGER_STRUCT_005("STRUCT", 5), //$NON-NLS-1$
    LEDGER_STRUCT_006("STRUCT", 6), //$NON-NLS-1$
    LEDGER_STRUCT_007("STRUCT", 7), //$NON-NLS-1$
    LEDGER_STRUCT_008("STRUCT", 8), //$NON-NLS-1$
    LEDGER_STRUCT_009("STRUCT", 9), //$NON-NLS-1$
    LEDGER_STRUCT_010("STRUCT", 10), //$NON-NLS-1$
    LEDGER_STRUCT_011("STRUCT", 11), //$NON-NLS-1$
    LEDGER_STRUCT_012("STRUCT", 12), //$NON-NLS-1$
    LEDGER_STRUCT_013("STRUCT", 13), //$NON-NLS-1$
    LEDGER_STRUCT_014("STRUCT", 14), //$NON-NLS-1$
    LEDGER_STRUCT_015("STRUCT", 15), //$NON-NLS-1$
    LEDGER_STRUCT_016("STRUCT", 16), //$NON-NLS-1$
    LEDGER_STRUCT_017("STRUCT", 17), //$NON-NLS-1$
    LEDGER_STRUCT_018("STRUCT", 18), //$NON-NLS-1$
    LEDGER_STRUCT_019("STRUCT", 19), //$NON-NLS-1$
    LEDGER_STRUCT_020("STRUCT", 20), //$NON-NLS-1$
    LEDGER_STRUCT_021("STRUCT", 21), //$NON-NLS-1$
    LEDGER_STRUCT_022("STRUCT", 22), //$NON-NLS-1$
    LEDGER_STRUCT_023("STRUCT", 23), //$NON-NLS-1$
    LEDGER_STRUCT_024("STRUCT", 24), //$NON-NLS-1$
    LEDGER_STRUCT_025("STRUCT", 25), //$NON-NLS-1$
    LEDGER_STRUCT_026("STRUCT", 26), //$NON-NLS-1$
    LEDGER_STRUCT_027("STRUCT", 27), //$NON-NLS-1$
    LEDGER_STRUCT_028("STRUCT", 28), //$NON-NLS-1$
    LEDGER_STRUCT_029("STRUCT", 29), //$NON-NLS-1$
    LEDGER_STRUCT_030("STRUCT", 30), //$NON-NLS-1$
    LEDGER_STRUCT_031("STRUCT", 31), //$NON-NLS-1$
    LEDGER_STRUCT_032("STRUCT", 32), //$NON-NLS-1$
    LEDGER_STRUCT_033("STRUCT", 33), //$NON-NLS-1$
    LEDGER_STRUCT_034("STRUCT", 34), //$NON-NLS-1$
    LEDGER_STRUCT_035("STRUCT", 35), //$NON-NLS-1$
    LEDGER_STRUCT_036("STRUCT", 36), //$NON-NLS-1$
    LEDGER_STRUCT_037("STRUCT", 37), //$NON-NLS-1$
    LEDGER_STRUCT_038("STRUCT", 38), //$NON-NLS-1$
    LEDGER_STRUCT_039("STRUCT", 39), //$NON-NLS-1$
    LEDGER_STRUCT_040("STRUCT", 40), //$NON-NLS-1$
    LEDGER_STRUCT_041("STRUCT", 41), //$NON-NLS-1$
    LEDGER_STRUCT_042("STRUCT", 42), //$NON-NLS-1$
    LEDGER_STRUCT_043("STRUCT", 43), //$NON-NLS-1$
    LEDGER_STRUCT_044("STRUCT", 44), //$NON-NLS-1$
    LEDGER_STRUCT_045("STRUCT", 45), //$NON-NLS-1$
    LEDGER_STRUCT_046("STRUCT", 46), //$NON-NLS-1$
    LEDGER_STRUCT_047("STRUCT", 47), //$NON-NLS-1$
    LEDGER_STRUCT_048("STRUCT", 48), //$NON-NLS-1$
    LEDGER_STRUCT_049("STRUCT", 49), //$NON-NLS-1$
    LEDGER_STRUCT_050("STRUCT", 50), //$NON-NLS-1$
    LEDGER_STRUCT_051("STRUCT", 51), //$NON-NLS-1$
    LEDGER_STRUCT_052("STRUCT", 52), //$NON-NLS-1$
    LEDGER_STRUCT_053("STRUCT", 53), //$NON-NLS-1$
    LEDGER_STRUCT_054("STRUCT", 54), //$NON-NLS-1$
    LEDGER_PROJ_001("PROJ", 1), //$NON-NLS-1$
    LEDGER_PROJ_002("PROJ", 2), //$NON-NLS-1$
    LEDGER_PROJ_003("PROJ", 3), //$NON-NLS-1$
    LEDGER_PROJ_004("PROJ", 4), //$NON-NLS-1$
    LEDGER_PROJ_005("PROJ", 5), //$NON-NLS-1$
    LEDGER_PROJ_006("PROJ", 6), //$NON-NLS-1$
    LEDGER_PERSIST_001("PERSIST", 1), //$NON-NLS-1$
    LEDGER_PERSIST_002("PERSIST", 2), //$NON-NLS-1$
    LEDGER_PERSIST_003("PERSIST", 3), //$NON-NLS-1$
    LEDGER_PERSIST_004("PERSIST", 4), //$NON-NLS-1$
    LEDGER_PERSIST_005("PERSIST", 5), //$NON-NLS-1$
    LEDGER_PERSIST_006("PERSIST", 6), //$NON-NLS-1$
    LEDGER_PERSIST_007("PERSIST", 7), //$NON-NLS-1$
    LEDGER_PERSIST_008("PERSIST", 8), //$NON-NLS-1$
    LEDGER_PERSIST_009("PERSIST", 9), //$NON-NLS-1$
    LEDGER_PERSIST_010("PERSIST", 10), //$NON-NLS-1$
    LEDGER_FOREX_001("FOREX", 1); //$NON-NLS-1$

    private final String group;
    private final String code;

    private LedgerDiagnosticCode(String group, int number)
    {
        this.group = group;
        this.code = "LEDGER-" + group + "-" + String.format(Locale.ROOT, "%03d", number); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    public String getCode()
    {
        return code;
    }

    public String getGroup()
    {
        return group;
    }

    public String prefix()
    {
        return "[" + code + "]"; //$NON-NLS-1$ //$NON-NLS-2$
    }

    public String message(String text)
    {
        return prefix() + " " + text; //$NON-NLS-1$
    }

    @Override
    public String toString()
    {
        return code;
    }
}
