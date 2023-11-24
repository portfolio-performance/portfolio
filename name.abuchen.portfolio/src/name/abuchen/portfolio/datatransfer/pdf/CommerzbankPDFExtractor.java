package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetFee;
import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.concatenate;
import static name.abuchen.portfolio.util.TextUtil.stripBlanks;
import static name.abuchen.portfolio.util.TextUtil.trim;

import java.math.BigDecimal;

import name.abuchen.portfolio.datatransfer.ExtrExchangeRate;
import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class CommerzbankPDFExtractor extends AbstractPDFExtractor
{
    public CommerzbankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("C O M M E R Z B A N K");
        addBankIdentifier("Commerzbank AG");
        addBankIdentifier("COBADE");

        addBuySellTransaction();
        addDividendeTransaction();
        addTaxTreatmentTransaction();
        addAccountStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Commerzbank AG";
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

        Block firstRelevantLine = new Block("^W e r t p a p i e r (k a u f|v e r k a u f)$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "Verkauf" change from BUY to SELL
                .section("type").optional()
                .match("(?<type>W e r t p a p i e r v e r k a u f)")
                .assign((t, v) -> {
                    if ("W e r t p a p i e r v e r k a u f".equals(v.get("type")))
                        t.setType(PortfolioTransaction.Type.SELL);
                })

                // W e r t p a p i e r - B e z e i c h n u n g W e r t p a p i e r k e n n n u m m e r
                // A l l i a n z SE 8 4 0 4 0 0
                // v i n k . N a m e n s - A k t i e n o . N .
                // S t . 0 , 5 7 2 EUR 4 3 , 6 4
                .section("name", "wkn", "nameContinued", "currency")
                .find("W e r t p a p i e r \\- B e z e i c h n u n g .*")
                .match("(?<name>.*)(,)? (?<wkn>([\\w]{6}|\\w\\s\\w\\s\\w\\s\\w\\s\\w\\s\\w))$")
                .match("(?<nameContinued>.*)")
                .match("^(Summe )?S([\\s]+)?t([\\s]+)?\\. [\\.,\\d\\s]+ (?<currency>[\\w]{3}) [\\.,\\d\\s]+( .*)?$")
                .assign((t, v) -> {
                    v.put("wkn", stripBlanks(v.get("wkn")));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                // S t . 2 0 0 EUR 2 0 1 , 7 0
                // Summe S t . 2 5 0 EUR 1 9 1 , 0 0 8 6 4 EUR 4 7 . 7 5 2 , 1 6
                .section("shares")
                .match("^(Summe )?S([\\s]+)?t([\\s]+)?\\. (?<shares>[\\.,\\d\\s]+) .*$")
                .assign((t, v) -> t.setShares(asShares(stripBlanks(v.get("shares")))))

                // 1 2 : 0 6 S t . 2 3 0 EUR 1 8 4 , 1 6 EUR 4 2 . 3 5 6 , 8 0
                .section("time").optional()
                .match("^(?<time>[\\d\\s]+:[\\d\\s]+) (S t .|St.) [\\.,\\d\\s]+ [\\w]{3} [\\.,\\d\\s]+ [\\w]{3} [\\.,\\d\\s]+$")
                .assign((t, v) -> type.getCurrentContext().put("time", stripBlanks(v.get("time"))))

                // H a n d e l s z e i t : 1 3 : 1 0 U h r (MEZ/MESZ)
                .section("time").optional()
                .match("^H a n d e l s z e i t : (?<time>[\\d\\s]+:[\\d\\s]+) .*$")
                .assign((t, v) -> type.getCurrentContext().put("time", stripBlanks(v.get("time"))))

                // G e s c h ä f t s t a g : 1 7 . 0 2 . 2 0 2 1 A b w i c k l u n g : F e s t p r e i s
                // G e s c h ä f t s t a g : 3 1 . 0 1 . 2 0 2 1 A u s f ü h r u n g s p l a t z : FRANKFURT
                .section("date")
                .match("^G e s c h . f t s t a g : (?<date>[\\d\\s]+\\.[\\d\\s]+\\.[\\d\\s]+) .*$")
                .assign((t, v) -> {
                    if (type.getCurrentContext().get("time") != null)
                        t.setDate(asDate(stripBlanks(v.get("date")), type.getCurrentContext().get("time")));
                    else
                        t.setDate(asDate(stripBlanks(v.get("date"))));
                })

                // DE26 1 0 0 4 0 0 4 8 0 6 8 0 4 0 3 3 0 2 EUR 2 0 . 0 4 . 2 0 1 7 EUR 2 4 , 9 6
                .section("currency", "amount")
                .match("^.* [\\w]{3} [\\d\\s]+\\.[\\d\\s]+\\.[\\d\\s]+ (?<currency>\\w{3})(?<amount>[\\.,\\d\\s]+)$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(stripBlanks(v.get("amount"))));
                })

                // @formatter:off
                // 1 0 8 0 4 3 1 3 7 2 7 0 Rechnungsnummer : 4 1 9 7 9 4 9 1 6 7 9 8 D 1 C 2
                // @formatter:on
                .section("note").optional() //
                .match("^.*Rechnungsnummer[\\s]{1,}: (?<note>.*)$") //
                .assign((t, v) -> t.setNote("R.-Nr.: " + stripBlanks(v.get("note"))))

                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addTaxTreatmentTransaction()
    {
        DocumentType type = new DocumentType("Steuerliche Behandlung: (Wertpapier(kauf|verkauf)|Verkauf|.*Dividende)");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.TAX_REFUND);
            return entry;
        });

        Block firstRelevantLine = new Block("^.*Referenz\\-Nummer:.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type is negative change from TAX_REFUND to TAXES
                .section("type").optional()
                .match("^(abgef.hrte Steuern|erstattete Steuern) [\\w]{3}(?<type>[\\-\\s]+)?[\\.,\\d\\s]+$")
                .assign((t, v) -> {
                    if ("-".equals(stripBlanks(v.get("type"))))
                        t.setType(AccountTransaction.Type.TAXES);
                })

                // Stk. -10,195 VERMOEGENSMA.BALANCE A EO , WKN / ISIN: A0M16S / LU0321021155
                // Zu Ihren Gunsten vor Steuern: EUR 1 . 4 3 9 , 1 3
                .section("name", "wkn", "isin", "currency")
                .match("^Stk\\. [\\-\\.,\\d]+ (?<name>.*) , WKN \\/ ISIN: (?<wkn>.*) \\/ (?<isin>[\\w]{12})$")
                .match("^(abgef.hrte Steuern|erstattete Steuern) (?<currency>[\\w]{3}) ([\\-\\s]+)?[\\.,\\d\\s]+$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // Stk. -10,195 VERMOEGENSMA.BALANCE A EO , WKN / ISIN: A0M16S / LU0321021155
                .section("shares")
                .match("^Stk\\. (\\-)?(?<shares>[\\.,\\d]+) .*$")
                .assign((t, v) -> t.setShares(asShares(stripBlanks(v.get("shares")))))

                // Steuerliche Behandlung: Wertpapierkauf Nr. 72006822 vom 17.02.2021
                .section("date")
                .match("^Steuerliche Behandlung: .* vom (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                .assign((t, v) -> t.setDateTime(asDate(stripBlanks(v.get("date")))))

                // abgeführte Steuern EUR 0 , 0 0
                // erstattete Steuern EUR 5 4 8 , 5 1
                .section("currency", "amount").optional()
                .match("^(abgef.hrte Steuern|erstattete Steuern) (?<currency>[\\w]{3}) ([\\-\\s]+)?(?<amount>[\\.,\\d\\s]+)$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(stripBlanks(v.get("amount"))));
                })

                // @formatter:off
                // 01111 City Referenz-Nummer: 0W7U3RJX11111111
                // @formatter:on
                .section("note").optional() //
                .match("^.*Referenz\\-Nummer: (?<note>.*)$") //
                .assign((t, v) -> t.setNote("Ref.-Nr.: " + trim(v.get("note"))))

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

        Block block = new Block("^(D i v i d e n d e n g u t s c h r i f t|E r t r a g s g u t s c h r i f t)$");
        type.addBlock(block);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.DIVIDENDS);
            return entry;
        });

        pdfTransaction
                // p e r 2 7 . 0 3 . 2 0 2 0 Samsung E l e c t r o n i c s Co. L t d . 881823
                // STK 1 2 , 0 0 0 R.Shs(NV)Pf(GDR144A)/25 SW 100 US7960502018
                // USD 7 ,219127 D i v i d e n d e p r o S t ü c k f ü r G e s c h ä f t s j a h r 0 1 . 0 1 . 2 0 b i s 3 1 . 1 2 . 2 0
                .section("name", "wkn", "nameContinued", "isin", "currency")
                .match("^p([\\s]+)?e([\\s]+)?r [\\d\\s]+.[\\d\\s]+.[\\d\\s]+ (?<name>.*) (?<wkn>([\\w]{6}|\\w\\s\\w\\s\\w\\s\\w\\s\\w\\s\\w))$")
                .match("^STK [\\.,\\d\\s]+ (?<nameContinued>.*) (?<isin>[\\w]{12})$")
                .match("^(?<currency>[\\w]{3}) [\\.,\\d\\s]+ (D i v i d e n d e|A u s s c h . t t u n g) .*$")
                .assign((t, v) -> {
                    v.put("wkn", stripBlanks(v.get("wkn")));
                    v.put("isin", stripBlanks(v.get("isin")));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                // STK 1 2 , 0 0 0 R.Shs(NV)Pf(GDR144A)/25 SW 100 US7960502018
                .section("shares")
                .match("^STK (?<shares>[\\.,\\d\\s]+) .*$")
                .assign((t, v) -> t.setShares(asShares(stripBlanks(v.get("shares")))))

                // p e r 2 7 . 0 3 . 2 0 2 0 Samsung E l e c t r o n i c s Co. L t d . 881823
                .section("date")
                .match("^p([\\s]+)?e([\\s]+)?r (?<date>[\\d\\s]+.[\\d\\s]+.[\\d\\s]+) .*$")
                .assign((t, v) -> t.setDateTime(asDate(stripBlanks(v.get("date")))))

                .section("currency", "amount")
                .match(".* [\\w]{3} [\\d\\s]+\\.[\\d\\s]+\\.[\\d\\s]+ (?<currency>[\\w]{3})(?<amount>[\\.,\\d\\s]+)$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(stripBlanks(v.get("amount"))));
                })

                // B r u t t o b e t r a g : USD 8 6 , 6 3
                // 2 2 , 0 0 0 % Q u e l l e n s t e u e r USD 1 9 , 0 6 -
                // Ausmachender B e t r a g USD 6 7 , 3 3
                // zum D e v i s e n k u r s : EUR/USD 1 ,098400 EUR 6 1 , 3 0
                .section("fxCurrency", "fxGross", "exchangeRate", "baseCurrency", "termCurrency", "currency").optional()
                .match("^B r u t t o b e t r a g : (?<fxCurrency>[\\w]{3}) (?<fxGross>[\\.,\\d\\s]+)$")
                .match("^.* (?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3}) (?<exchangeRate>[\\.,\\d\\s]+) (?<currency>[\\w]{3}) [\\.,\\d\\s]+$")
                .assign((t, v) -> {
                    v.put("exchangeRate", stripBlanks(v.get("exchangeRate")));

                    ExtrExchangeRate rate = asExchangeRate(v);
                    type.getCurrentContext().putType(rate);

                    Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(stripBlanks(v.get("fxGross"))));
                    Money gross = rate.convert(asCurrencyCode(v.get("currency")), fxGross);

                    checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                })

                // @formatter:off
                // ( R e f e r e n z - N r . 3345AO12BC3D4445E).
                // @formatter:on
                .section("note").optional() //
                .match("^.*R e f e r e n z - N r \\. (?<note>.*)\\).*$") //
                .assign((t, v) -> t.setNote("Ref.-Nr.: " + trim(v.get("note"))))

                // USD 7 ,219127 D i v i d e n d e p r o S t ü c k f ü r G e s c h ä f t s j a h r 0 1 . 0 1 . 2 0 b i s 3 1 . 1 2 . 2 0
                // z a h l b a r ab 2 6 . 0 5 . 2 0 2 0 Z w i s c h e n d i v i d e n d e
                // EUR 3 , 9 0 D i v i d e n d e p r o S t ü c k f ü r G e s c h ä f t s j a h r 0 1 . 1 0 . 1 8 b i s 3 0 . 0 9 . 1 9
                // Abrechnung D i v i d e n d e n g u t s c h r i f t
                .section("note1", "note2", "note3").optional()
                .match("^[\\w]{3} [\\.,\\d\\s]+ [\\D]+ (?<note2>[\\d\\s]+\\.[\\d\\s]+\\.[\\d\\s]+) [\\D]+ (?<note3>[\\d\\s]+\\.[\\d\\s]+\\.[\\d\\s]+)$")
                .match("^(.* [\\d\\s]+\\.[\\d\\s]+\\.[\\d\\s]+|Abrechnung) (?<note1>[\\D]+)$")
                .assign((t, v) -> t.setNote(concatenate(t.getNote(), //
                                stripBlanks(v.get("note1")) + " " + stripBlanks(v.get("note2")) + " - " + stripBlanks(v.get("note3")), //
                                " | ")))

                .conclude(ExtractorUtils.fixGrossValueA())

                .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);

        block.set(pdfTransaction);
    }

    private void addAccountStatementTransaction()
    {
        final DocumentType type = new DocumentType("Kontoauszug", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // Kontowährung Euro
                                        // @formatter:on
                                        .section("currency") //
                                        .match("^Kontow.hrung (?<currency>.*)$") //
                                        .assign((ctx, v) -> {
                                            if ("Euro".equals(trim(v.get("currency"))))
                                                ctx.put("currency", "EUR");
                                        })

                                        // @formatter:off
                                        // Kontoauszug vom 2 9 . 0 1 . 2 0 2 1
                                        // @formatter:on
                                        .section("year") //
                                        .match("^Kontoauszug vom [\\d\\s]+\\.[\\d\\s]+\\.(?<year>[\\d\\s]+)$") //
                                        .assign((ctx, v) -> ctx.put("year", stripBlanks(v.get("year")))));

        this.addDocumentTyp(type);

        Block removalblock = new Block("^((?!A l t e r Kontostand)(?!Neuer Kontostand).*) \\d \\, \\d \\d( -)$");
        type.addBlock(removalblock);
        removalblock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.REMOVAL);
                            return accountTransaction;
                        })

                        .section("day", "month", "amount") //
                        .documentContext("currency", "year") //
                        .match("^(.*)(?<day>(0 [1-9])|([1-2] [0-9])|(3 [0-1])) \\. (?<month>((0 [1-9])|(1 [0-2])) )(?<amount>((\\. )?(\\d ){1,3})+\\, (\\d \\d))( \\-)$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(stripBlanks(v.get("day")) + "." //
                                            + stripBlanks(v.get("month")) + "." //
                                            + v.get("year")));
                            t.setAmount(asAmount(stripBlanks(v.get("amount"))));
                            t.setCurrencyCode(v.get("currency"));
                        })

                        .wrap(TransactionItem::new));

        Block depositblock = new Block("^((?!A l t e r Kontostand)(?!Neuer Kontostand).*) \\d \\, \\d \\d$");
        type.addBlock(depositblock);
        depositblock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .section("day", "month", "amount") //
                        .documentContext("currency", "year") //
                        .match("^(.*)(?<day>(0 [1-9])|([1-2] [0-9])|(3 [0-1])) \\. (?<month>((0 [1-9])|(1 [0-2])) )(?<amount>((\\. )?(\\d ){1,3})+\\, (\\d \\d))$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(stripBlanks(v.get("day")) + "." //
                                            + stripBlanks(v.get("month")) + "." //
                                            + v.get("year")));
                            t.setAmount(asAmount(stripBlanks(v.get("amount"))));
                            t.setCurrencyCode(v.get("currency"));
                        })

                        .wrap(t -> {
                            if (t.getCurrencyCode() != null && t.getAmount() != 0)
                                return new TransactionItem(t);
                            return null;
                        }));
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                        // 2 2 , 0 0 0 % Q u e l l e n s t e u e r USD 1 9 , 0 6 -
                        .section("currency", "withHoldingTax").optional()
                        .match("^.* [\\.,\\d\\s]+ % Q u e l l e n s t e u e r (?<currency>\\w{3}) (?<withHoldingTax>[\\.,\\d\\s]+)( \\-)?$")
                        .assign((t, v) -> {
                            v.put("withHoldingTax", stripBlanks(v.get("withHoldingTax")));
                            processWithHoldingTaxEntries(t, v, "withHoldingTax", type);
                        });
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                        // f r emde Spesen USD 0 , 2 4 -
                        .section("currency", "fee").optional()
                        .match("^.* Spesen (?<currency>[\\w]{3}) (?<fee>[\\.,\\d\\s]+)( \\-)?$")
                        .assign((t, v) -> {
                            v.put("fee", stripBlanks(v.get("fee")));
                            processFeeEntries(t, v, type);
                        })

                        // 0 , 2 5 0 0 0 % P r o v i s i o n : EUR 4 9 , 6 3
                        .section("currency", "fee").optional()
                        .match("^.* [\\.,\\d\\s]+ % P r o v i s i o n : (?<currency>[\\w]{3}) (?<fee>[\\.,\\d\\s]+( \\-)?$)")
                        .assign((t, v) -> {
                            v.put("fee", stripBlanks(v.get("fee")));
                            processFeeEntries(t, v, type);
                        })

                        // 0 , 2 5 0 0 0 % G e s a m t p r o v i s i o n : EUR 1 1 9 , 3 8
                        .section("currency", "fee").optional()
                        .match("^.* [\\.,\\d\\s]+ % G e s a m t p r o v i s i o n : (?<currency>[\\w]{3}) (?<fee>[\\.,\\d\\s]+( \\-)?$)")
                        .assign((t, v) -> {
                            v.put("fee", stripBlanks(v.get("fee")));
                            processFeeEntries(t, v, type);
                        })

                        // S o c k e l b e t r a g : EUR 4 , 9 0
                        .section("currency", "fee").optional()
                        .match("^S o c k e l b e t r a g : (?<currency>[\\w]{3}) (?<fee>[\\.,\\d\\s]+( \\-)?$)")
                        .assign((t, v) -> {
                            v.put("fee", stripBlanks(v.get("fee")));
                            processFeeEntries(t, v, type);
                        })

                        // U m s c h r e i b e e n t g e l t : EUR 0 , 6 0
                        .section("currency", "fee").optional()
                        .match("^U m s c h r e i b e e n t g e l t : (?<currency>[\\w]{3}) (?<fee>[\\.,\\d\\s]+( \\-)?$)")
                        .assign((t, v) -> {
                            v.put("fee", stripBlanks(v.get("fee")));
                            processFeeEntries(t, v, type);
                        })

                        // 0 , 0 5 9 9 7 % V a r i a b l e B ö r s e n s p e s e n : EUR 2 4 , 1 9 -
                        .section("currency", "fee").optional()
                        .match("^.* [\\.,\\d\\s]+ % V a r i a b l e B . r s e n s p e s e n : (?<currency>[\\w]{3}) (?<fee>[\\.,\\d\\s]+( \\-)?$)")
                        .assign((t, v) -> {
                            v.put("fee", stripBlanks(v.get("fee")));
                            processFeeEntries(t, v, type);
                        })

                        // T r a n s a k t i o n s e n t g e l t : EUR 4 , 6 1 -
                        .section("currency", "fee").optional()
                        .match("^T r a n s a k t i o n s e n t g e l t : (?<currency>[\\w]{3}) (?<fee>[\\.,\\d\\s]+( \\-)?$)")
                        .assign((t, v) -> {
                            v.put("fee", stripBlanks(v.get("fee")));
                            processFeeEntries(t, v, type);
                        })

                        // X e t r a - E n t g e l t : EUR 2 , 7 3
                        .section("currency", "fee").optional()
                        .match("^X e t r a \\- E n t g e l t : (?<currency>[\\w]{3}) (?<fee>[\\.,\\d\\s]+)( \\-)?$")
                        .assign((t, v) -> {
                            v.put("fee", stripBlanks(v.get("fee")));
                            processFeeEntries(t, v, type);
                        })

                        // K u r s w e r t : EUR 4 9 , 9 2
                        // I n dem K u r s w e r t s i n d 2 , 4 3 9 0 2 % A u s g a b e a u f s c h l a g d e r B a n k e n t h a l t e n .
                        .section("currency", "amount", "percentageFee").optional()
                        .match("^K u r s w e r t : (?<currency>[\\w]{3}) (?<amount>[\\.,\\d\\s]+)( \\-)?$")
                        .match("^I n dem K u r s w e r t s i n d (?<percentageFee>[\\.,\\d\\s]+) % A u s g a b e a u f s c h l a g d e r B a n k e n t h a l t e n.*$")
                        .assign((t, v) -> {
                            BigDecimal percentageFee = asBigDecimal(stripBlanks(v.get("percentageFee")));
                            BigDecimal amount = asBigDecimal(stripBlanks(v.get("amount")));

                            if (percentageFee.compareTo(BigDecimal.ZERO) != 0)
                            {
                                BigDecimal fxFee = amount.multiply(percentageFee, Values.MC);

                                Money fee = Money.of(asCurrencyCode(v.get("currency")),
                                                fxFee.setScale(0, Values.MC.getRoundingMode()).longValue());

                                checkAndSetFee(fee, t, type.getCurrentContext());
                            }
                        });
    }
}
