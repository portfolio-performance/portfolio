package name.abuchen.portfolio.pdfbox3.layout;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

public final class PDFLayoutBcbcDebugTextExtractor
{
    public String extract(File file) throws IOException
    {
        try (PDDocument document = Loader.loadPDF(file))
        {
            var stripper = new Stripper();
            stripper.setSortByPosition(true);
            stripper.getText(document);

            return buildDebug(stripper.glyphs);
        }
    }

    public PDFLayoutSegmentDocument extractDocument(File file) throws IOException
    {
        try (PDDocument document = Loader.loadPDF(file))
        {
            var stripper = new Stripper();
            stripper.setSortByPosition(true);
            stripper.getText(document);

            List<PDFLayoutSegmentRow> rows = new ArrayList<>();

            Map<Integer, List<PDFLayoutGlyph>> byPage = groupByPage(stripper.glyphs);

            for (var page : byPage.entrySet())
                rows.addAll(buildSegmentRows(page.getValue()));

            List<PDFLayoutSegmentBlock> blocks = new PDFLayoutSegmentBlockBuilder().build(rows);

            return new PDFLayoutSegmentDocument(List.copyOf(rows), blocks);
        }
    }

    private String buildDebug(List<PDFLayoutGlyph> glyphs)
    {
        StringBuilder out = new StringBuilder();

        Map<Integer, List<PDFLayoutGlyph>> byPage = groupByPage(glyphs);

        for (var page : byPage.entrySet())
        {
            out.append("PAGE ").append(page.getKey()).append('\n');
            out.append("==================================================\n");

            List<PDFLayoutSegmentRow> segmentRows = buildSegmentRows(page.getValue());
            List<PDFLayoutSegmentBlock> blocks = new PDFLayoutSegmentBlockBuilder().build(segmentRows);
            writeBlocks(out, blocks);

            out.append('\n');
        }

        return out.toString();
    }

    private Map<Integer, List<PDFLayoutGlyph>> groupByPage(List<PDFLayoutGlyph> glyphs)
    {
        Map<Integer, List<PDFLayoutGlyph>> answer = new LinkedHashMap<>();

        glyphs.stream().sorted(Comparator.comparing(PDFLayoutGlyph::page).thenComparing(PDFLayoutGlyph::y)
                        .thenComparing(PDFLayoutGlyph::x))
                        .forEach(glyph -> answer.computeIfAbsent(glyph.page(), k -> new ArrayList<>()).add(glyph));

        return answer;
    }

    private List<List<PDFLayoutSegment>> buildRows(List<PDFLayoutGlyph> glyphs)
    {
        Map<Integer, List<PDFLayoutGlyph>> byY = new LinkedHashMap<>();

        glyphs.stream().sorted(Comparator.comparing(PDFLayoutGlyph::y).thenComparing(PDFLayoutGlyph::x))
                        .forEach(glyph -> byY.computeIfAbsent(yBucket(glyph.y()), k -> new ArrayList<>()).add(glyph));

        List<List<PDFLayoutSegment>> rows = new ArrayList<>();

        for (var entry : byY.entrySet())
        {
            List<PDFLayoutGlyph> rowGlyphs = entry.getValue();
            rowGlyphs.sort(Comparator.comparing(PDFLayoutGlyph::x));

            rows.add(buildSegments(rowGlyphs));
        }

        return List.copyOf(rows);
    }

    private List<PDFLayoutSegment> buildSegments(List<PDFLayoutGlyph> glyphs)
    {
        List<PDFLayoutSegment> answer = new ArrayList<>();

        StringBuilder text = new StringBuilder();

        PDFLayoutGlyph first = null;
        PDFLayoutGlyph previous = null;

        for (PDFLayoutGlyph glyph : glyphs)
        {
            if (previous != null && isSegmentGap(previous, glyph))
            {
                addSegment(answer, first, previous, text.toString());

                text.setLength(0);
                first = null;
            }

            if (first == null)
                first = glyph;

            if (previous != null && isWordGap(previous, glyph))
                text.append(' ');

            text.append(glyph.text());
            previous = glyph;
        }

        if (first != null && previous != null && !text.isEmpty())
            addSegment(answer, first, previous, text.toString());

        return List.copyOf(answer);
    }

    private void addSegment(List<PDFLayoutSegment> segments, PDFLayoutGlyph first, PDFLayoutGlyph last, String text)
    {
        if (text == null || text.isBlank())
            return;

        float xStart = first.x();
        float xEnd = last.x() + last.width();

        segments.add(new PDFLayoutSegment(first.page(), xStart, xEnd, first.y(), text));
    }

    private void writeBlocks(StringBuilder out, List<PDFLayoutSegmentBlock> blocks)
    {
        for (PDFLayoutSegmentBlock block : blocks)
        {
            out.append(block.id().toUpperCase(Locale.ROOT)).append('\n');
            out.append("page=").append(block.page()).append('\n');
            out.append("pattern=").append(block.pattern()).append('\n');
            out.append("rows=").append(block.rowCount()).append('\n');
            out.append("y=").append(String.format(Locale.US, "%.2f", block.yStart())).append("-")
                            .append(String.format(Locale.US, "%.2f", block.yEnd())).append('\n');
            out.append("--------------------------------------------------\n");

            for (PDFLayoutSegmentRow row : block.rows())
            {
                out.append("row=").append(row.index()).append(" ");
                out.append("y=").append(String.format(Locale.US, "%.2f", row.y())).append(" | ");

                boolean first = true;
                for (PDFLayoutSegment segment : row.segments())
                {
                    if (!first)
                        out.append(" | ");

                    out.append('[').append(segment.xBucket()).append("] ");
                    out.append(segment.text());
                    first = false;
                }

                out.append('\n');
            }

            out.append('\n');
        }
    }

    private int yBucket(float y)
    {
        return ((int) Math.floor(y / 3f)) * 3;
    }

    private boolean isSegmentGap(PDFLayoutGlyph left, PDFLayoutGlyph right)
    {
        float gap = right.x() - (left.x() + left.width());

        if (gap <= 0)
            return false;

        float averageWidth = (left.width() + right.width()) / 2f;

        return gap > Math.max(14f, averageWidth * 2.8f);
    }

    private boolean isWordGap(PDFLayoutGlyph left, PDFLayoutGlyph right)
    {
        float gap = right.x() - (left.x() + left.width());

        if (gap <= 0)
            return false;

        float averageWidth = (left.width() + right.width()) / 2f;

        return gap > Math.max(1.1f, averageWidth * 0.28f);
    }

    private static final class Stripper extends PDFTextStripper
    {
        private final List<PDFLayoutGlyph> glyphs = new ArrayList<>();

        private Stripper() throws IOException
        {

        }

        @Override
        protected void processTextPosition(TextPosition text)
        {
            if (text.getUnicode() == null || text.getUnicode().isBlank())
                return;

            glyphs.add(new PDFLayoutGlyph(getCurrentPageNo(), text.getXDirAdj(), text.getYDirAdj(),
                            text.getWidthDirAdj(), text.getHeightDir(), text.getUnicode()));
        }
    }

    private List<PDFLayoutSegmentRow> buildSegmentRows(List<PDFLayoutGlyph> glyphs)
    {
        List<List<PDFLayoutSegment>> rows = buildRows(glyphs);
        List<PDFLayoutSegmentRow> answer = new ArrayList<>();

        for (int ii = 0; ii < rows.size(); ii++)
        {
            List<PDFLayoutSegment> segments = rows.get(ii);
            if (segments.isEmpty())
                continue;

            answer.add(new PDFLayoutSegmentRow(segments.getFirst().page(), ii, segments.getFirst().y(), segments));
        }

        return List.copyOf(answer);
    }
}
