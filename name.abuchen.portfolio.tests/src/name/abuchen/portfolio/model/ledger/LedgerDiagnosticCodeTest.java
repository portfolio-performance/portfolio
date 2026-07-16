package name.abuchen.portfolio.model.ledger;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Test;

import name.abuchen.portfolio.model.LedgerDiagnosticCode;

@SuppressWarnings("nls")
public class LedgerDiagnosticCodeTest
{
    @Test
    public void testCodeTextAndPrefix()
    {
        assertThat(LedgerDiagnosticCode.LEDGER_CORE_001.getGroup(), is("CORE"));
        assertThat(LedgerDiagnosticCode.LEDGER_CORE_001.getCode(), is("LEDGER-CORE-001"));
        assertThat(LedgerDiagnosticCode.LEDGER_CORE_001.prefix(), is("[LEDGER-CORE-001]"));
        assertThat(LedgerDiagnosticCode.LEDGER_CORE_001.toString(), is("LEDGER-CORE-001"));
    }

    @Test
    public void testMessageFormattingKeepsTextSeparate()
    {
        assertThat(LedgerDiagnosticCode.LEDGER_CORE_001.message("Meldung"),
                        is("[LEDGER-CORE-001] Meldung"));
    }

    @Test
    public void testCodesAreUniqueAndSequentialPerGroup()
    {
        var codes = new HashSet<String>();
        var numbersByGroup = new TreeMap<String, Map<Integer, LedgerDiagnosticCode>>();

        for (var code : LedgerDiagnosticCode.values())
        {
            assertTrue("Duplicate diagnostic code: " + code.getCode(), codes.add(code.getCode()));

            var number = number(code);
            assertThat(code.name(), is("LEDGER_" + code.getGroup() + "_" + String.format("%03d", number)));
            assertThat(code.getCode(), is("LEDGER-" + code.getGroup() + "-" + String.format("%03d", number)));

            numbersByGroup.computeIfAbsent(code.getGroup(), group -> new HashMap<>()).put(number, code);
        }

        for (var entry : numbersByGroup.entrySet())
        {
            var expected = 1;

            for (var actual : entry.getValue().keySet().stream().sorted().toList())
            {
                assertThat("Gap in " + entry.getKey(), actual, is(expected));
                expected++;
            }
        }
    }

    private static int number(LedgerDiagnosticCode code)
    {
        return Integer.parseInt(code.name().substring(code.name().lastIndexOf('_') + 1));
    }
}
