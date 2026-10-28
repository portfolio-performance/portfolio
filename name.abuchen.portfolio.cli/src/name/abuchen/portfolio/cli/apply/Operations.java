package name.abuchen.portfolio.cli.apply;

import java.util.List;
import java.util.stream.Stream;

import name.abuchen.portfolio.cli.command.OperationSpec;

/** The single list of write operations, applied by the <code>apply</code> command. */
public final class Operations
{
    public static final List<OperationSpec<OperationContext>> ALL = Stream
                    .of(InstrumentOperations.ALL, AccountOperations.ALL, TransactionOperations.ALL,
                                    TaxonomyOperations.ALL)
                    .flatMap(List::stream).toList();

    private Operations()
    {
    }
}
