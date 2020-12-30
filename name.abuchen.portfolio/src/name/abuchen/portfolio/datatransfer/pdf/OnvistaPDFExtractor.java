package name.abuchen.portfolio.datatransfer.pdf;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.util.Map;
import java.util.Optional;
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

@SuppressWarnings("nls")
public class OnvistaPDFExtractor extends AbstractPDFExtractor
{

    public OnvistaPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier(""); //$NON-NLS-1$
        addBankIdentifier("onvista bank"); //$NON-NLS-1$

        addBuyTransaction();
        addSellTransaction();
        addTaxReturnTransaction();
        addChangeTransaction();
        addPayingTransaction();
        addDividendTransaction();
        addTransferInTransaction();
        addCapitalReductionTransaction();
        addCapitalIncreaseTransaction();
        addAddDididendRightsTransaction();
        addRemoveDididendRightsTransaction();
        addExchangeTransaction();
        addCompensationTransaction();
        addFusionTransaction();
        addDepositTransaction();
        addAccountStatementTransaction();
        addAccountStatementTransaction2017();
        addRegistrationFeeTransaction();
        addTaxVorabpauschaleTransaction();
    }

    private void addBuyTransaction()
    {
        DocumentType type = new DocumentType("Wir haben für Sie gekauft");
        this.addDocumentTyp(type);

        Block block = new Block("Wir haben für Sie gekauft(.*)", "(Dieser Beleg wird|Finanzamt)(.*)");
        type.addBlock(block);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        block.set(pdfTransaction);
        pdfTransaction.section("name", "isin", "currency") //
                        .find("Gattungsbezeichnung ISIN") //
                        .match("(?<name>.*) (?<isin>[^ ]\\S*)$") //
                        .find("Nominal Kurs") //
                        .match("^\\w{3} ([\\d,\\.]*) (?<currency>\\w{3}) ([\\d,\\.]*)") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("notation", "shares") //
                        .find("Nominal Kurs") //
                        .match("(?<notation>^\\w{3}) (?<shares>[\\d,\\.]*) (?<currency>\\w{3}) ([\\d,\\.]*)") //
                        .assign((t, v) -> {
                            String notation = v.get("notation");
                            if (notation != null && !notation.equalsIgnoreCase("STK"))
                            {
                                // Prozent-Notierung, Workaround..
                                t.setShares((asShares(v.get("shares")) / 100));
                            }
                            else
                            {
                                t.setShares(asShares(v.get("shares")));
                            }
                        })

                        .section("date") //
                        .match("Handelstag (?<date>\\d+\\.\\d+\\.\\d{4}) (.*)") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        .section("time") //
                        .optional() //
                        .match("Handelszeit (?<time>\\d+:\\d+)(.*)") //
                        .assign((t, v) -> {
                            LocalTime time = asTime(v.get("time"));
                            t.setDate(t.getPortfolioTransaction().getDateTime().with(time));
                        })

                        .oneOf( //
                                        section -> section.attributes("amount", "currency")
                                                        .find("Wert(\\s+)Konto-Nr. Betrag zu Ihren Lasten(\\s*)$")
                                                        // @formatter:off
                                                        // 14.01.2015 172306238 EUR 59,55
                                                        // Wert Konto-Nr. Betrag zu Ihren Lasten
                                                        // 01.06.2011 172306238 EUR 6,40
                                                        // @formatter:on
                                                        .match("(\\d+\\.\\d+\\.\\d{4}) (\\d{6,12}) (?<currency>\\w{3}) (?<amount>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)$")
                                                        .assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                        }),

                                        section -> section.attributes("amount", "currency", "forex", "exchangeRate")
                                                        .find("Wert(\\s+)Konto-Nr. Devisenkurs Betrag zu Ihren Lasten(\\s*)$")
                                                        .match("(\\d+\\.\\d+\\.\\d{4}) (\\d{6,12}) .../(?<forex>\\w{3}) (?<exchangeRate>[\\d,\\.]*) (?<currency>\\w{3}) (?<amount>[\\d,\\.]*)$")
                                                        .assign((t, v) -> {

                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));

                                                            String forex = asCurrencyCode(v.get("forex"));
                                                            if (t.getPortfolioTransaction().getSecurity()
                                                                            .getCurrencyCode().equals(forex))
                                                            {
                                                                BigDecimal exchangeRate = asExchangeRate(
                                                                                v.get("exchangeRate"));
                                                                BigDecimal reverseRate = BigDecimal.ONE.divide(
                                                                                exchangeRate, 10,
                                                                                RoundingMode.HALF_DOWN);

                                                                long fxAmount = exchangeRate.multiply(BigDecimal
                                                                                .valueOf(t.getPortfolioTransaction()
                                                                                                .getAmount()))
                                                                                .setScale(0, RoundingMode.HALF_DOWN)
                                                                                .longValue();

                                                                Unit grossValue = new Unit(Unit.Type.GROSS_VALUE,
                                                                                t.getPortfolioTransaction()
                                                                                                .getMonetaryAmount(),
                                                                                Money.of(forex, fxAmount),
                                                                                reverseRate);

                                                                t.getPortfolioTransaction().addUnit(grossValue);
                                                            }
                                                        }))

                        .wrap(BuySellEntryItem::new);

        addFeesSectionsTransaction(pdfTransaction);
    }

    private void addSellTransaction()
    {
        DocumentType type = new DocumentType("Wir haben für Sie verkauft");
        this.addDocumentTyp(type);

        Block block = new Block("Wir haben für Sie verkauft(.*)", "(Dieser Beleg wird|Finanzamt)(.*)");
        type.addBlock(block);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.SELL);
            return entry;
        });

        block.set(pdfTransaction);
        pdfTransaction.section("name", "isin") //
                        .find("Gattungsbezeichnung ISIN") //
                        .match("(?<name>.*) (?<isin>[^ ]\\S*)$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("notation", "shares").find("Nominal Kurs")
                        .match("(?<notation>^\\w{3}+) (?<shares>\\d{1,3}(\\.\\d{3})*(,\\d{3,})?)(.*)") //
                        .assign((t, v) -> {
                            String notation = v.get("notation");
                            if (notation != null && !notation.equalsIgnoreCase("STK"))
                            {
                                // Prozent-Notierung, Workaround..
                                t.setShares((asShares(v.get("shares")) / 100));
                            }
                            else
                            {
                                t.setShares(asShares(v.get("shares")));
                            }
                        })

                        .section("date") //
                        .match("Handelstag (?<date>\\d+\\.\\d+\\.\\d{4}) (.*)") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        .section("time") //
                        .optional() //
                        .match("Handelszeit (?<time>\\d+:\\d+)(.*)") //
                        .assign((t, v) -> {
                            LocalTime time = asTime(v.get("time"));
                            t.setDate(t.getPortfolioTransaction().getDateTime().with(time));
                        })

                        .oneOf( //
                                        section -> section.attributes("amount", "currency")
                                                        .find("Wert(\\s+)Konto-Nr. Betrag zu Ihren Gunsten(\\s*)$")
                                                        // @formatter:off
                                                        // Wert Konto-Nr. Betrag zu Ihren Lasten
                                                        // 01.06.2011 172306238 EUR 6,40
                                                        // @formatter:on
                                                        .match("(\\d+\\.\\d+\\.\\d{4}) (\\d{6,12}) (?<currency>\\w{3}) (?<amount>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)$")
                                                        .assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                        }),

                                        section -> section.attributes("amount", "currency", "forex", "exchangeRate")
                                                        .find("Wert(\\s+)Konto-Nr. Devisenkurs Betrag zu Ihren Gunsten(\\s*)$")
                                                        .match("(\\d+\\.\\d+\\.\\d{4}) (\\d{6,12}) .../(?<forex>\\w{3}) (?<exchangeRate>[\\d,\\.]*) (?<currency>\\w{3}) (?<amount>[\\d,\\.]*)$")
                                                        .assign((t, v) -> {

                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));

                                                            String forex = asCurrencyCode(v.get("forex"));
                                                            if (t.getPortfolioTransaction().getSecurity()
                                                                            .getCurrencyCode().equals(forex))
                                                            {
                                                                BigDecimal exchangeRate = asExchangeRate(
                                                                                v.get("exchangeRate"));
                                                                BigDecimal reverseRate = BigDecimal.ONE.divide(
                                                                                exchangeRate, 10,
                                                                                RoundingMode.HALF_DOWN);

                                                                long fxAmount = exchangeRate.multiply(BigDecimal
                                                                                .valueOf(t.getPortfolioTransaction()
                                                                                                .getAmount()))
                                                                                .setScale(0, RoundingMode.HALF_DOWN)
                                                                                .longValue();

                                                                Unit grossValue = new Unit(Unit.Type.GROSS_VALUE,
                                                                                t.getPortfolioTransaction()
                                                                                                .getMonetaryAmount(),
                                                                                Money.of(forex, fxAmount),
                                                                                reverseRate);

                                                                t.getPortfolioTransaction().addUnit(grossValue);
                                                            }
                                                        }))

                        .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction);
        addFeesSectionsTransaction(pdfTransaction);
    }

    private void addChangeTransaction()
    {
        DocumentType type = new DocumentType("Bestätigung");
        this.addDocumentTyp(type);

        Block block = new Block("Bestätigung(.*)");
        type.addBlock(block);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        block.set(pdfTransaction);
        pdfTransaction.section("name", "isin") //
                        .find("Gattungsbezeichnung ISIN") //
                        .match("(?<name>.*) (?<isin>[^ ]\\S*)$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("notation", "shares") //
                        .find("Nominal Kurs")
                        .match("(?<notation>^\\w{3}+) (?<shares>\\d{1,3}(\\.\\d{3})*(,\\d{3,})?)(.*)") //
                        .assign((t, v) -> {
                            String notation = v.get("notation");
                            if (notation != null && !notation.equalsIgnoreCase("STK"))
                            {
                                // Prozent-Notierung, Workaround..
                                t.setShares((asShares(v.get("shares")) / 100));
                            }
                            else
                            {
                                t.setShares(asShares(v.get("shares")));
                            }
                        })

                        .section("date", "amount", "currency") //
                        .find("Wert(\\s+)Konto-Nr. Betrag zu Ihren Lasten(\\s*)$")
                        // 14.01.2015 172306238 EUR 59,55
                        // Wert Konto-Nr. Betrag zu Ihren Lasten
                        // 01.06.2011 172306238 EUR 6,40
                        .match("(?<date>\\d+.\\d+.\\d{4}+) (\\d{6,12}) (?<currency>\\w{3}+) (?<amount>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)$")
                        .assign((t, v) -> {
                            t.setDate(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .wrap(BuySellEntryItem::new);

        addFeesSectionsTransaction(pdfTransaction);
    }

    private void addPayingTransaction()
    {
        DocumentType type = new DocumentType("Gutschriftsanzeige");
        this.addDocumentTyp(type);

        Block block = new Block("Gutschriftsanzeige(.*)");
        type.addBlock(block);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.SELL);
            return entry;
        });

        block.set(pdfTransaction);
        pdfTransaction.section("name", "isin") //
                        .find("Gattungsbezeichnung (.*) ISIN") //
                        .match("(?<name>.*) (.*) (?<isin>[^ ]\\S*)$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("notation", "shares") //
                        .find("Nominal Einlösung(.*)$") //
                        .match("(?<notation>^\\w{3}+) (?<shares>\\d{1,3}(\\.\\d{3})*(,\\d{3,})?)(.*)$")
                        .assign((t, v) -> {
                            String notation = v.get("notation");
                            if (notation != null && !notation.equalsIgnoreCase("STK"))
                            {
                                // Prozent-Notierung, Workaround..
                                t.setShares((asShares(v.get("shares")) / 100));
                            }
                            else
                            {
                                t.setShares(asShares(v.get("shares")));
                            }
                        })

                        .section("date", "amount", "currency") //
                        .find("Wert(\\s+)Konto-Nr. Betrag zu Ihren Gunsten$")
                        // 17.11.2014 172306238 EUR 51,85
                        .match("(?<date>\\d+.\\d+.\\d{4}+) (\\d{6,12}) (?<currency>\\w{3}+) (?<amount>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)")
                        .assign((t, v) -> {
                            t.setDate(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction);
    }

    private void addDividendTransaction()
    {
        DocumentType type = new DocumentType("Erträgnisgutschrift", (context, lines) -> {

            // save exchange rate in order to later convert tax entries

            Pattern pCurrency = Pattern.compile("^.* (?<currencypair>\\w{3}+/\\w{3}+) (?<exchangeRate>[\\d,.]+) .*$");
            for (String line : lines)
            {
                Matcher m = pCurrency.matcher(line);
                if (m.matches())
                    context.put(m.group("currencypair"), m.group("exchangeRate"));
            }
        });
        this.addDocumentTyp(type);

        // Erträgnisgutschrift allein ist nicht gut hier, da es schon in der
        // Kopfzeile steht. In neuen Dokumenten steht "Erträgnisgutschrift"
        // alleine auf einer Zeile
        Block block = new Block(
                        "Dividendengutschrift.*|Kupongutschrift.*|Erträgnisgutschrift.*(\\d+.\\d+.\\d{4})|Erträgnisgutschrift");
        type.addBlock(block);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            AccountTransaction transaction = new AccountTransaction();
            transaction.setType(AccountTransaction.Type.DIVIDENDS);
            return transaction;
        });

        block.set(pdfTransaction);
        pdfTransaction

                        .section("name", "isin", "currency") //
                        .find("Gattungsbezeichnung(.*) ISIN")
                        // Commerzbank AG Inhaber-Aktien o.N. DE000CBK1001
                        // 5,5% TUI AG Wandelanl.v.2009(2014) 17.11.2014
                        // 17.11.2010 DE000TUAG117
                        .match("(?<name>.*?) (\\d+.\\d+.\\d{4} ){0,2}(?<isin>[^ ]\\S*)$") //
                        .match(".* (?<currency>\\w{3}+) [\\d,.]+$")
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("notation", "shares", "date") //
                        .match("(?<notation>^\\w{3}+) (?<shares>[\\d.]+(,\\d{3,})?) (\\d+.\\d+.\\d{4}+ )?(?<date>\\d+\\.\\d+\\.\\d{4}+) .*")
                        .assign((t, v) -> {
                            String notation = v.get("notation");
                            if (notation != null && !"STK".equalsIgnoreCase(notation))
                            {
                                // Prozent-Notierung,
                                // Workaround..
                                t.setShares(asShares(v.get("shares")) / 100);
                            }
                            else
                            {
                                t.setShares(asShares(v.get("shares")));
                            }
                            t.setDateTime(asDate(v.get("date")));
                        })

                        .oneOf(

                                        // without forex
                                        section -> section
                                                        .attributes("amount", "currency") //
                                                        .match("(?<date>\\d+.\\d+.\\d{4}+) [^ ]* (?<currency>\\w{3}+) (?<amount>[\\d,.]+)")
                                                        .assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                        }),

                                        // reinvestition
                                        section -> section
                                                        .attributes("amount", "currency") //
                                                        .match("Leistungen aus dem steuerlichen Einlagenkonto .* (?<currency>\\w{3}+) (?<amount>[\\d,.]+)")
                                                        .assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                        }),

                                        // with forex
                                        section -> section
                                                        .attributes("amount", "currency", "exchangeRate", "fxCurrency") //
                                                        .match("(?<date>\\d+.\\d+.\\d{4}+) [^ ]* (\\w{3}+)/(?<fxCurrency>\\w{3}+) (?<exchangeRate>[\\d,.]+) (?<currency>\\w{3}+) (?<amount>[\\d,.]+)")
                                                        .assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            
                                                            // create gross unit

                                                            BigDecimal exchangeRate = asExchangeRate(
                                                                            v.get("exchangeRate"));

                                                            long fxAmount = exchangeRate
                                                                            .multiply(BigDecimal.valueOf(t.getAmount()))
                                                                            .setScale(0, RoundingMode.HALF_DOWN)
                                                                            .longValue();

                                                            Money forex = Money.of(asCurrencyCode(v.get("fxCurrency")),
                                                                            fxAmount);

                                                            Unit unit = new Unit(Unit.Type.GROSS_VALUE,
                                                                            t.getMonetaryAmount(), forex,
                                                                            BigDecimal.ONE.divide(exchangeRate, 10,
                                                                                            RoundingMode.HALF_DOWN));
                                                            
                                                            // add gross value unit only if currency code of
                                                            // security actually matches
                                                            if (unit.getForex().getCurrencyCode().equals(t.getSecurity().getCurrencyCode()))
                                                                t.addUnit(unit);
                                                        })

                        );
        
        // logic for processing taxes:
        // * if tax currency matches transaction, just add
        // * if it needs conversion, also fix gross value if present
        // * otherwise use exchange rate stored in context

        BiConsumer<AccountTransaction, Map<String, String>> taxAssignment = (t, v) -> {
            Money tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")));

            if (t.getCurrencyCode().equals(tax.getCurrencyCode()))
            {
                t.addUnit(new Unit(Unit.Type.TAX, tax));
                return;
            }
            
            Optional<Unit> grossValue = t.getUnit(Unit.Type.GROSS_VALUE);
            
            if (grossValue.isPresent() && grossValue.get().getForex().getCurrencyCode()
                            .equals(tax.getCurrencyCode()))
            {
                Unit gv = grossValue.get();

                Money money = Money.of(t.getCurrencyCode(), BigDecimal.valueOf(tax.getAmount())
                                .multiply(gv.getExchangeRate())
                                .setScale(0, RoundingMode.HALF_DOWN)
                                .longValue());

                t.addUnit(new Unit(Unit.Type.TAX, money, tax, gv.getExchangeRate()));

                // fix gross value

                t.removeUnit(grossValue.get());

                t.addUnit(new Unit(Unit.Type.GROSS_VALUE,
                                Money.of(gv.getAmount().getCurrencyCode(),
                                                grossValue.get().getAmount().getAmount()
                                                                + money.getAmount()),
                                Money.of(gv.getForex().getCurrencyCode(), gv.getForex().getAmount()
                                                                + tax.getAmount()),
                                gv.getExchangeRate()));
            }
            else if (type.getCurrentContext()
                            .containsKey(t.getCurrencyCode() + "/" + tax.getCurrencyCode()))
            {
                BigDecimal exchangeRate = asExchangeRate(type.getCurrentContext()
                                .get(t.getCurrencyCode() + "/" + tax.getCurrencyCode()));

                Money money = Money.of(t.getCurrencyCode(),
                                BigDecimal.valueOf(tax.getAmount())
                                                .divide(exchangeRate, 0, RoundingMode.HALF_DOWN)
                                                .longValue());

                t.addUnit(new Unit(Unit.Type.TAX, money));
            }
        };
        
        pdfTransaction
                        .section("tax", "currency").optional() //
                        .match("^davon anrechenbare US-Quellensteuer ([0-9,]+% )?(?<currency>\\w{3}+)\\s+(?<tax>[\\d.,]*)")
                        .assign(taxAssignment)

                        .section("tax", "currency").optional() //
                        .match("^ausländische Quellensteuer [0-9,]+% (?<currency>\\w{3}+)\\s+(?<tax>[\\d.,]+)")
                        .assign(taxAssignment)

                        .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction);

        // optional: Reinvestierung in:
        block = new Block("Reinvestierung.*");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.BUY);
                            return entry;
                        })

                        .section("date")
                        .match("(^\\w{3}+) (\\d{1,3}(\\.\\d{3})*(,\\d{3})?) (\\d+.\\d+.\\d{4}+) (?<date>\\d+.\\d+.\\d{4}+)?(.*)")
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        .section("name", "isin") //
                        .find("Die Dividende wurde wie folgt in neue Aktien reinvestiert:")
                        .find("Gattungsbezeichnung ISIN") //
                        .match("(?<name>.*) (?<isin>[^ ]\\S*)$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("notation", "shares", "amount", "currency") //
                        .find("Nominal Reinvestierungspreis")
                        // STK 25,000 EUR 0,700000
                        .match("(?<notation>^\\w{3}+) (?<shares>\\d{1,3}(\\.\\d{3})*(,\\d{3,})?) (?<currency>\\w{3}+) (?<amount>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)(.*)")
                        .assign((t, v) -> {
                            String notation = v.get("notation");
                            if (notation != null && !notation.equalsIgnoreCase("STK"))
                            {
                                // Prozent-Notierung, Workaround..
                                t.setShares((asShares(v.get("shares")) / 100));
                            }
                            else
                            {
                                t.setShares(asShares(v.get("shares")));
                            }
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount((asAmount(v.get("amount")) * asAmount(v.get("shares")) / 100));
                        })

                        .wrap(BuySellEntryItem::new));
    }

    private void addTransferInTransaction()
    {
        DocumentType type = new DocumentType("Wir erhielten zu Gunsten Ihres Depots");
        this.addDocumentTyp(type);

        Block block = new Block("Wir erhielten zu Gunsten Ihres Depots(.*)");
        type.addBlock(block);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.TRANSFER_IN);
            return entry;
        });

        block.set(pdfTransaction);
        pdfTransaction.section("name", "isin") //
                        .find("Gattungsbezeichnung ISIN") //
                        .match("(?<name>.*) (?<isin>[^ ]\\S*)$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("notation", "shares", "date") //
                        .find("Nominal Schlusstag Wert")
                        // STK 28,000 02.12.2011 02.12.2011
                        .match("(?<notation>^\\w{3}+) (?<shares>\\d{1,3}(\\.\\d{3})*(,\\d{3,})?) (\\d+.\\d+.\\d{4}+) (?<date>\\d+.\\d+.\\d{4}+)(.*)")
                        .assign((t, v) -> {
                            String notation = v.get("notation");
                            if (notation != null && !notation.equalsIgnoreCase("STK"))
                            {
                                // Prozent-Notierung, Workaround..
                                t.setShares((asShares(v.get("shares")) / 100));
                            }
                            else
                            {
                                t.setShares(asShares(v.get("shares")));
                            }
                            t.setDate(asDate(v.get("date")));
                            t.setCurrencyCode(asCurrencyCode(
                                            t.getPortfolioTransaction().getSecurity().getCurrencyCode()));
                        })

                        .wrap(BuySellEntryItem::new);

        addFeesSectionsTransaction(pdfTransaction);
    }

    private void addCapitalReductionTransaction()
    {
        DocumentType type = new DocumentType("Kapitalherabsetzung");
        this.addDocumentTyp(type);

        Block block = new Block("(Aus|Ein)buchung:(.*)");
        type.addBlock(block);

        Transaction<PortfolioTransaction> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            PortfolioTransaction entry = new PortfolioTransaction();
            entry.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
            return entry;
        });

        block.set(pdfTransaction);
        pdfTransaction.section("date").optional()
                        // STK 55,000 24.04.2013
                        .match("(^\\w{3}+) (\\d{1,3}(\\.\\d{3})*(,\\d{3})?) (?<date>\\d+.\\d+.\\d{4}+)?(.*)")
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            type.getCurrentContext().put("date", v.get("date"));
                        })

                        .section("name", "isin") //
                        .find("Gattungsbezeichnung ISIN") //
                        .match("(?<name>.*) (?<isin>[^ ]\\S*)$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("transactiontype") //
                        .match("^(?<transactiontype>.*buchung:)(.*)") //
                        .assign((t, v) -> {
                            String transactiontype = v.get("transactiontype");
                            if ("Einbuchung:".equalsIgnoreCase(transactiontype))
                            {
                                t.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
                            }
                            else if ("Ausbuchung:".equalsIgnoreCase(transactiontype))
                            {
                                t.setType(PortfolioTransaction.Type.DELIVERY_OUTBOUND);
                            }
                            else
                            {
                                // TODO: evtl. Warnung/Hinweis ausgeben?
                            }
                        })

                        // Nominal Ex-Tag
                        // STK 55,000 24.04.2013
                        .section("notation", "shares") //
                        .find("Nominal(.*)")
                        .match("(?<notation>^\\w{3}+) (?<shares>\\d{1,3}(\\.\\d{3})*(,\\d{3,})?)(.*)") //
                        .assign((t, v) -> {
                            String notation = v.get("notation");
                            if (notation != null && !notation.equalsIgnoreCase("STK"))
                            {
                                // Prozent-Notierung, Workaround..
                                t.setShares((asShares(v.get("shares")) / 100));
                            }
                            else
                            {
                                t.setShares(asShares(v.get("shares")));
                            }
                            t.setCurrencyCode(asCurrencyCode(t.getSecurity().getCurrencyCode()));
                            if (t.getDateTime() == null)
                            {
                                t.setDateTime(asDate(type.getCurrentContext().get("date")));
                            }
                        })

                        .wrap(TransactionItem::new);
    }

    private void addCapitalIncreaseTransaction()
    {
        DocumentType type = new DocumentType("Kapitalerhöhung");
        this.addDocumentTyp(type);

        Block block = new Block("Kapitalerhöhung(.*)");
        type.addBlock(block);

        Transaction<PortfolioTransaction> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            PortfolioTransaction entry = new PortfolioTransaction();
            entry.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
            return entry;
        });

        block.set(pdfTransaction);
        pdfTransaction.section("date")
                        // Frankfurt am Main, 06.04.2011
                        .match("(.*), (?<date>\\d+.\\d+.\\d{4}+)") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        .section("name", "isin") //
                        .find("Einbuchung:(\\s*)") //
                        .find("Gattungsbezeichnung ISIN") //
                        .match("(?<name>.*) (?<isin>[^ ]\\S*)$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // Nominal
                        // STK 55,000
                        .section("notation", "shares") //
                        .find("Nominal(.*)") //
                        .match("(?<notation>^\\w{3}+) (?<shares>\\d{1,3}(\\.\\d{3})*(,\\d{3,})?)(.*)") //
                        .assign((t, v) -> {
                            String notation = v.get("notation");
                            if (notation != null && !notation.equalsIgnoreCase("STK"))
                            {
                                // Prozent-Notierung, Workaround..
                                t.setShares((asShares(v.get("shares")) / 100));
                            }
                            else
                            {
                                t.setShares(asShares(v.get("shares")));
                            }
                            t.setCurrencyCode(asCurrencyCode(t.getSecurity().getCurrencyCode()));
                        })

                        .wrap(TransactionItem::new);
    }

    private void addAddDididendRightsTransaction()
    {
        DocumentType type = new DocumentType("Einbuchung von Rechten für die");
        this.addDocumentTyp(type);

        Block block = new Block("Einbuchung von Rechten für die(.*)");
        type.addBlock(block);

        Transaction<PortfolioTransaction> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            PortfolioTransaction entry = new PortfolioTransaction();
            entry.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
            return entry;
        });

        block.set(pdfTransaction);

        pdfTransaction.section("date")
                        // Frankfurt am Main, 25.05.2016
                        .match("(.*), (?<date>\\d+.\\d+.\\d{4}+)") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        .section("name", "isin") //
                        .find("Einbuchung:(\\s*)") //
                        .find("Gattungsbezeichnung ISIN") //
                        .match("(?<name>.*) (?<isin>[^ ]\\S*)$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("notation", "shares") //
                        .find("Nominal(.*)") //
                        .match("(?<notation>^\\w{3}+) (?<shares>\\d{1,3}(\\.\\d{3})*(,\\d{3,})?)(.*)") //
                        .assign((t, v) -> {
                            String notation = v.get("notation");
                            if (notation != null && !notation.equalsIgnoreCase("STK"))
                            {
                                // Prozent-Notierung, Workaround..
                                t.setShares((asShares(v.get("shares")) / 100));
                            }
                            else
                            {
                                t.setShares(asShares(v.get("shares")));
                            }
                            t.setCurrencyCode(asCurrencyCode(t.getSecurity().getCurrencyCode()));
                        })

                        .wrap(TransactionItem::new);
    }

    private void addRemoveDididendRightsTransaction()
    {
        DocumentType type = new DocumentType("Wertlose Ausbuchung");
        this.addDocumentTyp(type);

        Block block = new Block("Wertlose Ausbuchung(.*)");
        type.addBlock(block);

        Transaction<PortfolioTransaction> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            PortfolioTransaction entry = new PortfolioTransaction();
            entry.setType(PortfolioTransaction.Type.DELIVERY_OUTBOUND);
            return entry;
        });

        block.set(pdfTransaction);

        pdfTransaction.section("name", "isin") //
                        .find("Ausbuchung:(\\s*)") //
                        .find("Gattungsbezeichnung ISIN") //
                        .match("(?<name>.*) (?<isin>[^ ]\\S*)$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("notation", "shares", "date") //
                        .find("Nominal Ex-Tag")
                        // STK 25,000 21.06.2016
                        .match("(?<notation>^\\w{3}+) (?<shares>\\d{1,3}(\\.\\d{3})*(,\\d{3,})?) (?<date>\\d+.\\d+.\\d{4}+)(.*)")
                        .assign((t, v) -> {
                            String notation = v.get("notation");
                            if (notation != null && !notation.equalsIgnoreCase("STK"))
                            {
                                // Prozent-Notierung, Workaround..
                                t.setShares((asShares(v.get("shares")) / 100));
                            }
                            else
                            {
                                t.setShares(asShares(v.get("shares")));
                            }
                            t.setCurrencyCode(asCurrencyCode(t.getSecurity().getCurrencyCode()));
                            t.setDateTime(asDate(v.get("date")));
                        })

                        .wrap(TransactionItem::new);
    }

    private void addExchangeTransaction()
    {
        
        // variant if only one date exits in document, then remember it here...
        final DocumentType type = new DocumentType("Umtausch", (context, lines) -> {
            Pattern pDate = Pattern.compile("(^|.* / )(?<contextDate>\\d{2}\\.\\d{2}\\.\\d{4}) .*");
            // read the current context here
            for (String line : lines)
            {
                Matcher m = pDate.matcher(line);
                if (m.matches())
                {
                    context.put("contextDate", m.group(2));
                }
            }
        });
        this.addDocumentTyp(type);

        Block block = new Block("(Aus|Ein)buchung:(.*)");
        type.addBlock(block);

        Transaction<PortfolioTransaction> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            PortfolioTransaction entry = new PortfolioTransaction();
            entry.setType(PortfolioTransaction.Type.DELIVERY_OUTBOUND);
            return entry;
        });

        block.set(pdfTransaction);

        pdfTransaction.section("name", "isin") //
                        .find("Gattungsbezeichnung ISIN") //
                        .match("(?<name>.*) (?<isin>[^ ]\\S*)$") //
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                            // Merken für evtl. Steuerrückerstattung:
                            type.getCurrentContext().put("isin", v.get("isin"));
                        })

                        .section("transactiontype") //
                        .match("^(?<transactiontype>.*buchung:)(.*)") //
                        .assign((t, v) -> {
                            String transactiontype = v.get("transactiontype");
                            if ("Einbuchung:".equalsIgnoreCase(transactiontype))
                            {
                                t.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
                            }
                            else if ("Ausbuchung:".equalsIgnoreCase(transactiontype))
                            {
                                t.setType(PortfolioTransaction.Type.DELIVERY_OUTBOUND);
                            }
                            else
                            {
                                // TODO: evtl. Warnung/Hinweis ausgeben?
                            }
                        })

                        .section("date").optional() //
                        .find("(.*)(Schlusstag|Ex-Tag|Wert Konto-Nr.*)")
                        .match("(.*)(^|\\s+)(?<date>\\d+\\.\\d+\\.\\d{4}+).*") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            type.getCurrentContext().put("contextDate", v.get("date"));
                        })

                        .section("notation", "shares") //
                        .find("Nominal(.*)")
                        .match("(?<notation>^\\w{3}+) (?<shares>\\d{1,3}(\\.\\d{3})*(,\\d{3,})?)(.*)") //
                        .assign((t, v) -> {
                            String notation = v.get("notation");
                            if (notation != null && !notation.equalsIgnoreCase("STK"))
                            {
                                // Prozent-Notierung, Workaround..
                                t.setShares((asShares(v.get("shares")) / 100));
                            }
                            else
                            {
                                t.setShares(asShares(v.get("shares")));
                            }
                            t.setCurrencyCode(asCurrencyCode(t.getSecurity().getCurrencyCode()));
                            if (t.getDateTime() == null)
                            {
                                t.setDateTime(asDate(type.getCurrentContext().get("contextDate")));
                            }
                        })

                        .wrap(TransactionItem::new);

        addTaxBlock(type);
    }

    private void addCompensationTransaction()
    {
        DocumentType type = new DocumentType("Abfindung");
        this.addDocumentTyp(type);

        Block block = new Block("Ausbuchung(.*)");
        type.addBlock(block);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.SELL);
            return entry;
        });

        block.set(pdfTransaction);
        pdfTransaction.section("name", "isin") //
                        .find("Gattungsbezeichnung ISIN") //
                        .match("(?<name>.*) (?<isin>[^ ]\\S*)$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("transactiontype") //
                        .match("^(?<transactiontype>.*buchung:)(.*)") //
                        .assign((t, v) -> {
                            String transactiontype = v.get("transactiontype");
                            if ("Einbuchung:".equalsIgnoreCase(transactiontype))
                            {
                                t.getAccountTransaction().setType(AccountTransaction.Type.BUY);
                                t.getPortfolioTransaction().setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
                            }
                            else if ("Ausbuchung:".equalsIgnoreCase(transactiontype))
                            {
                                t.getAccountTransaction().setType(AccountTransaction.Type.SELL);
                                t.getPortfolioTransaction().setType(PortfolioTransaction.Type.DELIVERY_OUTBOUND);
                            }
                            else
                            {
                                // TODO: evtl. Warnung/Hinweis ausgeben?
                            }
                        })

                        .section("notation", "shares", "date") //
                        .find("Nominal Ex-Tag")
                        // STK 25,000 11.06.2013
                        .match("(?<notation>^\\w{3}+) (?<shares>\\d{1,3}(\\.\\d{3})*(,\\d{3,})?) (?<date>\\d+.\\d+.\\d{4}+)(.*)")
                        .assign((t, v) -> {
                            String notation = v.get("notation");
                            if (notation != null && !notation.equalsIgnoreCase("STK"))
                            {
                                // Prozent-Notierung, Workaround..
                                t.setShares((asShares(v.get("shares")) / 100));
                            }
                            else
                            {
                                t.setShares(asShares(v.get("shares")));
                            }
                            t.setDate(asDate(v.get("date")));
                            t.setCurrencyCode(asCurrencyCode(
                                            t.getPortfolioTransaction().getSecurity().getCurrencyCode()));
                        })

                        .section("date", "amount", "currency")
                        .find("Wert(\\s+)Konto-Nr. Betrag zu Ihren Gunsten(\\s*)$")
                        // 11.06.2013 172306238 EUR 17,50
                        .match("(?<date>\\d+.\\d+.\\d{4}+) (\\d{6,12}) (?<currency>\\w{3}+) (?<amount>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)")
                        .assign((t, v) -> {
                            t.setDate(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .wrap(BuySellEntryItem::new);

        addFeesSectionsTransaction(pdfTransaction);
    }
    
    private void addFusionTransaction()
    {
        DocumentType type = new DocumentType("(Fusion|Einstellung der Zertifizierung)");
        this.addDocumentTyp(type);

        Block block = new Block("(Aus|Ein)buchung:(.*)");
        type.addBlock(block);

        Transaction<PortfolioTransaction> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            PortfolioTransaction entry = new PortfolioTransaction();
            entry.setType(PortfolioTransaction.Type.DELIVERY_OUTBOUND);
            return entry;
        });

        block.set(pdfTransaction);

        pdfTransaction.section("name", "isin") //
                        .find("Gattungsbezeichnung ISIN") //
                        .match("(?<name>.*) (?<isin>[^ ]\\S*)$") //
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .section("transactiontype") //
                        .match("^(?<transactiontype>.*buchung:)(.*)") //
                        .assign((t, v) -> {
                            String transactiontype = v.get("transactiontype");
                            if ("Einbuchung:".equalsIgnoreCase(transactiontype))
                            {
                                t.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
                            }
                            else if ("Ausbuchung:".equalsIgnoreCase(transactiontype))
                            {
                                t.setType(PortfolioTransaction.Type.DELIVERY_OUTBOUND);
                            }
                            else
                            {
                                // TODO: evtl. Warnung/Hinweis ausgeben?
                            }
                        })
                        
                        .section("date").optional()
                        .find("(.*)(Schlusstag|Ex-Tag)")
                        .match("(.*)(?<date>\\d+.\\d+.\\d{4}+)") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            type.getCurrentContext().put("date", v.get("date"));
                        })
                        
                        // Nominal Ex-Tag
                        // STK 12,000 04.07.2017
                        .section("notation", "shares") //
                        .find("Nominal(.*)")
                        .match("(?<notation>^\\w{3}+) (?<shares>\\d{1,3}(\\.\\d{3})*(,\\d{3,})?)(.*)") //
                        .assign((t, v) -> {
                            String notation = v.get("notation");
                            if (notation != null && !notation.equalsIgnoreCase("STK"))
                            {
                                // Prozent-Notierung, Workaround..
                                t.setShares((asShares(v.get("shares")) / 100));
                            }
                            else
                            {
                                t.setShares(asShares(v.get("shares")));
                            }
                            t.setCurrencyCode(asCurrencyCode(t.getSecurity().getCurrencyCode()));
                            if (t.getDateTime() == null) 
                            {
                                t.setDateTime(asDate(type.getCurrentContext().get("date")));
                            }
                        })

                        .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction);
        addTaxBlock(type);
    }
    
    private void addRegistrationFeeTransaction()
    {
        DocumentType type = new DocumentType("Registrierung");
        this.addDocumentTyp(type);

        Block block = new Block("Registrierungsgeb.*(\\d+.\\d+.\\d{4})");
        type.addBlock(block);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            AccountTransaction transaction = new AccountTransaction();
            transaction.setType(AccountTransaction.Type.FEES);
            return transaction;
        });

        block.set(pdfTransaction);
        pdfTransaction

                        .section("name", "isin") //
                        .find("Gattungsbezeichnung(.*) ISIN")
                        // Vonovia SE Namens-Aktien o.N. DE000A1ML7J1
                        .match("(?<name>.*?) (?<isin>[^ ]\\S*)$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))
                        
                        //Registrierungsgebühr EUR 0,75-
                        //Dt. Umsatzsteuer EUR 0,14-
                        //Wert Konto-Nr. Betrag zu Ihren Lasten
                        //24.07.2017 172406048 EUR 0,89
                        .section("date", "currency", "amount")
                        .find("Wert(\\s*)Konto-Nr. Betrag zu Ihren Lasten(\\s*)$")
                        .match("(?<date>\\d+.\\d+.\\d{4}+) (\\d{6,12}) (?<currency>\\w{3}+) (?<amount>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)")
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            //t.setNote(note);
                        })

                        .wrap(TransactionItem::new);
    }

    private void addDepositTransaction()
    {
        final DocumentType type = new DocumentType("Depotauszug", (context, lines) -> {
            Pattern pDate = Pattern.compile(".*epotauszug per (\\d+.\\d+.\\d{4}+)?(.*)");
            Pattern pCurrency = Pattern.compile("(.*)Bewertung in[ ]+(\\w{3}+)");
            // read the current context here
            for (String line : lines)
            {
                Matcher m = pDate.matcher(line);
                if (m.matches())
                {
                    context.put("date", m.group(1));
                }
                m = pCurrency.matcher(line);
                if (m.matches())
                {
                    context.put("currency", m.group(2));
                }
            }
        });
        this.addDocumentTyp(type);

        Block block = new Block("(^\\w{3}+) (\\d{1,3}(\\.\\d{3})*(,\\d{3})?)(.*)");
        type.addBlock(block);

        Transaction<PortfolioTransaction> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            PortfolioTransaction entry = new PortfolioTransaction();
            entry.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
            return entry;
        });

        block.set(pdfTransaction);
        // Die WP-Bezeichnung muss hier leider über mehrere Zeilen hinweg
        // zusammengesucht werden, da im Depotauszug-PDF-Extrakt leider
        // (zumindest teilweise) Zeilenumbrüche innerhalb des Namens sind... (s.
        // Beispiel-Datei: OnvistaDepotauszug.txt)
        pdfTransaction.section("notation", "shares", "nameP1").optional()
                        // STK 4,000 Porsche Automobil
                        .match("(?<notation>^\\w{3}+) (?<shares>\\d{1,3}(\\.\\d{3})*(,\\d{3,})?) (?<nameP1>.*)")
                        .assign((t, v) -> {
                            type.getCurrentContext().put("nameP1", v.get("nameP1"));

                            String notation = v.get("notation");
                            if (notation != null && !notation.equalsIgnoreCase("STK"))
                            {
                                // Prozent-Notierung, Workaround..
                                t.setShares((asShares(v.get("shares")) / 100));
                            }
                            else
                            {
                                t.setShares(asShares(v.get("shares")));
                            }
                        })

                        .section("nameP3").optional() //
                        .find("(^\\w{3}+) (\\d{1,3}(\\.\\d{3})*(,\\d{3})?) (.*)")
                        // Inhaber-Vorzugsakti
                        .match("(?<nameP3>^[A-Za-z-]*)(\\s*)")
                        .assign((t, v) -> type.getCurrentContext().put("nameP3", v.get("nameP3")))

                        .oneOf(
                                        section -> section.attributes("nameP2", "isin")
                                        // Holding SE DE000PAH0038 Girosammelverwahrung 59,3400
                                                        .match("(?<nameP2>.* )(?<isin>\\w{12}) *([0-9.]{10} )?Girosammelverwahrung .*")
                                        .assign((t, v) -> {
                                            type.getCurrentContext().put("nameP2", v.get("nameP2"));
                                            type.getCurrentContext().put("isin", v.get("isin"));
                                        })
                        ,
                                        //Format für neuere Depotauszuege...
                                        section -> section.attributes("notation", "shares", "nameP1", "isin")
                                        // STK 6,000 Vonovia SE Namens-Aktien o.N.                          D   E  000A1ML7J1 Girosammelverwahrung 39,5600 EUR 237,36 0,00 
                                        .match("(?<notation>^\\w{3}+) (?<shares>\\d{1,3}(\\.\\d{3})*(,\\d{3,})?)(?<nameP1>((?:\\S|\\s(?!\\s))*))(\\s)(?<isin>.*)(\\s)(Girosammelverwahrung|Wertpapierrechnung)(.*)")
                                        .assign((t, v) -> {
                                            type.getCurrentContext().put("nameP1", v.get("nameP1"));
                                            type.getCurrentContext().put("isin",  v.get("isin").replaceAll("\\s", ""));
                                            
                                            String notation = v.get("notation");
                                            if (notation != null && !notation.equalsIgnoreCase("STK"))
                                            {
                                                // Prozent-Notierung, Workaround..
                                                t.setShares((asShares(v.get("shares")) / 100));
                                            }
                                            else
                                            {
                                                t.setShares(asShares(v.get("shares")));
                                            }
                                            v.put("isin", v.get("isin").replaceAll("\\s", ""));
                                        })
                        )

                        .section("nameP4").optional() //
                        .find("^(.*) (\\w{12}+) (.*)")
                        // en o.St.o.N
                        .match("^(?<nameP4>^.*\\.*)$")
                        .assign((t, v) -> type.getCurrentContext().put("nameP4", v.get("nameP4")))

                        .section("combine") //
                        .match("(?<combine>.*)") //
                        .assign((t, v) -> {
                            v.put("isin", type.getCurrentContext().get("isin"));
                            
                            StringBuilder sbName = new StringBuilder(type.getCurrentContext().get("nameP1"));
                            for (int i=2; i<=4;i++)  
                            {
                                if (type.getCurrentContext().get("nameP" + i) != null) 
                                {
                                    sbName.append(type.getCurrentContext().get("nameP" + i));
                                }   
                            }
                            String name = sbName.toString();
                            if (name.indexOf(v.get("isin")) > -1)
                            {
                                name = name.substring(0, name.indexOf(v.get("isin")));
                            }
                            if (name.indexOf("STK ") > -1)
                            {
                                name = name.substring(0, name.indexOf("STK "));
                            }
                            // WP-Bezeichnung nachbearbeiten, kann doppelte
                            // Leerzeichen enthalten...
                            name = name.replaceAll("\\s+", " ");
                            // oder auch überflüssige Nullen (00)...
                            name = name.replaceAll("0+%", "%");
                            // oder <Leerzeichen><Punkt> ( .)
                            name = name.replaceAll("\\s+\\.", ".");
                            v.put("name", name);

                            t.setSecurity(getOrCreateSecurity(v));

                            if (t.getDateTime() == null)
                            {
                                t.setDateTime(asDate(type.getCurrentContext().get("date")));
                            }
                            if (t.getCurrencyCode() == null)
                            {
                                t.setCurrencyCode(asCurrencyCode(type.getCurrentContext().get("currency")));
                            }
                        })

                        .wrap(TransactionItem::new);
    }

    private void addAccountStatementTransaction()
    {
        final DocumentType type = new DocumentType("KONTOAUSZUG Nr.", (context, lines) -> {
            Pattern pYear = Pattern.compile("KONTOAUSZUG Nr. \\d+ per \\d+.\\d+.(\\d{4}+)?(.*)");
            Pattern pCurrency = Pattern.compile("(.*)Customer Cash Account[ ]+(\\w{3}+)");
            // read the current context here
            for (String line : lines)
            {
                Matcher m = pYear.matcher(line);
                if (m.matches())
                {
                    context.put("year", m.group(1));
                }
                m = pCurrency.matcher(line);
                if (m.matches())
                {
                    context.put("currency", m.group(2));
                }
            }
        });
        this.addDocumentTyp(type);

        // 31.10. 31.10. REF: 000017304356 37,66
        Block block = new Block("^\\d+\\.\\d+\\.\\s+\\d+\\.\\d+\\.\\s+REF:\\s+\\d+\\s+[\\d.-]+,\\d+[+-]?(.*)");
        type.addBlock(block);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.DEPOSIT);
            return entry;
        });

        block.set(pdfTransaction);
        pdfTransaction.section("valuta", "amount", "sign") //
                        .match("^\\d+\\.\\d+\\.\\s+(?<valuta>\\d+\\.\\d+\\.)\\s+REF:\\s+\\d+\\s+(?<amount>[\\d.-]+,\\d+)(?<sign>[+-]?)(.*)")
                        .assign((t, v) -> {
                            Map<String, String> context = type.getCurrentContext();
                            String date = v.get("valuta");
                            if (date != null)
                            {
                                // create a long date from the year in the
                                // context
                                t.setDateTime(asDate(date + context.get("year")));
                            }
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                            // check for withdrawals
                            String sign = v.get("sign");
                            if ("-".equals(sign))
                            {
                                // change type for withdrawals
                                t.setType(AccountTransaction.Type.REMOVAL);
                            }
                        })

                        // Feintuning Buchungstyp...
                        .section("postingtype") //
                        .find("\\d+\\.\\d+\\.\\s+\\d+\\.\\d+\\. REF:(.*)") //
                        .match("(?<postingtype>.*?)").assign((t, v) -> {
                            String postingtype = v.get("postingtype");
                            if (postingtype != null)
                            {
                                switch (postingtype)
                                {
                                    case "Wertpapierkauf":
                                    case "Umtausch/Bezug":
                                    case "Sollbuchung HSBC":
                                        t.setType(AccountTransaction.Type.BUY);
                                        break;
                                    case "Wertpapierverkauf":
                                    case "Spitze Verkauf":
                                    case "Habenbuchung HSBC":
                                    case "Tilgung":
                                        t.setType(AccountTransaction.Type.SELL);
                                        break;
                                    case "Zinsen/Dividenden":
                                        t.setType(AccountTransaction.Type.DIVIDENDS);
                                        break;
                                    case "AbgSt. Optimierung":
                                        t.setType(AccountTransaction.Type.TAX_REFUND);
                                        break;
                                    default:
                                        break;
                                }
                            }
                        })

                        .wrap(t -> {
                            // Buchungen, die bereits durch den Import von
                            // WP-Abrechnungen abgedeckt sind (sein sollten),
                            // hier ausklammern, sonst hat man sie doppelt im
                            // Konto:
                            if (t.getType() != AccountTransaction.Type.DIVIDENDS
                                            && t.getType() != AccountTransaction.Type.BUY
                                            && t.getType() != AccountTransaction.Type.SELL
                                            && t.getType() != AccountTransaction.Type.TAX_REFUND)
                                return new TransactionItem(t);
                            return null;
                        });
    }

    private void addAccountStatementTransaction2017()
    {
        // this seems to be the new format of account statements from the year
        // 2017
        final DocumentType type = new DocumentType("Kontoauszug Nr.", (context, lines) -> {
            Pattern pYear = Pattern.compile("^Kontoauszug Nr. (\\d{4}) / .*\\.(\\d{4})$");
            Pattern pCurrency = Pattern.compile("^(\\w{3}+) - Verrechnungskonto: .*$");
            // read the current context here
            for (String line : lines)
            {
                Matcher m = pYear.matcher(line);
                if (m.matches())
                {
                    context.put("year", m.group(1));
                }
                m = pCurrency.matcher(line);
                if (m.matches())
                {
                    context.put("currency", m.group(1));
                }
            }
        });
        this.addDocumentTyp(type);

        // 31.10. 31.10. REF: 000017304356 37,66
        Block block = new Block("^\\d+\\.\\d+\\.\\s+\\d+\\.\\d+\\.\\s+REF:\\s+\\d+\\s+[\\d.-]+,\\d+[+-]?(.*)");
        type.addBlock(block);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.DEPOSIT);
            return entry;
        });

        block.set(pdfTransaction);
        pdfTransaction.section("valuta", "amount", "sign") //
                        .match("^\\d+\\.\\d+\\.\\s+(?<valuta>\\d+\\.\\d+\\.)\\s+REF:\\s+\\d+\\s+(?<amount>[\\d.-]+,\\d+)(?<sign>[+-]?)(.*)")
                        .assign((t, v) -> {
                            Map<String, String> context = type.getCurrentContext();
                            String date = v.get("valuta");
                            if (date != null)
                            {
                                // create a long date from the year in the
                                // context
                                t.setDateTime(asDate(date + context.get("year")));
                            }
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                            // check for withdrawals
                            String sign = v.get("sign");
                            if ("-".equals(sign))
                            {
                                // change type for withdrawals
                                t.setType(AccountTransaction.Type.REMOVAL);
                            }
                        })

                        // Feintuning Buchungstyp...
                        .section("postingtype") //
                        .find("\\d+\\.\\d+\\.\\s+\\d+\\.\\d+\\. REF:(.*)") //
                        .match("(?<postingtype>.*?)").assign((t, v) -> {
                            String postingtype = v.get("postingtype");
                            if (postingtype != null)
                            {
                                switch (postingtype)
                                {
                                    case "Wertpapierkauf":
                                    case "Umtausch/Bezug":
                                    case "Sollbuchung HSBC":
                                        t.setType(AccountTransaction.Type.BUY);
                                        break;
                                    case "Wertpapierverkauf":
                                    case "Spitze Verkauf":
                                    case "Habenbuchung HSBC":
                                    case "Tilgung":
                                        t.setType(AccountTransaction.Type.SELL);
                                        break;
                                    case "Zinsen/Dividenden":
                                        t.setType(AccountTransaction.Type.DIVIDENDS);
                                        break;
                                    case "AbgSt. Optimierung":
                                        t.setType(AccountTransaction.Type.TAX_REFUND);
                                        break;
                                    default:
                                        break;
                                }
                            }
                        })

                        .wrap(t -> {
                            // Buchungen, die bereits durch den Import von
                            // WP-Abrechnungen abgedeckt sind (sein sollten),
                            // hier ausklammern, sonst hat man sie doppelt im
                            // Konto:
                            if (t.getType() != AccountTransaction.Type.DIVIDENDS
                                            && t.getType() != AccountTransaction.Type.BUY
                                            && t.getType() != AccountTransaction.Type.SELL
                                            && t.getType() != AccountTransaction.Type.TAX_REFUND)
                                return new TransactionItem(t);
                            return null;
                        });
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T pdfTransaction)
    {
        pdfTransaction.section("tax", "withheld", "sign").optional() //
                        .match("(?<withheld>\\w+|^(Verwahrart\\s.*)?|^(Lagerland\\s.*)?)(\\s*)Kapitalertragsteuer(\\s*)(?<currency>\\w{3}+)(\\s+)(?<tax>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)(?<sign>-|\\s+|$)?")
                        .assign((t, v) -> {
                            if ("-".equalsIgnoreCase(v.get("sign"))
                                            || "einbehaltene".equalsIgnoreCase(v.get("withheld")))
                            {
                                if (t instanceof name.abuchen.portfolio.model.Transaction)
                                {
                                    ((name.abuchen.portfolio.model.Transaction) t).addUnit(new Unit(Unit.Type.TAX, Money
                                                    .of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")))));
                                }
                                else
                                {
                                    ((name.abuchen.portfolio.model.BuySellEntry) t).getPortfolioTransaction().addUnit(
                                                    new Unit(Unit.Type.TAX, Money.of(asCurrencyCode(v.get("currency")),
                                                                    asAmount(v.get("tax")))));
                                }
                            }
                        })

                        .section("soli", "withheld", "sign").optional()
                        .match("(?<withheld>\\w+|^|.*)(\\s*)Solidaritätszuschlag(\\s*)(?<currency>\\w{3}+)(\\s+)(?<soli>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)(?<sign>-|\\s+|$)?")
                        .assign((t, v) -> {
                            if ("-".equalsIgnoreCase(v.get("sign"))
                                            || "einbehaltener".equalsIgnoreCase(v.get("withheld")))
                            {
                                if (t instanceof name.abuchen.portfolio.model.Transaction)
                                {
                                    ((name.abuchen.portfolio.model.Transaction) t).addUnit(new Unit(Unit.Type.TAX, Money
                                                    .of(asCurrencyCode(v.get("currency")), asAmount(v.get("soli")))));
                                }
                                else
                                {
                                    ((name.abuchen.portfolio.model.BuySellEntry) t).getPortfolioTransaction().addUnit(
                                                    new Unit(Unit.Type.TAX, Money.of(asCurrencyCode(v.get("currency")),
                                                                    asAmount(v.get("soli")))));
                                }
                            }
                        })

                        .section("kirchenst", "withheld", "sign").optional()
                        .match("(?<withheld>\\w+|^)(\\s*)Kirchensteuer(\\s*)(?<currency>\\w{3}+)(\\s+)(?<kirchenst>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)(?<sign>-|\\s+|$)?")
                        .assign((t, v) -> {
                            if ("-".equalsIgnoreCase(v.get("sign"))
                                            || "einbehaltene".equalsIgnoreCase(v.get("withheld")))
                            {
                                if (t instanceof name.abuchen.portfolio.model.Transaction)
                                {
                                    ((name.abuchen.portfolio.model.Transaction) t).addUnit(
                                                    new Unit(Unit.Type.TAX, Money.of(asCurrencyCode(v.get("currency")),
                                                                    asAmount(v.get("kirchenst")))));
                                }
                                else
                                {
                                    ((name.abuchen.portfolio.model.BuySellEntry) t).getPortfolioTransaction().addUnit(
                                                    new Unit(Unit.Type.TAX, Money.of(asCurrencyCode(v.get("currency")),
                                                                    asAmount(v.get("kirchenst")))));
                                }
                            }
                        });
    }

    private void addFeesSectionsTransaction(Transaction<BuySellEntry> pdfTransaction)
    {
        BiConsumer<BuySellEntry, Map<String, String>> feeProcessor = (t, v) -> {
            String currency = asCurrencyCode(v.get("currency"));

            if (t.getPortfolioTransaction().getCurrencyCode().equals(currency))
            {
                t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee")))));
            }
            else
            {
                Optional<Unit> grossValue = t.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE);
                if (grossValue.isPresent() && grossValue.get().getForex().getCurrencyCode().equals(currency))
                {
                    Unit gv = grossValue.get();

                    Money forex = Money.of(currency, asAmount(v.get("fee")));
                    Money amount = Money.of(t.getPortfolioTransaction().getCurrencyCode(),
                                    gv.getExchangeRate().multiply(BigDecimal.valueOf(forex.getAmount()))
                                                    .setScale(0, RoundingMode.HALF_DOWN).longValue());

                    t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE, amount, forex, gv.getExchangeRate()));

                    // update gross value (fees are calculated *after* gross)

                    t.getPortfolioTransaction().removeUnit(gv);

                    Unit newGrossValue = new Unit(Unit.Type.GROSS_VALUE,
                                    Money.of(gv.getAmount().getCurrencyCode(),
                                                    gv.getAmount().getAmount() - amount.getAmount()),
                                    Money.of(gv.getForex().getCurrencyCode(),
                                                    gv.getForex().getAmount() - forex.getAmount()),
                                    gv.getExchangeRate());

                    t.getPortfolioTransaction().addUnit(newGrossValue);
                }
            }
        };

        pdfTransaction //
                        .section("fee", "currency") //
                        .optional()
                        .match("(^.*)(Orderprovision) (?<currency>\\w{3}) (?<fee>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)(-)")
                        .assign(feeProcessor)

                        .section("fee", "currency") //
                        .optional()
                        .match("(^.*) (B\\Drsengeb\\Dhr) (?<currency>\\w{3}) (?<fee>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)(-)")
                        .assign(feeProcessor)

                        .section("fee", "currency") //
                        .optional()
                        .match("(^.*) (Handelsplatzgeb\\Dhr) (?<currency>\\w{3}) (?<fee>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)(-)")
                        .assign(feeProcessor)

                        .section("fee", "currency") //
                        .optional()
                        .match("(^.*)(Maklercourtage)(\\s+)(?<currency>\\w{3}) (?<fee>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)(-)")
                        .assign(feeProcessor);
    }

    private void addTaxReturnTransaction()
    {
        DocumentType type = new DocumentType("Steuerausgleich nach § 43a");
        this.addDocumentTyp(type);

        Block block1 = new Block("Wir haben für Sie (ge|ver)kauft(.*)");
        type.addBlock(block1);

        Block block2 = new Block("(Aus|Ein)buchung:(.*)");
        type.addBlock(block2);

        Transaction<AccountTransaction> taxRefundTransaction = new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction entry = new AccountTransaction();
                            entry.setType(AccountTransaction.Type.TAX_REFUND);
                            return entry;
                        })

                        .section("name", "isin") //
                        .find("Gattungsbezeichnung ISIN") //
                        .match("(?<name>.*) (?<isin>[^ ]\\S*)$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("tax").optional()
                        .match("^Kapitalertragsteuer (?<currency>\\w{3}+) (?<tax>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)")
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("tax")));
                        })

                        .section("soli").optional()
                        .match("^Solidaritätszuschlag (?<currency>\\w{3}+) (?<soli>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)")
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(t.getAmount() + asAmount(v.get("soli")));
                        })

                        .section("kirchenst").optional()
                        .match("^Kirchensteuer (?<currency>\\w{3}+) (?<kirchenst>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)")
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(t.getAmount() + asAmount(v.get("kirchenst")));
                        })

                        .section("date", "currency").optional()
                        .find("Wert(\\s+)Konto-Nr.(\\s+)Abrechnungs-Nr.(\\s+)Betrag zu Ihren Gunsten(\\s*)$")
                        // Wert Konto-Nr. Abrechnungs-Nr. Betrag zu Ihren
                        // Gunsten
                        // 06.05.2013 172306238 56072633 EUR 3,05
                        .match("(^|\\s+)(?<date>\\d+\\.\\d+\\.\\d{4}+)(\\s)(\\d+)?(\\s)?(\\d+)?(\\s)(?<currency>\\w{3}+) (\\d{1,3}(\\.\\d{3})*(,\\d{2})?)")
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .wrap(t -> t.getAmount() != 0 ? new TransactionItem(t) : null);

        block1.set(taxRefundTransaction);
        block2.set(taxRefundTransaction);
    }

    private void addTaxBlock(DocumentType type)
    {

        // optional: Steuer dem Konto buchen
        Block block = new Block("(Kapitalertragsteuer)(.*)-$");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction entry = new AccountTransaction();
                            entry.setType(AccountTransaction.Type.TAXES);
                            return entry;
                        })

                        .section("tax", "currency").optional()
                        .match("^Kapitalertragsteuer (?<currency>\\w{3}+) (?<tax>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)(-)")
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("tax")));
                        })

                        .section("soli", "currency").optional()
                        .match("^Solidaritätszuschlag (?<currency>\\w{3}+) (?<soli>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)(-)")
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(t.getAmount() + asAmount(v.get("soli")));
                        })

                        .section("kirchenst", "currency").optional()
                        .match("^Kirchensteuer (?<currency>\\w{3}+) (?<kirchenst>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)(-)")
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(t.getAmount() + asAmount(v.get("kirchenst")));
                        })


                        .section("date", "currency").optional() //entweder...
                        .find("Wert(\\s+)Konto-Nr.(\\s+)Betrag zu Ihren Lasten(\\s*)$")
                        // Wert Konto-Nr. Betrag zu Ihren Lasten
                        // 23.11.2015 172306238 EUR 12,86
                        .match("(^|\\s+)(?<date>\\d+\\.\\d+\\.\\d{4}+)(\\s)(\\d+)(\\s)(?<currency>\\w{3}+) (\\d{1,3}(\\.\\d{3})*(,\\d{2})?)")
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            v.put("isin", type.getCurrentContext().get("isin"));
                            t.setSecurity(getOrCreateSecurity(v));
                                
                        })
                        .section("date", "currency", "exchangeRate", "amountSum").optional() //oder
                        .find("Wert(\\s+)Konto-Nr.(\\s+)(Devisenkurs\\s+)Betrag zu Ihren Lasten(\\s*)$")
                        //Wert Konto-Nr. Devisenkurs Betrag zu Ihren Lasten
                        //22.02.2019 356238049 EUR/USD 1,13375 EUR 0,27
                        .match("(^|\\s+)(?<date>\\d+\\.\\d+\\.\\d{4}+)(\\s)(\\d+)(\\s)(\\w{3}+\\/\\w{3}+ )(?<exchangeRate>[\\d,]+)[\\s]?(?<currency>\\w{3}+) (?<amountSum>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)")
                        .assign((t, v) -> {
                            v.put("isin", type.getCurrentContext().get("isin"));
                            t.setSecurity(getOrCreateSecurity(v));
                            t.setDateTime(asDate(v.get("date")));
                            String currencyCodeFx = t.getCurrencyCode();
                            if (t.getSecurity().getCurrencyCode().equalsIgnoreCase(currencyCodeFx))
                            {
                                Money mTaxesFx = Money.of(currencyCodeFx, t.getAmount());
                                Money mTaxesFxInEUR = Money.of(asCurrencyCode(v.get("currency")),
                                                asAmount(v.get("amountSum")));
                                BigDecimal inverseRate = BigDecimal.valueOf(asAmount(v.get("amountSum")))
                                                .divide(BigDecimal.valueOf(t.getAmount()), 10, RoundingMode.HALF_DOWN);
                                t.addUnit(new Unit(Unit.Type.TAX, mTaxesFxInEUR, mTaxesFx, inverseRate));
                                t.setAmount(asAmount(v.get("amountSum")));
                            }
                            else
                            {
                                t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                t.setAmount(asAmount(v.get("amountSum")));
                            }
                        })

                        .wrap(TransactionItem::new));
    }

    private void addTaxVorabpauschaleTransaction()
    {
        DocumentType type = new DocumentType("Steuerpflichtige Vorabpauschale");
        this.addDocumentTyp(type);

        Block block = new Block("Steuerpflichtige Vorabpauschale(.*)");
        type.addBlock(block);

        Transaction<AccountTransaction> taxVorabpauschaleTransaction = new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction entry = new AccountTransaction();
                            entry.setType(AccountTransaction.Type.TAXES);
                            return entry;
                        })

                        .section("name", "isin") //
                        .find("Gattungsbezeichnung ISIN") //
                        .match("(?<name>.*) (?<isin>[^ ]\\S*)$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("tax", "currency").optional()
                        .match("^Wert Konto-Nr. Betrag zu Ihren Lasten")
                        .match("(\\d+\\.\\d+\\.\\d{4}+) .* (?<currency>\\w{3}+) (?<tax>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)")
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("tax")));
                        })

                        .section("date").optional()
                        .match("^Nominal Ex-Tag Zahltag Jahreswert Vorabpauschale pro Stück")
                        // STK 1.212,000 02.01.2020 02.01.2020 EUR 0,1019
                        .match("STK .* (?<exdate>\\d+\\.\\d+\\.\\d{4}+) (?<date>\\d+\\.\\d+\\.\\d{4}+) (?<currency>\\w{3}+) .*")
                        .assign((t, v) -> {
                            // if all taxes are covered by Freistellungauftrag/Verlustopf, section "Wert Konto-Nr. Betrag zu
                            // Ihren Lasten" is not present -> extract currency here
                            if (t.getCurrencyCode() == null)
                                t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setDateTime(asDate(v.get("date")));
                        })

                        .wrap(t -> t.getAmount() != 0 ? new TransactionItem(t)
                                        : new NonImportableItem("Steuerpflichtige Vorabpauschale mit 0 "
                                                        + t.getCurrencyCode()));

        block.set(taxVorabpauschaleTransaction);
    }
    @Override
    public String getLabel()
    {
        return "Onvista-Bank"; //$NON-NLS-1$
    }
}
