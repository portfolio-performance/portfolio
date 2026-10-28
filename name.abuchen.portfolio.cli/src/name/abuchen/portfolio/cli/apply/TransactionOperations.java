package name.abuchen.portfolio.cli.apply;

import java.util.List;

import name.abuchen.portfolio.cli.command.OperationSpec;
import name.abuchen.portfolio.cli.command.Schema;
import name.abuchen.portfolio.cli.transaction.TransactionRecords;
import name.abuchen.portfolio.cli.transaction.TransactionShape;

/**
 * The transaction operations: <code>transaction.add</code>, which
 * {@link TransactionRecords} declares, and <code>transaction.update</code> and
 * <code>transaction.delete</code>, which {@link TransactionUpdates} applies.
 */
public final class TransactionOperations
{
    private static final String UPDATE_OP = "transaction.update";

    private static final String UPDATE_DESCRIPTION = "The fields of a transaction to change, in the shape the transactions output uses. Only the fields present change; null clears an optional field.";

    static final OperationSpec<OperationContext> ADD = new OperationSpec<>(TransactionRecords.ADD.name(),
                    TransactionRecords.ADD.summary(), TransactionRecords.ADD.input(),
                    TransactionRecords.ADD.handler()::apply);

    static final OperationSpec<OperationContext> UPDATE = new OperationSpec<>(UPDATE_OP,
                    "Changes a whole transaction record, found by the UUID of either of its sides. Only the fields present change; null clears an optional field. The patch is overlaid on the current record and then validated exactly like an add, so a partial edit can't bypass validation and nothing is recomputed: fees, taxes and forex units that no longer fit a changed amount or shares are refused with a field-level error. The type, and the kind of a transfer, can't change; delete the record and add a new one. Both sides stay in step and keep their UUIDs. The response is one updated change, with before and after for the changed fields, whose uuid is the record id. A change that would make an investment account's holding of an instrument negative at the end of any date is refused.",
                    TransactionShape.declarePatch(Schema.object(UPDATE_DESCRIPTION)
                                    .property(OperationSpec.OPERATION_MEMBER,
                                                    Schema.enumOf("Operation name.", List.of(UPDATE_OP)))
                                    .property(TransactionUpdates.UUID_MEMBER,
                                                    Schema.string("UUID of either side of the record to change."))),
                    TransactionUpdates::update);

    static final List<OperationSpec<OperationContext>> ALL = List.of(ADD, UPDATE, TransactionUpdates.DELETE);

    private TransactionOperations()
    {
    }
}
