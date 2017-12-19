package name.abuchen.portfolio.datatransfer.pdf;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.SecurityCache;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Section;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Values;

class PDFExtractionContext
{
    private Client client;
    private SecurityCache securityCache;
    private Map<String, String> globalContext = new HashMap<>();
    private Map<String, String> sectionContext = new HashMap<>();
    private NumberFormat numberFormat = NumberFormat.getInstance(Locale.GERMANY);

    public PDFExtractionContext(Client client, SecurityCache securityCache)
    {
        this.client = client;
        this.securityCache = securityCache;
    }
    
    public void setNumberFormat(NumberFormat numberFormat)
    {
        this.numberFormat = numberFormat;
    }

    /**
     * Gets the current context for this parse run.
     * 
     * @return current context map
     */
    public Map<String, String> getCurrentContext()
    {
        return globalContext;
    }

    public void parseDocumentType(DocumentType documentType, String filename, List<Item> items, String text)
    {
        String[] lines = text.split("\\r?\\n"); //$NON-NLS-1$

        // reset context and parse it from this file
        globalContext.clear();
        documentType.parseContext(globalContext, filename, lines);

        for (Block block : documentType.blocks)
            parseBlock(block, filename, items, lines);

    }

    public void parseBlock(Block block, String filename, List<Item> items, String[] lines)
    {
        List<Integer> blocks = new ArrayList<>();

        for (int ii = 0; ii < lines.length; ii++)
        {
            Matcher matcher = block.marker.matcher(lines[ii]);
            if (matcher.matches())
                blocks.add(ii);
        }

        for (int ii = 0; ii < blocks.size(); ii++)
        {
            int startLine = blocks.get(ii);
            int endLine = ii + 1 < blocks.size() ? blocks.get(ii + 1) - 1 : lines.length - 1;

            parseTransaction(block.transaction, filename, items, lines, startLine, endLine);
        }
    }

    public <T> void parseTransaction(Transaction<T> transaction, String filename, List<Item> items, String[] lines,
                    int lineNoStart, int lineNoEnd)
    {
        T target = transaction.supplier.get();

        for (Section<T> section : transaction.sections)
            parseSection(section, filename, items, lines, lineNoStart, lineNoEnd, target);

        if (transaction.wrapper == null)
            throw new IllegalArgumentException("Wrapping function missing"); //$NON-NLS-1$

        Item item = transaction.wrapper.apply(target);
        if (item != null)
            items.add(item);
    }

    public <T> void parseSection(Section<T> section, String filename, List<Item> items, String[] lines, int lineNo,
                    int lineNoEnd, T target)
    {
        sectionContext.clear();

        int patternNo = 0;
        for (int ii = lineNo; ii <= lineNoEnd; ii++)
        {
            Pattern p = section.pattern.get(patternNo);
            Matcher m = p.matcher(lines[ii]);
            if (m.matches())
            {
                // extract attributes
                section.extractAttributes(sectionContext, p, m);

                // next pattern?
                patternNo++;
                if (patternNo >= section.pattern.size())
                    break;
            }
        }

        if (patternNo < section.pattern.size())
        {
            // if section is optional, ignore if patterns do not match
            if (section.isOptional)
                return;

            throw new IllegalArgumentException(MessageFormat.format(Messages.MsgErrorNotAllPatternMatched, patternNo,
                            section.pattern.size(), section.pattern.toString(), filename));
        }

        if (sectionContext.size() != section.attributes.length)
            throw new IllegalArgumentException(MessageFormat.format(Messages.MsgErrorMissingValueMatches,
                            sectionContext.keySet().toString(), Arrays.toString(section.attributes), filename));

        if (section.assignment == null)
            throw new IllegalArgumentException("Assignment function missing"); //$NON-NLS-1$

        section.assignment.accept(target, this);
    }

    public String get(String key)
    {
        return sectionContext.get(key);
    }

    public void put(String key, String value)
    {
        sectionContext.put(key, value);
    }

    protected Security getOrCreateSecurity()
    {
        String isin = sectionContext.get("isin"); //$NON-NLS-1$
        if (isin != null)
            isin = isin.trim();

        String tickerSymbol = sectionContext.get("tickerSymbol"); //$NON-NLS-1$
        if (tickerSymbol != null)
            tickerSymbol = tickerSymbol.trim();

        String wkn = sectionContext.get("wkn"); //$NON-NLS-1$
        if (wkn != null)
            wkn = wkn.trim();

        String name = sectionContext.get("name"); //$NON-NLS-1$
        if (name != null)
            name = name.trim();

        Security security = securityCache.lookup(isin, tickerSymbol, wkn, name, () -> {
            Security s = new Security();
            s.setCurrencyCode(asCurrencyCode(sectionContext.get("currency"))); //$NON-NLS-1$
            return s;
        });

        if (security == null)
            throw new IllegalArgumentException("Unable to construct security: " + sectionContext.toString()); //$NON-NLS-1$

        return security;
    }

    protected String asCurrencyCode(String currency)
    {
        // ensure that the security is always created with a valid currency
        // code
        if (currency == null)
            return client.getBaseCurrency();

        CurrencyUnit unit = CurrencyUnit.getInstance(currency.trim());
        return unit == null ? client.getBaseCurrency() : unit.getCurrencyCode();
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

    protected BigDecimal asExchangeRate(String value)
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
}
