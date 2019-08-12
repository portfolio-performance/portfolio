package name.abuchen.portfolio.datatransfer.pdf;

import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.gson.Gson;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.json.JPDFExtractorDefinition;
import name.abuchen.portfolio.json.JPDFExtractorDefinition.JSection;
import name.abuchen.portfolio.json.JPDFExtractorDefinition.JSectionContext;
import name.abuchen.portfolio.json.JPDFExtractorDefinition.JTransactionMatcher;
import name.abuchen.portfolio.json.JSecurity;
import name.abuchen.portfolio.json.JTransaction;
import name.abuchen.portfolio.json.JTransactionUnit;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

public class JSONPDFExtractor extends AbstractPDFExtractor
{
    private JPDFExtractorDefinition definition;

    public JSONPDFExtractor(Client client, String resource)
    {
        super(client);

        try (InputStreamReader in = new InputStreamReader(
                        this.getClass().getResourceAsStream("/name/abuchen/portfolio/datatransfer/pdf/" + resource))) //$NON-NLS-1$
        {
            definition = new Gson().fromJson(in, JPDFExtractorDefinition.class);

            // do not check static bank identifier, check on the document itself
            addBankIdentifier(""); //$NON-NLS-1$

            addTransaction();

        }
        catch (IOException e)
        {
            throw new IllegalArgumentException(e);
        }
    }

    private void addTransaction()
    {
        DocumentType document = new DocumentType(
                        definition.getPattern().stream().map(Pattern::compile).collect(Collectors.toList()));
        addDocumentTyp(document);

        for (JTransactionMatcher jtx : definition.getTransactions())
        {
            Block block = new Block(jtx.getStartWith());
            document.addBlock(block);

            PDFParser.Transaction<JTransaction> pdftx = new PDFParser.Transaction<>();
            block.set(pdftx);

            pdftx.subject(() -> {
                JTransaction t = new JTransaction();
                t.setType(jtx.getType());
                return t;
            });

            addSections(jtx, pdftx);

            switch (jtx.getType())
            {
                case PURCHASE:
                    pdftx.wrap(t -> wrapBuySell(t, PortfolioTransaction.Type.BUY));
                    break;
                case SALE:
                    pdftx.wrap(t -> wrapBuySell(t, PortfolioTransaction.Type.SELL));
                    break;
                default:
                    throw new IllegalArgumentException();
            }
        }
    }

    private void addSections(JTransactionMatcher transaction, PDFParser.Transaction<JTransaction> tx)
    {
        for (JSection section : transaction.getSections())
        {
            List<String> attributes = new ArrayList<>();
            for (String regexp : section.getPattern())
            {
                Pattern p = Pattern.compile("\\(\\?\\<([A-Za-z0-9]*)\\>"); //$NON-NLS-1$
                Matcher matcher = p.matcher(regexp);
                while (matcher.find())
                    attributes.add(matcher.group(1));
            }

            PDFParser.Section<JTransaction> sec = tx.section(attributes.toArray(new String[0]));

            if (section.isOptional())
                sec.optional();

            for (String regexp : section.getPattern())
                sec.match(regexp);

            sec.assign((t, v) -> {

                // merge static attributes into extracted values
                Map<String, String> values = new HashMap<>();
                if (section.getAttributes() != null)
                    values.putAll(section.getAttributes());
                values.putAll(v);

                if (section.getContext() == JSectionContext.SECURITY)
                    setValuesToSecurity(t, values);
                else if (section.getContext() == JSectionContext.UNIT)
                    setValuesToUnit(t, values);
                else
                    setValues(t, values);
            });
        }
    }

    private void setValues(JTransaction t, Map<String, String> v)
    {
        asDouble(v.get("amount")).ifPresent(t::setAmount); //$NON-NLS-1$
        if (v.containsKey("currency")) //$NON-NLS-1$
            t.setCurrency(v.get("currency")); //$NON-NLS-1$

        asLocalDate(v.get("date")).ifPresent(t::setDate); //$NON-NLS-1$
        asLocalTime(v.get("time")).ifPresent(t::setTime); //$NON-NLS-1$
        asDouble(v.get("shares")).ifPresent(t::setShares); //$NON-NLS-1$
    }

    private void setValuesToSecurity(JTransaction t, Map<String, String> v)
    {
        JSecurity security = new JSecurity();
        security.setName(v.get("name")); //$NON-NLS-1$
        security.setIsin(v.get("isin")); //$NON-NLS-1$
        security.setTicker(v.get("ticker")); //$NON-NLS-1$
        security.setWkn(v.get("wkn")); //$NON-NLS-1$
        t.setSecurity(security);
    }

    private void setValuesToUnit(JTransaction t, Map<String, String> v)
    {
        JTransactionUnit unit = new JTransactionUnit();

        unit.setType(Transaction.Unit.Type.valueOf(v.get("type"))); //$NON-NLS-1$
        asDouble(v.get("amount")).ifPresent(unit::setAmount); //$NON-NLS-1$
        asDouble(v.get("fxAmount")).ifPresent(unit::setFxAmount); //$NON-NLS-1$
        asDouble(v.get("fxRateToBase")).ifPresent(d -> unit.setFxRateToBase(BigDecimal.valueOf(d))); //$NON-NLS-1$

        if (v.containsKey("fxCurrency")) //$NON-NLS-1$
            unit.setFxCurrency(v.get("fxCurrency")); //$NON-NLS-1$

        t.addUnit(unit);
    }

    private Optional<Double> asDouble(String value)
    {
        if (value == null)
            return Optional.empty();

        try
        {
            NumberFormat numberFormat = NumberFormat.getInstance(Locale.GERMANY);
            return Optional.of(Math.abs(numberFormat.parse(value).doubleValue()));
        }
        catch (ParseException e)
        {
            throw new IllegalArgumentException(e);
        }
    }

    private Optional<LocalDate> asLocalDate(String value)
    {
        if (value == null)
            return Optional.empty();

        return Optional.of(asDate(value).toLocalDate());
    }

    private Optional<LocalTime> asLocalTime(String value)
    {
        if (value == null)
            return Optional.empty();

        try
        {
            return Optional.of(LocalTime.parse(value, DateTimeFormatter.ofPattern("HH:mm"))); //$NON-NLS-1$
        }
        catch (Exception e)
        {
            return Optional.of(LocalTime.parse(value, DateTimeFormatter.ofPattern("HH:mm:ss")).withSecond(0)); //$NON-NLS-1$
        }
    }

    private Extractor.Item wrapBuySell(JTransaction t, PortfolioTransaction.Type txType)
    {
        BuySellEntry entry = new BuySellEntry();
        entry.setType(txType);

        if (t.getTime() != null)
            entry.setDate(t.getDate().atTime(t.getTime()));
        else
            entry.setDate(t.getDate().atStartOfDay());

        entry.setAmount(Values.Amount.factorize(t.getAmount()));
        entry.setCurrencyCode(t.getCurrency());

        entry.setShares(Values.Share.factorize(t.getShares()));

        Map<String, String> values = new HashMap<>();
        values.put("name", t.getSecurity().getName()); //$NON-NLS-1$
        values.put("isin", t.getSecurity().getIsin()); //$NON-NLS-1$
        values.put("tickerSymbol", t.getSecurity().getTicker()); //$NON-NLS-1$
        values.put("wkn", t.getSecurity().getWkn()); //$NON-NLS-1$
        entry.setSecurity(getOrCreateSecurity(values));

        t.getUnits().forEach(unit -> entry.getPortfolioTransaction().addUnit(new Transaction.Unit(unit.getType(),
                        Money.of(t.getCurrency(), Values.Amount.factorize(unit.getAmount())))));

        BuySellEntryItem item = new BuySellEntryItem(entry);
        item.setData(t);
        return item;
    }

    @Override
    public String getLabel()
    {
        return this.definition.getName();
    }
}
