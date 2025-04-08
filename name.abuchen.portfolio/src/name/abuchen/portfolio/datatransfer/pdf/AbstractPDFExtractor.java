package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.concatenate;
import static name.abuchen.portfolio.util.TextUtil.trim;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import com.google.common.annotations.VisibleForTesting;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.datatransfer.ExtrExchangeRate;
import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.SecurityCache;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.ParsedData;
import name.abuchen.portfolio.model.Annotated;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.CrossEntry;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.online.Factory;
import name.abuchen.portfolio.online.SecuritySearchProvider;
import name.abuchen.portfolio.online.SecuritySearchProvider.ResultItem;
import name.abuchen.portfolio.online.impl.CoinGeckoSearchProvider;
import name.abuchen.portfolio.online.impl.PortfolioReportNetSearchProvider;

public abstract class AbstractPDFExtractor implements Extractor
{
    protected static final String FAILURE = "FAILURE"; //$NON-NLS-1$

    private final NumberFormat numberFormat = NumberFormat.getInstance(Locale.GERMANY);

    private final Client client;
    private SecurityCache securityCache;

    private final List<String> bankIdentifier = new ArrayList<>();
    private final List<DocumentType> documentTypes = new ArrayList<>();

    public AbstractPDFExtractor(Client client)
    {
        this.client = client;
    }

    public final Client getClient()
    {
        return client;
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

    /* package */ NumberFormat getNumberFormat()
    {
        return numberFormat;
    }

    @Override
    public List<Item> extract(SecurityCache securityCache, Extractor.InputFile inputFile, List<Exception> errors)
    {
        // careful: security cache makes extractor stateful
        this.securityCache = securityCache;

        List<Item> results = new ArrayList<>();

        if (!(inputFile instanceof PDFInputFile))
            throw new IllegalArgumentException("input file doesn't seem to be a PDF-file but is of type " //$NON-NLS-1$
                            + inputFile.getClass().getName());

        String text = ((PDFInputFile) inputFile).getText();
        results.addAll(extract(inputFile.getFile().getName(), text, errors));

        this.securityCache = null;

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
                                MessageFormat.format(Messages.PDFdbMsgCannotDetermineFileType, getLabel(), filename)));
            }

            for (Item item : items)
            {
                Annotated subject = item.getSubject();

                if (subject instanceof Transaction tx)
                    tx.setSource(filename);
                else if (subject instanceof CrossEntry entry)
                    entry.setSource(filename);
                else if (subject.getNote() == null || trim(subject.getNote()).length() == 0)
                    item.getSubject().setNote(filename);
                else
                    item.getSubject().setNote(concatenate(trim(item.getSubject().getNote()), filename, " | ")); //$NON-NLS-1$
            }

            return items;
        }
        catch (IllegalArgumentException e)
        {
            errors.add(new IllegalArgumentException(e.getMessage() + " @ " + filename, e)); //$NON-NLS-1$
            return Collections.emptyList();
        }
        catch (NullPointerException e)
        {
            // NPE should not block further processing. Print full stack trace
            // to error log to enable further investigation

            IllegalArgumentException error = new IllegalArgumentException("NullPointerException @ " + filename, e); //$NON-NLS-1$
            PortfolioLog.error(error);
            errors.add(error);
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
            return;

        for (String identifier : bankIdentifier)
            if (text.contains(identifier))
                return;

        throw new UnsupportedOperationException( //
                        MessageFormat.format(Messages.PDFMsgFileNotSupported, filename, getLabel()));
    }

    protected Security getOrCreateSecurity(Map<String, String> values)
    {
        return getOrCreateSecurity(values, () -> new Security(null, asCurrencyCode(values.get("currency")))); //$NON-NLS-1$
    }

    /**
     * Cryptos are identified a) by the coin tickerSymbol (BTC, ETH) or directly
     * by name. Missing crypto currencies are created for use with coin gecko
     * quote feed
     */
    protected Security getOrCreateCryptoCurrency(Map<String, String> values)
    {
        // enrich values map with name to allow matching by name
        Optional<ResultItem> coin = lookupCoin(values);

        if (coin.isPresent())
            values.put("name", coin.get().getName()); //$NON-NLS-1$

        return getOrCreateSecurity(values, () -> {

            var currencyCode = asCurrencyCode(values.get("currency")); //$NON-NLS-1$

            if (coin.isPresent())
            {
                var security = coin.get().create(getClient());
                security.setCurrencyCode(currencyCode);
                return security;
            }
            else
            {
                return new Security(null, currencyCode);
            }
        });
    }

    private Optional<ResultItem> lookupCoin(Map<String, String> values)
    {
        try
        {
            String tickerSymbol = values.get("tickerSymbol").trim(); //$NON-NLS-1$

            for (SecuritySearchProvider provider : lookupCryptoProvider())
            {
                var coins = provider.getCoins();
                var candidate = coins.stream().filter(c -> c.getSymbol().equalsIgnoreCase(tickerSymbol)).findAny();
                if (candidate.isPresent())
                    return candidate;
            }
        }
        catch (IOException e)
        {
            PortfolioLog.error(e);
        }
        return Optional.empty();
    }

    @VisibleForTesting
    protected List<SecuritySearchProvider> lookupCryptoProvider()
    {
        return List.of(Factory.getSearchProvider(PortfolioReportNetSearchProvider.class),
                        Factory.getSearchProvider(CoinGeckoSearchProvider.class));
    }

    private Security getOrCreateSecurity(Map<String, String> values, Supplier<Security> factory)
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

        String nameRowTwo = values.get("nameContinued"); //$NON-NLS-1$
        if (nameRowTwo != null)
            name = name + " " + nameRowTwo.trim(); //$NON-NLS-1$

        Security security = securityCache.lookup(isin, tickerSymbol, wkn, name, factory);

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

    protected long asShares(String value, String language, String country)
    {
        return ExtractorUtils.asShares(value, language, country);
    }

    protected String asCurrencyCode(String currency)
    {
        // ensure that the security is always created with a valid currency code
        if (currency == null)
            return client.getBaseCurrency();

        CurrencyUnit unit = CurrencyUnit.getInstance(currency.trim());
        if (unit != null)
            return unit.getCurrencyCode();

        unit = CurrencyUnit.getInstanceBySymbol(currency.trim());
        return unit == null ? client.getBaseCurrency() : unit.getCurrencyCode();
    }

    protected long asAmount(String value)
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

    protected long asAmount(String value, String language, String country)
    {
        return ExtractorUtils.convertToNumberLong(value, Values.Amount, language, country);
    }

    protected ExtrExchangeRate asExchangeRate(Map<String, String> data)
    {
        return new ExtrExchangeRate(asExchangeRate(data.get("exchangeRate")), //$NON-NLS-1$
                        asCurrencyCode(data.get("baseCurrency")), //$NON-NLS-1$
                        asCurrencyCode(data.get("termCurrency"))); //$NON-NLS-1$
    }

    protected BigDecimal asExchangeRate(String value)
    {
        return asBigDecimal(value);
    }

    protected BigDecimal asBigDecimal(String value)
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

    protected LocalDateTime asDate(String value, Locale... hints)
    {
        return ExtractorUtils.asDate(value, hints);
    }

    protected LocalTime asTime(String value)
    {
        return ExtractorUtils.asTime(value);
    }

    protected LocalDateTime asDate(String date, String time)
    {
        return ExtractorUtils.asDate(date, time);
    }

    protected void processTaxEntries(Object t, Map<String, String> v, DocumentType type)
    {
        Money tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax"))); //$NON-NLS-1$ //$NON-NLS-2$
        ExtractorUtils.checkAndSetTax(tax, t, type.getCurrentContext());
    }

    protected void processFeeEntries(Object t, Map<String, String> v, DocumentType type)
    {
        Money fee = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee"))); //$NON-NLS-1$ //$NON-NLS-2$
        ExtractorUtils.checkAndSetFee(fee, t, type.getCurrentContext());
    }

    /**
     * Process withholding taxes. Bank documents typically contain multiple
     * pieces of information about withholding taxes: besides the paid
     * withholding taxes also which proportion of the withholding taxes might be
     * eligible for refund later. This method implements the following logic:
     * <ul>
     * <li>if withholding taxes are present, use only withholding taxes and
     * ignore other withholding tax information</li>
     * <li>if only information about creditable withholding taxes exist, use
     * them</li>
     * </ul>
     */
    protected void processWithHoldingTaxEntries(Object t, ParsedData data, String taxType, DocumentType type)
    {
        Money tax = Money.of(asCurrencyCode(data.get("currency")), asAmount(data.get(taxType))); //$NON-NLS-1$

        switch (taxType)
        {
            case "withHoldingTax": //$NON-NLS-1$
                if (data.getTransactionContext().getBoolean("creditableWithHoldingTax")) //$NON-NLS-1$
                    throw new IllegalArgumentException(
                                    "processing of withholding taxes must be done before creditable withholding taxes"); //$NON-NLS-1$

                ExtractorUtils.checkAndSetTax(tax, t, type.getCurrentContext());
                data.getTransactionContext().putBoolean(taxType, true);
                return;

            case "creditableWithHoldingTax": //$NON-NLS-1$
                if (!data.getTransactionContext().getBoolean("withHoldingTax")) //$NON-NLS-1$
                {
                    ExtractorUtils.checkAndSetTax(tax, t, type.getCurrentContext());
                    data.getTransactionContext().putBoolean(taxType, true);
                }
                return;

            default:
                throw new IllegalArgumentException("Unsupported withholding tax type: " + taxType); //$NON-NLS-1$
        }
    }
}
