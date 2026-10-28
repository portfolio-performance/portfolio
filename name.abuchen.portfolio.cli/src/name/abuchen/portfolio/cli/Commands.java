package name.abuchen.portfolio.cli;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import name.abuchen.portfolio.cli.apply.AccountOperations;
import name.abuchen.portfolio.cli.apply.ApplyCommand;
import name.abuchen.portfolio.cli.apply.InstrumentOperations;
import name.abuchen.portfolio.cli.apply.OperationSpec;
import name.abuchen.portfolio.cli.apply.RestoreCommand;
import name.abuchen.portfolio.cli.apply.TaxonomyOperations;
import name.abuchen.portfolio.cli.apply.TransactionOperations;
import name.abuchen.portfolio.cli.command.CliException;
import name.abuchen.portfolio.cli.command.CommandSpec;
import name.abuchen.portfolio.cli.command.ErrorCode;
import name.abuchen.portfolio.cli.performance.PerformanceCommand;
import name.abuchen.portfolio.cli.query.AllocationCommand;
import name.abuchen.portfolio.cli.query.CashAccountsCommand;
import name.abuchen.portfolio.cli.query.HoldingsCommand;
import name.abuchen.portfolio.cli.query.InfoCommand;
import name.abuchen.portfolio.cli.query.InstrumentsCommand;
import name.abuchen.portfolio.cli.query.InvestmentAccountsCommand;
import name.abuchen.portfolio.cli.query.PricesCommand;
import name.abuchen.portfolio.cli.query.TaxonomiesCommand;
import name.abuchen.portfolio.cli.query.TransactionsCommand;

/** The single list of declared commands. */
public final class Commands
{
    public static final List<CommandSpec> ALL = List.of(InfoCommand.SPEC, InstrumentsCommand.SPEC,
                    InvestmentAccountsCommand.SPEC, CashAccountsCommand.SPEC, PricesCommand.SPEC,
                    TransactionsCommand.SPEC, HoldingsCommand.SPEC, PerformanceCommand.SPEC, TaxonomiesCommand.SPEC,
                    AllocationCommand.SPEC, ApplyCommand.SPEC, RestoreCommand.SPEC);

    /** Write operations, applied by the <code>apply</code> command. */
    public static final List<OperationSpec> OPERATIONS = Stream.of(InstrumentOperations.ALL, AccountOperations.ALL,
                    TransactionOperations.ALL, TaxonomyOperations.ALL).flatMap(List::stream).toList();

    private Commands()
    {
    }

    static Optional<CommandSpec> find(String name)
    {
        return ALL.stream().filter(c -> c.name().equals(name)).findFirst();
    }

    /**
     * @throws CliException
     *             <code>unknown-command</code> if there is no such command
     */
    static CommandSpec require(String name) throws CliException
    {
        return find(name).orElseThrow(() -> CliException.usage(ErrorCode.UNKNOWN_COMMAND, "Unknown command",
                        "Unknown command '" + name + "'. Known commands: "
                                        + String.join(", ", ALL.stream().map(CommandSpec::name).toList())));
    }
}
