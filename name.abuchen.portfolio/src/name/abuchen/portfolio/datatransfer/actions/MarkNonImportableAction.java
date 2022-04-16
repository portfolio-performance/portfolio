package name.abuchen.portfolio.datatransfer.actions;

import java.text.MessageFormat;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.Extractor.NonImportableItem;
import name.abuchen.portfolio.datatransfer.ImportAction;
import name.abuchen.portfolio.datatransfer.ImportAction.Status.Code;

public class MarkNonImportableAction implements ImportAction
{
    @Override
    public Status process(NonImportableItem item)
    {
        return new Status(Code.ERROR, MessageFormat.format(Messages.MsgErrorTransactionTypeNotSupported,
                        "\n" + item.getTypeInformation())); //$NON-NLS-1$
    }
}
