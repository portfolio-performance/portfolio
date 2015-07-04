package name.abuchen.portfolio.checks;

import java.time.LocalDate;
import java.util.List;

public interface Issue
{
    LocalDate getDate();

    Object getEntity();

    Long getAmount();

    String getLabel();

    List<QuickFix> getAvailableFixes();
}
