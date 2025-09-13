package name.abuchen.portfolio.online.impl.TLVMarket.jsondata;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class FundHistory
{

    // private static DateTimeFormatter formatter =
    // DateTimeFormatter.ofPattern("dd/MM/yyyy"); //$NON-NLS-1$

    public FundHistoryEntry[] Table;
    public int Total;
    private LocalDateTime StartDate;
    private LocalDateTime EndDate;

    public LocalDate getDateFrom()
    {
        return StartDate.toLocalDate();
    }

    public void setDateFrom(LocalDateTime startDate)
    {
        StartDate = startDate;
    }

    public LocalDate getDateTo()
    {
        return EndDate.toLocalDate();
    }

    public void setDateTo(LocalDateTime endDate)
    {
        EndDate = endDate;
    }

    public void setTotalRecs(int total)
    {
        Total = total;
    }

    public int getTotalRecs()
    {
        return Total;
    }

    public FundHistoryEntry[] getItems()
    {
        return Table;
    }

    public void setItems(FundHistoryEntry[] items)
    {
        Table = items;
    }

    public static FundHistory fromMap(Map<String, Object> map)
    {
        FundHistory historyentry = new FundHistory();

        if (map.containsKey("StartDate")) //$NON-NLS-1$
        {
            historyentry.setDateFrom(LocalDateTime.parse((String) map.get("StartDate"))); //$NON-NLS-1$
        }
        if (map.containsKey("EndDate")) //$NON-NLS-1$
        {
            historyentry.setDateTo(LocalDateTime.parse((String) map.get("EndDate"))); //$NON-NLS-1$
        }
        if (map.containsKey("Table")) //$NON-NLS-1$
        {

            List<Map<String, Object>> rawItems = (List<Map<String, Object>>) map.get("Table"); //$NON-NLS-1$
            FundHistoryEntry[] entries = new FundHistoryEntry[rawItems.size()];
            for (int i = 0; i < rawItems.size(); i++)
            {
                entries[i] = FundHistoryEntry.fromMap(rawItems.get(i));
            }
            historyentry.setItems(entries);
            historyentry.setTotalRecs(entries.length);

        }
        return historyentry;
    }

    @Override
    public String toString()
    {
        return "FundHistory [Table=" + Arrays.toString(Table) + ", Total=" + Total + ", StartDate=" + StartDate //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                        + ", EndDate=" + EndDate + "]"; //$NON-NLS-1$ //$NON-NLS-2$
    }

}
