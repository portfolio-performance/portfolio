package name.abuchen.portfolio.cli.transaction;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.Test;

/**
 * The input descriptions of the ruled members are hand-written help, so they
 * can drift from the {@link RuleTable}. Every required cell must be named in
 * the description's "Required for ..." clause or its "Of a ..." / "accepted
 * only on ..." clause, and every refused cell in its "refused ..." clause, by
 * "refused otherwise" if the row is named nowhere else, or by an only-clause
 * that leaves the row out.
 */
public class RuleTableProseTest
{
    /** Patterns by which the descriptions name a row; case matters. */
    private static final String TRANSFERS = "(?<!cash |security )transfers";
    private static final String CASH_EVENTS = "cash-account events";

    private static final Map<RuleTable.Row, List<String>> NAMES = Map.ofEntries(
                    Map.entry(RuleTable.Row.DEPOSIT, List.of(CASH_EVENTS, "deposits")),
                    Map.entry(RuleTable.Row.REMOVAL, List.of(CASH_EVENTS, "removals")),
                    Map.entry(RuleTable.Row.INTEREST, List.of(CASH_EVENTS, "interest", "INTEREST")),
                    Map.entry(RuleTable.Row.INTEREST_CHARGE, List.of(CASH_EVENTS, "interest")),
                    Map.entry(RuleTable.Row.DIVIDENDS, List.of(CASH_EVENTS, "DIVIDENDS", "dividend")),
                    Map.entry(RuleTable.Row.FEES, List.of(CASH_EVENTS, "fees")),
                    Map.entry(RuleTable.Row.FEES_REFUND, List.of(CASH_EVENTS, "fees")),
                    Map.entry(RuleTable.Row.TAXES, List.of(CASH_EVENTS, "taxes")),
                    Map.entry(RuleTable.Row.TAX_REFUND, List.of(CASH_EVENTS, "taxes")),
                    Map.entry(RuleTable.Row.BUY, List.of("BUY")), //
                    Map.entry(RuleTable.Row.SELL, List.of("SELL")),
                    Map.entry(RuleTable.Row.DELIVERY_INBOUND, List.of("deliveries")),
                    Map.entry(RuleTable.Row.DELIVERY_OUTBOUND, List.of("deliveries")),
                    Map.entry(RuleTable.Row.CASH_TRANSFER, List.of("TRANSFER", "cash transfer", TRANSFERS)),
                    Map.entry(RuleTable.Row.SECURITY_TRANSFER, List.of("TRANSFER", "security transfer", TRANSFERS)));

    private enum Clause
    {
        REQUIRED, REFUSED, ONLY, OTHER
    }

    private record Segment(Clause clause, String text)
    {
    }

    private static List<Segment> segments(String description)
    {
        return Arrays.stream(description.split("[;:.]")).map(String::trim).filter(s -> !s.isEmpty()).map(text -> {
            var lower = text.toLowerCase(Locale.ROOT);
            if (lower.startsWith("required for"))
                return new Segment(Clause.REQUIRED, text);
            if (lower.startsWith("refused"))
                return new Segment(Clause.REFUSED, text);
            if (lower.startsWith("of a ") || lower.startsWith("accepted only on"))
                return new Segment(Clause.ONLY, text);
            return new Segment(Clause.OTHER, text);
        }).toList();
    }

    private static boolean names(String text, RuleTable.Row row)
    {
        return NAMES.get(row).stream().anyMatch(name -> Pattern.compile(name).matcher(text).find());
    }

    private static boolean namedIn(List<Segment> segments, RuleTable.Row row, Clause clause)
    {
        return segments.stream().anyMatch(s -> s.clause() == clause && names(s.text(), row));
    }

    /** The cells whose verdict the description does not name; empty if it names all of them. */
    static List<String> drift(RuleTable.Ruled member, String description)
    {
        var segments = segments(description);
        var hasOnly = segments.stream().anyMatch(s -> s.clause() == Clause.ONLY);
        var drift = new ArrayList<String>();

        for (var row : RuleTable.Row.values())
        {
            var rule = row.rule(member);
            if (rule == RuleTable.Rule.REQUIRED && !namedIn(segments, row, Clause.REQUIRED)
                            && !namedIn(segments, row, Clause.ONLY))
                drift.add(row + " x " + member.id() + " is required");

            if (rule == RuleTable.Rule.REFUSED)
            {
                var named = namedIn(segments, row, Clause.REFUSED);
                var otherwise = segments.stream().anyMatch(s -> s.clause() == Clause.REFUSED
                                && s.text().toLowerCase(Locale.ROOT).contains("otherwise"))
                                && segments.stream().noneMatch(s -> names(s.text(), row));
                var leftOut = hasOnly && !namedIn(segments, row, Clause.ONLY);
                if (!named && !otherwise && !leftOut)
                    drift.add(row + " x " + member.id() + " is refused");
            }
        }
        return drift;
    }

    @Test
    public void everyRequiredOrRefusedCellIsNamedInItsMembersDescription()
    {
        var drift = new ArrayList<String>();
        for (var member : RuleTable.Ruled.values())
            drift.addAll(drift(member, member.member().input().description()));

        assertThat(drift, empty());
    }

    @Test
    public void aRefusedCellLeftOutOfTheDescriptionIsDrift()
    {
        var description = TransactionShape.CASH_ACCOUNT.uuid().input().description().replace("deliveries and ",
                        "");

        assertThat(drift(RuleTable.Ruled.CASH_ACCOUNT, description).isEmpty(), org.hamcrest.Matchers.is(false));
    }
}
