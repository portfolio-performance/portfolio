package name.abuchen.portfolio.ui.dialogs.transactions;

import java.time.LocalDate;
import java.util.Optional;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;

import com.ibm.icu.text.MessageFormat;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityEvent;
import name.abuchen.portfolio.ui.Messages;

public class StockSplitWarningListener
{
    private TitleAreaDialog dialog;

    public StockSplitWarningListener(TitleAreaDialog dialog)
    {
        this.dialog = dialog;
    }

    public void check(Security security, LocalDate date)
    {
        Optional<SecurityEvent> evt = security.getEvents().stream()
                        .filter(e -> e.getType() == SecurityEvent.Type.STOCK_SPLIT)
                        .filter(e -> e.getDate().isAfter(date)).sorted((l, r) -> r.getDate().compareTo(l.getDate()))
                        .findFirst();

        if (evt.isPresent())
        {
            SecurityEvent event = evt.get();
            dialog.setMessage(MessageFormat.format(Messages.MsgPreviousStockSplit, event.getDate(), event.getDetails()),
                            IMessageProvider.WARNING);
        }
        else
        {
            dialog.setMessage(null);
        }
    }
}
