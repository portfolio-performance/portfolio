package name.abuchen.portfolio.datatransfer.csv;

import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.AmountField;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.Column;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.DateField;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.Field;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;

/* package */class CSVSecurityPriceExtractor extends CSVExtractor
{
    private List<Field> fields;

    /* package */ CSVSecurityPriceExtractor()
    {
        fields = new ArrayList<>();
        fields.add(new DateField(FieldCode.DATE, Messages.CSVColumn_Date));
        fields.add(new AmountField(FieldCode.QUOTE, Messages.CSVColumn_Quote, "Schluss", "Schlusskurs", "Close")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    @Override
    public String getCode()
    {
        return "investment-vehicle-price"; //$NON-NLS-1$
    }

    @Override
    public String getLabel()
    {
        return Messages.CSVDefHistoricalQuotes;
    }

    @Override
    public List<Field> getFields()
    {
        return fields;
    }

    @Override
    public List<Item> extract(int skipLines, List<String[]> rawValues, Map<String, Column> field2column,
                    List<Exception> errors)
    {
        var dummy = new Security(null, null);

        for (String[] line : rawValues)
        {
            try
            {
                var price = extract(line, field2column);
                if (price.getValue() >= 0)
                    dummy.addPrice(price);
            }
            catch (ParseException e)
            {
                errors.add(e);
            }
        }

        List<Item> result = new ArrayList<>();
        if (!dummy.getPrices().isEmpty())
            result.add(new SecurityItem(dummy));
        return result;
    }

    private SecurityPrice extract(String[] rawValues, Map<String, Column> field2column) throws ParseException
    {
        var date = getDate(FieldCode.DATE, null, rawValues, field2column);
        if (date == null)
            throw new ParseException(MessageFormat.format(Messages.CSVImportMissingField, Messages.CSVColumn_Date), 0);

        var amount = getQuote(FieldCode.QUOTE, rawValues, field2column);
        if (amount == null)
            throw new ParseException(MessageFormat.format(Messages.CSVImportMissingField, Messages.CSVColumn_Quote), 0);

        return new SecurityPrice(date.toLocalDate(), Math.abs(amount));
    }
}
