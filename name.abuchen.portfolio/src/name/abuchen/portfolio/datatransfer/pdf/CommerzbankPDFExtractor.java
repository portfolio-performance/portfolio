package name.abuchen.portfolio.datatransfer.pdf;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
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
public class CommerzbankPDFExtractor extends AbstractPDFExtractor
{
    public CommerzbankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("C O M M E R Z B A N K"); //$NON-NLS-1$
        addBankIdentifier("Commerzbank AG"); //$NON-NLS-1$
        addBankIdentifier("COBADE"); //$NON-NLS-1$

        addBuySellTransaction();
        addDividendeTransaction();
        addTaxTreatmentTransaction();
        addKontoauszugGiro();
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("W e r t p a p i e r (k a u f|v e r k a u f)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
                pdfTransaction.subject(() -> {
                    BuySellEntry entry = new BuySellEntry();
                    entry.setType(PortfolioTransaction.Type.BUY);
                    return entry;
        });

        Block firstRelevantLine = new Block("W e r t p a p i e r (k a u f|v e r k a u f)");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);
                
        pdfTransaction

                        .section("type").optional()
                        .match("(?<type>W e r t p a p i e r v e r k a u f)")
                        .assign((t, v) -> {
                            if (v.get("type").equals("W e r t p a p i e r v e r k a u f"))
                            {
                                t.setType(PortfolioTransaction.Type.SELL);
                            }
                        })

                        // W e r t p a p i e r - B e z e i c h n u n g W e r t p a p i e r k e n n n u m m e r
                        // A l l i a n z SE 8 4 0 4 0 0
                        // v i n k . N a m e n s - A k t i e n o . N .
                        .section("name", "wkn", "nameContinued")
                        .match(".*W e r t p a p i e r - B e z e i c h n u n g.*")
                        .match("(?<name>.*),? (?<wkn>([\\w]{6}|\\w\\s\\w\\s\\w\\s\\w\\s\\w\\s\\w))$")
                        .match("(?<nameContinued>.*)")
                        .assign((t, v) -> {
                            v.put("wkn", stripBlanks(v.get("wkn")));
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        // 1 2 : 0 6 S t . 2 3 0 EUR 1 8 4 , 1 6 EUR 4 2 . 3 5 6 , 8 0
                        .section("time").optional()
                        .match("^(?<time>[\\d\\s]+:[\\d\\s]+) (S t .|St.) [\\d\\s,.]* \\w{3} [\\d\\s,.]* \\w{3} [\\d\\s,.]*$")
                        .assign((t, v) -> type.getCurrentContext().put("time", stripBlanks(v.get("time"))))

                        // G e s c h ä f t s t a g : 1 7 . 0 2 . 2 0 2 1 A b w i c k l u n g : F e s t p r e i s
                        .section("date").optional()
                        .match("G e s c h ä f t s t a g : (?<date>[\\d\\s]+.[\\d\\s]+.[\\d\\s]+) .*")
                        .assign((t, v) -> {
                            if (type.getCurrentContext().get("time") != null)
                            {
                                t.setDate(asDate(stripBlanks(v.get("date")), type.getCurrentContext().get("time")));
                            }
                            else
                            {
                                t.setDate(asDate(stripBlanks(v.get("date"))));
                            }
                        })

                        // G e s c h ä f t s t a g : 3 1 . 0 1 . 2 0 2 1 A u s f ü h r u n g s p l a t z : FRANKFURT
                        // H a n d e l s z e i t : 1 3 : 1 0 U h r (MEZ/MESZ)
                        .section("date", "time").optional()
                        .match("G e s c h ä f t s t a g : (?<date>[\\d\\s]+.[\\d\\s]+.[\\d\\s]+) .*")
                        .match("H a n d e l s z e i t : (?<time>[\\d\\s]+:[\\d\\s]+) .*")
                        .assign((t, v) -> {
                            if (v.get("time") != null)
                                t.setDate(asDate(stripBlanks(v.get("date")), stripBlanks(v.get("time"))));
                            else
                                t.setDate(asDate(stripBlanks(v.get("date"))));
                        })

                        // S t . 2 0 0 EUR 2 0 1 , 7 0 
                        .section("shares").optional()
                        .match("(S t .|St.) (?<shares>[\\d\\s,.]*) .*")
                        .assign((t, v) -> t.setShares(asShares(stripBlanks(v.get("shares")))))

                        // Summe S t . 2 5 0 EUR 1 9 1 , 0 0 8 6 4 EUR 4 7 . 7 5 2 , 1 6
                        .section("shares").optional()
                        .match("^(Summe) (S t .|St.) (?<shares>[\\d\\s,.]*) .*")
                        .assign((t, v) -> t.setShares(asShares(stripBlanks(v.get("shares")))))
                        
                        .section("amount", "currency")
                        .match(".*(Zu I h r e n L a s t e n|Zu I h r e n Guns ten).*")
                        .match(".* \\w{3} [\\d\\s]+.[\\d\\s]+.[\\d\\s]+ (?<currency>\\w{3})(?<amount>[\\d\\s,.]*)$")
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(stripBlanks(v.get("amount"))));
                        })

                        // 0 , 2 5 0 0 0 % P r o v i s i o n : EUR 4 9 , 6 3
                        .section("fee", "currency").optional()
                        .match(".* [\\d\\s,.]* % P r o v i s i o n : (?<currency>\\w{3}) (?<fee>[\\d\\s,.-]*$)")
                        .assign((t, v) -> {
                            t.getPortfolioTransaction()
                                    .addUnit(new Unit(Unit.Type.FEE,
                                                    Money.of(asCurrencyCode(v.get("currency")),
                                                                            asAmount(stripBlanks(v.get("fee"))))));
                        })

                        // 0 , 2 5 0 0 0 % G e s a m t p r o v i s i o n : EUR 1 1 9 , 3 8
                        .section("fee", "currency").optional()
                        .match(".* [\\d\\s,.]* % G e s a m t p r o v i s i o n : (?<currency>\\w{3}) (?<fee>[\\d\\s,.-]*$)")
                        .assign((t, v) -> {
                            t.getPortfolioTransaction()
                                    .addUnit(new Unit(Unit.Type.FEE,
                                                    Money.of(asCurrencyCode(v.get("currency")),
                                                                            asAmount(stripBlanks(v.get("fee"))))));
                        })

                        // S o c k e l b e t r a g : EUR 4 , 9 0
                        .section("fee", "currency").optional()
                        .match("S o c k e l b e t r a g : (?<currency>\\w{3}) (?<fee>[\\d\\s,.-]*$)")
                        .assign((t, v) -> {
                            t.getPortfolioTransaction()
                                    .addUnit(new Unit(Unit.Type.FEE,
                                                    Money.of(asCurrencyCode(v.get("currency")),
                                                                            asAmount(stripBlanks(v.get("fee"))))));
                        })

                        // U m s c h r e i b e e n t g e l t : EUR 0 , 6 0
                        .section("fee", "currency").optional()
                        .match("U m s c h r e i b e e n t g e l t : (?<currency>\\w{3}) (?<fee>[\\d\\s,.-]*$)")
                        .assign((t, v) -> {
                            t.getPortfolioTransaction()
                                    .addUnit(new Unit(Unit.Type.FEE,
                                                    Money.of(asCurrencyCode(v.get("currency")),
                                                                            asAmount(stripBlanks(v.get("fee"))))));
                        })

                        // 0 , 0 5 9 9 7 % V a r i a b l e B ö r s e n s p e s e n : EUR 2 4 , 1 9 -
                        .section("fee", "currency").optional()
                        .match(".* [\\d\\s,.]* % V a r i a b l e B ö r s e n s p e s e n : (?<currency>\\w{3}) (?<fee>[\\d\\s,.-]*$)")
                        .assign((t, v) -> {
                            t.getPortfolioTransaction()
                                    .addUnit(new Unit(Unit.Type.FEE,
                                                    Money.of(asCurrencyCode(v.get("currency")),
                                                                            asAmount(stripBlanks(v.get("fee"))))));
                        })

                        // T r a n s a k t i o n s e n t g e l t : EUR 4 , 6 1 -
                        .section("fee", "currency").optional()
                        .match("T r a n s a k t i o n s e n t g e l t : (?<currency>\\w{3}) (?<fee>[\\d\\s,.-]*$)")
                        .assign((t, v) -> {
                            t.getPortfolioTransaction()
                                    .addUnit(new Unit(Unit.Type.FEE,
                                                    Money.of(asCurrencyCode(v.get("currency")),
                                                                            asAmount(stripBlanks(v.get("fee"))))));
                        })

                        // X e t r a - E n t g e l t : EUR 2 , 7 3
                        .section("fee", "currency").optional()
                        .match("X e t r a - E n t g e l t : (?<currency>\\w{3}) (?<fee>[\\d\\s,.-]*$)")
                        .assign((t, v) -> {
                            t.getPortfolioTransaction()
                                    .addUnit(new Unit(Unit.Type.FEE,
                                                    Money.of(asCurrencyCode(v.get("currency")),
                                                                            asAmount(stripBlanks(v.get("fee"))))));
                        })

                        .section("feeInPercent", "currency", "marketValue").optional()
                        .match("(S t .|St.) [\\d\\s,.]* (?<currency>\\w{3}) (?<marketValue>[\\d\\s,.]*)$")
                        .match("I n dem K u r s w e r t s i n d (?<feeInPercent>[\\d\\s,.]*) % A u s g a b e a u f s c h l a g d e r B a n k e n t h a l t e n.*")
                        .assign((t, v) -> {
                            // Fee in percent on the market value
                            double marketValue = Double.parseDouble(stripBlanks(v.get("marketValue")).replace(',', '.'));
                            double feeInPercent = Double.parseDouble(stripBlanks(v.get("feeInPercent")).replace(',', '.'));
                            String fee =  Double.toString(marketValue / 100.0 * feeInPercent).replace('.', ',');

                            t.getPortfolioTransaction()
                                    .addUnit(new Unit(Unit.Type.FEE,
                                                    Money.of(asCurrencyCode(v.get("currency")),
                                                                            asAmount(fee))));
                        })

                        .wrap(BuySellEntryItem::new);
    }

    private void addTaxTreatmentTransaction()
    {
        DocumentType type = new DocumentType("Steuerliche Behandlung: (Wertpapier(kauf|verkauf)|Verkauf|.*Dividende) .*");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("Steuerliche Behandlung: (Wertpapier(kauf|verkauf)|Verkauf|.*Dividende) .*");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                    .subject(() -> {
                        AccountTransaction t = new AccountTransaction();
                        t.setType(AccountTransaction.Type.TAX_REFUND);
                        return t;
                    })

                    // Steuerliche Behandlung: Wertpapierkauf Nr. 72006822 vom 17.02.2021
                    // Stk. 0,345 VERMOEGENSMA.BALANCE A EO , WKN / ISIN: A0M16S / LU0321021155
                    .section("shares", "name", "wkn", "isin", "date")
                    .match("Steuerliche Behandlung: .* vom (?<date>\\d+.\\d+.\\d{4})")
                    .match("Stk. -?(?<shares>[\\d,.]*) (?<name>.*) [\\s,]? WKN \\/ ISIN: (?<wkn>.*) \\/ (?<isin>\\w{12})")
                    .assign((t, v) -> {
                        t.setShares(asShares(v.get("shares")));
                        t.setSecurity(getOrCreateSecurity(v));
                        if (v.get("time") != null)
                            t.setDateTime(asDate(v.get("date"), v.get("time")));
                        else
                            t.setDateTime(asDate(v.get("date")));
                    })

                    // abgeführte Steuern EUR 0 , 0 0
                    .section("currency", "tax", "sign").optional()
                    .match("(abgeführte Steuern|erstattete Steuern) (?<currency>\\w{3})(?<sign>[-\\s]*)?(?<tax>[\\d\\s,.]*)")
                    .assign((t, v) -> {
                        t.setAmount(asAmount(stripBlanks(v.get("tax"))));
                        t.setCurrencyCode(asCurrencyCode(v.get("currency")));

                        String sign = stripBlanks(v.get("sign"));
                        if ("-".equals(sign))
                        {
                            // change type for withdrawals
                            t.setType(AccountTransaction.Type.TAXES);
                        }
                    })

                    .wrap(t -> {
                        if (t.getCurrencyCode() != null && t.getAmount() != 0)
                            return new TransactionItem(t);
                        return null;
                    });
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("(D i v i d e n d e n g u t s c h r i f t|E r t r a g s g u t s c h r i f t)");
        this.addDocumentTyp(type);

        Block block = new Block("(D i v i d e n d e n g u t s c h r i f t|E r t r a g s g u t s c h r i f t)");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction entry = new AccountTransaction();
                            entry.setType(AccountTransaction.Type.DIVIDENDS);
                            return entry;
                        });

        pdfTransaction
                        .section("date", "wkn", "shares", "name", "nameContinued", "isin")
                        .match(".*WKN\\/ISIN.*")
                        .match("(p e r|per) (?<date>[\\d\\s]+.[\\d\\s]+.[\\d\\s]+) (?<name>.*) (?<wkn>([\\w]{6}|\\w\\s\\w\\s\\w\\s\\w\\s\\w\\s\\w))$")
                        .match("STK (?<shares>[\\d\\s,.]+) (?<nameContinued>.*) (?<isin>[\\w]{12})$")
                        .assign((t, v) -> {
                            v.put("wkn", stripBlanks(v.get("wkn")));
                            v.put("isin", stripBlanks(v.get("isin")));
                            t.setDateTime(asDate(stripBlanks(v.get("date"))));
                            t.setShares(asShares(stripBlanks(v.get("shares"))));
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .section("amount", "currency")
                        .match(".*Zu I h r e n Gunsten.*")
                        .match(".* \\w{3} [\\d\\s]+.[\\d\\s]+.[\\d\\s]+ (?<currency>\\w{3})(?<amount>[\\d\\s,.]*)$")
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(stripBlanks(v.get("amount"))));
                        })

                        .section("note1", "note2", "note3").optional()
                        .match("^\\w{3} [\\d\\s,.]* .* (?<note2>[\\d\\s]+.[\\d\\s]+.[\\d\\s]+) .* (?<note3>[\\d\\s]+.[\\d\\s]+.[\\d\\s]+)$")
                        .match(".*")
                        .match("Abrechnung (?<note1>.*)")
                        .assign((t, v) -> 
                        {
                            String note = stripBlanks(v.get("note1")) + " " + stripBlanks(v.get("note2")) + " - " + stripBlanks(v.get("note3"));
                            t.setNote(note);
                        })

                        // B r u t t o b e t r a g : USD 8 6 , 6 3
                        // 2 2 , 0 0 0 % Q u e l l e n s t e u e r USD 1 9 , 0 6 -
                        // Ausmachender B e t r a g USD 6 7 , 3 3
                        // zum D e v i s e n k u r s : EUR/USD 1 ,098400 EUR 6 1 , 3 0
                        .section("exchangeRate", "fxAmount", "fxCurrency", "amount", "currency").optional()
                        .match("B r u t t o b e t r a g : (?<fxCurrency>\\w{3}) (?<fxAmount>[\\d\\s,.]*)$")
                        .match(".* \\w{3}\\/\\w{3} (?<exchangeRate>[\\d\\s,.]*) (?<currency>\\w{3}) (?<amount>[\\d\\s,.]*)$")                     
                        .assign((t, v) -> {
                            BigDecimal exchangeRate = asExchangeRate(stripBlanks(v.get("exchangeRate")));
                            if (t.getCurrencyCode().contentEquals(asCurrencyCode(v.get("fxCurrency"))))
                            {
                                exchangeRate = BigDecimal.ONE.divide(exchangeRate, 10, RoundingMode.HALF_DOWN);
                            }
                            type.getCurrentContext().put("exchangeRate", exchangeRate.toPlainString());

                            if (!t.getCurrencyCode().equals(t.getSecurity().getCurrencyCode()))
                            {
                                BigDecimal inverseRate = BigDecimal.ONE.divide(exchangeRate, 10,
                                                RoundingMode.HALF_DOWN);

                                // check, if forex currency is transaction
                                // currency or not and swap amount, if necessary
                                Unit grossValue;
                                if (!asCurrencyCode(v.get("fxCurrency")).equals(t.getCurrencyCode()))
                                {
                                    Money fxAmount = Money.of(asCurrencyCode(stripBlanks(v.get("fxCurrency"))),
                                                    asAmount(v.get("fxAmount")));
                                    Money amount = Money.of(asCurrencyCode(v.get("currency")),
                                                    asAmount(stripBlanks(v.get("amount"))));
                                    grossValue = new Unit(Unit.Type.GROSS_VALUE, amount, fxAmount, inverseRate);
                                }
                                else
                                {
                                    Money amount = Money.of(asCurrencyCode(v.get("fxCurrency")),
                                                    asAmount(stripBlanks(v.get("fxAmount"))));
                                    Money fxAmount = Money.of(asCurrencyCode(v.get("currency")),
                                                    asAmount(stripBlanks(v.get("amount"))));
                                    grossValue = new Unit(Unit.Type.GROSS_VALUE, amount, fxAmount, inverseRate);
                                }
                                t.addUnit(grossValue);
                            }
                        })

                        .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);

        block.set(pdfTransaction);
    }

    private void addKontoauszugGiro()
    {
        DocumentType type = new DocumentType("Kontoauszug", (context, lines) -> {
            Pattern pCurrency = Pattern.compile("^(Kontowährung )(\\w{3}).*$");
            // read the current context here
            for (String line : lines)
            {
                Matcher m = pCurrency.matcher(line);
                if (m.matches())
                {
                    context.put("currency", m.group(2).toUpperCase());
                }
            }
            Pattern pYear = Pattern.compile("^(Kontoauszug vom)( \\d \\d . \\d \\d .)(?<year> \\d \\d \\d \\d)$");
            // read the current context here
            for (String line : lines)
            {
                Matcher m = pYear.matcher(line);
                if (m.matches())
                {
                    // Read year
                    context.put("year", stripBlanks(m.group(3)));
                }
            }
        });
        this.addDocumentTyp(type);

        Block removalblock = new Block("^((?!A l t e r Kontostand)(?!Neuer Kontostand).*) \\d \\, \\d \\d( -)$");
        type.addBlock(removalblock);
        removalblock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction entry = new AccountTransaction();
                            entry.setType(AccountTransaction.Type.REMOVAL);
                            return entry;
                        })

                        .section("day", "month", "value")
                        .match("^(.*)(?<day>(0 [1-9])|([1-2] [0-9])|(3 [0-1])) \\. (?<month>((0 [1-9])|(1 [0-2])) )(?<value>((\\. )?(\\d ){1,3})+\\, (\\d \\d))( \\-)$")
                        .assign((t, v) -> {
                            Map<String, String> context = type.getCurrentContext();
                            t.setDateTime(asDate(stripBlanks(v.get("day"))+"."+stripBlanks(v.get("month"))+"."+context.get("year")));     
                            t.setAmount(asAmount(stripBlanks(v.get("value"))));
                            t.setCurrencyCode(context.get("currency"));
                        })

                        .wrap(TransactionItem::new));

        Block depositblock = new Block("^((?!A l t e r Kontostand)(?!Neuer Kontostand).*) \\d \\, \\d \\d$");
        type.addBlock(depositblock);
        depositblock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction entry = new AccountTransaction();
                            entry.setType(AccountTransaction.Type.DEPOSIT);
                            return entry;
                        })

                        .section("day", "month", "value")
                        .match("^(.*)(?<day>(0 [1-9])|([1-2] [0-9])|(3 [0-1])) \\. (?<month>((0 [1-9])|(1 [0-2])) )(?<value>((\\. )?(\\d ){1,3})+\\, (\\d \\d))$")
                        .assign((t, v) -> {
                            Map<String, String> context = type.getCurrentContext();
                            t.setDateTime(asDate(stripBlanks(v.get("day"))+"."+stripBlanks(v.get("month"))+"."+context.get("year")));     
                            t.setAmount(asAmount(stripBlanks(v.get("value"))));
                            t.setCurrencyCode(context.get("currency"));
                        })

                        .wrap(TransactionItem::new));
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                        // 2 2 , 0 0 0 % Q u e l l e n s t e u e r USD 1 9 , 0 6 -
                        .section("tax", "currency").optional()
                        .match(".* [\\d\\s,.]* % Q u e l l e n s t e u e r (?<currency>\\w{3}) (?<tax>[\\d\\s,.-]*)$")
                        .assign((t, v) -> {
                            v.put("tax", stripBlanks(v.get("tax")));
                            processTaxEntries(t, v, type);
                        });
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                        // f r emde Spesen USD 0 , 2 4 -
                        .section("fee", "currency").optional()
                        .match(".* Spesen (?<currency>\\w{3}) (?<fee>[\\d\\s,.-]*)$")
                        .assign((t, v) -> {
                            v.put("fee", stripBlanks(v.get("fee")));
                            processFeeEntries(t, v, type);
                        });
    }

    private void processTaxEntries(Object t, Map<String, String> v, DocumentType type)
    {
        if (t instanceof name.abuchen.portfolio.model.Transaction)
        {
            Money tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")));
            PDFExtractorUtils.checkAndSetTax(tax, 
                            (name.abuchen.portfolio.model.Transaction) t, type);
        }
        else
        {
            Money tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")));
            PDFExtractorUtils.checkAndSetTax(tax,
                            ((name.abuchen.portfolio.model.BuySellEntry) t).getPortfolioTransaction(), type);
        }
    }

    private void processFeeEntries(Object t, Map<String, String> v, DocumentType type)
    {
        if (t instanceof name.abuchen.portfolio.model.Transaction)
        {
            Money fee = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee")));
            PDFExtractorUtils.checkAndSetFee(fee, 
                            (name.abuchen.portfolio.model.Transaction) t, type);
        }
        else
        {
            Money fee = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee")));
            PDFExtractorUtils.checkAndSetFee(fee,
                            ((name.abuchen.portfolio.model.BuySellEntry) t).getPortfolioTransaction(), type);
        }
    }

    private String stripBlanks(String input)
    {
        return input.replaceAll(" ", ""); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Override
    public String getLabel()
    {
        return "Commerzbank"; //$NON-NLS-1$
    }
}
