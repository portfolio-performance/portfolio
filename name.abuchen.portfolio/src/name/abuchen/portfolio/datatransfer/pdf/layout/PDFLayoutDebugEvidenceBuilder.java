package name.abuchen.portfolio.datatransfer.pdf.layout;

import java.util.stream.Collectors;

public final class PDFLayoutDebugEvidenceBuilder
{
    public String build(PDFLayoutStructure structure)
    {
        StringBuilder out = new StringBuilder();

        appendKeyValues(out, structure);
        appendTables(out, structure);
        appendTextBlocks(out, structure);

        return out.toString();
    }

    private void appendKeyValues(StringBuilder out, PDFLayoutStructure structure)
    {
        out.append("KEY_VALUES\n"); //$NON-NLS-1$

        if (structure.keyValues().isEmpty())
        {
            out.append("  []\n"); //$NON-NLS-1$
            return;
        }

        structure.keyValues().stream().forEach(keyValue -> {
            String values = keyValue.valueTexts().stream().map(value -> quote(clean(value)))
                            .collect(Collectors.joining(", ")); //$NON-NLS-1$

            out.append("  ").append(quote(clean(keyValue.label()))).append(" -> [").append(values).append("]\n"); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
        });
    }

    private void appendTables(StringBuilder out, PDFLayoutStructure structure)
    {
        out.append("\nTABLES\n"); //$NON-NLS-1$

        if (structure.tableRegions().isEmpty())
        {
            out.append("  []\n"); //$NON-NLS-1$
            return;
        }

        int tableIndex = 1;

        for (PDFLayoutTableRegion table : structure.tableRegions())
        {
            String tableId = "table_" + String.format("%03d", tableIndex++); //$NON-NLS-1$//$NON-NLS-2$

            String columns = table.columns().stream().map(column -> quote(clean(column.headerText())))
                            .collect(Collectors.joining(", ")); //$NON-NLS-1$

            out.append("  TABLE ").append(tableId).append(" header=").append(quote(clean(table.header().text()))) //$NON-NLS-1$//$NON-NLS-2$
                            .append('\n');

            out.append("    COLUMNS [").append(columns).append("]\n"); //$NON-NLS-1$ //$NON-NLS-2$

            int entryIndex = 1;

            for (PDFLayoutTableEntry entry : table.entries())
            {
                String values = entry.values().stream().map(value -> quote(clean(value)))
                                .collect(Collectors.joining(", ")); //$NON-NLS-1$

                out.append("    ENTRY entry_").append(String.format("%03d", entryIndex++)).append(" [").append(values) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                                .append("]\n"); //$NON-NLS-1$
            }

            appendMatrixProjection(out, tableId, table);
            appendTableKeyValueProjection(out, tableId, table);
        }
    }

    private String quote(String value)
    {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
    }

    private String clean(String value)
    {
        if (value == null)
            return ""; //$NON-NLS-1$

        return value.trim().replaceAll("\\s+", " "); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private void appendTextBlocks(StringBuilder out, PDFLayoutStructure structure)
    {
        out.append("\nTEXT_BLOCKS\n"); //$NON-NLS-1$

        if (structure.textBlocks().isEmpty())
        {
            out.append("  []\n"); //$NON-NLS-1$
            return;
        }

        int index = 1;

        for (var block : structure.textBlocks())
        {
            out.append("  TEXT text_").append(String.format("%03d", index++)); //$NON-NLS-1$ //$NON-NLS-2$
            out.append(" ["); //$NON-NLS-1$

            String text = block.rows().stream().map(row -> clean(row.text())).filter(value -> !value.isBlank())
                            .collect(Collectors.joining(" | ")); //$NON-NLS-1$

            out.append(quote(text)).append("]\n"); //$NON-NLS-1$
        }
    }

    private void appendMatrixProjection(StringBuilder out, String tableId, PDFLayoutTableRegion table)
    {
        PDFLayoutMatrixProjection projection = new PDFLayoutMatrixProjectionBuilder().build(table);

        out.append("    MATRIX_PROJECTION ").append(tableId).append('\n'); //$NON-NLS-1$

        for (PDFLayoutMatrixRow row : projection.rows())
        {
            String cells = row.cells().stream().map(cell -> quote(clean(cell.text())))
                            .collect(Collectors.joining(", ")); //$NON-NLS-1$

            out.append("      ROW row_").append(String.format("%03d", row.rowIndex())).append(" [").append(cells) //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
                            .append("]\n"); //$NON-NLS-1$
        }
    }

    private void appendTableKeyValueProjection(StringBuilder out, String tableId, PDFLayoutTableRegion table)
    {
        PDFLayoutTableKeyValueProjection projection = new PDFLayoutTableKeyValueProjectionBuilder().build(table);

        if (projection.values().isEmpty())
            return;

        out.append("    KEY_VALUE_PROJECTION ").append(tableId).append('\n'); //$NON-NLS-1$

        for (PDFLayoutTableKeyValue value : projection.values())
        {
            String values = value.values().stream().map(item -> quote(clean(item))).collect(Collectors.joining(", ")); //$NON-NLS-1$

            out.append("      ").append(quote(clean(value.label()))).append(" -> [").append(values).append("]\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
    }
}
