package name.abuchen.portfolio.cli.apply;

import java.util.List;
import java.util.stream.Stream;

import name.abuchen.portfolio.cli.command.OperationSpec;
import name.abuchen.portfolio.cli.masterdata.CashAccounts;
import name.abuchen.portfolio.cli.masterdata.Instruments;
import name.abuchen.portfolio.cli.masterdata.InvestmentAccounts;
import name.abuchen.portfolio.cli.transaction.TransactionRecords;

/** The single list of write operations, applied by the <code>apply</code> command. */
public final class Operations
{
    public static final List<OperationSpec<OperationContext>> ALL = Stream
                    .of(adapt(Instruments.INSTRUMENTS.operations()), adapt(CashAccounts.CASH_ACCOUNTS.operations()),
                                    adapt(InvestmentAccounts.INVESTMENT_ACCOUNTS.operations()),
                                    adapt(List.of(TransactionRecords.ADD, TransactionRecords.UPDATE,
                                                    TransactionRecords.DELETE)),
                                    TaxonomyOperations.ALL)
                    .flatMap(List::stream).toList();

    private Operations()
    {
    }

    /** Operations declared outside <code>apply</code>, run with the batch's context. */
    static List<OperationSpec<OperationContext>> adapt(List<OperationSpec<OperationSpec.Context>> operations)
    {
        return operations.stream().map(Operations::adapt).toList();
    }

    static OperationSpec<OperationContext> adapt(OperationSpec<OperationSpec.Context> operation)
    {
        return operation.handledBy(operation.handler()::apply);
    }
}
