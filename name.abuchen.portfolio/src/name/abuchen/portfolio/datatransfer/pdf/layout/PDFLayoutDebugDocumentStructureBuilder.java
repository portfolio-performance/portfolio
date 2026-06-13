package name.abuchen.portfolio.datatransfer.pdf.layout;

import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.pdfbox3.layout.PDFLayoutSegment;
import name.abuchen.portfolio.pdfbox3.layout.PDFLayoutSegmentBlock;
import name.abuchen.portfolio.pdfbox3.layout.PDFLayoutSegmentDocument;
import name.abuchen.portfolio.pdfbox3.layout.PDFLayoutSegmentRow;

public final class PDFLayoutDebugDocumentStructureBuilder
{
    public String build(PDFLayoutSegmentDocument document)
    {
        StringBuilder out = new StringBuilder();

        out.append("Suggestions:\n"); //$NON-NLS-1$
        List<TableRegion> tableRegions = findTableRegions(document);

        appendTableCandidates(out, document, tableRegions);
        appendKeyValueGroupCandidates(out, document);
        appendMultiSegmentRowCandidates(out, document);
        appendTextBlockCandidates(out, document, tableRegions);

        return out.toString();
    }

    private record TableRegion(PDFLayoutSegmentRow header, List<PDFLayoutSegmentRow> rows)
    {
        boolean contains(PDFLayoutSegmentRow row)
        {
            return rows.contains(row);
        }
    }

    private void appendTableCandidates(StringBuilder out, PDFLayoutSegmentDocument document,
                    List<TableRegion> tableRegions)
    {
        out.append("  tableCandidates:\n"); //$NON-NLS-1$

        int index = 1;

        for (TableRegion region : tableRegions)
        {
            out.append("    - id: table_candidate_").append(String.format("%03d", index++)).append('\n'); //$NON-NLS-1$//$NON-NLS-2$
            out.append("      detection: headerRegion\n"); //$NON-NLS-1$
            out.append("      page: ").append(region.header().page()).append('\n'); //$NON-NLS-1$
            out.append("      header:\n"); //$NON-NLS-1$
            out.append("        row: ").append(region.header().index()).append('\n'); //$NON-NLS-1$
            out.append("        pattern: \"").append(region.header().pattern()).append("\"\n"); //$NON-NLS-1$//$NON-NLS-2$
            out.append("        text: \"").append(escape(clean(region.header().text()))).append("\"\n"); //$NON-NLS-1$ //$NON-NLS-2$
            out.append("      columns:\n"); //$NON-NLS-1$
            appendHeaderColumns(out, region.header());

            out.append("      rows: ").append(region.rows().size()).append('\n'); //$NON-NLS-1$
            out.append("      sampleRows:\n"); //$NON-NLS-1$

            region.rows().stream().limit(3).forEach(row -> {
                out.append("        - row: ").append(row.index()).append('\n'); //$NON-NLS-1$
                out.append("          page: ").append(row.page()).append('\n'); //$NON-NLS-1$
                out.append("          pattern: \"").append(row.pattern()).append("\"\n"); //$NON-NLS-1$//$NON-NLS-2$
                out.append("          cells:\n"); //$NON-NLS-1$

                for (PDFLayoutSegment segment : region.header().segments())
                {
                    out.append("            - column: \"").append(escape(clean(segment.text()))).append("\"\n"); //$NON-NLS-1$ //$NON-NLS-2$
                    out.append("              x: ").append(segment.xBucket()).append('\n'); //$NON-NLS-1$
                    out.append("              value: \"").append(escape(clean(textNearX(row, segment.xBucket(), 50)))) //$NON-NLS-1$
                                    .append("\"\n"); //$NON-NLS-1$
                }
            });
        }

        if (index == 1)
            out.append("    []\n"); //$NON-NLS-1$
    }

    private void appendKeyValueGroupCandidates(StringBuilder out, PDFLayoutSegmentDocument document)
    {
        out.append("  keyValueGroupCandidates:\n"); //$NON-NLS-1$

        int index = 1;

        for (PDFLayoutSegmentBlock block : document.blocks())
        {
            if (block.rowCount() < 2)
                continue;

            if (!isKeyValueGroup(block))
                continue;

            out.append("    - id: key_value_group_candidate_").append(String.format("%03d", index++)).append('\n'); //$NON-NLS-1$ //$NON-NLS-2$
            out.append("      block: ").append(block.id()).append('\n'); //$NON-NLS-1$
            out.append("      page: ").append(block.page()).append('\n'); //$NON-NLS-1$
            out.append("      rows: ").append(block.rowCount()).append('\n'); //$NON-NLS-1$
            out.append("      pattern: \"").append(block.pattern()).append("\"\n"); //$NON-NLS-1$//$NON-NLS-2$
            out.append("      sampleRows:\n"); //$NON-NLS-1$

            block.rows().stream().limit(8).forEach(row -> {
                out.append("        - row: ").append(row.index()).append('\n'); //$NON-NLS-1$
                out.append("          pattern: \"").append(row.pattern()).append("\"\n"); //$NON-NLS-1$//$NON-NLS-2$
                out.append("          segments:\n"); //$NON-NLS-1$

                for (int ii = 0; ii < row.segments().size(); ii++)
                {
                    PDFLayoutSegment segment = row.segments().get(ii);

                    out.append("            - index: ").append(ii).append('\n'); //$NON-NLS-1$
                    out.append("              x: ").append(segment.xBucket()).append('\n'); //$NON-NLS-1$
                    out.append("              text: \"").append(escape(clean(segment.text()))).append("\"\n"); //$NON-NLS-1$//$NON-NLS-2$
                }
            });
        }

        if (index == 1)
            out.append("    []\n"); //$NON-NLS-1$
    }

    private void appendMultiSegmentRowCandidates(StringBuilder out, PDFLayoutSegmentDocument document)
    {
        out.append("  multiSegmentRowCandidates:\n"); //$NON-NLS-1$

        int index = 1;

        for (PDFLayoutSegmentRow row : document.rows())
        {
            if (row.segments().size() < 4)
                continue;

            out.append("    - id: multi_segment_row_candidate_").append(String.format("%03d", index++)).append('\n'); //$NON-NLS-1$ //$NON-NLS-2$
            out.append("      row: ").append(row.index()).append('\n'); //$NON-NLS-1$
            out.append("      page: ").append(row.page()).append('\n'); //$NON-NLS-1$
            out.append("      pattern: \"").append(row.pattern()).append("\"\n"); //$NON-NLS-1$ //$NON-NLS-2$
            out.append("      text: \"").append(escape(clean(row.text()))).append("\"\n"); //$NON-NLS-1$//$NON-NLS-2$
            out.append("      segments:\n"); //$NON-NLS-1$

            for (int ii = 0; ii < row.segments().size(); ii++)
            {
                PDFLayoutSegment segment = row.segments().get(ii);

                out.append("        - index: ").append(ii).append('\n'); //$NON-NLS-1$
                out.append("          x: ").append(segment.xBucket()).append('\n'); //$NON-NLS-1$
                out.append("          text: \"").append(escape(clean(segment.text()))).append("\"\n"); //$NON-NLS-1$//$NON-NLS-2$
            }
        }

        if (index == 1)
            out.append("    []\n"); //$NON-NLS-1$
    }

    private void appendTextBlockCandidates(StringBuilder out, PDFLayoutSegmentDocument document,
                    List<TableRegion> tableRegions)
    {
        out.append("  textBlockCandidates:\n"); //$NON-NLS-1$

        int index = 1;

        for (PDFLayoutSegmentBlock block : document.blocks())
        {
            if (!isSingleColumnBlock(block))
                continue;

            if (isConsumedByTableRegion(block, tableRegions))
                continue;

            if (shouldSuppressTextBlock(block))
                continue;

            out.append("    - id: text_block_candidate_").append(String.format("%03d", index++)).append('\n'); //$NON-NLS-1$//$NON-NLS-2$
            out.append("      block: ").append(block.id()).append('\n'); //$NON-NLS-1$
            out.append("      page: ").append(block.page()).append('\n'); //$NON-NLS-1$
            out.append("      rows: ").append(block.rowCount()).append('\n'); //$NON-NLS-1$
            out.append("      pattern: \"").append(block.pattern()).append("\"\n"); //$NON-NLS-1$ //$NON-NLS-2$
            out.append("      priority: ").append(textBlockPriority(block)).append('\n'); //$NON-NLS-1$
            out.append("      sampleText:\n"); //$NON-NLS-1$

            block.rows().stream().limit(6).forEach(row -> {
                out.append("        - row: ").append(row.index()).append('\n'); //$NON-NLS-1$
                out.append("          text: \"").append(escape(clean(row.text()))).append("\"\n"); //$NON-NLS-1$//$NON-NLS-2$
            });
        }

        if (index == 1)
            out.append("    []\n"); //$NON-NLS-1$
    }

    private boolean isLikelyLabel(String text)
    {
        if (text.length() < 3 || text.length() > 60)
            return false;

        if (PDFLayoutTextClassifier.containsAmountLikeValue(text))
            return false;

        return text.matches(".*[A-Za-zÄÖÜäöüß].*"); //$NON-NLS-1$
    }

    private String escape(String value)
    {
        return value.replace("\\", "\\\\").replace("\"", "\\\""); //$NON-NLS-1$ //$NON-NLS-2$//$NON-NLS-3$//$NON-NLS-4$
    }

    private String clean(String value)
    {
        if (value == null)
            return ""; //$NON-NLS-1$

        return value.trim().replaceAll("\\s+", " "); //$NON-NLS-1$//$NON-NLS-2$
    }

    private boolean isSingleColumnBlock(PDFLayoutSegmentBlock block)
    {
        return block.rows().stream().filter(row -> !row.isEmpty()).allMatch(row -> row.segments().size() <= 1);
    }

    private boolean isKeyValueGroup(PDFLayoutSegmentBlock block)
    {
        long candidateRows = block.rows().stream().filter(row -> row.segments().size() >= 2)
                        .filter(row -> isLikelyLabel(row.segments().get(0).text())).count();

        return candidateRows >= 2 && candidateRows >= block.rowCount() * 0.6d;
    }

    private boolean isLikelyExplicitTableHeader(PDFLayoutSegmentRow row)
    {
        if (row.segments().size() < 3)
            return false;

        String text = clean(row.text());

        if (PDFLayoutTextClassifier.containsAmountLikeValue(text))
            return false;

        int namedSegments = 0;

        for (PDFLayoutSegment segment : row.segments())
        {
            String segmentText = clean(segment.text());

            if (segmentText.length() >= 3 && segmentText.matches(".*[A-Za-zÄÖÜäöüß].*")) //$NON-NLS-1$
                namedSegments++;
        }

        return namedSegments >= 2;
    }

    private List<PDFLayoutSegmentRow> collectRowsAfterHeader(List<PDFLayoutSegmentRow> rows, int start)
    {
        List<PDFLayoutSegmentRow> answer = new ArrayList<>();
        int page = rows.get(start - 1).page();

        for (int ii = start; ii < rows.size(); ii++)
        {
            PDFLayoutSegmentRow row = rows.get(ii);

            if (row.page() != page)
                break;

            if (isLikelyExplicitTableHeader(row) && answer.size() >= 3)
                break;

            if (isLongSingleSegmentRegion(rows, ii) && answer.size() >= 3)
                break;

            if (!clean(row.text()).isBlank())
                answer.add(row);
        }

        return List.copyOf(answer);
    }

    private boolean hasEnoughStructuredRows(List<PDFLayoutSegmentRow> rows)
    {
        long multiSegmentRows = rows.stream().filter(row -> row.segments().size() >= 2).count();

        return multiSegmentRows >= 3;
    }

    private void appendHeaderColumns(StringBuilder out, PDFLayoutSegmentRow header)
    {
        for (int ii = 0; ii < header.segments().size(); ii++)
        {
            PDFLayoutSegment segment = header.segments().get(ii);

            out.append("        - index: ").append(ii).append('\n'); //$NON-NLS-1$
            out.append("          x: ").append(segment.xBucket()).append('\n'); //$NON-NLS-1$
            out.append("          text: \"").append(escape(clean(segment.text()))).append("\"\n"); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    private boolean isLongSingleSegmentRegion(List<PDFLayoutSegmentRow> rows, int start)
    {
        int count = 0;

        for (int ii = start; ii < rows.size() && ii < start + 5; ii++)
        {
            if (rows.get(ii).segments().size() <= 1)
                count++;
        }

        return count >= 4;
    }

    private boolean shouldSuppressTextBlock(PDFLayoutSegmentBlock block)
    {
        if (isLongDenseTextBlock(block))
            return true;

        if (isLikelyFooterArea(block) && block.rowCount() >= 2)
            return true;

        return false;
    }

    private boolean isLongDenseTextBlock(PDFLayoutSegmentBlock block)
    {
        if (block.rowCount() < 4)
            return false;

        int words = 0;
        int numbers = 0;

        for (PDFLayoutSegmentRow row : block.rows())
        {
            String text = clean(row.text());

            if (text.isBlank())
                continue;

            words += text.split("\\s+").length; //$NON-NLS-1$

            if (text.matches(".*\\d.*")) //$NON-NLS-1$
                numbers++;
        }

        return words >= 45 && numbers <= block.rowCount() / 2;
    }

    private boolean isLikelyFooterArea(PDFLayoutSegmentBlock block)
    {
        return block.yStart() >= 730f;
    }

    private String textBlockPriority(PDFLayoutSegmentBlock block)
    {
        if (block.rowCount() <= 2 && block.yStart() < 730f)
            return "medium"; //$NON-NLS-1$

        return "low"; //$NON-NLS-1$
    }

    private boolean isConsumedByTableRegion(PDFLayoutSegmentBlock block, List<TableRegion> tableRegions)
    {
        for (PDFLayoutSegmentRow row : block.rows())
        {
            for (TableRegion region : tableRegions)
            {
                if (region.contains(row))
                    return true;
            }
        }

        return false;
    }

    private List<TableRegion> findTableRegions(PDFLayoutSegmentDocument document)
    {
        List<TableRegion> answer = new ArrayList<>();
        List<PDFLayoutSegmentRow> rows = document.rows();

        for (int ii = 0; ii < rows.size(); ii++)
        {
            PDFLayoutSegmentRow header = rows.get(ii);

            if (!isLikelyExplicitTableHeader(header))
                continue;

            List<PDFLayoutSegmentRow> tableRows = collectRowsAfterHeader(rows, ii + 1);

            if (tableRows.size() < 3)
                continue;

            if (!hasEnoughStructuredRows(tableRows))
                continue;

            answer.add(new TableRegion(header, tableRows));
        }

        return List.copyOf(answer);
    }

    private String textNearX(PDFLayoutSegmentRow row, int xBucket, int tolerance)
    {
        PDFLayoutSegment best = null;
        int bestDistance = Integer.MAX_VALUE;

        for (PDFLayoutSegment segment : row.segments())
        {
            int distance = Math.abs(segment.xBucket() - xBucket);

            if (distance <= tolerance && distance < bestDistance)
            {
                best = segment;
                bestDistance = distance;
            }
        }

        return best == null ? "" : best.text(); //$NON-NLS-1$
    }
}
