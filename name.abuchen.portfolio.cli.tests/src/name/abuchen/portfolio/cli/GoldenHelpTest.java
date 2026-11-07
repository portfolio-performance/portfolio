package name.abuchen.portfolio.cli;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

import name.abuchen.portfolio.cli.apply.Operations;

/**
 * The help contract guard: every help text an agent can ask for is compared
 * byte for byte with a checked-in golden file under <code>golden/help</code>.
 * A change to help is a contract change, so the golden file is edited by hand
 * along with it; there is deliberately no switch to regenerate them.
 */
public class GoldenHelpTest
{
    private static final String GOLDEN = "golden/help/";
    private static final int CONTEXT = 3;

    @Test
    public void everyHelpTextMatchesItsGoldenFile()
    {
        var mismatches = new ArrayList<String>();
        for (var entry : helpTexts().entrySet())
        {
            var expected = golden(entry.getKey());
            var actual = entry.getValue();
            if (expected == null)
                mismatches.add("missing golden file " + GOLDEN + entry.getKey());
            else if (!expected.equals(actual))
                mismatches.add(unifiedDiff(entry.getKey(), expected, actual));
        }

        if (!mismatches.isEmpty())
            fail("help differs from its golden file:\n" + String.join("\n", mismatches));
    }

    /** The golden file name of each help text, in a stable order. */
    private static Map<String, String> helpTexts()
    {
        var texts = new LinkedHashMap<String, String>();
        texts.put("global.txt", CliRun.run("--help").stdout);
        for (var command : Commands.ALL)
            texts.put(command.name() + ".txt", CliRun.run(command.name(), "--help").stdout);
        for (var operation : Operations.ALL)
            texts.put("apply/" + operation.name() + ".txt",
                            CliRun.run("apply", "--help", operation.name()).stdout);
        return texts;
    }

    private static String golden(String name)
    {
        try (var in = GoldenHelpTest.class.getResourceAsStream(GOLDEN + name))
        {
            return in == null ? null : new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    /** A unified diff of two texts, with three lines of context per hunk. */
    private static String unifiedDiff(String name, String expected, String actual)
    {
        var a = expected.split("\n", -1);
        var b = actual.split("\n", -1);

        // longest common subsequence lengths of the suffixes
        var lcs = new int[a.length + 1][b.length + 1];
        for (var i = a.length - 1; i >= 0; i--)
            for (var j = b.length - 1; j >= 0; j--)
                lcs[i][j] = a[i].equals(b[j]) ? lcs[i + 1][j + 1] + 1 : Math.max(lcs[i + 1][j], lcs[i][j + 1]);

        // the edit script: ' ', '-' or '+' with the line and its positions
        record Edit(char kind, String line, int ai, int bi)
        {
        }
        var edits = new ArrayList<Edit>();
        int i = 0;
        int j = 0;
        while (i < a.length || j < b.length)
        {
            if (i < a.length && j < b.length && a[i].equals(b[j]))
                edits.add(new Edit(' ', a[i], i++, j++));
            else if (i < a.length && (j == b.length || lcs[i + 1][j] >= lcs[i][j + 1]))
                edits.add(new Edit('-', a[i], i++, j));
            else
                edits.add(new Edit('+', b[j], i, j++));
        }

        var out = new StringBuilder();
        out.append("--- ").append(GOLDEN).append(name).append('\n');
        out.append("+++ actual ").append(name).append('\n');

        var k = 0;
        while (k < edits.size())
        {
            if (edits.get(k).kind() == ' ')
            {
                k++;
                continue;
            }

            // a hunk joins changes that are at most 2 * CONTEXT lines apart
            var last = k;
            for (var n = k + 1; n < edits.size() && n - last <= 2 * CONTEXT + 1; n++)
                if (edits.get(n).kind() != ' ')
                    last = n;

            var hunk = edits.subList(Math.max(0, k - CONTEXT), Math.min(edits.size(), last + CONTEXT + 1));
            var aCount = hunk.stream().filter(e -> e.kind() != '+').count();
            var bCount = hunk.stream().filter(e -> e.kind() != '-').count();
            out.append("@@ -").append(hunk.get(0).ai() + 1).append(',').append(aCount) //
                            .append(" +").append(hunk.get(0).bi() + 1).append(',').append(bCount).append(" @@\n");
            for (var edit : hunk)
                out.append(edit.kind()).append(edit.line()).append('\n');
            k = last + CONTEXT + 1;
        }
        return out.toString();
    }
}
