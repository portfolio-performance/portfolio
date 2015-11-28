package name.abuchen.portfolio.ui.wizards.datatransfer;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.Extractor.Item;

public class ExtractedEntry
{
    private final Extractor.Item item;

    private boolean isImported = true;
    private boolean isDuplicate = false;

    public ExtractedEntry(Item item)
    {
        this.item = item;
    }

    public Extractor.Item getItem()
    {
        return item;
    }

    public boolean isImported()
    {
        return isImported;
    }

    public void setImported(boolean isImported)
    {
        this.isImported = isImported;
    }

    public boolean isDuplicate()
    {
        return isDuplicate;
    }

    public void setDuplicate(boolean isDuplicate)
    {
        this.isDuplicate = isDuplicate;
    }
}