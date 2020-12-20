package name.abuchen.portfolio.datatransfer.csv;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.util.Isin;
import name.abuchen.portfolio.util.TextUtil;

public final class CSVImporter
{
    public static final class Column
    {
        private final int columnIndex;
        private final String label;
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
        private final String code;
        private final String label;
        private final Format format;

        public FieldFormat(String code, String label, Format format)
        {
            this.code = code;
            this.label = label;
            this.format = format;
        }

        public FieldFormat(String label, Format format)
        {
            this(null, label, format);
        }

        public FieldFormat(String code, String label, Supplier<Format> supplier)
        {
            this(code, label, supplier.get());
        }

        public String getCode()
        {
            return code;
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

        public String toPattern()
        {
            if (format instanceof SimpleDateFormat)
                return ((SimpleDateFormat) format).toPattern();
            else if (format instanceof DecimalFormat)
                return ((DecimalFormat) format).toPattern();
            else if (format instanceof ISINFormat)
                return Isin.PATTERN;
            else if (format instanceof EnumMapFormat)
                return ((EnumMapFormat<?>) format).map().toString();

            return null;
        }
    }

    public static class Field
    {
        /**
         * Unique code of the field which is used to identify the field in the
         * JSON configuration. The code is not translated, must be unique per
         * extractor, and must not change.
         */
        private final String code;

        private final String[] names;
        private final Set<String> normalizedNames;
        private boolean isOptional = false;

        public Field(String code, String... names)
        {
            if (names.length < 1)
                throw new IllegalArgumentException();

            this.code = code;
            this.names = names;
            this.normalizedNames = new HashSet<>();
            for (int ii = 0; ii < names.length; ii++)
                this.normalizedNames.add(normalizeColumnName(names[ii]));
        }

        public String getCode()
        {
            return code;
        }

        public String getName()
        {
            return names[0];
        }

        public Set<String> getNormalizedNames()
        {
            return normalizedNames;
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

        public List<FieldFormat> getAvailableFieldFormats()
        {
            return Collections.emptyList();
        }

        /**
         * Returns the initial format - optionally using Client and one value to
         * guess the best fitting format
         *
         * @param client
         *            target Client into which data will be imported
         * @param value
         *            example value from the current file; can be null
         */
        public FieldFormat guessFormat(Client client, String value) // NOSONAR
        {
            return null;
        }

        public String formatToText(FieldFormat fieldFormat)
        {
            throw new UnsupportedOperationException();
        }

        public FieldFormat textToFormat(String text)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public String toString()
        {
            return getName();
        }
    }

    private static class DateFieldFormat extends FieldFormat
    {
        public DateFieldFormat(String label, String pattern)
        {
            super(pattern, label, new SimpleDateFormat(pattern));
        }

        public DateFieldFormat(String label, String pattern, Locale locale)
        {
            super(pattern + ";" + locale.toString(), label, new SimpleDateFormat(pattern, locale)); //$NON-NLS-1$
        }
    }

    public static class DateField extends CSVImporter.Field
    {
        private static final List<FieldFormat> FORMATS = Collections.unmodifiableList(Arrays.asList( //
                        new DateFieldFormat(Messages.CSVFormatYYYYMMDD, "yyyy-MM-dd"), //$NON-NLS-1$
                        new DateFieldFormat(Messages.CSVFormatYYYYMMDDSlashes, "yyyy/MM/dd"), //$NON-NLS-1$
                        new DateFieldFormat(Messages.CSVFormatISO, "yyyyMMdd"), //$NON-NLS-1$
                        new DateFieldFormat(Messages.CSVFormatDDMMYYYY, "dd.MM.yyyy"), //$NON-NLS-1$
                        new DateFieldFormat(Messages.CSVFormatDDMMYY, "dd.MM.yy"), //$NON-NLS-1$
                        new DateFieldFormat(Messages.CSVFormatDDMMYYYY1, "dd/MM/yyyy"), //$NON-NLS-1$
                        new DateFieldFormat(Messages.CSVFormatDDMMYY1, "dd/MM/yy"), //$NON-NLS-1$
                        new DateFieldFormat(Messages.CSVFormatDDMMYYYY2, "dd-MM-yyyy"), //$NON-NLS-1$
                        new DateFieldFormat(Messages.CSVFormatDDMMYY2, "dd-MM-yy"), //$NON-NLS-1$
                        new DateFieldFormat(Messages.CSVFormatMMDDYYYY1, "MM/dd/yyyy"), //$NON-NLS-1$
                        new DateFieldFormat(Messages.CSVFormatMMDDYY1, "MM/dd/yy"), //$NON-NLS-1$
                        new DateFieldFormat(Messages.CSVFormatMMDDYY, "MM-dd-yy"), //$NON-NLS-1$
                        new DateFieldFormat(Messages.CSVFormatMMDDYYYY, "MM-dd-yyyy"), //$NON-NLS-1$
                        new DateFieldFormat(Messages.CSVFormatDDMMMYYYY, "dd-MMM-yyyy"), // NOSONAR //$NON-NLS-1$
                        new DateFieldFormat(Messages.CSVFormatDDMMMYYYY_German, "dd-MMM-yyyy", Locale.GERMAN), //$NON-NLS-1$
                        new DateFieldFormat(Messages.CSVFormatDDMMMYYYY_English, "dd-MMM-yyyy", Locale.US) //$NON-NLS-1$
        ));

        /* package */ DateField(String code, String name)
        {
            super(code, name);
        }

        @Override
        public List<FieldFormat> getAvailableFieldFormats()
        {
            return FORMATS;
        }

        /**
         * Guesses the used date format from the given value.
         * 
         * @param value
         *            value (can be null)
         * @return date format on success, else first date format
         */
        @Override
        public FieldFormat guessFormat(Client client, String value)
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

        @Override
        public String formatToText(FieldFormat fieldFormat)
        {
            return fieldFormat.getCode();
        }

        @Override
        public FieldFormat textToFormat(String text)
        {
            for (FieldFormat format : getAvailableFieldFormats())
            {
                if (format.getCode().equals(text))
                    return format;
            }

            return getAvailableFieldFormats().get(0);
        }
    }

    public static class AmountField extends CSVImporter.Field
    {
        private static final List<FieldFormat> FORMATS = Collections.unmodifiableList(Arrays.asList(
                        new FieldFormat("0.000,00", Messages.CSVFormatNumberGermany, //$NON-NLS-1$
                                        NumberFormat.getInstance(Locale.GERMANY)),
                        new FieldFormat("0,000.00", Messages.CSVFormatNumberUS, //$NON-NLS-1$
                                        NumberFormat.getInstance(Locale.US)),
                        new FieldFormat("0'000,00", Messages.CSVFormatApostrophe, () -> { //$NON-NLS-1$
                            DecimalFormatSymbols unusualSymbols = new DecimalFormatSymbols(Locale.US);
                            unusualSymbols.setGroupingSeparator('\'');
                            return new DecimalFormat("#,##0.###", unusualSymbols); //$NON-NLS-1$
                        })));

        /* package */ AmountField(String code, String... name)
        {
            super(code, name);
        }

        @Override
        public List<FieldFormat> getAvailableFieldFormats()
        {
            return FORMATS;
        }

        @Override
        public FieldFormat guessFormat(Client client, String value)
        {
            // pre-configured based on locale; as PP currently does not allow
            // arbitrary number format patterns, map it to the available FORMAT
            // objects

            if ("CH".equals(Locale.getDefault().getCountry())) //$NON-NLS-1$
                return FORMATS.get(2);
            if (TextUtil.DECIMAL_SEPARATOR == ',')
                return FORMATS.get(0);
            if (TextUtil.DECIMAL_SEPARATOR == '.')
                return FORMATS.get(1);

            // fallback
            return FORMATS.get(0);
        }

        @Override
        public String formatToText(FieldFormat fieldFormat)
        {
            return fieldFormat.getCode();
        }

        @Override
        public FieldFormat textToFormat(String text)
        {
            for (FieldFormat format : getAvailableFieldFormats())
            {
                if (format.getCode().equals(text))
                    return format;
            }

            return getAvailableFieldFormats().get(0);
        }

    }

    public static class EnumField<M extends Enum<M>> extends CSVImporter.Field
    {
        private final Class<M> enumType;

        /* package */ EnumField(String code, String name, Class<M> enumType)
        {
            super(code, name);
            this.enumType = enumType;
        }

        public Class<M> getEnumType()
        {
            return enumType;
        }

        @Override
        public FieldFormat guessFormat(Client client, String value)
        {
            return new FieldFormat(null, new EnumMapFormat<>(enumType));
        }

        @Override
        public String formatToText(FieldFormat fieldFormat)
        {
            EnumMapFormat<?> f = (EnumMapFormat<?>) fieldFormat.getFormat();

            StringJoiner answer = new StringJoiner(";"); //$NON-NLS-1$
            f.map().forEach((e, t) -> answer.add(e.name() + "=" + t)); //$NON-NLS-1$

            return answer.toString();
        }

        @Override
        public FieldFormat textToFormat(String text)
        {
            EnumMapFormat<M> format = new EnumMapFormat<>(enumType);
            FieldFormat answer = new FieldFormat(null, format);

            String[] entries = text.split(";"); //$NON-NLS-1$
            for (String e : entries)
            {
                String[] entry = e.split("="); //$NON-NLS-1$
                if (entry.length != 2)
                    continue;

                try
                {
                    M key = Enum.valueOf(enumType, entry[0]);
                    String value = entry[1];

                    format.map().put(key, value);
                }
                catch (IllegalArgumentException | NullPointerException ignore)
                {
                    PortfolioLog.error(ignore);
                }
            }
            return answer;
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

            // second: try as pattern

            for (Map.Entry<M, String> entry : enumMap.entrySet())
            {
                try
                {
                    Pattern p = Pattern.compile(entry.getValue());

                    if (p.matcher(source).find())
                    {
                        pos.setIndex(source.length());
                        return entry.getKey();
                    }
                }
                catch (PatternSyntaxException e)
                {
                    PortfolioLog.error(e);
                }
            }

            return null;
        }
    }

    public static class ISINField extends CSVImporter.Field
    {

        /* package */ ISINField(String code, String name)
        {
            super(code, name);
        }

        @Override
        public FieldFormat guessFormat(Client client, String value)
        {
            return new FieldFormat(null, new ISINFormat(client.getSecurities()));
        }

        @Override
        public String formatToText(FieldFormat fieldFormat)
        {
            return null;
        }

        @Override
        public FieldFormat textToFormat(String text)
        {
            return null;
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

    private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    private final Client client;
    private final File inputFile;
    private final List<CSVExtractor> extractors;

    private CSVExtractor currentExtractor;

    private char delimiter = TextUtil.getListSeparatorChar();
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
                        new CSVSecurityPriceExtractor(), new CSVPortfolioExtractor(client)));
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
        propertyChangeSupport.firePropertyChange("extractor", this.currentExtractor, //$NON-NLS-1$
                        this.currentExtractor = extractor); // NOSONAR
    }

    public CSVExtractor getExtractor()
    {
        return currentExtractor;
    }

    public CSVExtractor getSecurityPriceExtractor()
    {
        return extractors.stream().filter(e -> e instanceof CSVSecurityPriceExtractor).findAny()
                        .orElseThrow(IllegalArgumentException::new);
    }

    public Optional<CSVExtractor> getExtractorByCode(String code)
    {
        return extractors.stream().filter(e -> code.equals(e.getCode())).findAny();
    }

    public void setDelimiter(char delimiter)
    {
        propertyChangeSupport.firePropertyChange("delimiter", this.delimiter, this.delimiter = delimiter); //$NON-NLS-1$ //NOSONAR
    }

    public char getDelimiter()
    {
        return delimiter;
    }

    public void setEncoding(Charset encoding)
    {
        propertyChangeSupport.firePropertyChange("encoding", this.encoding, this.encoding = encoding); //$NON-NLS-1$ //NOSONAR
    }

    public Charset getEncoding()
    {
        return encoding;
    }

    public void setSkipLines(int skipLines)
    {
        propertyChangeSupport.firePropertyChange("skipLines", this.skipLines, this.skipLines = skipLines); //$NON-NLS-1$ //NOSONAR
    }

    public int getSkipLines()
    {
        return skipLines;
    }

    public void setFirstLineHeader(boolean isFirstLineHeader)
    {
        propertyChangeSupport.firePropertyChange("firstLineHeader", this.isFirstLineHeader, //$NON-NLS-1$
                        this.isFirstLineHeader = isFirstLineHeader); // NOSONAR
    }

    public boolean isFirstLineHeader()
    {
        return isFirstLineHeader;
    }

    public List<String[]> getRawValues()
    {
        return values;
    }

    public Column[] getColumns()
    {
        return columns;
    }

    /* package */ void setColumns(Column[] columns)
    {
        this.columns = columns;
    }

    private void processStream(InputStream stream, boolean remap) throws IOException
    {
        Reader reader = new InputStreamReader(stream, encoding);

        CSVFormat strategy = CSVFormat.newFormat(delimiter).withQuote('"').withRecordSeparator("\r\n"); //$NON-NLS-1$

        try
        {
            CSVParser parser = CSVParser.parse(reader, strategy);
            Iterator<CSVRecord> records = parser.iterator();
            for (int ii = 0; ii < skipLines && records.hasNext(); ii++)
                records.next();
            List<String[]> input = new ArrayList<>();
            String[] header = null;
            CSVRecord line = records.hasNext() ? records.next() : null;
            // no more data available after skipping lines
            if (line == null)
            {
                this.values = Collections.emptyList();
                if (remap)
                    this.columns = new Column[0];
                return;
            }
            if (isFirstLineHeader)
            {
                header = toStringArray(line);
            }
            else
            {
                header = new String[line.size()];
                for (int ii = 0; ii < header.length; ii++)
                    header[ii] = MessageFormat.format(Messages.CSVImportGenericColumnLabel, ii + 1);
                input.add(toStringArray(line));
            }
            while (records.hasNext())
                input.add(toStringArray(records.next()));
            this.values = input;
            if (this.columns == null || remap)
            {
                this.columns = new CSVImporter.Column[header.length];
                for (int ii = 0; ii < header.length; ii++)
                    this.columns[ii] = new Column(ii, header[ii]);

                mapToImportDefinition();
            }
        }
        catch (IllegalStateException e)
        {
            PortfolioLog.error(e);

            if (remap)
                this.columns = new Column[] { new Column(0, Messages.LabelError) };

            List<String[]> reply = new ArrayList<>();
            reply.add(new String[] { e.getMessage() });
            this.values = reply;
        }
    }

    private String[] toStringArray(CSVRecord line)
    {
        String[] answer = new String[line.size()];
        for (int ii = 0; ii < answer.length; ii++)
            answer[ii] = line.get(ii);
        return answer;
    }

    public void processFile(boolean remap) throws IOException
    {
        try (FileInputStream stream = new FileInputStream(inputFile))
        {
            processStream(stream, remap);
        }
        catch (IOException e)
        {
            PortfolioLog.error(e);

            try
            {
                // fallback for file names with umlaute on Linux
                byte[] ptext = inputFile.toString().getBytes(StandardCharsets.UTF_8);
                String str = new String(ptext, StandardCharsets.ISO_8859_1);
                Path path = Paths.get(URI.create("file://" + str)); //$NON-NLS-1$

                try (InputStream stream = Files.newInputStream(path))
                {
                    processStream(stream, remap);
                }
            }
            catch (IllegalArgumentException | IOException ignore)
            {
                PortfolioLog.error(ignore);
                throw e;
            }
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

                if (field.getNormalizedNames().contains(normalizedColumnName))
                {
                    column.setField(field);

                    String value = getFirstNonEmptyValue(column);
                    column.setFormat(field.guessFormat(client, value));

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
                field2column.put(column.getField().getName(), column);

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
    public String getFirstNonEmptyValue(Column column)
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

    public void addPropertyChangeListener(PropertyChangeListener listener)
    {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener)
    {
        propertyChangeSupport.addPropertyChangeListener(propertyName, listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener)
    {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }
}
