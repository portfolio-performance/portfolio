package name.abuchen.portfolio.model.ledger;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

import name.abuchen.portfolio.model.LedgerDiagnosticCode;

@SuppressWarnings("nls")
public class LedgerDiagnosticCodeTest
{
    @Test
    public void testCodeTextAndPrefix()
    {
        assertThat(LedgerDiagnosticCode.LEDGER_CONVERT_001.getGroup(), is("CONVERT"));
        assertThat(LedgerDiagnosticCode.LEDGER_CONVERT_001.getCode(), is("LEDGER-CONVERT-001"));
        assertThat(LedgerDiagnosticCode.LEDGER_CONVERT_002.getCode(), is("LEDGER-CONVERT-002"));
        assertThat(LedgerDiagnosticCode.LEDGER_CONVERT_071.getCode(), is("LEDGER-CONVERT-071"));
        assertThat(LedgerDiagnosticCode.LEDGER_CONVERT_001.prefix(), is("[LEDGER-CONVERT-001]"));
        assertThat(LedgerDiagnosticCode.LEDGER_CONVERT_001.toString(), is("LEDGER-CONVERT-001"));
        assertThat(LedgerDiagnosticCode.LEDGER_CORE_001.getGroup(), is("CORE"));
        assertThat(LedgerDiagnosticCode.LEDGER_CORE_001.getCode(), is("LEDGER-CORE-001"));
        assertThat(LedgerDiagnosticCode.LEDGER_CORE_026.getCode(), is("LEDGER-CORE-026"));
        assertThat(LedgerDiagnosticCode.LEDGER_IMPORT_001.getGroup(), is("IMPORT"));
        assertThat(LedgerDiagnosticCode.LEDGER_IMPORT_001.getCode(), is("LEDGER-IMPORT-001"));
        assertThat(LedgerDiagnosticCode.LEDGER_IMPORT_021.getCode(), is("LEDGER-IMPORT-021"));
        assertThat(LedgerDiagnosticCode.LEDGER_STRUCT_001.getCode(), is("LEDGER-STRUCT-001"));
        assertThat(LedgerDiagnosticCode.LEDGER_STRUCT_055.getCode(), is("LEDGER-STRUCT-055"));
        assertThat(LedgerDiagnosticCode.LEDGER_PROJ_001.getCode(), is("LEDGER-PROJ-001"));
        assertThat(LedgerDiagnosticCode.LEDGER_PROJ_077.getCode(), is("LEDGER-PROJ-077"));
        assertThat(LedgerDiagnosticCode.LEDGER_PERSIST_001.getCode(), is("LEDGER-PERSIST-001"));
        assertThat(LedgerDiagnosticCode.LEDGER_PERSIST_002.getCode(), is("LEDGER-PERSIST-002"));
        assertThat(LedgerDiagnosticCode.LEDGER_PERSIST_010.getCode(), is("LEDGER-PERSIST-010"));
        assertThat(LedgerDiagnosticCode.LEDGER_FOREX_001.getCode(), is("LEDGER-FOREX-001"));
        assertThat(LedgerDiagnosticCode.LEDGER_FOREX_005.getCode(), is("LEDGER-FOREX-005"));
        assertThat(LedgerDiagnosticCode.LEDGER_UI_001.getCode(), is("LEDGER-UI-001"));
        assertThat(LedgerDiagnosticCode.LEDGER_UI_002.getCode(), is("LEDGER-UI-002"));
        assertThat(LedgerDiagnosticCode.LEDGER_UI_020.getCode(), is("LEDGER-UI-020"));
    }

    @Test
    public void testMessageFormattingKeepsTextSeparate()
    {
        assertThat(LedgerDiagnosticCode.LEDGER_UI_001.message("Meldung"),
                        is("[LEDGER-UI-001] Meldung"));
    }
}
