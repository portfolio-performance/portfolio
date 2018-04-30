package name.abuchen.portfolio.datatransfer.csv;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.FieldPosition;
import java.text.Format;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVStrategy;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.util.Isin;

public class CSVImporter
{
    public static final class Column
    {
        private int columnIndex;
        private String label;
        private Field field;
        private FieldFormat format;

        /* package */ Column(int columnIndex, String label)
        {
            this.columnIndex = columnIndex;
            this.label = label;
        }

        public int getColumnIndex()
        {
            return columnIndex;
        }

        public String getLabel()
        {
            return label;
        }

        public void setField(Field field)
        {
            this.field = field;
            this.format = null;
        }

        public Field getField()
        {
            return field;
        }

        public void setFormat(FieldFormat format)
        {
            this.format = format;
        }

        public FieldFormat getFormat()
        {
            return format;
        }
    }

    public static class FieldFormat
    {
        private final String label;
        private final Format format;

        public FieldFormat(String label, Format format)
        {
            this.label = label;
            this.format = format;
        }

        public FieldFormat(String label, Supplier<Format> supplier)
        {
            this(label, supplier.get());
        }

        @Override
        public String toString()
        {
            return label;
        }

        public Format getFormat()
        {
            return format;
        }
    }

    public static class Field
    {
        private final String name;
        private final String normalizedName;
        private boolean isOptional = false;

        public Field(String name)
        {
            this.name = name;
            this.normalizedName = normalizeColumnName(name);
        }

        public String getName()
        {
            return name;
        }

        public String getNormalizedName()
        {
            return normalizedName;
        }

        public Field setOptional(boolean isOptional)
        {
            this.isOptional = isOptional;
            return this;
        }

        public boolean isOptional()
        {
            return isOptional;
        }

        @Override
        public String toString()
        {
            return name;
        }
    }

    public static class DateField extends CSVImporter.Field
    {
        public static final List<FieldFormat> FORMATS = Collections.unmodifiableList(Arrays.asList(
                        new FieldFormat(Messages.CSVFormatYYYYMMDD, new SimpleDateFormat("yyyy-MM-dd")), //$NON-NLS-1$
                        new FieldFormat(Messages.CSVFormatISO, new SimpleDateFormat("yyyyMMdd")), //$NON-NLS-1$
                        new FieldFormat(Messages.CSVFormatDDMMYYYY, new SimpleDateFormat("dd.MM.yyyy")), //$NON-NLS-1$
                        new FieldFormat(Messages.CSVFormatDDMMYY, new SimpleDateFormat("dd.MM.yy")), //$NON-NLS-1$
                        new FieldFormat(Messages.CSVFormatDDMMYYYY1, new SimpleDateFormat("dd/MM/yyyy")), //$NON-NLS-1$
                        new FieldFormat(Messages.CSVFormatDDMMYY1, new SimpleDateFormat("dd/MM/yy")), //$NON-NLS-1$
                        new FieldFormat(Messages.CSVFormatMMDDYY, new SimpleDateFormat("MM-dd-yy")), //$NON-NLS-1$
                        new FieldFormat(Messages.CSVFormatMMDDYYYY, new SimpleDateFormat("MM-dd-yyyy")), //$NON-NLS-1$
                        new FieldFormat(Messages.CSVFormatDDMMMYYYY, new SimpleDateFormat("dd-MMM-yyyy")) //$NON-NLS-1$
        ));

        /* package */ DateField(String name)
        {
            super(name);
        }

        /**
         * Guesses the used date format from the given value.
         * 
         * @param value
         *            value (can be null)
         * @return date format on success, else first date format
         */
        public static FieldFormat guessDateFormat(String value)
        {
            if (value != null)
            {
                for (FieldFormat f : FORMATS)
                {
                    try
                    {
                        // try to parse the value and return it on success
                        f.format.parseObject(value);
                        return f;
                    }
                    catch (ParseException e)
                    {
                        // ignore, try next date format
                    }
                }
            }
            // fallback
            return FORMATS.get(0);
        }
    }

    public static class AmountField extends CSVImporter.Field
    {
        public static final List<FieldFormat> FORMATS = Collections.unmodifiableList(Arrays.asList(
                        new FieldFormat(Messages.CSVFormatNumberGermany, NumberFormat.getInstance(Locale.GERMANY)),
                        new FieldFormat(Messages.CSVFormatNumberUS, NumberFormat.getInstance(Locale.US)),
                        new FieldFormat(Messages.CSVFormatApostrophe, () -> {
                            DecimalFormatSymbols unusualSymbols = new DecimalFormatSymbols(Locale.US);
                            unusualSymbols.setGroupingSeparator('\'');
                            return new DecimalFormat("#,##0.##", unusualSymbols); //$NON-NLS-1$
                        })));

        /* package */ AmountField(String name)
        {
            super(name);
        }
    }

    public static class EnumField<M extends Enum<M>> extends CSVImporter.Field
    {
        private final Class<M> enumType;

        /* package */ EnumField(String name, Class<M> enumType)
        {
            super(name);
            this.enumType = enumType;
        }

        public Class<M> getEnumType()
        {
            return enumType;
        }

        public EnumMapFormat<M> createFormat()
        {
            return new EnumMapFormat<>(enumType);
        }
    }

    public static class EnumMapFormat<M extends Enum<M>> extends Format
    {
        private static final long serialVersionUID = 1L;

        private EnumMap<M, String> enumMap;

        public EnumMapFormat(Class<M> enumType)
        {
            enumMap = new EnumMap<>(enumType);
            for (M element : enumType.getEnumConstants())
                enumMap.put(element, element.toString());
        }

        public EnumMap<M, String> map() // NOSONAR
        {
            return enumMap;
        }

        @Override
        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos)
        {
            String s = enumMap.get(obj);
            if (s == null)
                throw new IllegalArgumentException();

            return toAppendTo.append(s);
        }

        @Override
        public M parseObject(String source, ParsePosition pos)
        {
            if (pos == null)
                throw new NullPointerException();

            // first: try exact matches (example: "Fees" vs. "Fees Refund")

            for (Map.Entry<M, String> entry : enumMap.entrySet())
            {
                if (source.equalsIgnoreCase(entry.getValue()))
                {
                    pos.setIndex(source.length());
                    return entry.getKey();
                }
            }

            // second: try partial matches

            for (Map.Entry<M, String> entry : enumMap.entrySet())
            {
                int p = source.indexOf(entry.getValue());
                if (p >= 0)
                {
                    pos.setIndex(source.length());
                    return entry.getKey();
                }
            }

            return null;
        }
    }

    public static class ISINField extends CSVImporter.Field
    {

        /* package */ ISINField(String name)
        {
            super(name);
        }

        public ISINFormat createFormat(List<Security> securityList)
        {
            return new ISINFormat(securityList);
        }
    }

    public static class ISINFormat extends Format
    {
        private static final long serialVersionUID = 1L;

        private Set<String> existingISINs;

        public ISINFormat(List<Security> securityList)
        {
            existingISINs = securityList.stream().map(Security::getIsin)
                            .filter(isin -> isin != null && !isin.trim().isEmpty()).collect(Collectors.toSet());
        }

        @Override
        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos)
        {
            String s = (String) obj;
            if (s == null)
                throw new IllegalArgumentException();

            return toAppendTo.append(s);
        }

        @Override
        public Object parseObject(String source, ParsePosition pos)
        {
            Objects.requireNonNull(pos);

            String isin = source.trim().toUpperCase();

            // check for a partial match (ISIN maybe only part of the field:
            // "Zins/Dividende ISIN DE0007164600 SAP SE O."

            Pattern pattern = Pattern.compile("\\b(" + Isin.PATTERN + ")\\b"); //$NON-NLS-1$ //$NON-NLS-2$
            Matcher matcher = pattern.matcher(isin);
            if (matcher.find())
                isin = matcher.group(1);

            // return ISIN as valid if a) it is a valid ISIN number, and b) it
            // is one of the existing ISIN

            if (Isin.isValid(isin) && existingISINs.contains(isin))
            {
                pos.setIndex(source.length());
                return isin;
            }
            else
            {
                return null;
            }
        }
    }

    private final Client client;
    private final File inputFile;
    private final List<CSVExtractor> extractors;

    private CSVExtractor currentExtractor;

    private char delimiter = ';';
    private Charset encoding = Charset.defaultCharset();
    private int skipLines = 0;
    private boolean isFirstLineHeader = true;

    private Column[] columns;
    private List<String[]> values;

    public CSVImporter(Client client, File file)
    {
        this.client = client;
        this.inputFile = file;

        this.extractors = Collections.unmodifiableList(Arrays.asList(new CSVAccountTransactionExtractor(client),
                        new CSVPortfolioTransactionExtractor(client), new CSVSecurityExtractor(client),
                        new CSVSecurityPriceExtractor(client)));
        this.currentExtractor = extractors.get(0);
    }

    public Client getClient()
    {
        return client;
    }

    public File getInputFile()
    {
        return inputFile;
    }

    public List<CSVExtractor> getExtractors()
    {
        return extractors;
    }

    public void setExtractor(CSVExtractor extractor)
    {
        this.currentExtractor = extractor;
    }

    public CSVExtractor getExtractor()
    {
        return currentExtractor;
    }

    public CSVExtractor getSecurityPriceExtractor()
    {
        return extractors.get(3);
    }

    public void setDelimiter(char delimiter)
    {
        this.delimiter = delimiter;
    }

    public void setEncoding(Charset encoding)
    {
        this.encoding = encoding;
    }

    public void setSkipLines(int skipLines)
    {
        this.skipLines = skipLines;
    }

    public void setFirstLineHeader(boolean isFirstLineHeader)
    {
        this.isFirstLineHeader = isFirstLineHeader;
    }

    public List<String[]> getRawValues()
    {
        return values;
    }

    public Column[] getColumns()
    {
        return columns;
    }

    public void processFile() throws IOException
    {
        try (FileInputStream stream = new FileInputStream(inputFile))
        {
            Reader reader = new InputStreamReader(stream, encoding);

            CSVStrategy strategy = new CSVStrategy(delimiter, '"', CSVStrategy.COMMENTS_DISABLED,
                            CSVStrategy.ESCAPE_DISABLED, false, false, false, false);

            CSVParser parser = new CSVParser(reader, strategy);

            for (int ii = 0; ii < skipLines; ii++)
                parser.getLine();

            List<String[]> input = new ArrayList<>();
            String[] header = null;
            String[] line = parser.getLine();
            if (isFirstLineHeader)
            {
                header = line;
            }
            else
            {
                header = new String[line.length];
                for (int ii = 0; ii < header.length; ii++)
                    header[ii] = MessageFormat.format(Messages.CSVImportGenericColumnLabel, ii + 1);
                input.add(line);
            }

            while ((line = parser.getLine()) != null)
                input.add(line);

            this.columns = new CSVImporter.Column[header.length];
            for (int ii = 0; ii < header.length; ii++)
                this.columns[ii] = new Column(ii, header[ii]);

            this.values = input;

            mapToImportDefinition();
        }
    }

    private void mapToImportDefinition()
    {
        List<Field> list = new LinkedList<>(currentExtractor.getFields());

        for (Column column : columns)
        {
            column.setField(null);
            String normalizedColumnName = normalizeColumnName(column.getLabel());
            Iterator<Field> iter = list.iterator();
            while (iter.hasNext())
            {
                Field field = iter.next();
                if (field.getNormalizedName().equals(normalizedColumnName))
                {
                    column.setField(field);

                    if (field instanceof DateField)
                    {
                        // try to guess date format
                        String value = getFirstNonEmptyValue(column);
                        column.setFormat(DateField.guessDateFormat(value));
                    }
                    else if (field instanceof AmountField)
                    {
                        column.setFormat(AmountField.FORMATS.get(0));
                    }
                    else if (field instanceof ISINField)
                    {
                        column.setFormat(new FieldFormat(null,
                                        ((ISINField) field).createFormat(client.getSecurities())));
                    }
                    else if (field instanceof EnumField<?>)
                    {
                        column.setFormat(new FieldFormat(null, ((EnumField<?>) field).createFormat()));
                    }

                    iter.remove();
                    break;
                }

            }
        }
    }

    public List<Item> createItems(List<Exception> errors)
    {
        Map<String, Column> field2column = new HashMap<>();
        for (Column column : getColumns())
            if (column.getField() != null)
                field2column.put(column.getField().name, column);

        int startingLineNo = skipLines + (isFirstLineHeader ? 1 : 0);
        return currentExtractor.extract(startingLineNo, values, field2column, errors);
    }

    /**
     * Finds the first value that is not empty for the given column.
     * 
     * @param column
     *            {@link Column}
     * @return value on success, else null
     */
    private String getFirstNonEmptyValue(Column column)
    {
        int index = column.getColumnIndex();
        for (String[] rawValues : values)
        {
            String value = rawValues[index];
            // check if value is set and is not empty (ignore whitespace)
            if ((value != null) && (!value.trim().isEmpty()))
                return value;
        }
        return null;
    }

    /**
     * Normalizes the given column name for better matching to field names.
     * 
     * @param name
     *            name of the column
     * @return normalized name (upper case)
     */
    private static String normalizeColumnName(String name)
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++)
        {
            // get uppercase character
            char c = Character.toUpperCase(name.charAt(i));
            // transform special characters (Ä->AE etc.)
            switch (c)
            {
                case 'Ä':
                    sb.append("AE"); //$NON-NLS-1$
                    break;
                case 'Ö':
                    sb.append("OE"); //$NON-NLS-1$
                    break;
                case 'Ü':
                    sb.append("UE"); //$NON-NLS-1$
                    break;
                case 'ß':
                    sb.append("SS"); //$NON-NLS-1$
                    break;
                case ' ':
                    // strip whitespace
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }
}
