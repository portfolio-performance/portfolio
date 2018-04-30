package name.abuchen.portfolio.datatransfer.csv;

import java.util.HashMap;
import java.util.Map;

import name.abuchen.portfolio.datatransfer.csv.CSVImporter.AmountField;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.Column;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.DateField;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.Field;

final class CSVExtractorTestUtil
{
    private CSVExtractorTestUtil()
    {}

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

            if (f instanceof DateField)
                column.setFormat(DateField.FORMATS.get(0));
            else if (f instanceof AmountField)
                column.setFormat(AmountField.FORMATS.get(0));

            field2column.put(f.getName(), column);
        }
        return field2column;
    }

}
