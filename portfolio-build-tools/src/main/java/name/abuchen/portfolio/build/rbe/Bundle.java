package name.abuchen.portfolio.build.rbe;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

public final class Bundle
{
    private String comment = ""; //$NON-NLS-1$
    private final Map<String, BundleEntry> entries = new LinkedHashMap<>();

    public String getComment()
    {
        return comment;
    }

    public void setComment(String comment)
    {
        this.comment = comment == null ? "" : comment; //$NON-NLS-1$
    }

    public BundleEntry getEntry(String key)
    {
        return entries.get(key);
    }

    public void addEntry(BundleEntry entry)
    {
        if (entry == null || entry.getKey().trim().isEmpty())
            return;

        entries.put(entry.getKey(), entry);
    }

    public SortedSet<String> getKeys()
    {
        return new TreeSet<>(entries.keySet());
    }

    public List<String> getInsertionOrderKeys()
    {
        return new ArrayList<>(entries.keySet());
    }
}
