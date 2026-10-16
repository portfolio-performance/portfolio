package name.abuchen.portfolio.cli;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A failure that is reported to the caller as a problem+json document with a
 * stable machine-readable code and a non-zero exit code.
 */
public class CliException extends Exception
{
    private static final long serialVersionUID = 1L;

    /** Exit code for malformed invocations. */
    static final int EXIT_USAGE = 2;

    /** Exit code for every other failure. */
    static final int EXIT_FAILURE = 1;

    private final String code;
    private final String title;
    private final int status;
    private final int exitCode;
    private final transient Map<String, Object> extensions = new LinkedHashMap<>();

    private CliException(String code, String title, String detail, int status, int exitCode, Throwable cause)
    {
        super(detail, cause);
        this.code = code;
        this.title = title;
        this.status = status;
        this.exitCode = exitCode;
    }

    static CliException usage(String code, String title, String detail)
    {
        return new CliException(code, title, detail, 400, EXIT_USAGE, null);
    }

    static CliException failure(String code, String title, String detail, int status)
    {
        return new CliException(code, title, detail, status, EXIT_FAILURE, null);
    }

    static CliException failure(String code, String title, String detail, int status, Throwable cause)
    {
        return new CliException(code, title, detail, status, EXIT_FAILURE, cause);
    }

    /** Adds a machine-readable field next to the standard problem members. */
    CliException with(String name, Object value)
    {
        extensions.put(name, value);
        return this;
    }

    String getCode()
    {
        return code;
    }

    int getExitCode()
    {
        return exitCode;
    }

    Map<String, Object> toProblem()
    {
        var problem = new LinkedHashMap<String, Object>();
        problem.put("type", "urn:pp-cli:error:" + code);
        problem.put("title", title);
        problem.put("status", status);
        problem.put("code", code);
        problem.put("detail", getMessage());
        problem.putAll(extensions);
        return problem;
    }
}
