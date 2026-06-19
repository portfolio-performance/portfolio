package name.abuchen.portfolio.datatransfer.pdf.layout;

import java.util.ArrayList;
import java.util.List;

public final class PDFLayoutTableKeyValueProjectionBuilder
{
    public PDFLayoutTableKeyValueProjection build(PDFLayoutTableRegion table)
    {
        PDFLayoutMatrixProjection matrix = new PDFLayoutMatrixProjectionBuilder().build(table);
        List<PDFLayoutTableKeyValue> values = new ArrayList<>();

        for (PDFLayoutMatrixRow row : matrix.rows())
        {
            values.addAll(projectPairColumns(row.cells()));
            values.addAll(projectInlineTriplesAndPairs(row.cells()));
        }

        return new PDFLayoutTableKeyValueProjection(table, List.copyOf(values));
    }

    private List<PDFLayoutTableKeyValue> projectPairColumns(List<PDFLayoutTableCell> row)
    {
        List<PDFLayoutTableKeyValue> result = new ArrayList<>();

        for (int columnIndex = 0; columnIndex + 1 < row.size(); columnIndex += 2)
        {
            PDFLayoutTableCell labels = row.get(columnIndex);
            PDFLayoutTableCell values = row.get(columnIndex + 1);

            if (labels.isEmpty() || values.isEmpty())
                continue;

            if (labels.values().size() == values.values().size())
            {
                for (int ii = 0; ii < labels.values().size(); ii++)
                    result.add(new PDFLayoutTableKeyValue(labels.values().get(ii), List.of(values.values().get(ii))));
            }
            else if (PDFLayoutTextClassifier.looksLikeLabel(labels.text()))
            {
                result.add(new PDFLayoutTableKeyValue(labels.text(), values.values()));
            }
        }

        return result;
    }

    private List<PDFLayoutTableKeyValue> projectInlineTriplesAndPairs(List<PDFLayoutTableCell> row)
    {
        List<PDFLayoutTableKeyValue> result = new ArrayList<>();
        List<String> flat = row.stream().flatMap(cell -> cell.values().stream()).toList();

        for (int ii = 0; ii < flat.size();)
        {
            if (ii + 2 < flat.size())
            {
                String label = flat.get(ii);
                String unit = flat.get(ii + 1);
                String value = flat.get(ii + 2);

                if (PDFLayoutTextClassifier.looksLikeLabel(label) && looksLikeUnit(unit)
                                && looksLikeNumericValue(value))
                {
                    result.add(new PDFLayoutTableKeyValue(label, List.of(unit, value)));
                    ii += 3;
                    continue;
                }
            }

            if (ii + 1 < flat.size())
            {
                String label = flat.get(ii);
                String value = flat.get(ii + 1);

                if (PDFLayoutTextClassifier.looksLikeLabel(label) && !PDFLayoutTextClassifier.looksLikeLabel(value))
                    result.add(new PDFLayoutTableKeyValue(label, List.of(value)));

                ii += 2;
                continue;
            }

            ii++;
        }

        return result;
    }

    private boolean looksLikeUnit(String value)
    {
        return clean(value).matches("[A-Z]{2,5}"); //$NON-NLS-1$
    }

    private boolean looksLikeNumericValue(String value)
    {
        String text = clean(value);
        return text.matches("-?\\d+[,.]\\d+") || text.matches("-?\\d+"); //$NON-NLS-1$//$NON-NLS-2$
    }

    private String clean(String value)
    {
        if (value == null)
            return ""; //$NON-NLS-1$

        return value.trim().replaceAll("\\s+", " "); //$NON-NLS-1$//$NON-NLS-2$
    }
}
