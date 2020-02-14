package name.abuchen.portfolio.datatransfer.pdf;

import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
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
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.common.base.Strings;
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
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.util.TextUtil;

public class JSONPDFExtractor extends AbstractPDFExtractor
{
    private JPDFExtractorDefinition definition;

    public JSONPDFExtractor(Client client, String resource)
    {
        super(client);

        try (InputStreamReader in = new InputStreamReader(
                        this.getClass().getResourceAsStream("/name/abuchen/portfolio/datatransfer/pdf/" + resource), //$NON-NLS-1$
                        StandardCharsets.UTF_8))
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
                case DIVIDEND:
                    pdftx.wrap(t -> wrapAccountTransaction(t, AccountTransaction.Type.DIVIDENDS));
                    break;
                case INBOUND_DELIVERY:
                    pdftx.wrap(t -> wrapPortfolioTransaction(t, PortfolioTransaction.Type.DELIVERY_INBOUND));
                    break;
                case OUTBOUND_DELIVERY:
                    pdftx.wrap(t -> wrapPortfolioTransaction(t, PortfolioTransaction.Type.DELIVERY_OUTBOUND));
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
        security.setName(TextUtil.strip(v.get("name"))); //$NON-NLS-1$
        security.setIsin(v.get("isin")); //$NON-NLS-1$
        security.setTicker(v.get("ticker")); //$NON-NLS-1$
        security.setWkn(v.get("wkn")); //$NON-NLS-1$
        security.setCurrency(asCurrencyCode(v.get("currency"))); //$NON-NLS-1$
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

        Security security = convertToSecurity(t);
        entry.setSecurity(security);

        t.getUnits().map(u -> convertToUnit(security, t, u)).filter(Objects::nonNull)
                        .forEach(u -> entry.getPortfolioTransaction().addUnit(u));

        BuySellEntryItem item = new BuySellEntryItem(entry);
        item.setData(t);
        return item;
    }

    private Extractor.Item wrapAccountTransaction(JTransaction t, AccountTransaction.Type type)
    {
        AccountTransaction tx = new AccountTransaction();
        tx.setType(type);

        fillIn(t, tx);

        TransactionItem item = new TransactionItem(tx);
        item.setData(t);
        return item;
    }

    private Extractor.Item wrapPortfolioTransaction(JTransaction t, PortfolioTransaction.Type type)
    {
        PortfolioTransaction tx = new PortfolioTransaction();
        tx.setType(type);

        fillIn(t, tx);

        TransactionItem item = new TransactionItem(tx);
        item.setData(t);
        return item;
    }

    private void fillIn(JTransaction t, Transaction tx)
    {
        tx.setAmount(Values.Amount.factorize(t.getAmount()));
        tx.setCurrencyCode(t.getCurrency());
        tx.setShares(Values.Share.factorize(t.getShares()));

        if (t.getTime() != null)
            tx.setDateTime(t.getDate().atTime(t.getTime()));
        else
            tx.setDateTime(t.getDate().atStartOfDay());

        Security security = convertToSecurity(t);
        tx.setSecurity(security);

        t.getUnits().map(u -> convertToUnit(security, t, u)).filter(Objects::nonNull).forEach(tx::addUnit);
    }

    private Security convertToSecurity(JTransaction t)
    {
        Map<String, String> values = new HashMap<>();
        values.put("name", t.getSecurity().getName()); //$NON-NLS-1$
        values.put("isin", t.getSecurity().getIsin()); //$NON-NLS-1$
        values.put("tickerSymbol", t.getSecurity().getTicker()); //$NON-NLS-1$
        values.put("wkn", t.getSecurity().getWkn()); //$NON-NLS-1$
        values.put("currency", t.getSecurity().getCurrency()); //$NON-NLS-1$
        return getOrCreateSecurity(values);
    }

    private Transaction.Unit convertToUnit(Security security, JTransaction jtx, JTransactionUnit junit)
    {
        Money amount = null;

        if (junit.getAmount() == null || junit.getAmount() == 0d)
        {
            // if amount is not available, but fxAmount and fxRateToBase is,
            // calculate the value
            if (junit.getFxAmount() == null || junit.getFxAmount() == 0d //
                            || junit.getFxRateToBase() == null
                            || junit.getFxRateToBase().compareTo(BigDecimal.ZERO) == 0)
                return null;

            double value = BigDecimal.valueOf(junit.getFxAmount())
                            .divide(junit.getFxRateToBase(), 2, RoundingMode.HALF_DOWN).doubleValue();

            amount = Money.of(jtx.getCurrency(), Values.Amount.factorize(value));
        }
        else
        {
            amount = Money.of(jtx.getCurrency(), Values.Amount.factorize(junit.getAmount()));
        }

        if (junit.getType() != Transaction.Unit.Type.GROSS_VALUE
                        && (junit.getFxAmount() == null || junit.getFxAmount() == 0d))
            return new Transaction.Unit(junit.getType(), amount);

        // check currency
        if (Strings.isNullOrEmpty(junit.getFxCurrency()))
            return null;

        String fxCurrency = asCurrencyCode(junit.getFxCurrency());

        // two reasons to not have a fx unit:

        // a) fx part of unit is not needed if currency of security and unit
        // match

        boolean transactionAndUnitCurrencyMatch = jtx.getCurrency().equals(fxCurrency);

        // b) fx part of unit is not needed if the security is configured in the
        // currency of the transactions. However, it is important to check
        // against the resolved security as the parsed currency might differ

        boolean transactionAndSecurityCurrencyMatch = jtx.getCurrency().equals(security.getCurrencyCode());

        if (transactionAndUnitCurrencyMatch || transactionAndSecurityCurrencyMatch)
        {
            if (junit.getType() == Transaction.Unit.Type.GROSS_VALUE)
                return null;
            else
                new Transaction.Unit(junit.getType(), amount);
        }

        // check forex amount

        long forexAmount = junit.getFxAmount() != null ? Values.Amount.factorize(junit.getFxAmount()) : 0;

        if (forexAmount != 0L)
        {
            Money forex = Money.of(fxCurrency, forexAmount);

            BigDecimal fxRateToBase = junit.getFxRateToBase();
            if (fxRateToBase == null)
            {
                fxRateToBase = BigDecimal.valueOf(amount.getAmount()) //
                                .divide(BigDecimal.valueOf(forex.getAmount()), 10, RoundingMode.HALF_DOWN);
            }
            else
            {
                fxRateToBase = BigDecimal.ONE.divide(fxRateToBase, 10, RoundingMode.HALF_DOWN);
            }

            return new Transaction.Unit(junit.getType(), amount, forex, fxRateToBase);
        }
        else if (junit.getFxRateToBase() != null)
        {
            // of course this depends heavily on the quotation of the exchange
            // rate in the PDF document, but European PDF documents will most
            // likely use indirect quotation
            BigDecimal fxRateToBase = BigDecimal.ONE.divide(junit.getFxRateToBase(), 10, RoundingMode.HALF_DOWN);

            forexAmount = BigDecimal.valueOf(amount.getAmount()).multiply(junit.getFxRateToBase())
                            .setScale(0, RoundingMode.HALF_DOWN).longValue();

            return new Transaction.Unit(junit.getType(), amount, Money.of(fxCurrency, forexAmount),
                            fxRateToBase);
        }
        else
        {
            return null;
        }
    }

    @Override
    public String getLabel()
    {
        return this.definition.getName();
    }
}
