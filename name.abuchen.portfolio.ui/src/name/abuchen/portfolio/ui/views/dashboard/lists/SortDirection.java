package name.abuchen.portfolio.ui.views.dashboard.lists;

import java.time.LocalDate;
import java.util.Comparator;

import name.abuchen.portfolio.ui.Messages;

public enum SortDirection
{
    ASCENDING(Messages.FollowUpWidget_Option_SortingByDateAscending, Comparable::compareTo), //
    DESCENDING(Messages.FollowUpWidget_Option_SortingByDateDescending, (r, l) -> l.compareTo(r));

    private Comparator<LocalDate> comparator;
    private String label;

    private SortDirection(String label, Comparator<LocalDate> comparator)
    {
        this.label = label;
        this.comparator = comparator;
    }

    public Comparator<LocalDate> getComparator()
    {
        return comparator;
    }

    @Override
    public String toString()
    {
        return label;
    }
}
