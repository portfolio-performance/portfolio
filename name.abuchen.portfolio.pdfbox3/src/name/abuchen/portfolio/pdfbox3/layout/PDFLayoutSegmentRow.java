package name.abuchen.portfolio.pdfbox3.layout;

import java.util.List;
import java.util.stream.Collectors;

public record PDFLayoutSegmentRow(int page, int index, float y, List<PDFLayoutSegment> segments)
{
    public String pattern()
    {
        return segments.stream().map(segment -> Integer.toString(segment.xBucket()))
                        .collect(Collectors.joining(",", "[", "]"));
    }

    public String text()
    {
        return segments.stream().map(PDFLayoutSegment::text).collect(Collectors.joining(" "));
    }

    public boolean isEmpty()
    {
        return segments.isEmpty();
    }
}
