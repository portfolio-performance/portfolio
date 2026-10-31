package name.abuchen.portfolio.cli.apply;

import java.util.List;
import java.util.stream.Stream;

import name.abuchen.portfolio.cli.command.OperationSpec;
import name.abuchen.portfolio.cli.masterdata.CashAccounts;
import name.abuchen.portfolio.cli.masterdata.Instruments;
import name.abuchen.portfolio.cli.masterdata.InvestmentAccounts;
import name.abuchen.portfolio.cli.transaction.TransactionRecords;

/**
 * The single list of write operations, applied by the <code>apply</code>
 * command. An operation declared outside <code>apply</code> handles an
 * {@link OperationSpec.Context}, which the batch's {@link OperationContext}
 * is.
 */
public final class Operations
{
    public static final List<OperationSpec<? super OperationContext>> ALL = Stream
                    .<List<? extends OperationSpec<? super OperationContext>>> of(Instruments.INSTRUMENTS.operations(),
                                    CashAccounts.CASH_ACCOUNTS.operations(),
                                    InvestmentAccounts.INVESTMENT_ACCOUNTS.operations(),
                                    List.of(TransactionRecords.ADD, TransactionRecords.UPDATE,
                                                    TransactionRecords.DELETE),
                                    TaxonomyOperations.ALL)
                    .<OperationSpec<? super OperationContext>> flatMap(List::stream).toList();

    private Operations()
    {
    }
}
