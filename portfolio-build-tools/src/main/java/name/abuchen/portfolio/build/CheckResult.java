package name.abuchen.portfolio.build;

import java.util.List;

record CheckResult(List<Violation> violations, int rewrittenFiles)
{
    boolean hasViolations()
    {
        return !violations.isEmpty();
    }
}
