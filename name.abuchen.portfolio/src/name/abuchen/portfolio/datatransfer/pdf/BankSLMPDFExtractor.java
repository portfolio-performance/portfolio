package name.abuchen.portfolio.datatransfer.pdf;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

public class BankSLMPDFExtractor extends AbstractPDFExtractor
{
    private final DecimalFormat swissNumberFormat;

    public BankSLMPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier(getLabel());
        addBankIdentifier("Spar + Leihkasse"); //$NON-NLS-1$

        addBuyTransaction();
        addSellTransaction();
        addDividendTransaction();

        swissNumberFormat = (DecimalFormat) DecimalFormat.getInstance(new Locale("de", "CH")); //$NON-NLS-1$ //$NON-NLS-2$
        DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols();
        decimalFormatSymbols.setDecimalSeparator('.');
        decimalFormatSymbols.setGroupingSeparator('\'');
        swissNumberFormat.setDecimalFormatSymbols(decimalFormatSymbols);
    }

    @SuppressWarnings("nls")
    private void addBuyTransaction()
    {
        DocumentType type = new DocumentType("abrechnung - Kauf");
        this.addDocumentTyp(type);

        Block block = new Block("B.rsenabrechnung - Kauf");
        type.addBlock(block);
        Transaction<BuySellEntry> extractor = new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.BUY);
                            return entry;
                        })

                        .section("date", "shares", "name", "wkn", "currency")
                        .match("Wir haben f.r Sie am (?<date>\\d+.\\d+.\\d{4}+) gekauft.") //
                        .match("^(?<shares>[\\d.']+) .*$") //
                        .match("^(?<name>.*)$") //
                        .match("^Valor: (?<wkn>[^ ]*)$") //
                        .match("Total Kurswert (?<currency>\\w{3}+) .*") //
                        .assign((t, v) -> {
                            t.setDate(asDate(v.get("date")));
                            t.setShares(asShares(v.get("shares")));
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .section("amount", "currency") //
                        .match("Netto (?<currency>\\w{3}+) -(?<amount>[\\d.']+)") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                        })

                        .wrap(t -> new BuySellEntryItem(t));

        addForexGrossValue(extractor);
        addFeesSection(extractor);

        block.set(extractor);
    }

    @SuppressWarnings("nls")
    private void addSellTransaction()
    {
        DocumentType type = new DocumentType("abrechnung - Verkauf");
        this.addDocumentTyp(type);

        Block block = new Block("B.rsenabrechnung - Verkauf");
        type.addBlock(block);
        Transaction<BuySellEntry> extractor = new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.SELL);
                            return entry;
                        })

                        .section("date", "shares", "name", "wkn", "currency")
                        .match("Wir haben f.r Sie am (?<date>\\d+.\\d+.\\d{4}+) verkauft.") //
                        .match("^(?<shares>[\\d.']+) .*$") //
                        .match("^(?<name>.*)$") //
                        .match("^Valor: (?<wkn>[^ ]*)$") //
                        .match("Total Kurswert (?<currency>\\w{3}+) .*") //
                        .assign((t, v) -> {
                            t.setDate(asDate(v.get("date")));
                            t.setShares(asShares(v.get("shares")));
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .section("amount", "currency") //
                        .match("Netto (?<currency>\\w{3}+) (?<amount>[\\d.']+)") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                        })

                        .wrap(t -> new BuySellEntryItem(t));

        addForexGrossValue(extractor);
        addFeesSection(extractor);

        block.set(extractor);
    }

    @SuppressWarnings("nls")
    private void addForexGrossValue(Transaction<BuySellEntry> extractor)
    {
        extractor.section("forexSum", "forexCurrency", "grossValue", "currency", "exchangeRate") //
                        .optional() // only present if forex is available
                        .match("Total Kurswert (?<forexCurrency>\\w{3}+) (?<forexSum>[\\d.'-]+)") //
                        .match("Change .../... (?<exchangeRate>[\\d.']+) (?<currency>\\w{3}+) (?<grossValue>[\\d.'-]+)") //
                        .assign((t, v) -> {
                            Money grossValue = Money.of(asCurrencyCode(v.get("currency")),
                                            asAmount(v.get("grossValue")));
                            Money forex = Money.of(asCurrencyCode(v.get("forexCurrency")), asAmount(v.get("forexSum")));
                            BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                            Unit unit = new Unit(Unit.Type.GROSS_VALUE, grossValue, forex, exchangeRate);

                            // add gross value unit only if currency code of
                            // security actually matches
                            if (unit.getForex().getCurrencyCode()
                                            .equals(t.getPortfolioTransaction().getSecurity().getCurrencyCode()))
                                t.getPortfolioTransaction().addUnit(unit);
                        });

    }

    @SuppressWarnings("nls")
    private void addFeesSection(Transaction<BuySellEntry> extractor)
    {
        extractor.section("fees", "currency") //
                        .match("Eidg. Umsatzabgabe (?<currency>\\w{3}+) -(?<fees>[\\d.']+)") //
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE, //
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fees"))))))

                        .section("fees", "currency").optional() //
                        .match("B.rsengeb.hr (?<currency>\\w{3}+) -(?<fees>[\\d.']+)") //
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE, //
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fees"))))))

                        .section("fees", "currency").optional() //
                        .match("B.rsengeb.hr Inland (?<currency>\\w{3}+) -(?<fees>[\\d.']+)") //
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE, //
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fees"))))))

                        .section("fees", "currency").optional() //
                        .match("Eigene Courtage (?<currency>\\w{3}+) -(?<fees>[\\d.']+)") //
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE, //
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fees"))))));
    }

    @SuppressWarnings("nls")
    private void addDividendTransaction()
    {
        DocumentType type = new DocumentType("Dividende");
        this.addDocumentTyp(type);

        Block block = new Block("Dividende");
        type.addBlock(block);
        Transaction<AccountTransaction> extractor = new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction transaction = new AccountTransaction();
                            transaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return transaction;
                        })

                        .section("date", "shares", "name", "wkn", "currency")
                        .match("Am (?<date>\\d+.\\d+.\\d{4}+) wurde folgende Dividende gutgeschrieben:") //
                        .match("^.*$") //
                        .match("^(?<name>.*)$") //
                        .match("^Valor: (?<wkn>[^ ]*)$") //
                        .match("Brutto \\((?<shares>[\\d.']+) \\* ... ([\\d.']+)\\) (?<currency>\\w{3}+) ([\\d.']+)") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setShares(asShares(v.get("shares")));
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .section("amount", "currency") //
                        .match("Netto (?<currency>\\w{3}+) (?<amount>[\\d.']+)") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                        })

                        .section("fees", "currency").optional() //
                        .match(".* Verrechnungssteuer (?<currency>\\w{3}+) -(?<fees>[\\d.']+)") //
                        .assign((t, v) -> t.addUnit(new Unit(Unit.Type.TAX,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fees"))))))

                        .section("fees", "currency").optional() //
                        .match(".* Quellensteuer (?<currency>\\w{3}+) -(?<fees>[\\d.']+)") //
                        .assign((t, v) -> t.addUnit(new Unit(Unit.Type.TAX,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fees"))))))

                        .section("fees", "currency").optional() //
                        .match(".* Nicht r.ckforderbare Steuern (?<currency>\\w{3}+) -(?<fees>[\\d.']+)") //
                        .assign((t, v) -> t.addUnit(new Unit(Unit.Type.TAX,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fees"))))))

                        .section("grossValue", "forexSum", "forexCurrency", "totalValue", "currency", "exchangeRate") //
                        .optional() // only present if forex is available
                        .match("Brutto \\(([\\d.']+) \\* ... ([\\d.']+)\\) (\\w{3}+) (?<grossValue>[\\d.']+)") //
                        .match("Netto (?<forexCurrency>\\w{3}+) (?<forexSum>[\\d.']+)") //
                        .match("Change ... / ... (?<exchangeRate>[\\d.']+) (?<currency>\\w{3}+) (?<totalValue>[\\d.'-]+)") //
                        .assign((t, v) -> { // NOSONAR

                            // if we end up in the branch, then we have forex
                            // dividends and must convert taxes in local
                            // currency
                            Money totalValue = Money.of(asCurrencyCode(v.get("currency")),
                                            asAmount(v.get("totalValue")));
                            t.setMonetaryAmount(totalValue);

                            // keep tax units in case we need to convert them
                            List<Unit> tax = t.getUnits().collect(Collectors.toList());
                            t.clearUnits();

                            Money forexGrossValue = Money.of(asCurrencyCode(v.get("forexCurrency")),
                                            asAmount(v.get("grossValue")));
                            BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                            Money grossValue = Money.of(totalValue.getCurrencyCode(),
                                            Math.round(exchangeRate.doubleValue() * forexGrossValue.getAmount()));
                            Unit unit = new Unit(Unit.Type.GROSS_VALUE, grossValue, forexGrossValue, exchangeRate);
                            t.addUnit(unit);

                            // convert tax units
                            tax.stream().forEach(u -> {
                                if (u.getAmount().getCurrencyCode().equals(t.getCurrencyCode()))
                                {
                                    t.addUnit(u);
                                }
                                else
                                {
                                    Money txm = Money.of(t.getCurrencyCode(),
                                                    Math.round(exchangeRate.doubleValue() * u.getAmount().getAmount()));
                                    Unit fu = new Unit(Unit.Type.TAX, txm, u.getAmount(), exchangeRate);
                                    t.addUnit(fu);
                                }
                            });
                        })

                        .wrap(t -> new TransactionItem(t));

        block.set(extractor);
    }

    @Override
    public String getLabel()
    {
        return "Bank SLM"; //$NON-NLS-1$
    }

    @Override
    protected long asAmount(String value)
    {
        return asValue(value, Values.Amount);
    }

    @Override
    protected long asShares(String value)
    {
        return asValue(value, Values.Share);
    }

    protected long asValue(String value, Values<Long> valueType)
    {
        try
        {
            return Math.abs(Math.round(swissNumberFormat.parse(value).doubleValue() * valueType.factor()));
        }
        catch (ParseException e)
        {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    protected BigDecimal asExchangeRate(String value)
    {
        try
        {
            return BigDecimal.valueOf(swissNumberFormat.parse(value).doubleValue());
        }
        catch (ParseException e)
        {
            throw new IllegalArgumentException(e);
        }
    }

}
