package name.abuchen.portfolio.rest.internal.mcp;

import java.util.ArrayList;
import java.util.List;

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
    public record FileSummary(String id, String label, String alias)
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
        if (files == null || files.isEmpty())
        {
            // Authorised, but nothing shared. Not an error, and not something
            // the model can fix by trying again with different arguments.
            return "No files are currently shared with this connector. The user has to open a portfolio file in "
                            + "Portfolio Performance and enable it under Preferences → MCP Server & REST API.";
        }

        var rows = new ArrayList<String>();
        for (FileSummary file : files)
        {
            rows.add(file.alias() != null ? "- " + file.label() + " — use `" + file.alias() + "` or `" + file.id() + "`"
                            : "- " + file.label() + " — use `" + file.id() + "`");
        }
        return "These files are available:\n" + String.join("\n", rows);
    }

    public static String explainProblem(ApiException problem, Context context)
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
                    return "The file is shared, so what was not found is the entity inside it: " + detail
                                    + ". Check the id against a listing tool before trying again — the same id will "
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
                // not a permissions problem, and no retry helps
                return ("That instrument cannot be deleted while it is still referenced. "
                                + (problem.getDetail() != null ? problem.getDetail() : "")
                                + "\n\nThe user would have to remove the transactions that use it first, in Portfolio "
                                + "Performance. Do not try again.").trim();

            case "validation":
                return tool + " was rejected as invalid:\n" + describeFieldErrors(problem, detail);

            case "invalid-request":
                return tool + " was called with something the API would not accept:\n"
                                + describeFieldErrors(problem, detail);

            case "user-interaction":
                // Explained, not retried: waiting for the dialog to close
                // would park one of RestApiServer's two worker threads on a
                // user, which is the same argument that refuses an SSE stream.
                return "Portfolio Performance has a dialog open and cannot answer until it is closed"
                                + retryAfter(problem) + ". Ask the user to finish what they are doing in the "
                                + "application, then try again.";

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
        return header == null ? "" : " — it asked to be left for " + header + " seconds";
    }
}
