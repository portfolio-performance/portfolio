package name.abuchen.portfolio.pdfbox3.layout;

public record PDFLayoutSegment(int page, float xStart, float xEnd, float y, String text)
{
    public int xBucket()
    {
        return ((int) Math.floor(xStart / 10f)) * 10;
    }
}
