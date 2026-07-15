package name.abuchen.portfolio.datatransfer.csv;

import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityUpdateItem;
import name.abuchen.portfolio.datatransfer.SecurityUpdate;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.AmountField;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.Column;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.DateField;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.Field;
import name.abuchen.portfolio.model.AttributeType;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.impl.YahooFinanceQuoteFeed;

/* package */class CSVSecurityExtractor extends BaseCSVExtractor
{
    private Map<Security, SecurityUpdateItem> pendingUpdates;
    private List<AttributeType> importableAttributes;

    /* package */ CSVSecurityExtractor(Client client)
    {
        super(client, Messages.CSVDefSecurities);

        var fields = getFields();
        fields.add(new Field(FieldCode.ISIN, Messages.CSVColumn_ISIN).setOptional(true));
        fields.add(new Field(FieldCode.WKN, Messages.CSVColumn_WKN).setOptional(true));
        fields.add(new Field(FieldCode.TICKER, Messages.CSVColumn_TickerSymbol).setOptional(true));
        fields.add(new Field(FieldCode.NAME, Messages.CSVColumn_SecurityName).setOptional(true));
        fields.add(new Field(FieldCode.CURRENCY, Messages.CSVColumn_Currency).setOptional(true));
        fields.add(new Field(FieldCode.NOTE, Messages.CSVColumn_Note).setOptional(true));

        fields.add(new DateField(FieldCode.DATE, Messages.CSVColumn_DateQuote).setOptional(true));
        fields.add(new AmountField(FieldCode.QUOTE, Messages.CSVColumn_Quote, "Schluss", "Schlusskurs", "Close") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                        .setOptional(true));

        SecurityAttributeColumns.importable(getClient()).forEach(attribute -> //
                        fields.add(new Field(SecurityAttributeColumns.fieldCode(attribute),
                                        SecurityAttributeColumns.columnNames(attribute)).setOptional(true)));
    }

    @Override
    public String getCode()
    {
        return "investment-vehicle"; //$NON-NLS-1$
    }

    @Override
    public List<Item> extract(int skipLines, List<String[]> rawValues, Map<String, Column> field2column,
                    List<Exception> errors)
    {
        pendingUpdates = null;
        importableAttributes = SecurityAttributeColumns.importable(getClient()).toList();
        return super.extract(skipLines, rawValues, field2column, errors);
    }

    @Override
    void extract(List<Item> items, String[] rawValues, Map<String, Column> field2column) throws ParseException
    {
        // 1. parse pending attribute values up front (row is rejected atomically
        //    if a converter fails, before any security/price item is created)
        List<SecurityUpdate.AttributeUpdate> parsed = new ArrayList<>();
        for (AttributeType attribute : importableAttributes)
        {
            String raw = getText(SecurityAttributeColumns.fieldCode(attribute), rawValues, field2column);
            if (raw == null)
                continue; // blank cell -> not provided, never clears

            Object value;
            try
            {
                value = attribute.getConverter().fromString(raw);
            }
            catch (IllegalArgumentException e)
            {
                throw new ParseException(MessageFormat.format(Messages.CSVFormatInvalid,
                                SecurityAttributeColumns.preferredColumnName(attribute), raw), 0);
            }

            if (value != null)
                parsed.add(new SecurityUpdate.AttributeUpdate(attribute, value));
        }

        var security = getSecurity(rawValues, field2column, s -> {
            s.setCurrencyCode(getCurrencyCode(FieldCode.CURRENCY, rawValues, field2column));

            var note = getText(FieldCode.NOTE, rawValues, field2column);
            s.setNote(note);

            var tickerSymbol = getText(FieldCode.TICKER, rawValues, field2column);
            if (tickerSymbol != null && !tickerSymbol.isBlank())
            {
                s.setTickerSymbol(tickerSymbol);
                s.setFeed(YahooFinanceQuoteFeed.ID);
            }

            // set attributes directly on the fresh, still-unattached security
            for (SecurityUpdate.AttributeUpdate u : parsed)
                u.applyTo(s);

            items.add(new Extractor.SecurityItem(s));
        });

        if (security == null)
            throw new ParseException(MessageFormat.format(Messages.CSVImportMissingSecurity,
                            new StringJoiner(", ").add(Messages.CSVColumn_ISIN) //$NON-NLS-1$
                                            .add(Messages.CSVColumn_TickerSymbol).add(Messages.CSVColumn_WKN)
                                            .toString()),
                            0);

        // 2. for existing securities, collect changes into a deferred update item.
        //    use wasSecurityCreated (not the creation callback) so the 2nd row of a
        //    freshly created security is also treated as "new", not "existing"
        if (!wasSecurityCreated(security))
            collectUpdate(items, security, parsed, rawValues, field2column);

        getSecurityPrice(FieldCode.DATE, rawValues, field2column)
                        .ifPresent(price -> items.add(new SecurityPriceItem(security, price)));
    }

    private void collectUpdate(List<Item> items, Security security,
                    List<SecurityUpdate.AttributeUpdate> attributeUpdates, String[] rawValues,
                    Map<String, Column> field2column)
    {
        if (pendingUpdates == null)
            pendingUpdates = new HashMap<>();

        // build this row's desired changes, suppressing no-ops. the baseline is
        // deliberately the security's stored value, not the value a previous row
        // claimed: a row that merely restates what is already stored is treated
        // as "no change" and therefore never competes for conflict detection. as
        // a consequence, if one row changes an attribute and another row restates
        // the stored value, the change wins silently rather than being flagged as
        // a conflict. that only matters when the same existing security appears in
        // several rows of one file (uncommon for master data) and is the intended
        // semantic; switching to a stated-value baseline would require tracking
        // suppressed values separately from the applied updates.
        List<SecurityUpdate> rowUpdates = new ArrayList<>();

        for (SecurityUpdate.AttributeUpdate u : attributeUpdates)
        {
            Object current = security.getAttributes().get(u.type());
            if (!Objects.equals(current, u.value()))
                rowUpdates.add(u);
        }

        var note = getText(FieldCode.NOTE, rawValues, field2column);
        if (note != null && !Objects.equals(note, security.getNote()))
            rowUpdates.add(new SecurityUpdate.NoteUpdate(note));

        if (rowUpdates.isEmpty())
            return;

        SecurityUpdateItem item = pendingUpdates.get(security);
        if (item == null)
        {
            item = new SecurityUpdateItem(security, new ArrayList<>(rowUpdates));
            pendingUpdates.put(security, item);
            items.add(item);
            return;
        }

        // merge into the existing item; conflicting value for same label -> failure
        for (SecurityUpdate u : rowUpdates)
            mergeOrConflict(item, u, security);
    }

    private void mergeOrConflict(SecurityUpdateItem item, SecurityUpdate incoming, Security security)
    {
        for (SecurityUpdate existing : item.getUpdates())
        {
            if (existing instanceof SecurityUpdate.AttributeUpdate ea
                            && incoming instanceof SecurityUpdate.AttributeUpdate ia
                            && ea.type().equals(ia.type()))
            {
                if (!Objects.equals(ea.value(), ia.value()) && !item.isFailure())
                    item.setFailureMessage(MessageFormat.format(Messages.CSVSecurityUpdateConflict,
                                    SecurityAttributeColumns.preferredColumnName(ia.type()), security.getName()));
                return; // same attribute already present
            }
            if (existing instanceof SecurityUpdate.NoteUpdate en
                            && incoming instanceof SecurityUpdate.NoteUpdate in)
            {
                if (!Objects.equals(en.note(), in.note()) && !item.isFailure())
                    item.setFailureMessage(MessageFormat.format(Messages.CSVSecurityUpdateConflict,
                                    Messages.CSVColumn_Note, security.getName()));
                return;
            }
        }
        item.getUpdates().add(incoming);
    }
}
