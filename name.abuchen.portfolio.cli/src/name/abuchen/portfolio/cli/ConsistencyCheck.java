package name.abuchen.portfolio.cli;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import name.abuchen.portfolio.checks.Checker;
import name.abuchen.portfolio.checks.Issue;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;

/**
 * The no-new-issues rule: core's consistency checker runs on the client as it
 * was and as it would be written, and the write is refused if the change
 * introduces an issue that wasn't there before.
 */
final class ConsistencyCheck
{
    static final WarningSpec PRE_EXISTING = new WarningSpec("consistency-issues-preexisting",
                    "Core's consistency checker found issues in the file that the change did not cause, listed in issues; the write went ahead.");

    /**
     * An issue as reported by core. Core's issues have no identity that
     * survives a reload, so two are the same if their check (the kind of
     * issue) and entity are; an entity with more issues of a kind than before
     * has a new one. The message is for the reader and not compared, because
     * it contains names that a change may legitimately edit.
     */
    record Finding(String check, String entity, String message)
    {
        private List<Object> key()
        {
            return List.of(check, entity);
        }

        Map<String, Object> toJson()
        {
            var json = new LinkedHashMap<String, Object>();
            json.put("check", check);
            json.put("entity", entity);
            json.put("message", message);
            return json;
        }
    }

    private ConsistencyCheck()
    {
    }

    /**
     * Runs core's checker. Eight of its checks repair the model while they
     * run, so the client must be a scratch copy that is never written.
     */
    static List<Finding> run(Client scratch)
    {
        var findings = new ArrayList<Finding>();
        for (Issue issue : Checker.runAll(scratch))
            findings.add(new Finding(issue.getClass().getSimpleName(), entityOf(issue.getEntity()), issue.getLabel()));
        return findings;
    }

    /**
     * @return the warning about the issues that were already there, or none
     * @throws CliException
     *             <code>consistency-check-failed</code> if there is any other
     *             issue
     */
    static List<Warning> verify(List<Finding> before, List<Finding> after) throws CliException
    {
        var known = new HashMap<List<Object>, Integer>();
        before.forEach(finding -> known.merge(finding.key(), 1, Integer::sum));

        var introduced = new ArrayList<Finding>();
        var preExisting = new ArrayList<Finding>();
        for (var finding : after)
        {
            if (known.merge(finding.key(), -1, Integer::sum) >= 0)
                preExisting.add(finding);
            else
                introduced.add(finding);
        }

        if (!introduced.isEmpty())
            throw CliException.failure("consistency-check-failed", "Change would make the file inconsistent",
                            "The change introduces " + introduced.size()
                                            + " consistency issue(s) that core's checker did not find before. Nothing was written.",
                            422).with("issues", introduced.stream().map(Finding::toJson).toList());

        if (preExisting.isEmpty())
            return List.of();

        return List.of(new Warning(PRE_EXISTING.code(),
                        preExisting.size() + " consistency issue(s) already in the file, not caused by this change.")
                                        .with("count", preExisting.size())
                                        .with("issues", preExisting.stream().map(Finding::toJson).toList()));
    }

    private static String entityOf(Object entity)
    {
        return switch (entity)
        {
            case null -> "none";
            case Client c -> "client";
            case Account a -> "account:" + a.getUUID();
            case Portfolio p -> "portfolio:" + p.getUUID();
            case Security s -> "security:" + s.getUUID();
            case Transaction t -> "transaction:" + t.getUUID();
            default -> entity.getClass().getSimpleName();
        };
    }
}
