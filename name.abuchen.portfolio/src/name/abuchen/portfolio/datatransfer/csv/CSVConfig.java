package name.abuchen.portfolio.datatransfer.csv;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.Column;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.Field;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.FieldFormat;

public class CSVConfig
{
    @SuppressWarnings("nls")
    private interface Key // NOSONAR
    {
        String FIELD = "field";
        String TARGET = "target";
        String LABEL = "label";
        String FORMAT = "format";
        String DELIMITER = "delimiter";
        String ENCODING = "encoding";
        String SKIP_LINES = "skipLines";
        String IS_FIRST_LINE_HEADER = "isFirstLineHeader";
        String COLUMNS = "columns";
    }

    private static class CSVColumnConfig
    {
        private String label;
        private String code;
        private String format;

        public String getLabel()
        {
            return label;
        }

        public void setLabel(String label)
        {
            this.label = label;
        }

        public String getCode()
        {
            return code;
        }

        public void setCode(String code)
        {
            this.code = code;
        }

        public String getFormat()
        {
            return format;
        }

        public void setFormat(String format)
        {
            this.format = format;
        }
    }

    private String label;

    private String target;

    private char delimiter = ';';
    private Charset encoding = Charset.defaultCharset();
    private int skipLines = 0;
    private boolean isFirstLineHeader = true;

    private List<CSVColumnConfig> columns = new ArrayList<>();

    public String getLabel()
    {
        return label;
    }

    public void setLabel(String label)
    {
        this.label = label;
    }

    public String getTarget()
    {
        return this.target;
    }

    public void setTarget(String target)
    {
        this.target = target;
    }

    public void readFrom(CSVImporter importer)
    {
        this.target = importer.getExtractor().getCode();

        this.delimiter = importer.getDelimiter();
        this.encoding = importer.getEncoding();
        this.skipLines = importer.getSkipLines();
        this.isFirstLineHeader = importer.isFirstLineHeader();

        this.columns.clear();
        for (CSVImporter.Column column : importer.getColumns())
        {
            CSVColumnConfig copy = new CSVColumnConfig();
            copy.setLabel(column.getLabel());

            Field field = column.getField();
            if (field != null)
            {
                copy.setCode(field.getCode());

                FieldFormat format = column.getFormat();
                if (format != null)
                    copy.setFormat(field.formatToText(format));
            }

            this.columns.add(copy);
        }
    }
    public void writeTo(CSVImporter importer)
    {
        importer.getExtractorByCode(this.target).ifPresent(importer::setExtractor);

        importer.setDelimiter(this.delimiter);
        importer.setEncoding(this.encoding);
        importer.setSkipLines(this.skipLines);
        importer.setFirstLineHeader(this.isFirstLineHeader);

        List<Column> setup = new ArrayList<>();

        int index = 0;
        for (CSVColumnConfig column : columns)
        {
            Column copy = new Column(index, column.getLabel());

            Optional<Field> field = importer.getExtractor().getFields().stream()
                            .filter(f -> f.getCode().equals(column.getCode())).findAny();

            if (field.isPresent())
            {
                copy.setField(field.get());

                String format = column.getFormat();
                if (format != null)
                    copy.setFormat(field.get().textToFormat(format));

                if (copy.getFormat() == null)
                    field.get().guessFormat(importer.getClient(), null);
            }

            setup.add(copy);

            index++;
        }

        importer.setColumns(setup.toArray(new Column[0]));
    }

    @SuppressWarnings("unchecked")
    public JSONObject toJSON()
    {
        JSONObject answer = new JSONObject();

        answer.put(Key.LABEL, this.label);

        answer.put(Key.TARGET, this.target);

        answer.put(Key.DELIMITER, String.valueOf(this.delimiter));
        answer.put(Key.ENCODING, this.encoding.name());
        answer.put(Key.SKIP_LINES, this.skipLines);
        answer.put(Key.IS_FIRST_LINE_HEADER, this.isFirstLineHeader);

        JSONArray columnArray = new JSONArray();
        answer.put(Key.COLUMNS, columnArray);

        columns.stream().map(c -> {
            JSONObject col = new JSONObject();
            col.put(Key.LABEL, c.getLabel());

            if (c.getCode() != null)
                col.put(Key.FIELD, c.getCode());

            if (c.getFormat() != null)
                col.put(Key.FORMAT, c.getFormat());
            return col;
        }).forEach(columnArray::add);

        return answer;
    }

    /**
     * Update the CSVConfig from the given JSONObject
     * 
     * @param json
     * @throws NullPointerException
     *             if required values are missing
     * @throws IllegalArgumentException
     *             if the JSON includes illegal values
     */
    public void fromJSON(JSONObject json)
    {
        this.label = Objects.requireNonNull((String) json.get(Key.LABEL),
                        MessageFormat.format(Messages.MsgErrorMissingKeyValueInJSON, Key.LABEL));

        this.target = Objects.requireNonNull((String) json.get(Key.TARGET),
                        MessageFormat.format(Messages.MsgErrorMissingKeyValueInJSON, Key.TARGET));

        char d = Objects.requireNonNull((String) json.get(Key.DELIMITER),
                        MessageFormat.format(Messages.MsgErrorMissingKeyValueInJSON, Key.DELIMITER)).charAt(0);
        this.delimiter = ",;\t".indexOf(d) >= 0 ? d : ';'; //$NON-NLS-1$

        try
        {
            String charsetName = (String) json.get(Key.ENCODING);
            if (charsetName != null)
                this.encoding = Charset.forName(charsetName);
        }
        catch (IllegalCharsetNameException | UnsupportedCharsetException e)
        {
            PortfolioLog.error(e);
            this.encoding = Charset.defaultCharset();
        }

        this.skipLines = Objects
                        .requireNonNull((Long) json.get(Key.SKIP_LINES),
                                        MessageFormat.format(Messages.MsgErrorMissingKeyValueInJSON, Key.SKIP_LINES))
                        .intValue();
        this.isFirstLineHeader = Objects.requireNonNull((boolean) json.get(Key.IS_FIRST_LINE_HEADER),
                        MessageFormat.format(Messages.MsgErrorMissingKeyValueInJSON, Key.IS_FIRST_LINE_HEADER));

        JSONArray array = (JSONArray) json.get(Key.COLUMNS);
        if (array == null || array.isEmpty())
            throw new IllegalArgumentException(
                            MessageFormat.format(Messages.MsgErrorMissingKeyValueInJSON, Key.COLUMNS)); // $NON-NLS-1$

        this.columns.clear();
        for (int ii = 0; ii < array.size(); ii++)
        {
            JSONObject col = (JSONObject) array.get(ii);

            CSVColumnConfig column = new CSVColumnConfig();
            column.setLabel(Objects.requireNonNull((String) col.get(Key.LABEL),
                            MessageFormat.format(Messages.MsgErrorMissingKeyValueInJSON, Key.LABEL)));
            column.setCode((String) col.get(Key.FIELD));
            column.setFormat((String) col.get(Key.FORMAT));

            this.columns.add(column);
        }
    }
}
