package name.abuchen.portfolio.checks;

import java.util.Date;
import java.util.List;

public interface Issue
{
    Date getDate();

    Object getEntity();

    Long getAmount();

    String getLabel();

    List<QuickFix> getAvailableFixes();
}
