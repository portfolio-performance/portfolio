package name.abuchen.portfolio.ui.dialogs.transactions;

import java.time.LocalDate;
import java.util.Optional;

import com.ibm.icu.text.MessageFormat;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityEvent;
import name.abuchen.portfolio.ui.Messages;

public class StockSplitWarning
{
    public String check(Security security, LocalDate date)
    {
        Optional<SecurityEvent> evt = security.getEvents().stream()
                        .filter(e -> e.getType() == SecurityEvent.Type.STOCK_SPLIT)
                        .filter(e -> e.getDate().isAfter(date)).sorted((l, r) -> r.getDate().compareTo(l.getDate()))
                        .findFirst();

        if (evt.isPresent())
        {
            SecurityEvent event = evt.get();
            return MessageFormat.format(Messages.MsgPreviousStockSplit, event.getDate(), event.getRatio());
        }
        else
        {
            return null;
        }
    }
}
