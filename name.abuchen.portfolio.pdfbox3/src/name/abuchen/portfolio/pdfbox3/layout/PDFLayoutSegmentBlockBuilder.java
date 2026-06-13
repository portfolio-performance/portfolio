package name.abuchen.portfolio.pdfbox3.layout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PDFLayoutSegmentBlockBuilder
{
    public List<PDFLayoutSegmentBlock> build(List<PDFLayoutSegmentRow> rows)
    {
        List<PDFLayoutSegmentBlock> answer = new ArrayList<>();

        List<PDFLayoutSegmentRow> current = new ArrayList<>();
        String currentPattern = null;
        int blockIndex = 1;

        for (PDFLayoutSegmentRow row : rows)
        {
            if (row.isEmpty())
                continue;

            String pattern = row.pattern();

            if (currentPattern != null && !currentPattern.equals(pattern) && !current.isEmpty())
            {
                answer.add(createBlock(blockIndex++, currentPattern, current));
                current = new ArrayList<>();
            }

            current.add(row);
            currentPattern = pattern;
        }

        if (!current.isEmpty())
            answer.add(createBlock(blockIndex, currentPattern, current));

        return List.copyOf(answer);
    }

    private PDFLayoutSegmentBlock createBlock(int index, String pattern, List<PDFLayoutSegmentRow> rows)
    {
        int page = rows.getFirst().page();

        return new PDFLayoutSegmentBlock("block-" + String.format(Locale.ROOT, "%03d", index), page, pattern,
                        List.copyOf(rows));
    }
}
