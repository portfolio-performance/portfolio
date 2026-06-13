package name.abuchen.portfolio.datatransfer.pdf.layout;

public final class PDFLayoutDebugDataStructureBuilder
{
    public String build(PDFLayoutStructure structure)
    {
        StringBuilder out = new StringBuilder();

        appendKeyValues(out, structure);
        appendTables(out, structure);

        return out.toString();
    }

    private void appendKeyValues(StringBuilder out, PDFLayoutStructure structure)
    {
        out.append("keyValues:\n"); //$NON-NLS-1$

        if (structure.keyValues().isEmpty())
        {
            out.append("  []\n"); //$NON-NLS-1$
            return;
        }

        structure.keyValues().stream().limit(5).forEach(keyValue -> {
            out.append("  - label: \"").append(clean(keyValue.label())).append("\"\n"); //$NON-NLS-1$ //$NON-NLS-2$
            out.append("    row: ").append(keyValue.rowIndex()).append('\n'); //$NON-NLS-1$
            out.append("    page: ").append(keyValue.page()).append('\n'); //$NON-NLS-1$
            out.append("    values:\n"); //$NON-NLS-1$

            for (int ii = 0; ii < keyValue.values().size(); ii++)
            {
                PDFLayoutValueMatch value = keyValue.values().get(ii);

                out.append("      - index: ").append(ii).append('\n'); //$NON-NLS-1$
                out.append("        text: \"").append(clean(value.text())).append("\"\n"); //$NON-NLS-1$//$NON-NLS-2$
            }
        });
    }

    private void appendTables(StringBuilder out, PDFLayoutStructure structure)
    {
        out.append("\ntables:\n"); //$NON-NLS-1$

        if (structure.tableRegions().isEmpty())
        {
            out.append("  []\n"); //$NON-NLS-1$
            return;
        }

        int index = 1;

        for (PDFLayoutTableRegion table : structure.tableRegions().stream().limit(3).toList())
        {
            out.append("  - id: table_").append(String.format("%03d", index++)).append('\n'); //$NON-NLS-1$//$NON-NLS-2$
            out.append("    header: \"").append(clean(table.header().text())).append("\"\n"); //$NON-NLS-1$//$NON-NLS-2$
            out.append("    page: ").append(table.header().page()).append('\n'); //$NON-NLS-1$
            out.append("    rows: ").append(table.rows().size()).append('\n'); //$NON-NLS-1$

            out.append("    columns:\n"); //$NON-NLS-1$

            for (PDFLayoutTableColumn column : table.columns())
            {
                out.append("      - index: ").append(column.segmentIndex()).append('\n'); //$NON-NLS-1$
                out.append("        x: ").append(column.xBucket()).append('\n'); //$NON-NLS-1$
                out.append("        header: \"").append(clean(column.headerText())).append("\"\n"); //$NON-NLS-1$ //$NON-NLS-2$
            }

            out.append("    sampleRows:\n"); //$NON-NLS-1$

            table.tableRows().stream().limit(3).forEach(row -> {
                out.append("      - row: ").append(row.rowIndex()).append('\n'); //$NON-NLS-1$
                out.append("        page: ").append(row.page()).append('\n'); //$NON-NLS-1$
                out.append("        cells:\n"); //$NON-NLS-1$

                for (PDFLayoutTableColumn column : table.columns())
                {
                    out.append("          - column: \"").append(clean(column.headerText())).append("\"\n"); //$NON-NLS-1$//$NON-NLS-2$
                    out.append("            x: ").append(column.xBucket()).append('\n'); //$NON-NLS-1$
                    out.append("            value: \"").append(clean(row.textNearX(column.xBucket(), 50))) //$NON-NLS-1$
                                    .append("\"\n"); //$NON-NLS-1$
                }
            });
        }
    }

    private String clean(String value)
    {
        if (value == null)
            return ""; //$NON-NLS-1$

        return value.trim().replaceAll("\\s+", " "); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
