package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.pdf.PDFExtractorUtils.checkAndSetFee;
import static name.abuchen.portfolio.datatransfer.pdf.PDFExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.trim;

import java.math.BigDecimal;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentContext;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class INGDiBaPDFExtractor extends AbstractPDFExtractor
{
    private static final String isJointAccount = "isJointAccount"; //$NON-NLS-1$

    BiConsumer<DocumentContext, String[]> jointAccount = (context, lines) -> {
        Pattern pJointAccount = Pattern.compile("KapSt anteilig 50,00 %.*"); //$NON-NLS-1$
        Boolean bJointAccount = false;

        for (String line : lines)
        {
            Matcher m = pJointAccount.matcher(line);
            if (m.matches())
            {
                context.put(isJointAccount, Boolean.TRUE.toString());
                bJointAccount = true;
                break;
            }
        }

        if (!bJointAccount)
            context.put(isJointAccount, Boolean.FALSE.toString());

    };

    public INGDiBaPDFExtractor(Client client)
    {
        super(client);

        addBuySellTransaction();
        addDividendeTransaction();
        addAdvanceTaxTransaction();
    }

    @Override
    public String getLabel()
    {
        return "ING-DiBa AG"; //$NON-NLS-1$
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("(Wertpapierabrechnung (Kauf|Bezug|Verkauf|Verk\\. Teil\\-\\/Bezugsr\\.)|R.ckzahlung)", jointAccount);
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^(Wertpapierabrechnung (Kauf|Bezug|Verkauf|Verk. Teil\\-\\/Bezugsr\\.)|R.ckzahlung).*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "Verkauf" change from BUY to SELL
                .section("type").optional()
                .match("^(Wertpapierabrechnung )?(?<type>(Kauf|Bezug|Verkauf|Verk. Teil\\-\\/Bezugsr\\.)|R.ckzahlung).*$")
                .assign((t, v) -> {                    
                    if (v.get("type").equals("Verkauf")
                            || v.get("type").equals("Verk. Teil-/Bezugsr.")
                            || v.get("type").equals("Rückzahlung"))
                    {
                        t.setType(PortfolioTransaction.Type.SELL);
                    }
                })

                // ISIN (WKN) DE0002635307 (263530)
                // Wertpapierbezeichnung iSh.STOXX Europe 600 U.ETF DE
                // Inhaber-Anteile
                // Kurswert EUR 4.997,22
                .section("isin", "wkn", "name", "name1", "currency")
                .match("^ISIN \\(WKN\\) (?<isin>[\\w]{12}) \\((?<wkn>.*)\\)$")
                .match("^Wertpapierbezeichnung (?<name>.*)$")
                .match("^(?<name1>.*)$")
                .match("^Kurswert (?<currency>[\\w]{3}) [\\.,\\d]+$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Nominale"))
                        v.put("name", v.get("name") + " " + v.get("name1"));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Nominale Stück 14,00
                // Nominale 11,00 Stück
                .section("shares")
                .match("^Nominale( St.ck)? (?<shares>[\\.,\\d]+)( St.ck)?$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // Ausführungstag / -zeit 17.11.2015 um 16:17:32 Uhr
                // Schlusstag / -zeit 20.03.2012 um 19:35:40 Uhr
                .section("time").optional()
                .match("^(Ausf.hrungstag|Schlusstag) \\/ \\-zeit [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} .* (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}) .*$")
                .assign((t, v) -> type.getCurrentContext().put("time", v.get("time")))
                
                // Ausführungstag 15.12.2015
                // Schlusstag / -zeit 20.03.2012 um 19:35:40 Uhr
                // Fälligkeit 25.05.2017
                .section("date").multipleTimes()
                .match("^(Ausf.hrungstag|Schlusstag|F.lligkeit)( \\/ -zeit)? (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})( .*)?$")
                .assign((t, v) -> {
                    if (type.getCurrentContext().get("time") != null)
                        t.setDate(asDate(v.get("date"), type.getCurrentContext().get("time")));
                    else
                        t.setDate(asDate(v.get("date")));
                })

                // Endbetrag zu Ihren Lasten EUR 533,39
                // Endbetrag zu Ihren Gunsten EUR 1.887,64
                // Endbetrag EUR 256,66
                .section("currency", "amount")
                .match("^Endbetrag( zu Ihren (Lasten|Gunsten))? (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // Kurswert USD 1.503,75
                // umger. zum Devisenkurs EUR 1.311,99 (USD = 1,146163)
                // Endbetrag zu Ihren Lasten EUR 1.335,07
                .section("fxCurrency", "fxGross", "currency", "baseCurrency", "gross", "termCurrency", "exchangeRate").optional()
                .match("^Kurswert (?<fxCurrency>[\\w]{3}) (?<fxGross>[\\.,\\d]+)$")
                .match("^.* Devisenkurs (?<baseCurrency>[\\w]{3}) (?<gross>[\\.,\\d]+) \\((?<termCurrency>[\\w]{3}) = (?<exchangeRate>[\\.,\\d]+)\\)$")
                .match("^Endbetrag( zu Ihren (Lasten|Gunsten))? (?<currency>[\\w]{3}) [\\.,\\d]+$")
                .assign((t, v) -> {
                    type.getCurrentContext().putType(asExchangeRate(v));

                    Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("gross")));
                    Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));

                    checkAndSetGrossUnit(gross, fxGross, t, type);
                })

                // Diese Order wurde mit folgendem Limit / -typ erteilt: 38,10 EUR
                // Diese Order wurde mit folgendem Limit / -typ erteilt: 57,00 EUR / Dynamisches Stop / Abstand 6,661 EUR
                .section("note1", "note2").optional()
                .match("^Diese Order wurde mit folgendem (?<note1>Limit) .*: (?<note2>[\\.,\\d]+ [\\w]{3})( .*)?$")
                .assign((t, v) -> t.setNote(trim(v.get("note1")) + ": " + trim(v.get("note2"))))

                // Rückzahlung
                .section("note").optional()
                .match("^(?<note>R.ckzahlung)$")
                .assign((t, v) -> t.setNote(trim(v.get("note"))))

                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("(Dividendengutschrift|Ertragsgutschrift|Zinsgutschrift)", jointAccount);
        this.addDocumentTyp(type);

        Block block = new Block("^(Dividendengutschrift|Ertragsgutschrift|Zinsgutschrift).*$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.DIVIDENDS);
            return entry;
        });

        pdfTransaction
                // ISIN (WKN) US5801351017 (856958)
                // Wertpapierbezeichnung McDonald's Corp.
                // Registered Shares DL-,01
                // Zins-/Dividendensatz 0,94 USD
                .section("isin", "wkn", "name", "name1", "currency").optional()
                .match("^ISIN \\(WKN\\) (?<isin>[\\w]{12}) \\((?<wkn>.*)\\)$")
                .match("^Wertpapierbezeichnung (?<name>.*)$")
                .match("^(?<name1>.*)$")
                .match("^(Zins\\-\\/Dividendensatz|(Ertragsaussch.ttung|Vorabpauschale) per St.ck) [\\.,\\d]+ (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Nominale"))
                        v.put("name", v.get("name") + " " + v.get("name1"));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                // ISIN (WKN) DE000A1PGUT9 (A1PGUT)
                // Wertpapierbezeichnung 7,25000% posterXXL AG
                // Inh.-Schv.v.2012(2015/2017)
                // Nominale 1.000,00 EUR
                .section("isin", "wkn", "name", "name1", "currency").optional()
                .match("^ISIN \\(WKN\\) (?<isin>[\\w]{12}) \\((?<wkn>.*)\\)$")
                .match("^Wertpapierbezeichnung (?<name>.*)$")
                .match("^(?<name1>.*)$")
                .match("^Nominale [\\.,\\d]+ (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Nominale"))
                        v.put("name", v.get("name") + " " + v.get("name1"));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Nominale 66,00 Stück
                // Nominale 1.000,00 EUR
                .section("shares", "notation")
                .match("^Nominale (?<shares>[\\.,\\d]+) (?<notation>(St.ck|[\\w]{3}))$")
                .assign((t, v) -> {
                    // Percentage quotation, workaround for bonds
                    if (v.get("notation") != null && !v.get("notation").equalsIgnoreCase("Stück"))
                        t.setShares((asShares(v.get("shares")) / 100));
                    else
                        t.setShares(asShares(v.get("shares")));
                })

                // Valuta 15.12.2016
                .section("date")
                .match("^Valuta (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // Gesamtbetrag zu Ihren Gunsten EUR 44,01
                .section("amount", "currency").optional()
                .match("^Gesamtbetrag zu Ihren Gunsten (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // If the total amount is negative, then we change the
                // transaction type from DIVIDENDS to TAXES.

                // Gesamtbetrag zu Ihren Lasten EUR - 20,03
                .section("currency", "amount").optional()
                .match("^Gesamtbetrag zu Ihren Lasten (?<currency>[\\w]{3}) \\- (?<amount>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    t.setType(AccountTransaction.Type.TAXES);

                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // Brutto USD 62,04
                // Umg. z. Dev.-Kurs (1,049623) EUR 50,24
                .section("fxCurrency", "fxGross", "exchangeRate", "currency").optional()
                .match("^Brutto (?<fxCurrency>[\\w]{3}) (?<fxGross>[\\.,\\d]+)")
                .match("^Umg\\. z\\. Dev\\.\\-Kurs \\((?<exchangeRate>[\\.,\\d]+)\\) (?<currency>[\\w]{3}) [\\.,\\d]+$")
                .assign((t, v) -> {
                    v.put("baseCurrency", asCurrencyCode(type.getCurrentContext().get("currency")));
                    v.put("termCurrency", asCurrencyCode(v.get("fxCurrency")));

                    PDFExchangeRate rate = asExchangeRate(v);
                    type.getCurrentContext().putType(rate);

                    Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));
                    Money gross = rate.convert(asCurrencyCode(v.get("currency")), fxGross);

                    checkAndSetGrossUnit(gross, fxGross, t, type);
                })

                .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);

        block.set(pdfTransaction);
    }

    private void addAdvanceTaxTransaction()
    {
        DocumentType type = new DocumentType("Vorabpauschale");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^Vorabpauschale$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                .subject(() -> {
                    AccountTransaction t = new AccountTransaction();
                    t.setType(AccountTransaction.Type.TAXES);
                    return t;
                })

                // ISIN (WKN) IE00BKPT2S34 (A2P1KU)
                // Wertpapierbezeichnung iShsIII-Gl.Infl.L.Gov.Bd U.ETF
                // Reg. Shs HGD EUR Acc. oN
                // Nominale 378,00 Stück
                // Vorabpauschale per Stück 0,00245279 EUR
                .section("isin", "wkn", "name", "name1", "currency")
                .match("^ISIN \\(WKN\\) (?<isin>[\\w]{12}) \\((?<wkn>.*)\\)$")
                .match("^Wertpapierbezeichnung (?<name>.*)$")
                .match("^(?<name1>.*)$")
                .match("^Vorabpauschale per St.ck [\\.,\\d]+ (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Nominale"))
                        v.put("name", v.get("name") + " " + v.get("name1"));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Nominale 378,00 Stück
                .section("shares")
                .match("^Nominale (?<shares>[\\.,\\d]+) .*")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // Valuta 04.01.2021
                .section("date")
                .match("^Valuta (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // Gesamtbetrag zu Ihren Lasten EUR - 0,16
                .section("currency", "sign", "amount")
                .match("^Gesamtbetrag zu Ihren Lasten (?<currency>[\\w]{3}) (?<sign>[\\-\\s]+)?(?<amount>[.,\\d]+)$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));

                    // Is sign --> "-" change from TAXES to TAX_REFUND
                    if ("".equals(trim(v.get("sign"))))
                        t.setType(AccountTransaction.Type.TAX_REFUND);
                })

                .wrap(t -> {
                    if (t.getCurrencyCode() != null && t.getAmount() != 0)
                        return new TransactionItem(t);
                    return null;
                });
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Kapitalertragsteuer (Account)
                // Kapitalertragsteuer 25,00 % EUR 18,32
                // Kapitalertragsteuer 25,00% EUR 5,91
                .section("currency", "tax").optional()
                .match("^Kapitalertragsteuer [\\.,\\d]+([\\s]+)?% (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    if (!Boolean.parseBoolean(type.getCurrentContext().get(isJointAccount)))
                        processTaxEntries(t, v, type);
                })

                // Kapitalerstragsteuer (Joint Account)
                // KapSt anteilig 50,00 % 24,45% EUR 79,46
                // KapSt anteilig 50,00 % 24,45 % EUR 79,46
                .section("currency1", "tax1", "currency2", "tax2").optional()
                .match("^KapSt anteilig [\\.,\\d]+([\\s]+)?% [\\.,\\d]+([\\s]+)?% (?<currency1>[\\w]{3}) (?<tax1>[\\.,\\d]+)$")
                .match("^KapSt anteilig [\\.,\\d]+([\\s]+)?% [\\.,\\d]+([\\s]+)?% (?<currency2>[\\w]{3}) (?<tax2>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    if (Boolean.parseBoolean(type.getCurrentContext().get(isJointAccount)))
                    {
                        // Account 1
                        v.put("currency", v.get("currency1"));
                        v.put("tax", v.get("tax1"));
                        processTaxEntries(t, v, type);

                        // Account 2
                        v.put("currency", v.get("currency2"));
                        v.put("tax", v.get("tax2"));
                        processTaxEntries(t, v, type);
                    }
                })

                // Solidaritätszuschlag (Account)
                // Solidarit‰tszuschlag 5,50 % EUR 1,00
                // Solidaritätszuschlag 5,50% EUR 0,32
                .section("currency", "tax").optional()
                .match("^Solidarit.tszuschlag [\\.,\\d]+([\\s]+)?% (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    if (!Boolean.parseBoolean(type.getCurrentContext().get(isJointAccount)))
                        processTaxEntries(t, v, type);
                })

                // Solitaritätszuschlag (Joint Account)
                // Solidaritätszuschlag 5,50% EUR 4,37
                // Solidaritätszuschlag 5,50 % EUR 4,37
                .section("currency1", "tax1", "currency2", "tax2").optional()
                .match("^Solidarit.tszuschlag [\\.,\\d]+([\\s]+)?% (?<currency1>[\\w]{3}) (?<tax1>[\\.,\\d]+)$")
                .match("^Solidarit.tszuschlag [\\.,\\d]+([\\s]+)?% (?<currency2>[\\w]{3}) (?<tax2>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    if (Boolean.parseBoolean(type.getCurrentContext().get(isJointAccount)))
                    {
                        // Account 1
                        v.put("currency", v.get("currency1"));
                        v.put("tax", v.get("tax1"));
                        processTaxEntries(t, v, type);

                        // Account 2
                        v.put("currency", v.get("currency2"));
                        v.put("tax", v.get("tax2"));
                        processTaxEntries(t, v, type);
                    }
                })

                // Kirchensteuer (Account)
                // Kirchensteuer 5,50 % EUR 1,00      
                // Kirchensteuer 5,50% EUR 0,32
                .section("currency", "tax").optional()
                .match("^Kirchensteuer [\\.,\\d]+([\\s]+)?% (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    if (!Boolean.parseBoolean(type.getCurrentContext().get(isJointAccount)))
                        processTaxEntries(t, v, type);
                })

                // Kirchensteuer (Joint Account)
                // Kirchensteuer 9,00 % EUR 7,15
                // Kirchensteuer 9,00% EUR 7,15
                .section("currency1", "tax1", "currency2", "tax2").optional()
                .match("^Kirchensteuer [\\.,\\d]+([\\s]+)?% (?<currency1>[\\w]{3}) (?<tax1>[\\.,\\d]+)$")
                .match("^Kirchensteuer [\\.,\\d]+([\\s]+)?% (?<currency2>[\\w]{3}) (?<tax2>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    if (Boolean.parseBoolean(type.getCurrentContext().get(isJointAccount)))
                    {
                        // Account 1
                        v.put("currency", v.get("currency1"));
                        v.put("tax", v.get("tax1"));
                        processTaxEntries(t, v, type);

                        // Account 2
                        v.put("currency", v.get("currency2"));
                        v.put("tax", v.get("tax2"));
                        processTaxEntries(t, v, type);
                    }
                })

                // QuSt 15,00 % (EUR 8,87) USD 9,31
                .section("currency", "withHoldingTax").optional()
                .match("^QuSt [\\.,\\d]+([\\s]+)?% \\((?<currency>[\\w]{3}) (?<withHoldingTax>[\\.,\\d]+)\\) [\\w]{3} [\\.,\\d]+$")
                .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                // QuSt 30,00 % EUR 16,50
                .section("currency", "withHoldingTax").optional()
                .match("^QuSt [\\.,\\d]+([\\s]+)?% (?<currency>[\\w]{3}) (?<withHoldingTax>[\\.,\\d]+)$")
                .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Handelsplatzgebühr EUR 2,50
                .section("currency", "fee").optional()
                .match("^Handelsplatzgeb.hr (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Provision EUR 9,90
                .section("currency", "fee").optional()
                .match("^Provision (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Handelsentgelt EUR 3,00
                .section("currency", "fee").optional()
                .match("^Handelsentgelt (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Kurswert EUR 52,63
                // Rabatt EUR - 2,63
                // Der regul.re Ausgabeaufschlag von 5,263% ist im Kurs enthalten.
                .section("currency", "amount", "discountCurrency", "discount", "percentageFee").optional()
                .match("^Kurswert (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$")
                .match("^Rabatt (?<discountCurrency>[\\w]{3}) \\- (?<discount>[\\.,\\d]+)$")
                .match("^Der regul.re Ausgabeaufschlag von (?<percentageFee>[\\.,\\d]+)% .*$")
                .assign((t, v) -> {
                    BigDecimal percentageFee = asBigDecimal(v.get("percentageFee"));
                    BigDecimal amount = asBigDecimal(v.get("amount"));
                    Money discount = Money.of(asCurrencyCode(v.get("discountCurrency")), asAmount(v.get("discount")));

                    if (percentageFee.compareTo(BigDecimal.ZERO) != 0 && discount.isPositive())
                    {
                        // feeAmount = (amount / (1 + percentageFee / 100)) * (percentageFee / 100)
                        BigDecimal fxFee = amount
                                        .divide(percentageFee.divide(BigDecimal.valueOf(100))
                                                        .add(BigDecimal.ONE), Values.MC)
                                        .multiply(percentageFee, Values.MC);

                        Money fee = Money.of(asCurrencyCode(v.get("currency")),
                                        fxFee.setScale(0, Values.MC.getRoundingMode()).longValue());

                        // fee = fee - discount
                        fee = fee.subtract(discount);

                        checkAndSetFee(fee, t, type);
                    }
                });
    }
}