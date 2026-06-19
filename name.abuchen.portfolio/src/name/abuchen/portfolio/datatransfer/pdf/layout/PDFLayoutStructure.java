package name.abuchen.portfolio.datatransfer.pdf.layout;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import name.abuchen.portfolio.pdfbox3.layout.PDFLayoutSegmentBlock;
import name.abuchen.portfolio.pdfbox3.layout.PDFLayoutSegmentDocument;
import name.abuchen.portfolio.pdfbox3.layout.PDFLayoutSegmentRow;

public final class PDFLayoutStructure
{
    private final PDFLayoutSegmentDocument document;
    private final List<PDFLayoutKeyValue> keyValues;
    private final List<PDFLayoutTableRegion> tableRegions;
    private final List<PDFLayoutSegmentBlock> textBlocks;

    PDFLayoutStructure(PDFLayoutSegmentDocument document, List<PDFLayoutKeyValue> keyValues,
                    List<PDFLayoutTableRegion> tableRegions, List<PDFLayoutSegmentBlock> textBlocks)
    {
        this.document = document;
        this.keyValues = List.copyOf(keyValues);
        this.tableRegions = List.copyOf(tableRegions);
        this.textBlocks = List.copyOf(textBlocks);
    }

    public PDFLayoutSegmentDocument document()
    {
        return document;
    }

    public List<PDFLayoutSegmentRow> rows()
    {
        return document.rows();
    }

    public List<PDFLayoutSegmentBlock> blocks()
    {
        return document.blocks();
    }

    public List<PDFLayoutKeyValue> keyValues()
    {
        return keyValues;
    }

    public List<PDFLayoutSegmentBlock> textBlocks()
    {
        return textBlocks;
    }

    public Optional<PDFLayoutKeyValue> findKeyValue(String label)
    {
        String needle = clean(label);

        return keyValues.stream().filter(value -> clean(value.label()).equals(needle)).findFirst();
    }

    public List<PDFLayoutKeyValue> findKeyValues(String label)
    {
        String needle = clean(label);

        return keyValues.stream().filter(value -> clean(value.label()).equals(needle)).collect(Collectors.toList());
    }

    public List<PDFLayoutKeyValue> findKeyValuesContaining(String labelPart)
    {
        String needle = clean(labelPart);

        return keyValues.stream().filter(value -> clean(value.label()).contains(needle)).collect(Collectors.toList());
    }

    public List<PDFLayoutSegmentRow> findRowsByPattern(String pattern)
    {
        return document.rows().stream().filter(row -> row.pattern().equals(pattern)).collect(Collectors.toList());
    }

    public List<PDFLayoutTableRegion> tableRegions()
    {
        return tableRegions;
    }

    public Optional<PDFLayoutTableRegion> findTableByHeader(String... headerParts)
    {
        return tableRegions.stream().filter(table -> table.headerContainsAll(headerParts)).findFirst();
    }

    private static String clean(String value)
    {
        if (value == null)
            return ""; //$NON-NLS-1$

        return value.trim().replaceAll("\\s+", " "); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public List<PDFLayoutSegmentRow> findRowsContaining(String text)
    {
        String needle = clean(text);

        return document.rows().stream().filter(row -> clean(row.text()).contains(needle)).collect(Collectors.toList());
    }

    public Optional<PDFLayoutSegmentRow> findFirstRowContaining(String text)
    {
        String needle = clean(text);

        return document.rows().stream().filter(row -> clean(row.text()).contains(needle)).findFirst();
    }

    public List<PDFLayoutSegmentRow> rowsAfter(PDFLayoutSegmentRow row, int count)
    {
        return document.rows().stream().filter(candidate -> candidate.page() == row.page())
                        .filter(candidate -> candidate.index() > row.index()).limit(count).collect(Collectors.toList());
    }
}
