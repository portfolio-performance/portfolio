package name.abuchen.portfolio.datatransfer.csv;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import junit.framework.AssertionFailedError;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.AmountField;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.Column;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.DateField;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.EnumField;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.Field;

final class CSVExtractorTestUtil
{
    private CSVExtractorTestUtil()
    {
    }

    /**
     * Build column config for unit testing, e.g. expect all values exactly in
     * the order they have been configured and with the first format option
     */
    /* package */ static Map<String, Column> buildField2Column(CSVExtractor extractor)
    {
        Map<String, Column> field2column = new HashMap<>();

        int index = 0;
        for (Field f : extractor.getFields())
        {
            Column column = new Column(index++, f.getName());
            column.setField(f);

            configure(f, column);

            field2column.put(f.getName(), column);
        }
        return field2column;
    }

    /**
     * Build column config for unit testing based on the keywords of the first
     * line. The keywords must match the <strong>code</strong> defined in the
     * extractor.
     */
    /* package */ static Map<String, Column> buildField2Column(CSVExtractor extractor, String[] firstLine)
    {
        var field2column = new HashMap<String, Column>();

        var code2field = extractor.getFields().stream()
                        .collect(Collectors.toMap(f -> f.getCode().toLowerCase(), f -> f));

        int index = 0;
        for (String row : firstLine)
        {
            var field = code2field.get(row.toLowerCase());
            if (field == null)
                throw new AssertionFailedError("CSV field not found: " + row); //$NON-NLS-1$

            Column column = new Column(index++, field.getName());
            column.setField(field);

            configure(field, column);

            field2column.put(field.getName(), column);
        }
        return field2column;
    }

    private static void configure(Field field, Column column)
    {
        if (field instanceof EnumField)
        {
            // when running the tests, do not set a format and convert the
            // raw string directly to the enum (see CSVExtractor#getEnum)
        }
        else if (field instanceof DateField)
        {
            // use a ISO date format as default import format when testing
            column.setFormat(field.textToFormat("yyyy-MM-dd")); //$NON-NLS-1$
        }
        else if (field instanceof AmountField)
        {
            // use the English date format
            column.setFormat(field.textToFormat("0,000.00")); //$NON-NLS-1$
        }
        else
        {
            var formats = field.getAvailableFieldFormats();

            if (!formats.isEmpty())
                column.setFormat(formats.get(0));
        }
    }

}
