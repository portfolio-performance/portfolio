package name.abuchen.portfolio.datatransfer.csv;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.text.ParseException;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.SecurityCache;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.Column;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.Field;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.FieldFormat;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.util.Isin;
import name.abuchen.portfolio.util.TextUtil;

public abstract class CSVExtractor implements Extractor
{
    public abstract List<Field> getFields();

    public abstract List<Item> extract(int skipLines, List<String[]> rawValues, Map<String, Column> field2column,
                    List<Exception> errors);

    public abstract String getCode();

    @Override
    public List<Item> extract(SecurityCache securityCache, Extractor.InputFile file, List<Exception> errors)
    {
        throw new UnsupportedOperationException();
    }

    protected final String getText(String name, String[] rawValues, Map<String, Column> field2column)
    {
        Column column = field2column.get(name);
        if (column == null)
            return null;

        int columnIndex = column.getColumnIndex();

        if (columnIndex < 0 || columnIndex >= rawValues.length)
            return null;

        String value = rawValues[columnIndex];
        return value != null && value.trim().length() == 0 ? null : value;
    }

    protected final String getISIN(String name, String[] rawValues, Map<String, Column> field2column)
    {
        Column column = field2column.get(name);
        if (column == null)
            return null;

        int columnIndex = column.getColumnIndex();

        if (columnIndex < 0 || columnIndex >= rawValues.length)
            return null;

        String value = rawValues[columnIndex];
        if (value == null)
            return null;

        value = value.trim().toUpperCase();

        Pattern pattern = Pattern.compile("\\b(" + Isin.PATTERN + ")\\b"); //$NON-NLS-1$ //$NON-NLS-2$
        Matcher matcher = pattern.matcher(value);
        if (matcher.find())
            value = matcher.group(1);

        return value.length() == 0 ? null : value;
    }

    protected final Long getAmount(String name, String[] rawValues, Map<String, Column> field2column)
                    throws ParseException
    {
        return getValue(name, rawValues, field2column, Values.Amount);
    }

    protected final Long getQuote(String name, String[] rawValues, Map<String, Column> field2column)
                    throws ParseException
    {
        return getValue(name, rawValues, field2column, Values.Quote);
    }

    protected final Long getValue(String name, String[] rawValues, Map<String, Column> field2column,
                    Values<Long> values) throws ParseException
    {
        String value = getText(name, rawValues, field2column);
        if (value == null)
            return null;

        try
        {
            Number num = (Number) field2column.get(name).getFormat().getFormat()
                            .parseObject(TextUtil.stripNonNumberCharacters(value));
            return Long.valueOf((long) Math.round(num.doubleValue() * values.factor()));
        }
        catch (ParseException e)
        {
            // Improve error message by adding context
            throw new ParseException(MessageFormat.format(Messages.MsgErrorParseErrorWithGivenPattern, value,
                            field2column.get(name).getFormat().toPattern()), e.getErrorOffset());
        }

    }

    protected final LocalDateTime getDate(String dateColumn, String timeColumn, String[] rawValues,
                    Map<String, Column> field2column) throws ParseException
    {
        String dateValue = getText(dateColumn, rawValues, field2column);
        if (dateValue == null)
            return null;

        LocalDateTime result;
        try
        {
            Date date = (Date) field2column.get(dateColumn).getFormat().getFormat().parseObject(dateValue);
            result = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
        }
        catch (ParseException e)
        {
            // Improve error message by adding context
            throw new ParseException(MessageFormat.format(Messages.MsgErrorParseErrorWithGivenPattern, dateValue,
                            field2column.get(dateColumn).getFormat().toPattern()), e.getErrorOffset());
        }

        if (timeColumn == null)
            return result;

        String timeValue = getText(timeColumn, rawValues, field2column);
        if (timeValue != null)
        {
            String[] timeToks = timeValue.split(":"); //$NON-NLS-1$
            if (timeToks.length > 1)
            {
                try
                {
                    int hour = Integer.parseInt(timeToks[0]);
                    int minute = Integer.parseInt(timeToks[1]);

                    result = result.withHour(hour).withMinute(minute);
                }
                catch (NumberFormatException | DateTimeException ignore)
                {
                    // ignore time, just use the date - not parseable
                }
            }
        }

        return result;
    }

    protected final BigDecimal getBigDecimal(String name, String[] rawValues, Map<String, Column> field2column)
                    throws ParseException
    {
        String value = getText(name, rawValues, field2column);
        if (value == null)
            return null;

        try
        {
            Number num = (Number) field2column.get(name).getFormat().getFormat().parseObject(value);
            return BigDecimal.valueOf(num.doubleValue());
        }
        catch (ParseException e)
        {
            // Improve error message by adding context
            throw new ParseException(MessageFormat.format(Messages.MsgErrorParseErrorWithGivenPattern, value,
                            field2column.get(name).getFormat().toPattern()), e.getErrorOffset());
        }
    }

    protected final Long getShares(String name, String[] rawValues, Map<String, Column> field2column)
                    throws ParseException
    {
        Long value = getValue(name, rawValues, field2column, Values.Share);
        if (value == null)
            return null;
        return Math.abs(value);
    }

    @SuppressWarnings("unchecked")
    protected final <E extends Enum<E>> E getEnum(String name, Class<E> type, String[] rawValues,
                    Map<String, Column> field2column) throws ParseException
    {
        String value = getText(name, rawValues, field2column);
        if (value == null)
            return null;
        FieldFormat ff = field2column.get(name).getFormat();

        try
        {
            if (ff != null && ff.getFormat() != null)
                return (E) ff.getFormat().parseObject(value);
            else
                return Enum.valueOf(type, value);
        }
        catch (ParseException e)
        {
            // Improve error message by adding context
            throw new ParseException(MessageFormat.format(Messages.MsgErrorParseErrorWithGivenPattern, value,
                            field2column.get(name).getFormat().toPattern()), e.getErrorOffset());
        }
    }

    protected final Account getAccount(Client client, String[] rawValues, Map<String, Column> field2column)
    {
        return getAccount(client, rawValues, field2column, false);
    }

    protected final Account getAccount(Client client, String[] rawValues, Map<String, Column> field2column,
                    boolean use2nd)
    {
        String type = use2nd ? Messages.CSVColumn_AccountName2nd : Messages.CSVColumn_AccountName;
        String accountName = getText(type, rawValues, field2column);
        Account account = null;
        if (accountName != null && !accountName.isEmpty())
        {
            account = client.getAccounts().stream().filter(x -> x.getName().equals(accountName)).findFirst()
                            .orElse(null);
        }
        return account;
    }

    protected final Portfolio getPortfolio(Client client, String[] rawValues, Map<String, Column> field2column)
    {
        return getPortfolio(client, rawValues, field2column, false);
    }

    protected final Portfolio getPortfolio(Client client, String[] rawValues, Map<String, Column> field2column,
                    boolean use2nd)
    {
        String type = use2nd ? Messages.CSVColumn_PortfolioName2nd : Messages.CSVColumn_PortfolioName;
        String portfolioName = getText(type, rawValues, field2column);
        Portfolio portfolio = null;
        if (portfolioName != null && !portfolioName.isEmpty())
        {
            portfolio = client.getPortfolios().stream().filter(x -> x.getName().equals(portfolioName)).findFirst()
                            .orElse(null);
        }
        return portfolio;
    }
}
