package name.abuchen.portfolio.datatransfer.pdf.layout;

import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.pdfbox3.layout.PDFLayoutSegment;
import name.abuchen.portfolio.pdfbox3.layout.PDFLayoutSegmentBlock;
import name.abuchen.portfolio.pdfbox3.layout.PDFLayoutSegmentDocument;
import name.abuchen.portfolio.pdfbox3.layout.PDFLayoutSegmentRow;

public final class PDFLayoutStructureBuilder
{
    public PDFLayoutStructure build(PDFLayoutSegmentDocument document)
    {
        List<PDFLayoutKeyValue> keyValues = buildKeyValues(document);

        List<PDFLayoutTableRegion> tables = buildTableRegions(document);
        tables = mergeConsecutiveTableRegions(document, tables);

        List<PDFLayoutSegmentBlock> textBlocks = buildTextBlocks(document, tables, keyValues);

        return new PDFLayoutStructure(document, keyValues, tables, textBlocks);
    }

    private List<PDFLayoutKeyValue> buildKeyValues(PDFLayoutSegmentDocument document)
    {
        List<PDFLayoutKeyValue> answer = new ArrayList<>();

        for (PDFLayoutSegmentRow row : document.rows())
        {
            if (row.segments().size() < 2)
                continue;

            PDFLayoutSegment label = row.segments().get(0);

            if (!isLikelyLabel(label.text()))
                continue;

            List<PDFLayoutValueMatch> values = new ArrayList<>();

            for (int segmentIndex = 1; segmentIndex < row.segments().size(); segmentIndex++)
                values.add(new PDFLayoutValueMatch(row, segmentIndex, row.segments().get(segmentIndex)));

            int rowIndex = document.rows().indexOf(row);
            List<PDFLayoutSegmentRow> continuationRows = collectKeyValueContinuationRows(document.rows(), rowIndex + 1);

            answer.add(new PDFLayoutKeyValue(clean(label.text()), row, List.copyOf(values), continuationRows));
        }

        return List.copyOf(answer);
    }

    private List<PDFLayoutSegmentRow> collectKeyValueContinuationRows(List<PDFLayoutSegmentRow> rows, int start)
    {
        List<PDFLayoutSegmentRow> answer = new ArrayList<>();

        if (start <= 0 || start >= rows.size())
            return List.of();

        PDFLayoutSegmentRow previous = rows.get(start - 1);

        for (int ii = start; ii < rows.size(); ii++)
        {
            PDFLayoutSegmentRow row = rows.get(ii);

            if (row.page() != previous.page())
                break;

            if (!isLikelyKeyValueContinuation(previous, row))
                break;

            answer.add(row);
            previous = row;
        }

        return List.copyOf(answer);
    }

    private boolean isLikelyKeyValueContinuation(PDFLayoutSegmentRow previous, PDFLayoutSegmentRow row)
    {
        if (row.segments().size() != 1)
            return false;

        String text = clean(row.text());

        if (text.isBlank())
            return false;

        if (PDFLayoutTextClassifier.containsAmountLikeValue(text))
            return false;

        if (isLikelyExplicitTableHeader(row))
            return false;

        if (looksLikeLongSentence(text))
            return false;

        int valueX = previous.segments().get(previous.segments().size() - 1).xBucket();
        int rowX = row.segments().get(0).xBucket();

        if (Math.abs(rowX - valueX) > 30)
            return false;

        double yDistance = row.y() - previous.y();

        return yDistance > 0 && yDistance <= 18;
    }

    private boolean looksLikeLongSentence(String text)
    {
        String value = clean(text);

        if (value.length() > 90)
            return true;

        return value.endsWith(".") && value.split("\\s+").length > 8; //$NON-NLS-1$ //$NON-NLS-2$
    }

    private List<PDFLayoutTableRegion> buildTableRegions(PDFLayoutSegmentDocument document)
    {
        List<PDFLayoutTableRegion> answer = new ArrayList<>();
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

            answer.add(new PDFLayoutTableRegion(header, tableRows));
        }

        return List.copyOf(answer);
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

            if (!clean(row.text()).isBlank())
                answer.add(row);
        }

        return List.copyOf(answer);
    }

    private boolean isLikelyExplicitTableHeader(PDFLayoutSegmentRow row)
    {
        if (row.segments().size() < 3)
            return false;

        if (PDFLayoutTextClassifier.containsAmountLikeValue(row.text()))
            return false;

        int namedSegments = 0;

        for (PDFLayoutSegment segment : row.segments())
        {
            String text = clean(segment.text());

            if (text.length() >= 3 && text.matches(".*[A-Za-zÄÖÜäöüß].*")) //$NON-NLS-1$
                namedSegments++;
        }

        return namedSegments >= 2;
    }

    private boolean hasEnoughStructuredRows(List<PDFLayoutSegmentRow> rows)
    {
        long multiSegmentRows = rows.stream().filter(row -> row.segments().size() >= 2).count();

        return multiSegmentRows >= 3;
    }

    private boolean isLikelyLabel(String text)
    {
        String cleaned = clean(text);

        if (cleaned.length() < 3 || cleaned.length() > 80)
            return false;

        if (PDFLayoutTextClassifier.containsAmountLikeValue(cleaned))
            return false;

        return cleaned.matches(".*[A-Za-zÄÖÜäöüß].*"); //$NON-NLS-1$
    }

    private String clean(String value)
    {
        if (value == null)
            return ""; //$NON-NLS-1$

        return value.trim().replaceAll("\\s+", " "); //$NON-NLS-1$//$NON-NLS-2$
    }

    private List<PDFLayoutTableRegion> mergeConsecutiveTableRegions(PDFLayoutSegmentDocument document,
                    List<PDFLayoutTableRegion> tables)
    {
        List<PDFLayoutTableRegion> merged = new ArrayList<>();

        for (PDFLayoutTableRegion table : tables)
        {
            if (merged.isEmpty())
            {
                merged.add(table);
                continue;
            }

            PDFLayoutTableRegion previous = merged.get(merged.size() - 1);

            if (canMerge(document, previous, table))
            {
                List<PDFLayoutSegmentRow> rows = new ArrayList<>(previous.rows());
                rows.addAll(table.rows());

                merged.set(merged.size() - 1, new PDFLayoutTableRegion(previous.header(), List.copyOf(rows)));
            }
            else
            {
                merged.add(table);
            }
        }

        return List.copyOf(merged);
    }

    private boolean canMerge(PDFLayoutSegmentDocument document, PDFLayoutTableRegion left, PDFLayoutTableRegion right)
    {
        if (!sameHeader(left, right))
            return false;

        if (!sameColumns(left, right))
            return false;

        if (hasInterveningStructuralText(document, left, right))
            return false;

        return true;
    }

    private List<PDFLayoutSegmentBlock> buildTextBlocks(PDFLayoutSegmentDocument document,
                    List<PDFLayoutTableRegion> tables, List<PDFLayoutKeyValue> keyValues)
    {
        List<PDFLayoutSegmentBlock> answer = new ArrayList<>();

        for (PDFLayoutSegmentBlock block : document.blocks())
        {
            if (!isSingleColumnBlock(block))
                continue;

            if (isConsumedByTable(block, tables))
                continue;

            if (isConsumedByKeyValue(block, keyValues))
                continue;

            answer.add(block);
        }

        return List.copyOf(answer);
    }

    private boolean isConsumedByKeyValue(PDFLayoutSegmentBlock block, List<PDFLayoutKeyValue> keyValues)
    {
        for (PDFLayoutSegmentRow row : block.rows())
        {
            for (PDFLayoutKeyValue keyValue : keyValues)
            {
                if (keyValue.consumedRows().contains(row))
                    return true;
            }
        }

        return false;
    }

    private boolean isConsumedByTable(PDFLayoutSegmentBlock block, List<PDFLayoutTableRegion> tables)
    {
        for (PDFLayoutSegmentRow row : block.rows())
        {
            for (PDFLayoutTableRegion table : tables)
            {
                if (table.rows().contains(row))
                    return true;
            }
        }

        return false;
    }

    private boolean isSingleColumnBlock(PDFLayoutSegmentBlock block)
    {
        return block.rows().stream().filter(row -> !row.isEmpty()).allMatch(row -> row.segments().size() <= 1);
    }

    private boolean sameHeader(PDFLayoutTableRegion left, PDFLayoutTableRegion right)
    {
        return clean(left.header().text()).equals(clean(right.header().text()));
    }

    private boolean sameColumns(PDFLayoutTableRegion left, PDFLayoutTableRegion right)
    {
        if (left.columns().size() != right.columns().size())
            return false;

        for (int ii = 0; ii < left.columns().size(); ii++)
        {
            PDFLayoutTableColumn leftColumn = left.columns().get(ii);
            PDFLayoutTableColumn rightColumn = right.columns().get(ii);

            if (!clean(leftColumn.headerText()).equals(clean(rightColumn.headerText())))
                return false;

            if (Math.abs(leftColumn.xBucket() - rightColumn.xBucket()) > 20)
                return false;
        }

        return true;
    }

    private boolean hasInterveningStructuralText(PDFLayoutSegmentDocument document, PDFLayoutTableRegion left,
                    PDFLayoutTableRegion right)
    {
        int leftEnd = tableEndAbsoluteIndex(document, left);
        int rightStart = rowAbsoluteIndex(document, right.header());

        if (leftEnd < 0 || rightStart < 0 || rightStart <= leftEnd)
            return false;

        return false;
    }

    private int tableEndAbsoluteIndex(PDFLayoutSegmentDocument document, PDFLayoutTableRegion table)
    {
        int max = rowAbsoluteIndex(document, table.header());

        for (PDFLayoutSegmentRow row : table.rows())
        {
            int index = rowAbsoluteIndex(document, row);

            if (index > max)
                max = index;
        }

        return max;
    }

    private int rowAbsoluteIndex(PDFLayoutSegmentDocument document, PDFLayoutSegmentRow row)
    {
        List<PDFLayoutSegmentRow> rows = document.rows();

        for (int ii = 0; ii < rows.size(); ii++)
        {
            if (rows.get(ii) == row)
                return ii;
        }

        return -1;
    }
}
