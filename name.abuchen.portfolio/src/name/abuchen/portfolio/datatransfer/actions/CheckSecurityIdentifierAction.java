package name.abuchen.portfolio.datatransfer.actions;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.ImportAction;
import name.abuchen.portfolio.model.Security;

/**
 * Warns if a new security is created without any identifier (ISIN, WKN,
 * ticker symbol). Such securities can only be matched by name, therefore the
 * user should check manually whether the security already exists.
 */
public class CheckSecurityIdentifierAction implements ImportAction
{
    @Override
    public Status process(Security security)
    {
        if (isEmpty(security.getIsin()) && isEmpty(security.getWkn()) && isEmpty(security.getTickerSymbol()))
            return new Status(Status.Code.WARNING, Messages.MsgCheckSecurityWithoutIdentifier);

        return Status.OK_STATUS;
    }

    private static boolean isEmpty(String value)
    {
        return value == null || value.isBlank();
    }
}
