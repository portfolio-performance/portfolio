package name.abuchen.portfolio.datatransfer.csv;

import java.text.MessageFormat;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.AmountField;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.Column;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.DateField;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.Field;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;

/* package */class CSVSecurityPriceExtractor extends CSVExtractor
{
    private List<Field> fields;

    /* package */ CSVSecurityPriceExtractor(Client client)
    {
        fields = new ArrayList<>();
        fields.add(new DateField(Messages.CSVColumn_Date));
        fields.add(new AmountField(Messages.CSVColumn_Quote));
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
        Security dummy = new Security();

        for (String[] line : rawValues)
        {
            try
            {
                SecurityPrice p = extract(line, field2column);
                if (p != null)
                    dummy.addPrice(p);
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
        LocalDateTime date = getDate(Messages.CSVColumn_Date, null, rawValues, field2column);
        if (date == null)
            throw new ParseException(MessageFormat.format(Messages.CSVImportMissingField, Messages.CSVColumn_Date), 0);

        Long amount = getQuote(Messages.CSVColumn_Quote, rawValues, field2column);
        if (amount == null)
            throw new ParseException(MessageFormat.format(Messages.CSVImportMissingField, Messages.CSVColumn_Quote), 0);

        return new SecurityPrice(date.toLocalDate(), Math.abs(amount));
    }
}
