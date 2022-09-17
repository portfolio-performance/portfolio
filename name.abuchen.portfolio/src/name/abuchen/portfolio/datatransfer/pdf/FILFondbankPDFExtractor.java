package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.pdf.PDFExtractorUtils.checkAndSetGrossUnit;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Money;

@SuppressWarnings("nls")
public class FILFondbankPDFExtractor extends AbstractPDFExtractor
{
    public FILFondbankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("FIL Fondsbank GmbH"); //$NON-NLS-1$

        addBuySellTransaction();
        addSellForAccountFeeTransaction();
        addDividendeTransaction();
    }

    @Override
    public String getLabel()
    {
        return "FIL Fondsbank GmbH (Fidelity Group)"; //$NON-NLS-1$
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("Fondsabrechnung");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^(Splittkauf|Splitkauf|Wiederanlage|Kauf|Verkauf) .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "Verkauf" change from BUY to SELL
                .section("type").optional()
                .match("^(?<type>(Splittkauf|Splitkauf|Wiederanlage|Kauf|Verkauf)) .*$")
                .assign((t, v) -> {                  
                    if (v.get("type").equals("Verkauf"))
                    {
                        t.setType(PortfolioTransaction.Type.SELL);
                        type.getCurrentContext().put("sale", "X");
                    }
                })

                // Kauf UBS Emer.Mkt.Soc.Resp.UETFADIS 89,56 EUR 12,6399 USD 7,728
                // 2540401213 A110QD / LU1048313891 1,090667 USD 02.10.2019 45,394
                .section("note", "name", "currency", "wkn", "isin").optional()
                .match("^(?<note>Splittkauf|Splitkauf|Wiederanlage|Kauf|Verkauf)( Betrag)? (?<name>.*) [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ (?<currency>[\\w]{3}) ([\\-|\\+])?[\\.,\\d]+$")
                .match("^[\\d]+ (?<wkn>.*) \\/ (?<isin>[\\w]{12}) .*$")
                .assign((t, v) -> {
                    if (v.get("note").equals("Splittkauf"))
                        v.put("note", "Splitkauf");

                    t.setSecurity(getOrCreateSecurity(v));
                    t.setNote(v.get("note"));
                })

                // Splittkauf Betrag hausInvest 20,00 EUR 42,2300 EUR 0,474
                // 2490116288 DE0009807016 / 980701 04.01.2016 3,818
                .section("note", "name", "currency", "isin", "wkn").optional()
                .match("^(?<note>Splittkauf|Splitkauf|Wiederanlage|Kauf|Verkauf)( Betrag)? (?<name>.*) [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ (?<currency>[\\w]{3}) ([\\-|\\+])?[\\.,\\d]+$")
                .match("^[\\d]+ (?<isin>[\\w]{12}) \\/ (?<wkn>[\\w]{6}) .*$")
                .assign((t, v) -> {
                    if (v.get("note").equals("Splittkauf"))
                        v.put("note", "Splitkauf");

                    t.setSecurity(getOrCreateSecurity(v));
                    t.setNote(v.get("note"));
                })

                // Kauf UBS Emer.Mkt.Soc.Resp.UETFADIS 89,56 EUR 12,6399 USD 7,728
                .section("shares")
                .match("^(Splittkauf|Splitkauf|Wiederanlage|Kauf|Verkauf)( Betrag)? .* ([\\-|\\+])?(?<shares>[\\.,\\d]+)$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // Handelsuhrzeit 16:51:24
                .section("time").optional()
                .match("^Handelsuhrzeit (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2})$")
                .assign((t, v) -> type.getCurrentContext().put("time", v.get("time")))

                // 2536717769 A0X97T / LU0446734526 1,125168 USD 16.04.2019 1,334
                .section("date")
                .match("^.* (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\.,\\d]+$")
                .assign((t, v) -> {
                    if (type.getCurrentContext().get("time") != null)
                        t.setDate(asDate(v.get("date"), type.getCurrentContext().get("time")));
                    else
                        t.setDate(asDate(v.get("date")));
                })

                // Abrechnungsbetrag 1,77 EUR
                // Auszahlungsbetrag 0,00 EUR
                .section("amount", "currency")
                .match("^(Abrechnungsbetrag|Auszahlungsbetrag) (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                // In the fund financial statements, all amounts are
                // reported exclusively as net amounts. If the net
                // amount is stated in foreign currency, we convert
                // this.

                // Splittkauf Betrag UBS Msci Pacific exJap.U ETF A 1,77 EUR 44,4324 USD 0,045
                // 2536717769 A0X97T / LU0446734526 1,125168 USD 16.04.2019 1,334
                // UBS (Luxembourg) S.A. 1,99 USD 0,0000 EUR 1,379
                .section("currency", "exchangeRate", "fxGross", "fxCurrency").optional()
                .match("^(Splittkauf|Splitkauf|Wiederanlage|Kauf|Verkauf)( Betrag)? .* [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+ [\\w]{3} ([\\-|\\+])?[\\.,\\d]+$")
                .match("^[\\d]+ .* \\/ .* (?<exchangeRate>[\\.,\\d]+) [\\w]{3} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\.,\\d]+$")
                .match("^.* (?<fxGross>[\\.,\\d]+) (?<fxCurrency>[\\w]{3}) [\\.,\\d]+ [\\w]{3} [\\.,\\d]+$")
                .assign((t, v) -> {
                    v.put("baseCurrency", asCurrencyCode(v.get("currency")));
                    v.put("termCurrency", asCurrencyCode(v.get("fxCurrency")));

                    PDFExchangeRate rate = asExchangeRate(v);
                    type.getCurrentContext().putType(rate);

                    Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));
                    Money gross = rate.convert(asCurrencyCode(v.get("currency")), fxGross);

                    checkAndSetGrossUnit(gross, fxGross, t, type);
                })

                // We fix the gross value
                .conclude(PDFExtractorUtils.fixGrossValueBuySell())

                .wrap(t -> {
                    // If we have a sell transaction, we set a flag. So
                    // that in the case of a possible charge by sale,
                    // (Entgeltbelastung), taxes are not included if
                    // they are in the block. Otherwise the flag will be
                    // removed.
                    type.getCurrentContext().remove("sale");

                    return new BuySellEntryItem(t);
                });

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addSellForAccountFeeTransaction()
    {
        DocumentType type = new DocumentType("Entgeltbelastung");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.SELL);
            return entry;
        });

        Block firstRelevantLine = new Block("^Entgeltbelastung .*");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // We have a sell transaction, we set a flag.
                .section("type").optional()
                .match("^(?<type>Entgeltbelastung) .*$")
                .assign((t, v) -> type.getCurrentContext().put("sale", "X"))

                // Entgeltbelastung UBS-MSCI WLD.SOC.RESP.A.D.E. 1,01 EUR 98,1753 USD -0,011
                // 2540818151 A1JA1R / LU0629459743 1,113832 USD 15.10.2019 17,187
                .section("note", "name", "currency", "wkn", "isin").optional()
                .match("^(?<note>Entgeltbelastung) (?<name>.*) [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ (?<currency>[\\w]{3}) \\-[\\.,\\d]+$")
                .match("^[\\d]+ (?<wkn>.*) \\/ (?<isin>[\\w]{12}) .*$")
                .assign((t, v) -> {
                    t.setSecurity(getOrCreateSecurity(v));
                    t.setNote(v.get("note"));
                })

                // Entgeltbelastung Templeton Growth (Euro) Fund 25,00 EUR 15,3000 EUR -1,634
                // 2490658604 LU0114760746 / 941034 04.01.2016 10,848
                .section("note", "name", "currency", "wkn", "isin").optional()
                .match("^(?<note>Entgeltbelastung) (?<name>.*) [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ (?<currency>[\\w]{3}) \\-[\\.,\\d]+$")
                .match("^[\\d]+ (?<isin>[\\w]{12}) \\/ (?<wkn>[\\w]{6}) .*$")
                .assign((t, v) -> {
                    t.setSecurity(getOrCreateSecurity(v));
                    t.setNote(v.get("note"));
                })

                // Entgeltbelastung Templeton Growth (Euro) Fund 25,00 EUR 15,3000 EUR -1,634
                .section("shares")
                .match("^Entgeltbelastung .* ([\\-|\\+])?(?<shares>[\\.,\\d]+)$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // 2540818151 A1JA1R / LU0629459743 1,113832 USD 15.10.2019 17,187
                .section("date")
                .match("^.* (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\.,\\d]+$")
                .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                .oneOf(
                                // Verwahrentgelt Fonds ohne Abschlussfolgeprovision 2019 1,00 EUR
                                section -> section
                                        .attributes("amount", "currency")
                                        .match("^Verwahrentgelt Fonds ohne Abschlussfolgeprovision .* (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                                        .assign((t, v) -> {
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                            t.setAmount(asAmount(v.get("amount")));
                                        })
                                ,
                                // Entgeltbelastung UBS-MSCI WLD.SOC.RESP.A.D.E. 1,01 EUR 98,1753 USD -0,011
                                section -> section
                                        .attributes("currency", "amount")
                                        .match("^Entgeltbelastung .* (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3}) [\\.,\\d]+ [\\w]{3} \\-[\\.,\\d]+$")
                                        .assign((t, v) -> {
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                            t.setAmount(asAmount(v.get("amount")));
                                        })
                        )

                // Entgeltbelastung UBS-MSCI WLD.SOC.RESP.A.D.E. 1,01 EUR 98,1753 USD -0,011
                // 2540818151 A1JA1R / LU0629459743 1,113832 USD 15.10.2019 17,187
                .section("gross", "currency", "fxCurrency", "exchangeRate", "termCurrency").optional()
                .match("^Entgeltbelastung .* (?<gross>[\\.,\\d]+) (?<currency>[\\w]{3}) [\\.,\\d]+ (?<fxCurrency>[\\w]{3}) \\-[\\.,\\d]+$")
                .match("^[\\d]+ .* \\/ .* (?<exchangeRate>[\\.,\\d]+) (?<termCurrency>[\\w]{3}) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\.,\\d]+$")
                .assign((t, v) -> {
                    v.put("baseCurrency", asCurrencyCode(v.get("currency")));

                    PDFExchangeRate rate = asExchangeRate(v);
                    type.getCurrentContext().putType(rate);

                    Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("gross")));
                    Money fxGross = rate.convert(asCurrencyCode(v.get("fxCurrency")), gross);

                    checkAndSetGrossUnit(gross, fxGross, t, type);
                })

                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
        addFeeForAccountBlock(type);
    }

    private void addFeeForAccountBlock(DocumentType type)
    {
        Block block = new Block("^Entgeltbelastung .*$");
        type.addBlock(block);       
        block.set(new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction t = new AccountTransaction();
            t.setType(AccountTransaction.Type.FEES);
            return t;
        })

                // Entgeltbelastung UBS-MSCI WLD.SOC.RESP.A.D.E. 1,01 EUR 98,1753 USD -0,011
                // 2540818151 A1JA1R / LU0629459743 1,113832 USD 15.10.2019 17,187
                .section("name", "currency", "wkn", "isin").optional()
                .match("^Entgeltbelastung (?<name>.*) [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ (?<currency>[\\w]{3}) \\-[\\.,\\d]+$")
                .match("^[\\d]+ (?<wkn>.*) \\/ (?<isin>[\\w]{12}) .*$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // Entgeltbelastung Templeton Growth (Euro) Fund 25,00 EUR 15,3000 EUR -1,634
                // 2490658604 LU0114760746 / 941034 04.01.2016 10,848
                .section("name", "currency", "isin", "wkn").optional()
                .match("^Entgeltbelastung (?<name>.*) [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ (?<currency>[\\w]{3}) \\-[\\.,\\d]+$")
                .match("^[\\d]+ (?<isin>[\\w]{12}) \\/ (?<wkn>[\\w]{6}) .*$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // Entgeltbelastung Templeton Growth (Euro) Fund 25,00 EUR 15,3000 EUR -1,634
                .section("shares")
                .match("^Entgeltbelastung .* ([\\-|\\+])?(?<shares>[\\.,\\d]+)$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // 2540818151 A1JA1R / LU0629459743 1,113832 USD 15.10.2019 17,187
                .section("date")
                .match("^.* (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\.,\\d]+$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                .oneOf(
                                // lfd. Vermögensverwaltungsentgelt gem. separatem Auftrag 0,43 EUR
                                section -> section
                                        .attributes("amount", "currency")
                                        .match("^lfd\\. Verm.gensverwaltungsentgelt gem. separatem Auftrag (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                                        .assign((t, v) -> {
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                            t.setAmount(asAmount(v.get("amount")));
                                        })
                                ,
                                // Entgeltbelastung UBS-MSCI WLD.SOC.RESP.A.D.E. 1,01 EUR 98,1753 USD -0,011
                                section -> section
                                        .attributes("currency", "amount")
                                        .match("^Entgeltbelastung .* (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3}) [\\.,\\d]+ [\\w]{3} \\-[\\.,\\d]+$")
                                        .assign((t, v) -> {
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                            t.setAmount(asAmount(v.get("amount")));
                                        })
                        )

                // Entgeltbelastung UBS-MSCI WLD.SOC.RESP.A.D.E. 1,01 EUR 98,1753 USD -0,011
                // 2540818151 A1JA1R / LU0629459743 1,113832 USD 15.10.2019 17,187
                .section("gross", "currency", "fxCurrency", "exchangeRate", "termCurrency").optional()
                .match("^Entgeltbelastung .* (?<gross>[\\.,\\d]+) (?<currency>[\\w]{3}) [\\.,\\d]+ (?<fxCurrency>[\\w]{3}) \\-[\\.,\\d]+$")
                .match("^[\\d]+ .* \\/ .* (?<exchangeRate>[\\.,\\d]+) (?<termCurrency>[\\w]{3}) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\.,\\d]+$")
                .assign((t, v) -> {
                    v.put("baseCurrency", asCurrencyCode(v.get("currency")));

                    PDFExchangeRate rate = asExchangeRate(v);
                    type.getCurrentContext().putType(rate);

                    Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("gross")));
                    Money fxGross = rate.convert(asCurrencyCode(v.get("fxCurrency")), gross);

                    checkAndSetGrossUnit(gross, fxGross, t, type);
                })

                // Depotführungsentgelt 2018 25,00 EUR
                // Depotführungsentgelt 25,00 EUR
                .section("note").optional()
                .match("^(?<note>Depotf.hrungsentgelt( [\\d]+)?) [\\.,\\d]+ [\\w]{3}$")
                .assign((t, v) -> t.setNote(v.get("note")))

                // Verwahrentgelt Fonds ohne Abschlussfolgeprovision 2019 1,00 EUR
                .section("note1", "note2").optional()
                .match("^(?<note1>Verwahrentgelt Fonds) .* (?<note2>[\\d]{4}) .*$")
                .assign((t, v) -> t.setNote(v.get("note1") + " " + v.get("note2")))

                // lfd. Vermögensverwaltungsentgelt gem. separatem Auftrag 0,09 EUR
                .section("note").optional()
                .match("^(?<note>lfd\\. Verm.gensverwaltungsentgelt) .*$")
                .assign((t, v) -> t.setNote(v.get("note")))

                .wrap(TransactionItem::new));
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("(Aussch.ttungsanzeige|Datum der Aussch.ttung)");
        this.addDocumentTyp(type);

        Block block = new Block("^Fondsname .*$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.DIVIDENDS);
            return entry;
        });

        pdfTransaction
                // Fondsname iShs S&P SmallCap 600 UCITS ET Datum der Ausschüttung 25.07.2018
                // WKN / ISIN A0Q1YY / IE00B2QWCY14 Turnus halbjährlich
                // Ausschüttung               0,289000000 USD 0,03 EUR
                .section("name", "wkn", "isin", "currency")
                .match("^Fondsname (?<name>.*) Datum der Aussch.ttung [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}$")
                .match("^WKN \\/ ISIN (?<wkn>.*) \\/ (?<isin>[\\w]{12}) .*$")
                .match("^Aussch.ttung( vor Teilfreistellung)? ([\\s]+)?[\\.,\\d]+ [\\w]{3} [\\.,\\d]+ (?<currency>[\\w]{3})")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // Fondsgesellschaft iShares Anteilsbestand per 12.07.2018 0,0930 St.
                .section("shares")
                .match("^.* Anteilsbestand per [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<shares>[\\.,\\d]+) St\\.$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // Fondsname iShs S&P SmallCap 600 UCITS ET Datum der Ausschüttung 25.07.2018
                .section("date")
                .match("^Fondsname .* Datum der Aussch.ttung (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                .oneOf(
                                // Folgender Betrag wurde zugunsten Ihrer Referenzbankverbindung überwiesen 0,03 EUR
                                section -> section
                                        .attributes("amount", "currency")
                                        .match("^Folgender Betrag wurde zugunsten Ihrer Referenzbankverbindung .berwiesen (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                                        .assign((t, v) -> {
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                            t.setAmount(asAmount(v.get("amount")));
                                        })
                                ,
                                // zur Wiederanlage zur Verfügung stehend 15,67 EUR
                                section -> section
                                        .attributes("currency", "amount")
                                        .match("^zur Wiederanlage zur Verf.gung stehend (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                                        .assign((t, v) -> {
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                            t.setAmount(asAmount(v.get("amount")));
                                        })
                        )

                .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);

        block.set(pdfTransaction);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Kapitalertragsteuer               0,00 EUR
                .section("tax", "currency").optional()
                .match("^Kapitalertragsteuer ([\\s]+)?(?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("sale")))
                        processTaxEntries(t, v, type);
                })

                // abgeführte Kapitalertragsteuer 21,15 EUR
                .section("tax", "currency").optional()
                .match("^abgef.hrte Kapitalertragsteuer ([\\s]+)?(?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if ("X".equals(type.getCurrentContext().get("sale")))
                        processTaxEntries(t, v, type);
                })

                // Solidaritätszuschlag               0,00 EUR
                .section("tax", "currency").optional()
                .match("^Solidarit.tszuschlag ([\\s]+)(?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("sale")))
                        processTaxEntries(t, v, type);
                })

                // abgeführter Solidaritätszuschlag 1,16 EUR
                .section("tax", "currency").optional()
                .match("^abgeführter Solidarit.tszuschlag ([\\s]+)?(?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if ("X".equals(type.getCurrentContext().get("sale")))
                        processTaxEntries(t, v, type);
                })

                // Kirchensteuer               0,00 EURR
                .section("tax", "currency").optional()
                .match("^Kirchensteuer ([\\s]+)(?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("sale")))
                        processTaxEntries(t, v, type);
                })

                // abgeführte Kirchensteuer 0,91 EUR
                .section("tax", "currency").optional()
                .match("^abgef.hrte Kirchensteuer ([\\s]+)?(?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if ("X".equals(type.getCurrentContext().get("sale")))
                        processTaxEntries(t, v, type);
                });
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Ausgabeaufschlag / Provision (0,00 %) 0,00 EUR
                .section("fee", "currency").optional()
                .match("^Ausgabeaufschlag \\/ Provision .* (?<fee>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Additional Trading Costs 0,52 EUR
                .section("fee", "currency").optional()
                .match("^Additional Trading Costs (?<fee>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // ETF Transaktionskosten FFB 0,00 EUR
                .section("fee", "currency").optional()
                .match("^ETF Transaktionskosten FFB (?<fee>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type));
    }
}