package name.abuchen.portfolio.rest.internal.mcp;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import name.abuchen.portfolio.rest.internal.ApiException;

/**
 * Every failure the model is allowed to see, in words. The REST API answers
 * correctly and tersely; a chat client needs to know what to <em>do</em>, and
 * several of those answers are ambiguous by construction.
 * <p/>
 * Endpoint-local: unreachability and pairing explanations do not apply here.
 */
@SuppressWarnings("nls")
public final class McpExplain
{
    /** a file as {@code GET /v1/files} reports it */
    /**
     * @param use
     *            the value to send as {@code file}, as {@code GET /v1/files}
     *            reports it
     */
    public record FileSummary(String id, String label, String alias, String use)
    {
    }

    /**
     * What is known about the failed call beyond the problem itself.
     *
     * @param files
     *            the shared files, when they were consulted to disambiguate, or
     *            null when they were not
     * @param fileIsShared
     *            whether the addressed file appears in that list, or null when
     *            it was not looked up
     */
    public record Context(String toolName, List<FileSummary> files, Boolean fileIsShared, String fileArgument)
    {
        public static Context of(String toolName)
        {
            return new Context(toolName, null, null, null);
        }
    }

    private McpExplain()
    {
    }

    /**
     * How the available files are offered back. When {@code file} is wrong or
     * missing they are handed over <em>inline</em>, so the model recovers
     * inside the same call instead of spending a turn on pp_list_files.
     */
    public static String describeFiles(List<FileSummary> files)
    {
        if (files == null)
        {
            // Nothing was looked up. Saying "no files are shared" here would be
            // a claim about the user's application that this call never checked.
            return "Which files are shared was not looked up for this call; pp_list_files answers it.";
        }

        if (files.isEmpty())
        {
            // Authorised, but nothing shared. Not an error, and not something
            // the model can fix by trying again with different arguments.
            return "No files are currently shared with this connector. The user has to open a portfolio file in "
                            + "Portfolio Performance and enable it under Preferences → MCP Server & REST API.";
        }

        var rows = new ArrayList<String>();
        for (FileSummary file : files)
        {
            // only `use` is offered: naming both values here would undo what the
            // tool descriptions say about there being one address
            rows.add("- " + file.label() + " — use `" + file.use() + "`");
        }
        return "These files are available:\n" + String.join("\n", rows);
    }

    public static String explainProblem(ApiException problem, Context context)
    {
        // the prose tells a model what to do; the trailer states the same thing
        // as data, so a client can act on it instead of reading between sentences
        return describeProblem(problem, context) + hints(problem);
    }

    private static String describeProblem(ApiException problem, Context context)
    {
        var tool = context.toolName();
        var detail = problem.getDetail() != null ? problem.getDetail() : problem.getMessage();

        switch (problem.getType())
        {
            case "not-found":
                // Every 404 is byte-identical, which defends against
                // enumeration by a token holder - but does not oblige this
                // endpoint to stay confused about its own request.
                if (Boolean.FALSE.equals(context.fileIsShared()))
                {
                    return "Portfolio Performance is not sharing a file called `" + context.fileArgument() + "`, so "
                                    + tool + " cannot reach it.\n\nEither the name is wrong, or the file is open but "
                                    + "not enabled — and only the user can enable it, under Preferences → MCP Server & "
                                    + "REST API.\n\n" + describeFiles(context.files());
                }
                if (Boolean.TRUE.equals(context.fileIsShared()))
                {
                    // the REST detail is the bare title "Not found", which only
                    // repeated what the sentence already says
                    return "The file is shared, so what was not found is the entity inside it"
                                    + (problem.getDetail() != null ? ": " + problem.getDetail() : "")
                                    + ". Each kind of id resolves only in its own tools — an instrument uuid in the "
                                    + "instrument tools, a cash or investment account uuid in the account tools. Check "
                                    + "the id against the matching listing tool before trying again — the same id will "
                                    + "not start working.";
                }
                return "Not found: " + detail;

            case "no-activity-in-period":
                // a 404 meaning the opposite of the rest: the entity exists,
                // and the window is empty
                return "Nothing was held over that period, so there is nothing to report. The instrument exists — it "
                                + "simply had no activity and no holdings in the window. Do not retry with a different "
                                + "id; widen the period instead if that is what the user meant."
                                + (problem.getDetail() != null ? "\n\n" + problem.getDetail() : "");

            case "file-not-open":
                return "That file is enabled but not currently open in Portfolio Performance. Only the user can open "
                                + "it; ask them to, then try again."
                                + (problem.getDetail() != null ? "\n\n" + problem.getDetail() : "");

            case "ambiguous-alias":
                return "The name `" + context.fileArgument() + "` matches more than one shared file, so it is not "
                                + "usable. Use the id instead.\n\n" + describeFiles(context.files());

            case "delete-blocked":
                // not a permissions problem, and no retry helps. The reasons
                // travel as field errors, and what the user has to remove
                // depends on which of them applies.
                return describeDeleteBlocked(problem);

            case "validation":
                return tool + " was rejected as invalid:\n" + describeFieldErrors(problem, detail);

            case "invalid-request":
                return tool + " was called with something the API would not accept:\n"
                                + describeFieldErrors(problem, detail);

            case "user-interaction":
                // Explained, not retried: waiting for the dialog to close
                // would park a request on a user, which is the same argument
                // that refuses an SSE stream.
                return "Portfolio Performance has a dialog open and cannot answer until it is closed"
                                + retryAfter(problem) + ". Ask the user to finish what they are doing in the "
                                + "application, then try again.";

            case "ui-busy":
                // The UI thread did not take the request on within the bounded
                // wait, and the request will never run - or it was a read, which
                // changes nothing. Nobody can be asked to close anything:
                // whatever is slow has to finish.
                return "Portfolio Performance did not answer this request on its UI thread within the bounded wait"
                                + retryAfter(problem) + ", so nothing was changed. Something else is holding "
                                + "that thread - a long-running job or a large file. Try again after a moment; if it "
                                + "keeps failing, say so rather than retrying in a loop.";

            case "ui-request-underway":
                // The request was already running on the UI thread when the
                // wait expired. It will finish, and sending a write again would
                // apply it twice, so this must not read as a clean refusal.
                return "Portfolio Performance was already executing this request on its UI thread when the bounded "
                                + "wait expired, and it will complete - do not send the same call again. Read the file "
                                + "back with the tool that lists or describes what this call changes, and tell the "
                                + "user which state it is in.";

            case "calculation-timeout":
                // asking the same question again takes just as long
                return "Portfolio Performance could not finish this calculation in time"
                                + (problem.getDetail() != null ? " - " + problem.getDetail().toLowerCase(Locale.ROOT) : "")
                                + ". Asking again unchanged will take just as long. Ask a smaller question: a shorter "
                                + "interval, a single instrument instead of all of them, or only open or only closed "
                                + "trades.";

            case "server-busy":
                return "Portfolio Performance is answering too many requests at once, or every calculation slot "
                                + "stayed taken while this call waited for one - calculations that ran out of time keep "
                                + "theirs until they finish" + retryAfter(problem)
                                + ". Send fewer calls in parallel and try again.";

            case "request-too-large":
                return ("That edit was too large for Portfolio Performance to accept. "
                                + (problem.getDetail() != null ? problem.getDetail() : "")).trim();

            case "internal-error":
                return "Portfolio Performance hit an internal error handling " + tool + ": " + detail
                                + ". Nothing the user did caused this, and retrying the same call will probably fail "
                                + "the same way.";

            case "method-not-allowed":
                return tool + " used a method that route does not support. That is a fault in this connector; please "
                                + "report it.";

            default:
                return "Portfolio Performance refused " + tool + " with " + problem.getStatus() + " "
                                + problem.getMessage() + (problem.getDetail() != null ? " " + problem.getDetail() : "");
        }
    }

    /**
     * Names what keeps the instrument alive - its transactions, an investment
     * plan, or both - and only asks the user to remove what is actually there.
     */
    private static String describeDeleteBlocked(ApiException problem)
    {
        var fields = problem.getErrors().stream().map(ApiException.FieldError::field).toList();
        var transactions = fields.contains("transactions");
        var plans = fields.contains("plans");

        String reason;
        String remedy;
        if (transactions && plans)
        {
            reason = "it has transactions and an investment plan uses it";
            remedy = "delete its transactions and remove it from every investment plan";
        }
        else if (plans)
        {
            reason = "an investment plan uses it";
            remedy = "remove it from every investment plan, or delete that plan";
        }
        else if (transactions)
        {
            reason = "it has transactions";
            remedy = "delete the transactions that use it";
        }
        else
        {
            reason = "it is still referenced";
            remedy = "remove what still refers to it";
        }

        return "That instrument cannot be deleted because " + reason + ". The user would have to " + remedy
                        + " first, in Portfolio Performance. Do not try again."
                        + (problem.getDetail() != null ? "\n\n" + problem.getDetail() : "");
    }

    /**
     * Field errors pass through nearly verbatim: {@code errors[]} already names
     * the field, the code and the permitted values, and rewriting it would lose
     * that precision.
     */
    private static String describeFieldErrors(ApiException problem, String fallback)
    {
        if (problem.getErrors().isEmpty())
            return fallback;

        var rows = new ArrayList<String>();
        for (var error : problem.getErrors())
            rows.add("- `" + error.field() + "` (" + error.code() + "): " + error.message());
        return String.join("\n", rows);
    }

    /** the header is the only place this number exists, and "later" with no number is useless */
    private static String retryAfter(ApiException problem)
    {
        var header = problem.getHeaders().get("Retry-After");
        return header == null ? "" : " — try again in about " + header + " seconds";
    }

    /**
     * The machine-readable trailer of a refusal: whether trying again can ever
     * help, and which problem this is.
     * <p/>
     * Retryable means only that the same call may succeed later without changing
     * anything - a dialog closing, a cooldown passing, a file being opened. It is
     * not permission to retry in a loop: the prose above names who has to act.
     * A 404 is never retryable, and neither is a rejection that names a field.
     */
    private static String hints(ApiException problem)
    {
        // a 503 is retryable unless it says the request is still running or
        // would take just as long again
        var notRetryable = Set.of("ui-request-underway", "calculation-timeout");
        var retryable = !notRetryable.contains(problem.getType())
                        && (problem.getStatus() == 423 || problem.getStatus() == 429 || problem.getStatus() == 503
                                        || "file-not-open".equals(problem.getType()));

        var trailer = "\n\nretryable: " + (retryable ? "yes" : "no") + " · reasonCode: " + problem.getType();
        var header = problem.getHeaders().get("Retry-After");
        return header == null ? trailer : trailer + " · retryAfterSeconds: " + header;
    }
}
