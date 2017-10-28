package name.abuchen.portfolio.datatransfer.pdf;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.SecurityCache;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Values;

public abstract class AbstractPDFExtractor implements Extractor
{
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d.M.yyyy", Locale.GERMANY); //$NON-NLS-1$

    private final NumberFormat numberFormat = NumberFormat.getInstance(Locale.GERMANY);

    private final Client client;
    private SecurityCache securityCache;

    private final List<String> bankIdentifier = new ArrayList<>();
    private final List<DocumentType> documentTypes = new ArrayList<>();

    public AbstractPDFExtractor(Client client)
    {
        this.client = client;
    }

    protected final void addDocumentTyp(DocumentType type)
    {
        this.documentTypes.add(type);
    }

    protected final void addBankIdentifier(String identifier)
    {
        this.bankIdentifier.add(identifier);
    }

    public List<String> getBankIdentifier()
    {
        return bankIdentifier;
    }

    public String getPDFAuthor()
    {
        return null;
    }

    @Override
    public String getFilterExtension()
    {
        return "*.pdf"; //$NON-NLS-1$
    }

    @Override
    public List<Item> extract(List<Extractor.InputFile> files, List<Exception> errors)
    {
        // careful: security cache makes extractor stateful
        securityCache = new SecurityCache(client);

        List<Item> results = new ArrayList<>();
        for (Extractor.InputFile f : files)
        {
            if (!(f instanceof PDFInputFile))
                throw new IllegalArgumentException();

            PDFInputFile inputFile = (PDFInputFile) f;

            String text = inputFile.getText();
            results.addAll(extract(inputFile.getFile().getName(), text, errors));
        }

        results.addAll(securityCache.createMissingSecurityItems(results));

        securityCache = null;

        return results;
    }

    private final List<Item> extract(String filename, String text, List<Exception> errors)
    {
        try
        {
            checkBankIdentifier(filename, text);

            List<Item> items = parseDocumentTypes(documentTypes, filename, text);

            if (items.isEmpty())
            {
                errors.add(new UnsupportedOperationException(
                                MessageFormat.format(Messages.PDFdbMsgCannotDetermineFileType, filename)));
            }

            for (Item item : items)
                item.getSubject().setNote(filename);

            return items;
        }
        catch (IllegalArgumentException e)
        {
            errors.add(new IllegalArgumentException(e.getMessage() + " @ " + filename, e)); //$NON-NLS-1$
            return Collections.emptyList();
        }
        catch (UnsupportedOperationException e)
        {
            errors.add(e);
            return Collections.emptyList();
        }
    }

    protected final List<Item> parseDocumentTypes(List<DocumentType> documentTypes, String filename, String text)
    {
        List<Item> items = new ArrayList<>();
        for (DocumentType type : documentTypes)
        {
            if (type.matches(text))
                type.parse(filename, items, text);
        }
        return items;
    }

    private void checkBankIdentifier(String filename, String text)
    {
        if (bankIdentifier.isEmpty())
            bankIdentifier.add(getLabel());

        for (String identifier : bankIdentifier)
            if (text.contains(identifier))
                return;

        throw new UnsupportedOperationException( //
                        MessageFormat.format(Messages.PDFMsgFileNotSupported, filename, getLabel()));
    }

    protected Security getOrCreateSecurity(Map<String, String> values)
    {
        String isin = values.get("isin"); //$NON-NLS-1$
        if (isin != null)
            isin = isin.trim();

        String tickerSymbol = values.get("tickerSymbol"); //$NON-NLS-1$
        if (tickerSymbol != null)
            tickerSymbol = tickerSymbol.trim();

        String wkn = values.get("wkn"); //$NON-NLS-1$
        if (wkn != null)
            wkn = wkn.trim();

        String name = values.get("name"); //$NON-NLS-1$
        if (name != null)
            name = name.trim();

        Security security = securityCache.lookup(isin, tickerSymbol, wkn, name, () -> {
            Security s = new Security();
            s.setCurrencyCode(asCurrencyCode(values.get("currency"))); //$NON-NLS-1$
            return s;
        });

        if (security == null)
            throw new IllegalArgumentException("Unable to construct security: " + values.toString()); //$NON-NLS-1$

        return security;
    }

    protected long asShares(String value)
    {
        try
        {
            return Math.round(numberFormat.parse(value).doubleValue() * Values.Share.factor());
        }
        catch (ParseException e)
        {
            throw new IllegalArgumentException(e);
        }
    }

    protected String asCurrencyCode(String currency)
    {
        // ensure that the security is always created with a valid currency code
        if (currency == null)
            return client.getBaseCurrency();

        CurrencyUnit unit = CurrencyUnit.getInstance(currency.trim());
        return unit == null ? client.getBaseCurrency() : unit.getCurrencyCode();
    }

    /* protected */long asAmount(String value)
    {
        try
        {
            return Math.abs(Math.round(numberFormat.parse(value).doubleValue() * Values.Amount.factor()));
        }
        catch (ParseException e)
        {
            throw new IllegalArgumentException(e);
        }
    }

    /* protected */BigDecimal asExchangeRate(String value)
    {
        try
        {
            return BigDecimal.valueOf(numberFormat.parse(value).doubleValue());
        }
        catch (ParseException e)
        {
            throw new IllegalArgumentException(e);
        }
    }

    /* protected */LocalDate asDate(String value)
    {
        return LocalDate.parse(value, DATE_FORMAT);
    }
}
