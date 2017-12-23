package name.abuchen.portfolio.datatransfer.pdf;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;

public class HelloBankPDFExtractor extends AbstractPDFExtractor
{
    public HelloBankPDFExtractor(Client client) throws IOException
    {
        super(client);

        addBankIdentifier("Hellobank"); //$NON-NLS-1$
        addBankIdentifier("Hello bank!"); //$NON-NLS-1$

        addBuyTransaction();
        addSellTransaction();
        addDividendTransaction();
        addInboundDelivery();
    }

    @SuppressWarnings("nls")
    private void addBuyTransaction()
    {
        DocumentType type = new DocumentType("Geschäftsart: Kauf");
        this.addDocumentTyp(type);

        Block block = new Block("Geschäftsart: Kauf");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.BUY);
                            return entry;
                        })

                        .section("isin", "name", "currency") //
                        .match("Titel: (?<isin>\\S*) (?<name>.*)$") //
                        .match("Kurs: [\\d+,.]* (?<currency>\\w{3}+) *")
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("shares") //
                        .match("^Zugang: (?<shares>[\\d+,.]*) Stk.*")
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .section("amount", "currency") //
                        .match("Zu Lasten .* -(?<amount>[\\d+,.]*) (?<currency>\\w{3}+) *$").assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .section("date") //
                        .match("Handelszeit: (?<date>\\d+.\\d+.\\d{4}+).*")
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        .section("fee", "currency") //
                        .optional() //
                        .match("Grundgebühr: -(?<fee>[\\d+,.]*) (?<currency>\\w{3}+) *")
                        .assign((t, v) -> t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.FEE,
                                                        Money.of(asCurrencyCode(v.get("currency")),
                                                                        asAmount(v.get("fee"))))))

                        .section("fee", "currency") //
                        .optional() //
                        .match("Fremde Spesen: -(?<fee>[\\d+,.]*) (?<currency>\\w{3}+) *")
                        .assign((t, v) -> t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.FEE,
                                                        Money.of(asCurrencyCode(v.get("currency")),
                                                                        asAmount(v.get("fee"))))))

                        .section("gross", "currency", "exchangeRate") //
                        .optional() //
                        .match("Kurswert: -(?<gross>[\\d+,.-]*) (?<currency>\\w{3}+) *") //
                        .match("-[\\d+,.-]* \\w{3}+ *")
                        .match("Devisenkurs: (?<exchangeRate>[\\d+,.]*) \\(\\d+.\\d+.\\d{4}+\\) -[\\d+,.]* \\w{3}+ *")
                        .assign((t, v) -> {
                            long gross = asAmount(v.get("gross"));
                            String currency = asCurrencyCode(v.get("currency"));
                            BigDecimal exchangeRate = BigDecimal.ONE.divide(asExchangeRate(v.get("exchangeRate")), 10,
                                            BigDecimal.ROUND_HALF_UP);

                            PortfolioTransaction tx = t.getPortfolioTransaction();
                            if (currency.equals(tx.getSecurity().getCurrencyCode()))
                            {
                                long convertedGross = BigDecimal.valueOf(gross).multiply(exchangeRate)
                                                .setScale(0, BigDecimal.ROUND_HALF_UP).longValue();

                                tx.addUnit(new Unit(Unit.Type.GROSS_VALUE,
                                                Money.of(tx.getCurrencyCode(), convertedGross),
                                                Money.of(currency, gross), exchangeRate));
                            }
                        })

                        .wrap(BuySellEntryItem::new));
    }

    @SuppressWarnings("nls")
    private void addSellTransaction()
    {
        DocumentType type = new DocumentType("Geschäftsart: Verkauf");
        this.addDocumentTyp(type);

        Block block = new Block("Geschäftsart: Verkauf");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.SELL);
                            return entry;
                        })

                        .section("isin", "name", "currency") //
                        .match("Titel: (?<isin>\\S*) (?<name>.*)$") //
                        .match("Kurs: [\\d+,.]* (?<currency>\\w{3}+) *")
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("shares") //
                        .match("^Abgang: (?<shares>[\\d+,.]*) Stk.*")
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .section("amount", "currency") //
                        .match("Zu Gunsten .* (?<amount>[\\d+,.]*) (?<currency>\\w{3}+) *$").assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .section("date") //
                        .match("Handelszeit: (?<date>\\d+.\\d+.\\d{4}+).*")
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        .section("fee", "currency") //
                        .optional() //
                        .match("Grundgebühr: -(?<fee>[\\d+,.]*) (?<currency>\\w{3}+) *")
                        .assign((t, v) -> t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.FEE,
                                                        Money.of(asCurrencyCode(v.get("currency")),
                                                                        asAmount(v.get("fee"))))))

                        .section("fee", "currency") //
                        .optional() //
                        .match("Fremde Spesen: -(?<fee>[\\d+,.]*) (?<currency>\\w{3}+) *")
                        .assign((t, v) -> t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.FEE,
                                                        Money.of(asCurrencyCode(v.get("currency")),
                                                                        asAmount(v.get("fee"))))))

                        .section("fee", "currency") //
                        .optional() //
                        .match("Eigene Spesen: -(?<fee>[\\d+,.]*) (?<currency>\\w{3}+) *")
                        .assign((t, v) -> t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.FEE,
                                                        Money.of(asCurrencyCode(v.get("currency")),
                                                                        asAmount(v.get("fee"))))))

                        .section("gross", "tax", "currency", "exchangeRate") //
                        // .optional() //
                        .match("Kurswert: (?<gross>[\\d+,.-]*) (?<currency>\\w{3}+) *") //
                        .match("Kapitalertragsteuer: -(?<tax>[\\d+,.-]*) \\w{3}+ *") //
                        .match("[\\d+,.-]* \\w{3}+ *") //
                        .match("Devisenkurs: (?<exchangeRate>[\\d+,.]*) \\(\\d+.\\d+.\\d{4}+\\) [\\d+,.]* \\w{3}+ *")
                        .assign((t, v) -> {
                            long gross = asAmount(v.get("gross"));
                            long tax = asAmount(v.get("tax"));
                            String currency = asCurrencyCode(v.get("currency"));
                            BigDecimal exchangeRate = BigDecimal.ONE.divide(asExchangeRate(v.get("exchangeRate")), 10,
                                            BigDecimal.ROUND_HALF_UP);

                            PortfolioTransaction tx = t.getPortfolioTransaction();
                            if (currency.equals(tx.getSecurity().getCurrencyCode()))
                            {
                                long convertedGross = BigDecimal.valueOf(gross).multiply(exchangeRate)
                                                .setScale(0, BigDecimal.ROUND_HALF_UP).longValue();
                                tx.addUnit(new Unit(Unit.Type.GROSS_VALUE,
                                                Money.of(tx.getCurrencyCode(), convertedGross),
                                                Money.of(currency, gross), exchangeRate));

                                long convertedTax = BigDecimal.valueOf(tax).multiply(exchangeRate)
                                                .setScale(0, BigDecimal.ROUND_HALF_UP).longValue();
                                tx.addUnit(new Unit(Unit.Type.TAX, Money.of(tx.getCurrencyCode(), convertedTax),
                                                Money.of(currency, tax), exchangeRate));
                            }
                            else
                            {
                                long convertedTax = BigDecimal.valueOf(tax).multiply(exchangeRate)
                                                .setScale(0, BigDecimal.ROUND_HALF_UP).longValue();
                                tx.addUnit(new Unit(Unit.Type.TAX, Money.of(tx.getCurrencyCode(), convertedTax)));
                            }
                        })

                        .wrap(BuySellEntryItem::new));
    }

    @SuppressWarnings("nls")
    private void addDividendTransaction()
    {
        DocumentType type = new DocumentType("Geschäftsart: Ertrag", (context, lines) -> {
            Pattern exchangeRatePattern = Pattern.compile(
                            "Devisenkurs: (?<exchangeRate>[\\d+,.]*) \\(\\d+.\\d+.\\d{4}+\\) [\\d+,.]* \\w{3}+ *");

            for (String line : lines)
            {
                Matcher matcher = exchangeRatePattern.matcher(line);
                if (matcher.matches())
                    context.put("exchangeRate", matcher.group(1));
            }
        });
        this.addDocumentTyp(type);

        Block block = new Block("Geschäftsart: Ertrag");
        type.addBlock(block);

        BiConsumer<AccountTransaction, Map<String, String>> taxProcessor = (t, v) -> {

            long tax = asAmount(v.get("tax"));
            String currency = asCurrencyCode(v.get("currency"));

            // logic: if taxes are in the transaction currency, simply add them
            // if taxes are in forex then convert them, but add them with forex
            // only if the security is actually also in forex

            if (currency.equals(t.getCurrencyCode()))
            {
                t.addUnit(new Unit(Unit.Type.TAX, Money.of(t.getCurrencyCode(), tax)));
            }
            else
            {
                String exchangeRateString = type.getCurrentContext().get("exchangeRate");

                if (exchangeRateString != null)
                {
                    BigDecimal exchangeRate = BigDecimal.ONE.divide(asExchangeRate(exchangeRateString), 10,
                                    BigDecimal.ROUND_HALF_UP);

                    if (currency.equals(t.getSecurity().getCurrencyCode()))
                    {
                        long convertedTax = BigDecimal.valueOf(tax).multiply(exchangeRate)
                                        .setScale(0, BigDecimal.ROUND_HALF_UP).longValue();
                        t.addUnit(new Unit(Unit.Type.TAX, Money.of(t.getCurrencyCode(), convertedTax),
                                        Money.of(currency, tax), exchangeRate));
                    }
                    else
                    {
                        long convertedTax = BigDecimal.valueOf(tax).multiply(exchangeRate)
                                        .setScale(0, BigDecimal.ROUND_HALF_UP).longValue();
                        t.addUnit(new Unit(Unit.Type.TAX, Money.of(t.getCurrencyCode(), convertedTax)));
                    }
                }
            }
        };

        block.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction transaction = new AccountTransaction();
                            transaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return transaction;
                        })

                        .section("isin", "name", "currency") //
                        .match("Titel: (?<isin>\\S*) (?<name>.*)$") //
                        .match("Dividende: [\\d+,.]* (?<currency>\\w{3}+) *")
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("shares") //
                        .match("^(Abgang: )?(?<shares>[\\d+,.]*) Stk$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .section("amount", "currency")
                        .match("Zu Gunsten .* (?<amount>[\\d+,.]*) (?<currency>\\w{3}+) *$").assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .section("date") //
                        .match("Valuta (?<date>\\d+.\\d+.\\d{4}+)") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        // FIXME should be fee (not tax) -> change once dividend
                        // transactions support fees
                        .section("tax", "currency") //
                        .optional() //
                        .match("Inkassoprovision: -(?<tax>[\\d+,.]*) (?<currency>\\w{3}+) *").assign(taxProcessor)

                        .section("tax", "currency") //
                        .optional() //
                        .match("Umsatzsteuer: -(?<tax>[\\d+,.]*) (?<currency>\\w{3}+) *").assign(taxProcessor)

                        .section("tax", "currency") //
                        .optional() //
                        .match("Fremde Spesen: -(?<tax>[\\d+,.-]*) (?<currency>\\w{3}+) *") //
                        .assign(taxProcessor)

                        .section("tax", "currency") //
                        .optional() //
                        .match("KESt Ausländische Dividende: -(?<tax>[\\d+,.]*) (?<currency>\\w{3}+) *") //
                        .assign(taxProcessor)

                        .section("tax", "currency") //
                        .optional() //
                        .match("Quellensteuer[^.]*: -(?<tax>[\\d+,.]*) (?<currency>\\w{3}+) *") //
                        .assign(taxProcessor)

                        .section("gross", "currency") //
                        .optional() //
                        .match("Bruttoertrag: (?<gross>[\\d+,.-]*) (?<currency>\\w{3}+) *").assign((t, v) -> {

                            long gross = asAmount(v.get("gross"));
                            String currency = asCurrencyCode(v.get("currency"));

                            // record fx only if security currency actually
                            // matches the fx currency of the transaction (many
                            // users actually do not maintain the security in
                            // the fx currency)

                            if (currency.equals(t.getSecurity().getCurrencyCode()))
                            {
                                String exchangeRateString = type.getCurrentContext().get("exchangeRate");

                                if (exchangeRateString != null)
                                {
                                    BigDecimal exchangeRate = BigDecimal.ONE.divide(asExchangeRate(exchangeRateString),
                                                    10, BigDecimal.ROUND_HALF_UP);

                                    long convertedGross = BigDecimal.valueOf(gross).multiply(exchangeRate)
                                                    .setScale(0, BigDecimal.ROUND_HALF_UP).longValue();
                                    t.addUnit(new Unit(Unit.Type.GROSS_VALUE,
                                                    Money.of(t.getCurrencyCode(), convertedGross),
                                                    Money.of(currency, gross), exchangeRate));
                                }
                            }
                        })

                        .wrap(TransactionItem::new));
    }

    @SuppressWarnings("nls")
    private void addInboundDelivery()
    {
        DocumentType type = new DocumentType("Freier Erhalt");
        this.addDocumentTyp(type);

        Block block = new Block("Gesch.ftsart: Freier Erhalt");
        type.addBlock(block);
        block.set(new Transaction<PortfolioTransaction>()

                        .subject(() -> {
                            PortfolioTransaction transaction = new PortfolioTransaction();
                            transaction.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
                            return transaction;
                        })

                        .section("isin", "name", "currency") //
                        .match("Titel: (?<isin>\\S*) (?<name>.*)$") //
                        .match("steuerlicher Anschaffungswert: [\\d+,.-]* (?<currency>\\w{3}+) *")
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("shares") //
                        .match("^Zugang: (?<shares>[\\d+,.]*) Stk.*")
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .section("date") //
                        .match("Kassatag: (?<date>\\d+.\\d+.\\d{4}+).*")
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        .section("amount", "currency") //
                        .match("steuerlicher Anschaffungswert: (?<amount>[\\d+,.-]*) (?<currency>\\w{3}+) *")
                        .assign((t, v) -> t.setMonetaryAmount(
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")))))

                        .wrap(TransactionItem::new));
    }

    @Override
    public String getLabel()
    {
        return "Hello Bank"; //$NON-NLS-1$
    }
}
