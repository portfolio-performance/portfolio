package name.abuchen.portfolio.datatransfer.csv;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import name.abuchen.portfolio.datatransfer.csv.CSVImporter.Column;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.EnumField;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.Field;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.FieldFormat;

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

            if (f instanceof EnumField)
            {
                // when running the tests, do not set a format and convert the
                // raw string directly to the enum (see CSVExtractor#getEnum)
            }
            else
            {
                List<FieldFormat> formats = f.getAvailableFieldFormats();

                if (!formats.isEmpty())
                    column.setFormat(formats.get(0));
            }

            field2column.put(f.getName(), column);
        }
        return field2column;
    }

}
