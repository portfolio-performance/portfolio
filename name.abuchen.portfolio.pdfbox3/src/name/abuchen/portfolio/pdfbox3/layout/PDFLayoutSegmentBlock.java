package name.abuchen.portfolio.pdfbox3.layout;

import java.util.List;

public record PDFLayoutSegmentBlock(String id, int page, String pattern, List<PDFLayoutSegmentRow> rows)
{
    public int rowCount()
    {
        return rows.size();
    }

    public float yStart()
    {
        return rows.isEmpty() ? 0f : rows.getFirst().y();
    }

    public float yEnd()
    {
        return rows.isEmpty() ? 0f : rows.getLast().y();
    }
}
