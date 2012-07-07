package name.abuchen.portfolio.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.text.FieldPosition;
import java.text.Format;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.util.CSVImportDefinition.AccountTransactionDef;
import name.abuchen.portfolio.util.CSVImportDefinition.PortfolioTransactionDef;
import name.abuchen.portfolio.util.CSVImportDefinition.SecurityDef;
import name.abuchen.portfolio.util.CSVImportDefinition.SecurityPriceDef;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVStrategy;

public class CSVImporter
{
    public static final class Column
    {
        private int columnIndex;
        private String label;
        private Field field;
        private FieldFormat format;

        private Column(int columnIndex, String label)
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

        public Field(String name)
        {
            this.name = name;
        }

        public String getName()
        {
            return name;
        }

        @Override
        public String toString()
        {
            return name;
        }
    }

    public static class DateField extends CSVImporter.Field
    {
        public static final FieldFormat[] FORMATS = new FieldFormat[] {
                        new FieldFormat(Messages.CSVFormatYYYYMMDD, new SimpleDateFormat("yyyy-MM-dd")), //$NON-NLS-1$
                        new FieldFormat(Messages.CSVFormatDDMMYYYY, new SimpleDateFormat("dd.MM.yyyy")) }; //$NON-NLS-1$

        /* package */DateField(String name)
        {
            super(name);
        }
    }

    public static class AmountField extends CSVImporter.Field
    {
        public static final FieldFormat[] FORMATS = new FieldFormat[] {
                        new FieldFormat(Messages.CSVFormatNumberGermany, NumberFormat.getInstance(Locale.GERMANY)),
                        new FieldFormat(Messages.CSVFormatNumberUS, NumberFormat.getInstance(Locale.US)) };

        /* package */AmountField(String name)
        {
            super(name);
        }
    }

    public static class EnumField<M extends Enum<M>> extends CSVImporter.Field
    {
        private final Class<M> enumType;

        /* package */EnumField(String name, Class<M> enumType)
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
            return new EnumMapFormat<M>(enumType);
        }
    }

    public static class EnumMapFormat<M extends Enum<M>> extends Format
    {
        private static final long serialVersionUID = 1L;

        private EnumMap<M, String> enumMap;

        public EnumMapFormat(Class<M> enumType)
        {
            enumMap = new EnumMap<M, String>(enumType);
            for (M element : enumType.getEnumConstants())
                enumMap.put(element, element.name());
        }

        public EnumMap<M, String> map()
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

    private final Client client;
    private final File inputFile;
    private final CSVImportDefinition[] definitions = new CSVImportDefinition[] { new AccountTransactionDef(),
                    new PortfolioTransactionDef(), new SecurityPriceDef(), new SecurityDef() };

    private CSVImportDefinition importDefinition;
    private Object importTarget;

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
    }

    public Client getClient()
    {
        return client;
    }

    public CSVImportDefinition[] getDefinitions()
    {
        return definitions;
    }

    public void setDefinition(CSVImportDefinition target)
    {
        this.importDefinition = target;
        if (importTarget != null && !this.importDefinition.getTargets(client).contains(importTarget))
            importTarget = null;
    }

    public CSVImportDefinition getDefinition()
    {
        return importDefinition;
    }

    public Object getImportTarget()
    {
        return importTarget;
    }

    public void setImportTarget(Object target)
    {
        if (target != null && !this.importDefinition.getTargets(client).contains(target))
            throw new IllegalArgumentException();
        this.importTarget = target;
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
        FileInputStream stream = null;
        try
        {
            stream = new FileInputStream(inputFile);
            Reader reader = new InputStreamReader(stream, encoding);

            CSVStrategy strategy = new CSVStrategy(delimiter, '"', CSVStrategy.COMMENTS_DISABLED,
                            CSVStrategy.ESCAPE_DISABLED, false, false, false, false);

            CSVParser parser = new CSVParser(reader, strategy);

            for (int ii = 0; ii < skipLines; ii++)
                parser.getLine();

            List<String[]> values = new ArrayList<String[]>();
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
                values.add(line);
            }

            while ((line = parser.getLine()) != null)
                values.add(line);

            this.columns = new CSVImporter.Column[header.length];
            for (int ii = 0; ii < header.length; ii++)
                this.columns[ii] = new Column(ii, header[ii]);

            this.values = values;

            mapToImportDefinition();
        }
        finally
        {
            if (stream != null)
            {
                try
                {
                    stream.close();
                }
                catch (IOException ignore)
                {}
            }
        }
    }

    private void mapToImportDefinition()
    {
        List<Field> list = new LinkedList<Field>(importDefinition.getFields());

        for (Column column : columns)
        {
            column.setField(null);
            Iterator<Field> iter = list.iterator();
            while (iter.hasNext())
            {
                Field field = iter.next();
                if (field.getName().equalsIgnoreCase(column.getLabel()))
                {
                    column.setField(field);

                    if (field instanceof DateField)
                        column.setFormat(DateField.FORMATS[0]);
                    else if (field instanceof AmountField)
                        column.setFormat(AmountField.FORMATS[0]);
                    else if (field instanceof EnumField<?>)
                        column.setFormat(new FieldFormat(null, ((EnumField<?>) field).createFormat()));

                    iter.remove();
                    break;
                }

            }
        }
    }

    public void createObjects(List<Exception> errors)
    {
        if (importDefinition == null)
            throw new IllegalArgumentException();
        if (importTarget == null)
            throw new IllegalArgumentException();

        Map<String, Column> field2column = new HashMap<String, Column>();
        for (Column column : getColumns())
            if (column.getField() != null)
                field2column.put(column.getField().name, column);

        for (String[] rawValues : values)
        {
            try
            {
                importDefinition.build(client, importTarget, rawValues, field2column);
            }
            catch (ParseException e)
            {
                errors.add(new IOException(MessageFormat.format(Messages.CSVImportError, Arrays.toString(rawValues),
                                e.getMessage()), e));
            }
        }
    }
}
