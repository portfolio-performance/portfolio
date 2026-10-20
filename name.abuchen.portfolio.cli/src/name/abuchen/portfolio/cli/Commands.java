package name.abuchen.portfolio.cli;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/** The single list of declared commands. */
final class Commands
{
    static final List<CommandSpec> ALL = List.of(InfoCommand.SPEC, DescribeCommand.SPEC, InstrumentsCommand.SPEC,
                    InvestmentAccountsCommand.SPEC, CashAccountsCommand.SPEC, PricesCommand.SPEC,
                    TransactionsCommand.SPEC, HoldingsCommand.SPEC, PerformanceCommand.SPEC, TaxonomiesCommand.SPEC,
                    AllocationCommand.SPEC, ApplyCommand.SPEC);

    /** Write operations, applied by the <code>apply</code> command. */
    static final List<OperationSpec> OPERATIONS = Stream
                    .concat(InstrumentOperations.ALL.stream(), AccountOperations.ALL.stream()).toList();

    private Commands()
    {
    }

    static Optional<CommandSpec> find(String name)
    {
        return ALL.stream().filter(c -> c.name().equals(name)).findFirst();
    }
}
