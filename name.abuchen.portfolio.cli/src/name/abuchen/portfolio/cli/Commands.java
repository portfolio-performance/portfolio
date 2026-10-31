package name.abuchen.portfolio.cli;

import java.util.List;
import java.util.Optional;

import name.abuchen.portfolio.cli.apply.ApplyCommand;
import name.abuchen.portfolio.cli.apply.RestoreCommand;
import name.abuchen.portfolio.cli.command.CliException;
import name.abuchen.portfolio.cli.command.CommandSpec;
import name.abuchen.portfolio.cli.command.ErrorCode;
import name.abuchen.portfolio.cli.masterdata.CashAccounts;
import name.abuchen.portfolio.cli.masterdata.Instruments;
import name.abuchen.portfolio.cli.masterdata.InvestmentAccounts;
import name.abuchen.portfolio.cli.performance.PerformanceCommand;
import name.abuchen.portfolio.cli.query.AllocationCommand;
import name.abuchen.portfolio.cli.query.HoldingsCommand;
import name.abuchen.portfolio.cli.query.InfoCommand;
import name.abuchen.portfolio.cli.query.PricesCommand;
import name.abuchen.portfolio.cli.query.TaxonomiesCommand;
import name.abuchen.portfolio.cli.query.TransactionsCommand;

/** The single list of declared commands. */
public final class Commands
{
    public static final List<CommandSpec> ALL = List.of(InfoCommand.SPEC, Instruments.INSTRUMENTS.command(),
                    InvestmentAccounts.INVESTMENT_ACCOUNTS.command(), CashAccounts.CASH_ACCOUNTS.command(),
                    PricesCommand.SPEC, TransactionsCommand.SPEC, HoldingsCommand.SPEC, PerformanceCommand.SPEC,
                    TaxonomiesCommand.SPEC, AllocationCommand.SPEC, ApplyCommand.SPEC, RestoreCommand.SPEC);

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
