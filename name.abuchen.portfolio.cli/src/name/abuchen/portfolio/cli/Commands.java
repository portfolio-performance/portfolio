package name.abuchen.portfolio.cli;

import java.util.List;
import java.util.Optional;

/** The single list of declared commands. */
final class Commands
{
    static final List<CommandSpec> ALL = List.of(InfoCommand.SPEC, DescribeCommand.SPEC);

    /** Write operations; filled in by the apply tickets. */
    static final List<OperationSpec> OPERATIONS = List.of();

    private Commands()
    {
    }

    static Optional<CommandSpec> find(String name)
    {
        return ALL.stream().filter(c -> c.name().equals(name)).findFirst();
    }
}
