package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.trim;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

@SuppressWarnings("nls")
public class INGDiBaPDFExtractor extends AbstractPDFExtractor
{
    private static final String IS_JOINT_ACCOUNT = "isjointaccount"; //$NON-NLS-1$

    BiConsumer<Map<String, String>, String[]> isJointAccount = (context, lines) -> {
        Pattern pJointAccount = Pattern.compile("KapSt anteilig 50,00 %.*"); //$NON-NLS-1$
        Boolean bJointAccount = false;

        for (String line : lines)
        {
            Matcher m = pJointAccount.matcher(line);
            if (m.matches())
            {
                context.put(IS_JOINT_ACCOUNT, Boolean.TRUE.toString());
                bJointAccount = true;
                break;
            }
        }

        if (!bJointAccount)
            context.put(IS_JOINT_ACCOUNT, Boolean.FALSE.toString());

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
        DocumentType type = new DocumentType("(Wertpapierabrechnung (Kauf|Bezug|Verkauf|Verk. Teil-/Bezugsr.)|Rückzahlung)", isJointAccount);
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^(Wertpapierabrechnung (Kauf|Bezug|Verkauf|Verk. Teil-\\/Bezugsr\\.)|R.ckzahlung).*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "Verkauf" change from BUY to SELL
                .section("type").optional()
                .match("^(Wertpapierabrechnung )?(?<type>(Kauf|Bezug|Verkauf|Verk. Teil-\\/Bezugsr\\.)|R.ckzahlung).*$")
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
                .match("^(Ausf.hrungstag \\/ \\-zeit|Ausf.hrungstag|Schlusstag \\/ \\-zeit|Schlusstag|F.lligkeit) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} .* (?<time>\\d+:\\d+:\\d+) .*$")
                .assign((t, v) -> type.getCurrentContext().put("time", v.get("time")))
                
                // Ausführungstag 15.12.2015
                // Schlusstag 01.02.2012
                // Fälligkeit 25.05.2017
                .section("date").multipleTimes()
                .match("^(Ausf.hrungstag \\/ -zeit|Ausf.hrungstag|Schlusstag \\/ -zeit|Schlusstag|F.lligkeit) (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})( .*)?$")
                .assign((t, v) -> {
                    if (type.getCurrentContext().get("time") != null)
                        t.setDate(asDate(v.get("date"), type.getCurrentContext().get("time")));
                    else
                        t.setDate(asDate(v.get("date")));
                })

                // Endbetrag zu Ihren Lasten EUR 533,39
                // Endbetrag zu Ihren Gunsten EUR 1.887,64
                // Endbetrag EUR 256,66
                .section("amount", "currency")
                .match("^Endbetrag( zu Ihren (Lasten|Gunsten))? (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // Kurswert USD 1.503,75
                // umger. zum Devisenkurs EUR 1.311,99 (USD = 1,146163)
                .section("currency", "amount", "fxcurrency", "fxamount", "exchangeRate").optional()
                .match("^Kurswert (?<fxcurrency>[\\w]{3}) (?<fxamount>[\\.,\\d]+)$")
                .match("^.* Devisenkurs (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+) \\([\\w]{3} = (?<exchangeRate>[.,\\d]+)\\)$")
                .assign((t, v) -> {                   
                    // read the forex currency, exchange rate and gross
                    // amount in forex currency
                    String forex = asCurrencyCode(v.get("fxcurrency"));
                    if (t.getPortfolioTransaction().getSecurity().getCurrencyCode().equals(forex))
                    {
                        BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                        BigDecimal reverseRate = BigDecimal.ONE.divide(exchangeRate, 10,
                                        RoundingMode.HALF_DOWN);

                        // gross given in forex currency
                        long fxAmount = asAmount(v.get("fxamount"));
                        long amount = reverseRate.multiply(BigDecimal.valueOf(fxAmount))
                                        .setScale(0, RoundingMode.HALF_DOWN).longValue();

                        Unit grossValue = new Unit(Unit.Type.GROSS_VALUE,
                                        Money.of(t.getPortfolioTransaction().getCurrencyCode(), amount),
                                        Money.of(forex, fxAmount), reverseRate);

                        t.getPortfolioTransaction().addUnit(grossValue);
                    }
                })

                // umger. zum Devisenkurs EUR 1.311,99 (USD = 1,146163)
                .section("exchangeRate").optional()
                .match("^.* Devisenkurs [\\w]{3} [\\.,\\d]+ \\([\\w]{3} = (?<exchangeRate>[.,\\d]+)\\)$")
                .assign((t, v) -> {
                    BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                    type.getCurrentContext().put("exchangeRate", exchangeRate.toPlainString());
                })

                // Diese Order wurde mit folgendem Limit / -typ erteilt: 38,10 EUR
                // Diese Order wurde mit folgendem Limit / -typ erteilt: 57,00 EUR / Dynamisches Stop / Abstand 6,661 EUR
                .section("note1", "note2").optional()
                .match("^Diese Order wurde mit folgendem (?<note1>Limit) .*: (?<note2>[\\.,\\d]+ [\\w]{3})( .*)?$")
                .assign((t, v) -> {
                    t.setNote(trim(v.get("note1")) + ": " + trim(v.get("note2")));   
                })

                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("(Dividendengutschrift|Ertragsgutschrift|Zinsgutschrift)", isJointAccount);
        this.addDocumentTyp(type);

        Block block = new Block("^(Dividendengutschrift|Ertragsgutschrift|Zinsgutschrift)(.*)?$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>()
            .subject(() -> {
                AccountTransaction entry = new AccountTransaction();
                entry.setType(AccountTransaction.Type.DIVIDENDS);
                return entry;
            });

        pdfTransaction
                // ISIN (WKN) US5801351017 (856958)
                // Wertpapierbezeichnung McDonald's Corp.
                // Registered Shares DL-,01
                // Zins-/Dividendensatz 0,94 USD
                .section("wkn", "isin", "name", "name1", "currency").optional()
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
                .section("wkn", "isin", "name", "name1", "currency").optional()
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
                    if (v.get("notation") != null && !v.get("notation").equalsIgnoreCase("Stück"))
                    {
                        // Prozent-Notierung, Workaround..
                        t.setShares((asShares(v.get("shares")) / 100));
                    }
                    else
                    {
                        t.setShares(asShares(v.get("shares")));
                    }
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

                /***
                 * Is the total amount negative, 
                 * then change from DIVIDENDS to TAXES
                 */
                //Gesamtbetrag zu Ihren Lasten EUR - 20,03
                .section("amount", "currency").optional()
                .match("^Gesamtbetrag zu Ihren Lasten (?<currency>[\\w]{3}) \\- (?<amount>[\\.,\\d]+)$")
                .assign((t, v) -> {                    
                    t.setType(AccountTransaction.Type.TAXES);

                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // Brutto USD 62,04
                // Umg. z. Dev.-Kurs (1,049623) EUR 50,24
                .section("fxAmount", "fxCurrency", "currency", "exchangeRate").optional()
                .match("^Brutto (?<fxCurrency>[\\w]{3}) (?<fxAmount>[\\.,\\d]+)")
                .match("^Umg\\. z\\. Dev\\.\\-Kurs \\((?<exchangeRate>[\\.,\\d]+)\\) (?<currency>[\\w]{3}) [\\.,\\d]+$")
                .assign((t, v) -> {
                    BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                    type.getCurrentContext().put("exchangeRate", exchangeRate.toPlainString());

                    if (!t.getCurrencyCode().equals(t.getSecurity().getCurrencyCode()))
                    {
                        BigDecimal inverseRate = BigDecimal.ONE.divide(exchangeRate, 10,
                                        RoundingMode.HALF_DOWN);

                        Unit grossValue;
                        Money fxAmount = Money.of(asCurrencyCode(v.get("fxCurrency")),
                                        asAmount(v.get("fxAmount")));
                        Money amount = Money.of(asCurrencyCode(v.get("currency")),
                                        BigDecimal.valueOf(fxAmount.getAmount()).multiply(inverseRate)
                                                        .setScale(0, RoundingMode.HALF_UP).longValue());
                        grossValue = new Unit(Unit.Type.GROSS_VALUE, amount, fxAmount, inverseRate);
                        t.addUnit(grossValue);
                    }
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
                .section("wkn", "isin", "name", "name1", "currency")
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
                .section("currency", "tax", "sign").optional()
                .match("^Gesamtbetrag zu Ihren Lasten (?<currency>[\\w]{3}) (?<sign>[\\-\\s]+)?(?<tax>[.,\\d]+)$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("tax")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));

                    String sign = trim(v.get("sign"));
                    if ("".equals(sign))
                    {
                        // change type for withdrawals
                        t.setType(AccountTransaction.Type.TAX_REFUND);
                    }
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
                .section("tax", "currency").optional()
                .match("^Kapitalertragsteuer [\\.,\\d]+([\\s]+)?% (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    if (!Boolean.parseBoolean(type.getCurrentContext().get(IS_JOINT_ACCOUNT)))
                    {
                        processTaxEntries(t, v, type);
                    }
                })

                // Kapitalerstragsteuer (Joint Account)
                // KapSt anteilig 50,00 % 24,45% EUR 79,46
                // KapSt anteilig 50,00 % 24,45 % EUR 79,46
                .section("tax1", "currency1", "tax2", "currency2").optional()
                .match("^KapSt anteilig [\\.,\\d]+([\\s]+)?% [\\.,\\d]+([\\s]+)?% (?<currency1>[\\w]{3}) (?<tax1>[\\.,\\d]+)$")
                .match("^KapSt anteilig [\\.,\\d]+([\\s]+)?% [\\.,\\d]+([\\s]+)?% (?<currency2>[\\w]{3}) (?<tax2>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    if (Boolean.parseBoolean(type.getCurrentContext().get(IS_JOINT_ACCOUNT)))
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
                .section("tax", "currency").optional()
                .match("^Solidarit.tszuschlag [\\.,\\d]+([\\s]+)?% (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    if (!Boolean.parseBoolean(type.getCurrentContext().get(IS_JOINT_ACCOUNT)))
                    {
                        processTaxEntries(t, v, type);
                    }
                })

                // Solitaritätszuschlag (Joint Account)
                // Solidaritätszuschlag 5,50% EUR 4,37
                // Solidaritätszuschlag 5,50 % EUR 4,37
                .section("tax1", "currency1", "tax2", "currency2").optional()
                .match("^Solidarit.tszuschlag [\\.,\\d]+([\\s]+)?% (?<currency1>[\\w]{3}) (?<tax1>[\\.,\\d]+)$")
                .match("^Solidarit.tszuschlag [\\.,\\d]+([\\s]+)?% (?<currency2>[\\w]{3}) (?<tax2>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    if (Boolean.parseBoolean(type.getCurrentContext().get(IS_JOINT_ACCOUNT)))
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
                .section("tax", "currency").optional()
                .match("^Kirchensteuer [\\.,\\d]+([\\s]+)?% (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    if (!Boolean.parseBoolean(type.getCurrentContext().get(IS_JOINT_ACCOUNT)))
                    {
                        processTaxEntries(t, v, type);
                    }
                })

                // Kirchensteuer (Joint Account)
                // Kirchensteuer 9,00 % EUR 7,15
                // Kirchensteuer 9,00% EUR 7,15
                .section("tax1", "currency1", "tax2", "currency2").optional()
                .match("^Kirchensteuer [\\.,\\d]+([\\s]+)?% (?<currency1>[\\w]{3}) (?<tax1>[\\.,\\d]+)$")
                .match("^Kirchensteuer [\\.,\\d]+([\\s]+)?% (?<currency2>[\\w]{3}) (?<tax2>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    if (Boolean.parseBoolean(type.getCurrentContext().get(IS_JOINT_ACCOUNT)))
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
                .section("withHoldingTax", "currency").optional()
                .match("^QuSt [\\.,\\d]+([\\s]+)?% \\((?<currency>[\\w]{3}) (?<withHoldingTax>[\\.,\\d]+)\\) [\\w]{3} [\\.,\\d]+$")
                .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                // QuSt 30,00 % EUR 16,50
                .section("withHoldingTax", "currency").optional()
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
                // Der regul�re Ausgabeaufschlag von 5,263% ist im Kurs enthalten.
                .section("amountFx", "currency", "feeFy", "feeFx").optional()
                .match("^Kurswert (?<currency>[\\w]{3}) (?<amountFx>[\\.,\\d]+)$")
                .match("^Rabatt [\\w]{3} \\- (?<feeFy>[\\.,\\d]+)$")
                .match("^Der regul.re Ausgabeaufschlag von (?<feeFx>[\\.,\\d]+)% .*$")
                .assign((t, v) -> {
                    // Calculation of the fee discount
                    double amountFx = Double.parseDouble(v.get("amountFx").replace(".", "").replace(",", "."));
                    double feeFy = Double.parseDouble(v.get("feeFy").replace(".", "").replace(",", "."));
                    double feeFx = Double.parseDouble(v.get("feeFx").replace(".", "").replace(",", "."));
                    String fee = Double.toString(amountFx / (1 + feeFx / 100) * (feeFx / 100) - feeFy).replace(".", ",");
                    v.put("fee", fee);

                    processFeeEntries(t, v, type);
                });
    }
}