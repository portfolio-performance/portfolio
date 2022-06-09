package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.pdf.PDFExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.trim;

import java.math.BigDecimal;
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
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class BaaderBankPDFExtractor extends AbstractPDFExtractor
{
    public BaaderBankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Baader Bank"); //$NON-NLS-1$

        addBuySellTransaction();
        addDividendeTransaction();
        addAdvanceTaxTransaction();
        addTaxAdjustmentTransaction();
        addDepotStatementTransaction();
        addFeesAssetManagerTransaction();
        addDeliveryInOutBoundTransaction();
        addTransferOutTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Baader Bank AG / Scalable Capital Vermögensverwaltung GmbH"; //$NON-NLS-1$
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("((Wertpapierabrechnung|Transaction Statement): (Kauf|Verkauf|Purchase|Sale)|Zeichnung|Spitzenregulierung( .*)?)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^(Referenz\\-Nr|Reference No)\\.: .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "Verkauf" change from BUY to SELL
                .section("type").optional()
                .match("^(Wertpapierabrechnung|Transaction Statement): (?<type>(Kauf|Verkauf|Purchase|Sale)).*$")
                .assign((t, v) -> {
                    if (v.get("type").equals("Verkauf") || v.get("type").equals("Sale"))
                        t.setType(PortfolioTransaction.Type.SELL);
                })

                // Is type --> "Spitzenregulierung" change from BUY to SELL
                .section("type").optional()
                .match("^(?<type>Spitzenregulierung)( .*)?$")
                .assign((t, v) -> {
                    if (v.get("type").equals("Spitzenregulierung"))
                        t.setType(PortfolioTransaction.Type.SELL);
                })

                // Nominale ISIN: IE0032895942 WKN: 911950 Kurs 
                // STK 2 iShs DL Corp Bond UCITS ETF EUR 104,37
                // Registered Shares o.N.
                // Kurswert EUR 208,74
                .section("isin", "wkn", "name", "nameContinued", "currency").optional()
                .match("^(Nominale|Quantity) ISIN: (?<isin>[\\w]{12}) WKN: (?<wkn>.*) (Kurs|Bezugspreis|Barabfindung|Price).*$")
                .match("^(STK|Units) [\\.,\\d]+ (?<name>.*) (?<currency>[\\w]{3}) .*$")
                .match("^(?<nameContinued>.*)$")
                .assign((t, v) -> {
                    if (v.get("nameContinued").endsWith("p.STK"))
                        v.put("nameContinued", v.get("nameContinued").replace("p.STK", ""));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                // STK 2 iShs DL Corp Bond UCITS ETF EUR 104,37
                // Units 2.734 iShsIII-Core MSCI World U.ETF EUR 73.128
                .section("local", "shares")
                .match("^(?<local>(STK|Units)) (?<shares>[\\.,\\d]+) .*$")
                .assign((t, v) -> {
                    if (v.get("local").equals("Units"))
                        t.setShares(asShares(v.get("shares"), "en", "US"));
                    else
                        t.setShares(asShares(v.get("shares")));
                })

                .oneOf(
                                // Handelsdatum Handelsuhrzeit
                                // 20.03.2017 15:31:10:00
                                section -> section
                                        .attributes("date", "time")
                                        .find("Handelsdatum Handelsuhrzeit")
                                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}).*")
                                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))
                                ,
                                // Handels- Handels- 
                                // STK   70 EUR 14,045 GETTEX - MM Munich 24.02.2021 14:49:46:04
                                section -> section
                                        .attributes("date", "time")
                                        .find("Handels- Handels- ")
                                        .match("^.* (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}).*$")
                                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))
                                ,
                                // Einbuchung in Depot yyyyyyyyyy per 09.03.2021
                                section -> section
                                        .attributes("date")
                                        .match("^Einbuchung in Depot .* per (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))
                                ,
                                // Ausbuchung aus Depot 11 per 05.05.2020
                                section -> section
                                        .attributes("date")
                                        .match("^Ausbuchung aus Depot .* per (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))
                                ,
                                // Trade Date Trade Time
                                // 2022-02-28 13:48:52:44
                                section -> section
                                        .attributes("date", "time")
                                        .find("Trade Date Trade Time")
                                        .match("^(?<date>[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}).*$")
                                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))
                                ,
                                // Order Date: 2022-04-29 Execution Venue: GETTEX - MM Munich
                                // Order Time: 09:32:39:19
                                section -> section
                                        .attributes("date", "time")
                                        .match("^Order Date: (?<date>[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}) .*$")
                                        .match("^Order Time: (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}):[\\d]{2}$")
                                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))
                        )

                .oneOf(
                                // Zu Lasten Konto 12345004 Valuta: 22.03.2017 EUR 208,95
                                // Zu Gunsten Konto 12345004 Valuta: 12.05.2017 EUR 75,92
                                section -> section
                                        .attributes("currency", "amount")
                                        .match("^Zu (Gunsten|Lasten) Konto .* (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$")
                                        .assign((t, v) -> {
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                            t.setAmount(asAmount(v.get("amount")));
                                        })
                                ,
                                // Amount debited to account 1960017000 Value: 2022-03-02 EUR 199.93
                                section -> section
                                        .attributes("currency", "amount")
                                        .match("^Amount debited to account [\\d]+ Value: [\\d]{4}\\-[\\d]{2}\\-[\\d]{2} (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$")
                                        .assign((t, v) -> {
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                            t.setAmount(asAmount(v.get("amount")));
                                        })
                        )

                // Kurswert Umrechnungskurs CAD/EUR: 1,4595 EUR 8,85
                .section("termCurrency", "baseCurrency", "exchangeRate", "currency", "gross").optional()
                .match("^Kurswert Umrechnungskurs (?<termCurrency>[\\w]{3})\\/(?<baseCurrency>[\\w]{3}): (?<exchangeRate>[\\.,\\d]+) (?<currency>[\\w]{3}) (?<gross>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    PDFExchangeRate rate = asExchangeRate(v);
                    type.getCurrentContext().putType(asExchangeRate(v));
                    
                    Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("gross")));
                    Money fxGross = rate.convert(asCurrencyCode(v.get("termCurrency")), gross);

                    checkAndSetGrossUnit(gross, fxGross, t, type);
                })

                // Verhältnis: 1 : 1 
                .section("note").optional()
                .match("^(?<note>Verh.ltnis: .*)$")
                .assign((t, v) -> t.setNote(trim(v.get("note"))))

                // Spitzenregulierung KOPIE
                .section("note").optional()
                .match("^(?<note>Spitzenregulierung)( .*)?$")
                .assign((t, v) -> t.setNote(trim(v.get("note"))))

                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("(Fondsaussch.ttung"
                        + "|Ertragsthesaurierung"
                        + "|Dividendenabrechnung"
                        + "|Aussch.ttung aus"
                        + "|Wahldividende"
                        + "|Fund Distribution)");
        this.addDocumentTyp(type);

        Block block = new Block("^Ex\\-(Tag|Date): .*$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.DIVIDENDS);
            return entry;
        });

        pdfTransaction
                // If we have a positive amount and a gross reinvestment,
                // there is a tax refund.
                // If the amount is negative, then it is taxes.
                .section("type", "sign").optional()
                .match("^Nominale ISIN: .* (?<type>(Aussch.ttung|Thesaurierung brutto))$")
                .match("^Zu (?<sign>(Gunsten|Lasten)) Konto [\\d]+ Valuta: [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\w]{3} [\\.,\\d]+$")
                .assign((t, v) -> {
                    if (v.get("type").equals("Thesaurierung brutto") && v.get("sign").equals("Gunsten"))
                        t.setType(AccountTransaction.Type.TAX_REFUND);

                    if (v.get("type").equals("Thesaurierung brutto") && v.get("sign").equals("Lasten"))
                        t.setType(AccountTransaction.Type.TAXES);
                })

                // Nominale ISIN: FR0000130577 WKN: 859386 Ausschüttung
                // STK 57 Publicis Groupe S.A. EUR 2,00 p.STK
                // Zahlungszeitraum: 17.06.2021 - 30.06.2021 
                .section("isin", "wkn", "name", "name1", "currency")
                .match("^(Nominale|Quantity) ISIN: (?<isin>[\\w]{12}) WKN: (?<wkn>[\\w]{6}) .*$")
                .match("^(STK|Units) [\\.,\\d]+ (?<name>.*) (?<currency>[\\w]{3}) .*$")
                .match("^(?<name1>.*)$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Zahlungszeitraum") && !v.get("name1").startsWith("Payment"))
                        v.put("name", v.get("name") + " " + v.get("name1"));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                // STK 57 Publicis Groupe S.A. EUR 2,00 p.STK
                .section("local", "shares")
                .match("^(?<local>(STK|Units)) (?<shares>[\\.,\\d]+) .*$")
                .assign((t, v) -> {
                    if (v.get("local").equals("Units"))
                        t.setShares(asShares(v.get("shares"), "en", "US"));
                    else
                        t.setShares(asShares(v.get("shares")));
                })

                .oneOf(
                                // Zu Gunsten Konto 1111111111 Valuta: 06.07.2021 EUR 68,22
                                section -> section
                                        .attributes("date")
                                        .match("^Zu Gunsten Konto [\\d]+ Valuta: (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\w]{3} [\\.,\\d]+$")
                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))
                                ,
                                // Amount credited to account 1209625007 Value: 2022-02-25 EUR 0.20
                                section -> section
                                        .attributes("date")
                                        .match("^Amount credited to account [\\d]+ Value: (?<date>[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}) [\\w]{3} [\\.,\\d]+$")
                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))
                        )

                .oneOf(
                                // Zu Gunsten Konto 1111111111 Valuta: 06.07.2021 EUR 68,22
                                section -> section
                                        .attributes("currency", "amount")
                                        .match("^Zu Gunsten Konto [\\d]+ Valuta: [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$")
                                        .assign((t, v) -> {
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                            t.setAmount(asAmount(v.get("amount")));
                                        })
                                ,
                                // Amount credited to account 1209625007 Value: 2022-02-25 EUR 0.20
                                section -> section
                                        .attributes("currency", "amount")
                                        .match("^Amount credited to account [\\d]+ Value: [\\d]{4}\\-[\\d]{2}\\-[\\d]{2} (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$")
                                        .assign((t, v) -> {
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                            t.setAmount(asAmount(v.get("amount")));
                                        })
                        )

                // Umrechnungskurs: EUR/USD 1,1452
                // Bruttobetrag USD 3,94
                // Bruttobetrag EUR 3,44
                .section("baseCurrency", "termCurrency", "exchangeRate", "fxGross", "fxCurrency", "gross", "currency").optional()
                .match("^(Umrechnungskurs|Exchange Rate): (?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3}) (?<exchangeRate>[\\.,\\d]+)$")
                .match("^(Bruttobetrag|Gross Amount) (?<fxCurrency>[\\w]{3}) (?<fxGross>[\\.,\\d]+)$")
                .match("^(Bruttobetrag|Gross Amount) (?<currency>[\\w]{3}) (?<gross>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    type.getCurrentContext().putType(asExchangeRate(v));

                    Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("gross")));
                    Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));

                    checkAndSetGrossUnit(gross, fxGross, t, type);
                })

                .wrap(t -> {
                    // If we have multiple entries in the document, then
                    // the "noTax" flag must be removed.
                    type.getCurrentContext().remove("noTax");

                    return new TransactionItem(t);
                });

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);

        block.set(pdfTransaction);
    }

    private void addAdvanceTaxTransaction()
    {
        DocumentType type = new DocumentType("Vorabpauschale");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.TAXES);
            return entry;
        });

        Block firstRelevantLine = new Block("^.* Portfolio: .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Nominale ISIN: IE00BWBXM385 WKN: A14QBZ
                // STK 112 SPDR S+P US Con.Sta.Sel.S.UETF
                .section("isin", "wkn", "name", "name1")
                .match("^Nominale ISIN: (?<isin>[\\w]{12}) WKN: (?<wkn>.*)$")
                .match("^STK [\\.,\\d]+ (?<name>.*)$")
                .match("^(?<name1>.*)$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Zahlungszeitraum"))
                        v.put("name", v.get("name") + " " + v.get("name1"));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                // STK 112 SPDR S+P US Con.Sta.Sel.S.UETF
                .section("shares")
                .match("^STK (?<shares>[\\.,\\d]+) .*$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // Zu Lasten Konto 1247201005 Valuta: 04.01.2021 EUR 0,04
                .section("date", "currency", "amount")
                .match("^Zu Lasten Konto [\\d]+ Valuta: (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                    t.setDateTime(asDate(v.get("date")));
                })

                // Zahlungszeitraum: 01.01.2020 - 31.12.2020 
                .section("note").optional()
                .match("^(?<note>Zahlungszeitraum: .*)$")
                .assign((t, v) -> t.setNote(trim(v.get("note"))))

                .wrap(TransactionItem::new);
    }

    private void addTaxAdjustmentTransaction()
    {
        DocumentType type = new DocumentType("Steuerausgleichsrechnung");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.TAX_REFUND);
            return entry;
        });

        Block firstRelevantLine = new Block("^.*Seite 1\\/[\\d]$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Unterschleißheim, 22.06.2017
                // 26.06.2020
                .section("date")
                .match("^(.* )?(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // Erstattung EUR 9,01
                .section("currency", "amount")
                .match("^Erstattung (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // wir haben für Sie eine Steuerausgleichsrechnung durchgeführt, die der Optimierung der Steuerbelastung 
                .section("note").optional()
                .match("^.* (?<note>Steuerausgleichsrechnung) .*$")
                .assign((t, v) -> t.setNote(v.get("note")))

                .wrap(TransactionItem::new);
    }

    private void addDepotStatementTransaction()
    {
        final DocumentType type = new DocumentType("(Perioden\\-Kontoauszug|Tageskontoauszug|Periodic Account Statement)", (context, lines) -> {
            Pattern pCurrency = Pattern.compile("(Perioden\\-Kontoauszug|Tageskontoauszug|Periodic Account Statement): (?<currency>[\\w]{3})(\\-Konto| Account)");
            // read the current context here
            for (String line : lines)
            {
                Matcher m = pCurrency.matcher(line);
                if (m.matches())
                    context.put("currency", m.group("currency"));
            }
        });
        this.addDocumentTyp(type);

        Block depositBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (Lastschrift aktiv|Gutschrift) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\.,\\d]+$");
        type.addBlock(depositBlock);
        depositBlock.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction t = new AccountTransaction();
                    t.setType(AccountTransaction.Type.DEPOSIT);
                    return t;
                })

                // 12.04.2018 Lastschrift aktiv 12.04.2018 10.000,00
                .section("note", "date", "amount")
                .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<note>(Lastschrift aktiv|Gutschrift)) (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<amount>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();
                    t.setDateTime(asDate(v.get("date")));
                    t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                    t.setNote(v.get("note"));
                })

                .wrap(t -> {
                    if (t.getCurrencyCode() != null && t.getAmount() != 0)
                        return new TransactionItem(t);
                    return null;
                }));

        Block depositEnglishBlock = new Block("^[\\d]{4}\\-[\\d]{2}\\-[\\d]{2} (Credit SEPA|Direct Debit) [\\d]{4}\\-[\\d]{2}\\-[\\d]{2} [\\.,\\d]+$");
        type.addBlock(depositEnglishBlock);
        depositEnglishBlock.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction t = new AccountTransaction();
                    t.setType(AccountTransaction.Type.DEPOSIT);
                    return t;
                })

                // 2022-02-02 Credit SEPA 2022-02-02 1,000.00
                // 2022-02-03 Direct Debit 2022-02-03 1.00
                .section("note", "date", "amount")
                .match("^[\\d]{4}\\-[\\d]{2}\\-[\\d]{2} (?<note>(Credit SEPA|Direct Debit)) (?<date>[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}) (?<amount>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();
                    t.setDateTime(asDate(v.get("date")));
                    t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                    t.setNote(v.get("note"));
                })

                .wrap(t -> {
                    if (t.getCurrencyCode() != null && t.getAmount() != 0)
                        return new TransactionItem(t);
                    return null;
                }));

        Block removalBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (Lastschrift aktiv|SEPA\\-Ueberweisung) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\.,\\d]+ \\-$");
        type.addBlock(removalBlock);
        removalBlock.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction t = new AccountTransaction();
                    t.setType(AccountTransaction.Type.REMOVAL);
                    return t;
                })

                .section("note", "date", "amount")
                .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<note>(Lastschrift aktiv|SEPA\\-Ueberweisung)) (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<amount>[\\.,\\d]+) \\-$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();
                    t.setDateTime(asDate(v.get("date")));
                    t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                    t.setNote(v.get("note"));
                })

                .wrap(t -> {
                    if (t.getCurrencyCode() != null && t.getAmount() != 0)
                        return new TransactionItem(t);
                    return null;
                }));

        Block feesBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} Transaktionskostenpauschale o\\. MwSt\\. [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\.,\\d]+ \\-$");
        type.addBlock(feesBlock);
        feesBlock.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction t = new AccountTransaction();
                    t.setType(AccountTransaction.Type.FEES);
                    return t;
                })

                .section("note", "date", "amount")
                .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<note>Transaktionskostenpauschale o\\. MwSt\\.) (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<amount>[\\.,\\d]+) \\-$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();
                    t.setDateTime(asDate(v.get("date")));
                    t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                    t.setNote(v.get("note"));
                })

                .wrap(t -> {
                    if (t.getCurrencyCode() != null && t.getAmount() != 0)
                        return new TransactionItem(t);
                    return null;
                }));

        Block feesEnglishBlock = new Block("^[\\d]{4}\\-[\\d]{2}\\-[\\d]{2} Ordergeb.hr [\\d]{4}\\-[\\d]{2}\\-[\\d]{2} [\\.,\\d]+ \\-$");
        type.addBlock(feesEnglishBlock);
        feesEnglishBlock.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction t = new AccountTransaction();
                    t.setType(AccountTransaction.Type.FEES);
                    return t;
                })

                .section("note", "date", "amount")
                .match("^[\\d]{4}\\-[\\d]{2}\\-[\\d]{2} (?<note>Ordergeb.hr) (?<date>[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}) (?<amount>[\\.,\\d]+) \\-$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();
                    t.setDateTime(asDate(v.get("date")));
                    t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                    t.setNote(v.get("note"));
                })

                .wrap(t -> {
                    if (t.getCurrencyCode() != null && t.getAmount() != 0)
                        return new TransactionItem(t);
                    return null;
                }));
    }

    private void addFeesAssetManagerTransaction()
    {
        DocumentType type = new DocumentType("Verg.tung des Verm.gensverwalters");
        this.addDocumentTyp(type);

        Block block = new Block("^Rechnung f.r .*$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.FEES);
            return entry;
        });

        pdfTransaction
                // Leistungen Beträge (EUR)
                // Rechnungsbetrag 6,48
                .section("currency", "amount")
                .match("^Leistungen Betr.ge \\((?<currency>[\\w]{3})\\)$")
                .match("^Rechnungsbetrag *(?<amount>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // Abbuchungsdatum: 02.08.2017
                .section("date")
                .match("^Abbuchungsdatum: (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // Abrechnungszeitraum 01.07.2017 - 31.07.2017
                .section("note").optional()
                .match("^(?<note>Abrechnungszeitraum .*)$")
                .assign((t, v) -> t.setNote(v.get("note")))

                .wrap(TransactionItem::new);

        block.set(pdfTransaction);
    }

    private void addDeliveryInOutBoundTransaction()
    {
        DocumentType type = new DocumentType("Kapitalerh.hung gegen Bareinzahlung");
        this.addDocumentTyp(type);

        Transaction<PortfolioTransaction> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            PortfolioTransaction entry = new PortfolioTransaction();
            entry.setType(PortfolioTransaction.Type.DELIVERY_OUTBOUND);
            return entry;
        });

        Block firstRelevantLine = new Block("^(Einbuchung in Depot|Ausbuchung aus Depot) .* per [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "Einbuchung" change from DELIVERY_OUTBOUND to DELIVERY_INBOUND
                .section("type").optional()
                .match("^(?<type>Einbuchung) in Depot .* per [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}$")
                .assign((t, v) -> {
                    if (v.get("type").equals("Einbuchung"))
                        t.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
                })

                // Ausbuchung aus Depot yyyyyyyyyy per 09.03.2021
                // Nominale ISIN: DE000A3H3MF2 WKN: A3H3MF
                // STK 96 Enapter AG
                // Inhaber-Bezugsrechte
                .section("isin", "wkn", "name", "nameContinued").optional()
                .find("^Ausbuchung aus Depot .* per [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}$")
                .match("^Nominale ISIN: (?<isin>[\\w]{12}) WKN: (?<wkn>[\\w]{6})( .*)?$")
                .match("^STK [\\.,\\d]+ (?<name>.*)$")
                .match("^(?<nameContinued>.*)$")
                .assign((t, v) -> {
                    t.setSecurity(getOrCreateSecurity(v));
                    t.setCurrencyCode(asCurrencyCode(t.getSecurity().getCurrencyCode()));
                    t.setAmount(0L);
                })

                // Einbuchung in Depot yyyyyyyyyy per 09.03.2021
                // Nominale ISIN: DE000A3H3MG0 WKN: A3H3MG Bezugspreis:
                // STK 6 Enapter AG EUR 22,00 p.STK
                // junge Inhaber-Aktien o.N.
                .section("isin", "wkn", "name", "currency", "nameContinued").optional()
                .find("^Einbuchung in Depot .* per [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}$")
                .match("^Nominale ISIN: (?<isin>[\\w]{12}) WKN: (?<wkn>[\\w]{6})( .*)?$")
                .match("^STK [\\.,\\d]+ (?<name>.*) (?<currency>[\\w]{3}) [\\.,\\d]+ .*$")
                .match("^(?<nameContinued>.*)$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // STK 96 Enapter AG
                .section("shares")
                .match("^STK (?<shares>[\\.,\\d]+) .*$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // Einbuchung in Depot yyyyyyyyyy per 09.03.2021
                // Ausbuchung aus Depot yyyyyyyyyy per 09.03.2021
                .section("date")
                .match("^(Einbuchung|Ausbuchung) .* per (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // Bezugspreis EUR 132,00
                // Bruttobetrag EUR 1.012,00
                .section("currency", "amount").optional()
                .match("^(Bezugspreis|Bruttobetrag) (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // Bezugsverhältnis: 16 : 1
                .section("note").optional()
                .match("^(?<note>Bezugsverh.ltnis: .*)$")
                .assign((t, v) -> t.setNote(trim(v.get("note"))))

                .wrap(TransactionItem::new);
    }

    private void addTransferOutTransaction()
    {
        DocumentType type = new DocumentType("Ablauf der Optionsfrist");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.SELL);
            return entry;
        });

        Block firstRelevantLine = new Block("^Referenz\\-Nr\\.: .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Nominale ISIN: DE000HB2KBG9 WKN: HB2KBG Barabfindung
                // STK 6 UniCredit Bank AG EUR 10,00 p.STK
                // HVB Inline 18.05.22 BASF 45-70
                .section("isin", "wkn", "name", "nameContinued", "currency").optional()
                .match("^Nominale ISIN: (?<isin>[\\w]{12}) WKN: (?<wkn>.*) Barabfindung$")
                .match("^STK [\\.,\\d]+ (?<name>.*) (?<currency>[\\w]{3}) .*$")
                .match("^(?<nameContinued>.*)$")
                .assign((t, v) -> {
                    if (v.get("nameContinued").endsWith("p.STK"))
                        v.put("nameContinued", v.get("nameContinued").replace("p.STK", ""));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                // STK 6 UniCredit Bank AG EUR 10,00 p.STK
                .section("shares")
                .match("^STK (?<shares>[\\.,\\d]+) .*$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // Ausbuchung aus Depot 1234567001 per 25.05.2022
                .section("date")
                .match("^Ausbuchung .* per (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                // Zu Gunsten Konto 1234567005 Valuta: 25.05.2022 EUR 60,00
                .section("date", "currency", "amount")
                .match("^Zu Gunsten Konto [\\d]+ Valuta: (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // Ablauf der Optionsfrist
                .section("note").optional()
                .match("^(?<note>Ablauf der Optionsfrist)$")
                .assign((t, v) -> t.setNote(trim(v.get("note"))))

                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        // If we have a gross reinvestment,
        // we set a flag and don't book tax below.
        transaction
                .section("n").optional()
                .match("^Ertragsthesaurierung .*$")
                .match("Steuerliquidit.t (?<n>.*)")
                .assign((t, v) -> type.getCurrentContext().put("noTax", "X"));

        transaction
                // Span. Finanztransaktionssteuer EUR 1,97
                .section("tax", "currency").optional()
                .match("^.* Finanztransaktionssteuer (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("noTax")))
                        processTaxEntries(t, v, type);
                })

                // Kapitalertragsteuer EUR 127,73 -
                .section("tax", "currency").optional()
                .match("^Kapitalertragsteuer (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+) \\-$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("noTax")))
                        processTaxEntries(t, v, type);
                })

                // Kirchensteuer EUR 11,49 -
                .section("tax", "currency").optional()
                .match("^Kirchensteuer (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+) \\-$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("noTax")))
                        processTaxEntries(t, v, type);
                })

                // Solidaritätszuschlag EUR 7,02 -
                .section("tax", "currency").optional()
                .match("^Solidarit.tszuschlag (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+) \\-$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("noTax")))
                        processTaxEntries(t, v, type);
                })

                // Quellensteuer EUR 30,21 -
                // US-Quellensteuer EUR 0,17 -
                .section("withHoldingTax", "currency").optional()
                .match("^(US-)?Quellensteuer (?<currency>[\\w]{3}) (?<withHoldingTax>[\\.,\\d]+) \\-$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("noTax")))
                        processWithHoldingTaxEntries(t, v, "withHoldingTax", type);
                });
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Provision EUR 0,21
                // Provision EUR 0,08 -
                .section("currency", "fee").optional()
                .match("^Provision (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)( \\-)?$")
                .assign((t, v) -> processFeeEntries(t, v, type));
    }

    @Override
    protected long asAmount(String value)
    {
        String language = "de"; //$NON-NLS-1$
        String country = "DE"; //$NON-NLS-1$

        int lastDot = value.lastIndexOf("."); //$NON-NLS-1$
        int lastComma = value.lastIndexOf(","); //$NON-NLS-1$

        // returns the greater of two int values
        if (Math.max(lastDot, lastComma) == lastDot)
        {
            language = "en"; //$NON-NLS-1$
            country = "US"; //$NON-NLS-1$
        }

        return PDFExtractorUtils.convertToNumberLong(value, Values.Amount, language, country);
    }

    @Override
    protected BigDecimal asExchangeRate(String value)
    {
        String language = "de"; //$NON-NLS-1$
        String country = "DE"; //$NON-NLS-1$

        int lastDot = value.lastIndexOf("."); //$NON-NLS-1$
        int lastComma = value.lastIndexOf(","); //$NON-NLS-1$

        // returns the greater of two int values
        if (Math.max(lastDot, lastComma) == lastDot)
        {
            language = "en"; //$NON-NLS-1$
            country = "US"; //$NON-NLS-1$
        }

        return PDFExtractorUtils.convertToNumberBigDecimal(value, Values.Share, language, country);
    }
}
