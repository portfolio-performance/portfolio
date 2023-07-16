package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;

import static name.abuchen.portfolio.util.TextUtil.trim;

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

@SuppressWarnings("nls")
public class FILFondbankPDFExtractor extends AbstractPDFExtractor
{
    public FILFondbankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("FIL Fondsbank GmbH");

        addBuySellTransaction();
        addSellForAccountFeeTransaction();
        addDividendeTransaction();
    }

    @Override
    public String getLabel()
    {
        return "FIL Fondsbank GmbH (Fidelity Group)";
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
                    if ("Verkauf".equals(v.get("type")))
                    {
                        t.setType(PortfolioTransaction.Type.SELL);
                        type.getCurrentContext().putBoolean("sale", true);
                    }
                })

                .oneOf(
                                // @formatter:off
                                // Kauf UBS Emer.Mkt.Soc.Resp.UETFADIS 89,56 EUR 12,6399 USD 7,728
                                // 2540401213 A110QD / LU1048313891 1,090667 USD 02.10.2019 45,394
                                // @formatter:on
                                section -> section
                                        .attributes("name", "currency", "wkn", "isin")
                                        .match("^(Splittkauf|Splitkauf|Wiederanlage|Kauf|Verkauf)( Betrag)? (?<name>.*) [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ (?<currency>[\\w]{3}) ([\\-|\\+])?[\\.,\\d]+$")
                                        .match("^[\\d]+ (?<wkn>[A-Z0-9]{6}) \\/ (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) .*$")
                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))
                                ,
                                // @formatter:off
                                // Splittkauf Betrag hausInvest 20,00 EUR 42,2300 EUR 0,474
                                // 2490116288 DE0009807016 / 980701 04.01.2016 3,818
                                // @formatter:on
                                section -> section
                                        .attributes("name", "currency", "isin", "wkn")
                                        .match("^(Splittkauf|Splitkauf|Wiederanlage|Kauf|Verkauf)( Betrag)? (?<name>.*) [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ (?<currency>[\\w]{3}) ([\\-|\\+])?[\\.,\\d]+$")
                                        .match("^[\\d]+ (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\/ (?<wkn>[A-Z0-9]{6}) .*$")
                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))
                        )

                // @formatter:off
                // Kauf UBS Emer.Mkt.Soc.Resp.UETFADIS 89,56 EUR 12,6399 USD 7,728
                // @formatter:on
                .section("shares")
                .match("^(Splittkauf|Splitkauf|Wiederanlage|Kauf|Verkauf)( Betrag)? .* ([\\-|\\+])?(?<shares>[\\.,\\d]+)$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // @formatter:off
                // Handelsuhrzeit 16:51:24
                // @formatter:on
                .section("time").optional()
                .match("^Handelsuhrzeit (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2})$")
                .assign((t, v) -> type.getCurrentContext().put("time", v.get("time")))

                // @formatter:off
                // 2536717769 A0X97T / LU0446734526 1,125168 USD 16.04.2019 1,334
                // @formatter:on
                .section("date")
                .match("^.* (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\.,\\d]+$")
                .assign((t, v) -> {
                    if (type.getCurrentContext().get("time") != null)
                        t.setDate(asDate(v.get("date"), type.getCurrentContext().get("time")));
                    else
                        t.setDate(asDate(v.get("date")));
                })

                // @formatter:off
                // Abrechnungsbetrag 1,77 EUR
                // Auszahlungsbetrag 0,00 EUR
                // @formatter:on
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

                // @formatter:off
                // Splittkauf Betrag UBS Msci Pacific exJap.U ETF A 1,77 EUR 44,4324 USD 0,045
                // 2536717769 A0X97T / LU0446734526 1,125168 USD 16.04.2019 1,334
                // UBS (Luxembourg) S.A. 1,99 USD 0,0000 EUR 1,379
                // @formatter:on
                .section("currency", "exchangeRate", "fxGross", "fxCurrency").optional()
                .match("^(Splittkauf|Splitkauf|Wiederanlage|Kauf|Verkauf)( Betrag)? .* [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+ [\\w]{3} ([\\-|\\+])?[\\.,\\d]+$")
                .match("^[\\d]+ .* \\/ .* (?<exchangeRate>[\\.,\\d]+) [\\w]{3} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\.,\\d]+$")
                .match("^.* (?<fxGross>[\\.,\\d]+) (?<fxCurrency>[\\w]{3}) [\\.,\\d]+ [\\w]{3} [\\.,\\d]+$")
                .assign((t, v) -> {
                    v.put("baseCurrency", asCurrencyCode(v.get("currency")));
                    v.put("termCurrency", asCurrencyCode(v.get("fxCurrency")));

                    ExtrExchangeRate rate = asExchangeRate(v);
                    type.getCurrentContext().putType(rate);

                    Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));
                    Money gross = rate.convert(asCurrencyCode(v.get("currency")), fxGross);

                    checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                })

                // @formatter:off
                // 2540401213 A110QD / LU1048313891 1,090667 USD 02.10.2019 45,394
                // 2490116288 DE0009807016 / 980701 04.01.2016 3,818
                // @formatter:on
                .section("note").optional()
                .match("^(?<note>[\\d]+) .* \\/ .* [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\.,\\d]+$")
                .assign((t, v) -> t.setNote("Auftrags-Nr. " + v.get("note")))

                // @formatter:off
                // Splittkauf Betrag UBS Msci Pacific exJap.U ETF A 1,77 EUR 44,4324 USD 0,045
                // @formatter:on
                .section("note").optional()
                .match("^(?<note>(Splittkauf|Splitkauf|Wiederanlage)) .*$")
                .assign((t, v) -> {
                    if ("Splittkauf".equals(v.get("note")))
                        v.put("note", "Splitkauf");

                    if (t.getNote() != null)
                        t.setNote(t.getNote() + " | " + v.get("note"));
                    else
                        t.setNote(trim(v.get("note")));
                })

                .conclude(ExtractorUtils.fixGrossValueBuySell())

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
                .assign((t, v) -> type.getCurrentContext().putBoolean("sale", true))

                .oneOf(
                                // @formatter:off
                                // Entgeltbelastung UBS-MSCI WLD.SOC.RESP.A.D.E. 1,01 EUR 98,1753 USD -0,011
                                // 2540818151 A1JA1R / LU0629459743 1,113832 USD 15.10.2019 17,187
                                // @formatter:on
                                section -> section
                                        .attributes("name", "currency", "wkn", "isin")
                                        .match("^(?<note>Entgeltbelastung) (?<name>.*) [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ (?<currency>[\\w]{3}) \\-[\\.,\\d]+$")
                                        .match("^[\\d]+ (?<wkn>[A-Z0-9]{6}) \\/ (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) .*$")
                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))
                                ,
                                // @formatter:off
                                // Entgeltbelastung Templeton Growth (Euro) Fund 25,00 EUR 15,3000 EUR -1,634
                                // 2490658604 LU0114760746 / 941034 04.01.2016 10,848
                                // @formatter:on
                                section -> section
                                        .attributes("name", "currency", "wkn", "isin")
                                        .match("^(?<note>Entgeltbelastung) (?<name>.*) [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ (?<currency>[\\w]{3}) \\-[\\.,\\d]+$")
                                        .match("^[\\d]+ (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\/ (?<wkn>[A-Z0-9]{6}) .*$")
                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))
                        )

                // @formatter:off
                // Entgeltbelastung Templeton Growth (Euro) Fund 25,00 EUR 15,3000 EUR -1,634
                // @formatter:on
                .section("shares")
                .match("^Entgeltbelastung .* ([\\-|\\+])?(?<shares>[\\.,\\d]+)$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // @formatter:off
                // 2540818151 A1JA1R / LU0629459743 1,113832 USD 15.10.2019 17,187
                // @formatter:on
                .section("date")
                .match("^.* (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\.,\\d]+$")
                .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                .oneOf(
                                // @formatter:off
                                // Verwahrentgelt Fonds ohne Abschlussfolgeprovision 2019 1,00 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("amount", "currency")
                                        .match("^Verwahrentgelt Fonds ohne Abschlussfolgeprovision .* (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                                        .assign((t, v) -> {
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                            t.setAmount(asAmount(v.get("amount")));
                                        })
                                ,
                                // @formatter:off
                                // Entgeltbelastung UBS-MSCI WLD.SOC.RESP.A.D.E. 1,01 EUR 98,1753 USD -0,011
                                // @formatter:on
                                section -> section
                                        .attributes("currency", "amount")
                                        .match("^Entgeltbelastung .* (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3}) [\\.,\\d]+ [\\w]{3} \\-[\\.,\\d]+$")
                                        .assign((t, v) -> {
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                            t.setAmount(asAmount(v.get("amount")));
                                        })
                        )

                // @formatter:off
                // Entgeltbelastung UBS-MSCI WLD.SOC.RESP.A.D.E. 1,01 EUR 98,1753 USD -0,011
                // 2540818151 A1JA1R / LU0629459743 1,113832 USD 15.10.2019 17,187
                // @formatter:on
                .section("gross", "currency", "fxCurrency", "exchangeRate", "termCurrency").optional()
                .match("^Entgeltbelastung .* (?<gross>[\\.,\\d]+) (?<currency>[\\w]{3}) [\\.,\\d]+ (?<fxCurrency>[\\w]{3}) \\-[\\.,\\d]+$")
                .match("^[\\d]+ .* \\/ .* (?<exchangeRate>[\\.,\\d]+) (?<termCurrency>[\\w]{3}) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\.,\\d]+$")
                .assign((t, v) -> {
                    v.put("baseCurrency", asCurrencyCode(v.get("currency")));

                    ExtrExchangeRate rate = asExchangeRate(v);
                    type.getCurrentContext().putType(rate);

                    Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("gross")));
                    Money fxGross = rate.convert(asCurrencyCode(v.get("fxCurrency")), gross);

                    checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                })

                // @formatter:off
                // 540818151 A1JA1R / LU0629459743 1,113832 USD 15.10.2019 17,187
                // 2490658604 LU0114760746 / 941034 04.01.2016 10,848
                // @formatter:on
                .section("note1", "note2").optional()
                .match("^(?<note1>Entgeltbelastung) .* (?<gross>[\\.,\\d]+) (?<currency>[\\w]{3}) [\\.,\\d]+ (?<fxCurrency>[\\w]{3}) \\-[\\.,\\d]+$")
                .match("^(?<note2>[\\d]+) .* \\/ .* [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\.,\\d]+$")
                .assign((t, v) -> t.setNote("Auftrags-Nr. " + v.get("note2") + " | " +  v.get("note1")))

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

                .oneOf(
                                // @formatter:off
                                // Entgeltbelastung UBS-MSCI WLD.SOC.RESP.A.D.E. 1,01 EUR 98,1753 USD -0,011
                                // 2540818151 A1JA1R / LU0629459743 1,113832 USD 15.10.2019 17,187
                                // @formatter:on
                                section -> section
                                        .attributes("name", "currency", "wkn", "isin")
                                        .match("^Entgeltbelastung (?<name>.*) [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ (?<currency>[\\w]{3}) \\-[\\.,\\d]+$")
                                        .match("^[\\d]+ (?<wkn>[A-Z0-9]{6}) \\/ (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) .*$")
                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))
                                ,
                                // @formatter:off
                                // Entgeltbelastung Templeton Growth (Euro) Fund 25,00 EUR 15,3000 EUR -1,634
                                // 2490658604 LU0114760746 / 941034 04.01.2016 10,848
                                // @formatter:on
                                section -> section
                                        .attributes("name", "currency", "isin", "wkn")
                                        .match("^Entgeltbelastung (?<name>.*) [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ (?<currency>[\\w]{3}) \\-[\\.,\\d]+$")
                                        .match("^[\\d]+ (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\/ (?<wkn>[A-Z0-9]{6}) .*$")
                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))
                        )

                // @formatter:off
                // Entgeltbelastung Templeton Growth (Euro) Fund 25,00 EUR 15,3000 EUR -1,634
                // @formatter:on
                .section("shares")
                .match("^Entgeltbelastung .* ([\\-|\\+])?(?<shares>[\\.,\\d]+)$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // @formatter:off
                // 2540818151 A1JA1R / LU0629459743 1,113832 USD 15.10.2019 17,187
                // @formatter:on
                .section("date")
                .match("^.* (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\.,\\d]+$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                .oneOf(
                                // @formatter:off
                                // lfd. Vermögensverwaltungsentgelt gem. separatem Auftrag 0,43 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("amount", "currency")
                                        .match("^lfd\\. Verm.gensverwaltungsentgelt gem. separatem Auftrag (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                                        .assign((t, v) -> {
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                            t.setAmount(asAmount(v.get("amount")));
                                        })
                                ,
                                // @formatter:off
                                // Entgeltbelastung UBS-MSCI WLD.SOC.RESP.A.D.E. 1,01 EUR 98,1753 USD -0,011
                                // @formatter:on
                                section -> section
                                        .attributes("currency", "amount")
                                        .match("^Entgeltbelastung .* (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3}) [\\.,\\d]+ [\\w]{3} \\-[\\.,\\d]+$")
                                        .assign((t, v) -> {
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                            t.setAmount(asAmount(v.get("amount")));
                                        })
                        )

                // @formatter:off
                // Entgeltbelastung UBS-MSCI WLD.SOC.RESP.A.D.E. 1,01 EUR 98,1753 USD -0,011
                // 2540818151 A1JA1R / LU0629459743 1,113832 USD 15.10.2019 17,187
                // @formatter:on
                .section("gross", "currency", "fxCurrency", "exchangeRate", "termCurrency").optional()
                .match("^Entgeltbelastung .* (?<gross>[\\.,\\d]+) (?<currency>[\\w]{3}) [\\.,\\d]+ (?<fxCurrency>[\\w]{3}) \\-[\\.,\\d]+$")
                .match("^[\\d]+ .* \\/ .* (?<exchangeRate>[\\.,\\d]+) (?<termCurrency>[\\w]{3}) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\.,\\d]+$")
                .assign((t, v) -> {
                    v.put("baseCurrency", asCurrencyCode(v.get("currency")));

                    ExtrExchangeRate rate = asExchangeRate(v);
                    type.getCurrentContext().putType(rate);

                    Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("gross")));
                    Money fxGross = rate.convert(asCurrencyCode(v.get("fxCurrency")), gross);

                    checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                })

                // @formatter:off
                // 2540818151 A1JA1R / LU0629459743 1,113832 USD 15.10.2019 17,187
                // 2490658604 LU0114760746 / 941034 04.01.2016 10,848
                // @formatter:on
                .section("note").optional()
                .match("^(?<note>[\\d]+) .* \\/ .* [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\.,\\d]+$")
                .assign((t, v) -> t.setNote("Auftrags-Nr. " + v.get("note")))

                // @formatter:off
                // Depotführungsentgelt 2018 25,00 EUR
                // Depotführungsentgelt 25,00 EUR
                // @formatter:on
                .section("note").optional()
                .match("^(?<note>Depotf.hrungsentgelt( [\\d]+)?) [\\.,\\d]+ [\\w]{3}$")
                .assign((t, v) -> {
                    if (t.getNote() != null)
                        t.setNote(t.getNote() + " | " + v.get("note"));
                    else
                        t.setNote(trim(v.get("note")));
                })

                // @formatter:off
                // Verwahrentgelt Fonds ohne Abschlussfolgeprovision 2019 1,00 EUR
                // @formatter:on
                .section("note1", "note2").optional()
                .match("^(?<note1>Verwahrentgelt Fonds) .* (?<note2>[\\d]{4}) .*$")
                .assign((t, v) -> {
                    if (t.getNote() != null)
                        t.setNote(t.getNote() + " | " + v.get("note1") + " " + v.get("note2"));
                    else
                        t.setNote(v.get("note1") + " " + v.get("note2"));
                })

                // @formatter:off
                // lfd. Vermögensverwaltungsentgelt gem. separatem Auftrag 0,09 EUR
                // @formatter:on
                .section("note").optional()
                .match("^(?<note>lfd\\. Verm.gensverwaltungsentgelt) .*$")
                .assign((t, v) -> {
                    if (t.getNote() != null)
                        t.setNote(t.getNote() + " | " + v.get("note"));
                    else
                        t.setNote(trim(v.get("note")));
                })

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
                // @formatter:off
                // Fondsname iShs S&P SmallCap 600 UCITS ET Datum der Ausschüttung 25.07.2018
                // WKN / ISIN A0Q1YY / IE00B2QWCY14 Turnus halbjährlich
                // Ausschüttung               0,289000000 USD 0,03 EUR
                // @formatter:on
                .section("name", "wkn", "isin", "currency")
                .match("^Fondsname (?<name>.*) Datum der Aussch.ttung [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}$")
                .match("^WKN \\/ ISIN (?<wkn>[A-Z0-9]{6}) \\/ (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) .*$")
                .match("^Aussch.ttung( vor Teilfreistellung)? ([\\s]+)?[\\.,\\d]+ [\\w]{3} [\\.,\\d]+ (?<currency>[\\w]{3})")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // @formatter:off
                // Fondsgesellschaft iShares Anteilsbestand per 12.07.2018 0,0930 St.
                // @formatter:on
                .section("shares")
                .match("^.* Anteilsbestand per [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<shares>[\\.,\\d]+) St\\.$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // @formatter:off
                // Fondsname iShs S&P SmallCap 600 UCITS ET Datum der Ausschüttung 25.07.2018
                // @formatter:on
                .section("date")
                .match("^Fondsname .* Datum der Aussch.ttung (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                .oneOf(
                                // @formatter:off
                                // Folgender Betrag wurde zugunsten Ihrer Referenzbankverbindung überwiesen 0,03 EUR
                                // Folgender Betrag wurde auf Ihr Abwicklungskonto überwiesen 15,46 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("amount", "currency")
                                        .match("^Folgender Betrag .* .berwiesen (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                                        .assign((t, v) -> {
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                            t.setAmount(asAmount(v.get("amount")));
                                        })
                                ,
                                // @formatter:off
                                // zur Wiederanlage zur Verfügung stehend 15,67 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("currency", "amount")
                                        .match("^zur Wiederanlage zur Verf.gung stehend (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                                        .assign((t, v) -> {
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                            t.setAmount(asAmount(v.get("amount")));
                                        })
                        )

                // @formatter:off
                // WKN / ISIN A1JSY2 / LU0731782826 Turnus monatlich
                // @formatter:on
                .section("note").optional()
                .match("^WKN \\/ ISIN .* \\/ .* (?<note>Turnus .*)$")
                .assign((t, v) -> t.setNote(trim(v.get("note"))))

                .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);

        block.set(pdfTransaction);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // @formatter:off
                // Kapitalertragsteuer               0,00 EUR
                // @formatter:on
                .section("tax", "currency").optional()
                .match("^Kapitalertrags(s)?teuer ([\\s]+)?(?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!type.getCurrentContext().getBoolean("sale"))
                        processTaxEntries(t, v, type);
                })

                // @formatter:off
                // abgeführte Kapitalertragsteuer 21,15 EUR
                // @formatter:on
                .section("tax", "currency").optional()
                .match("^abgef.hrte Kapitalertrags(s)?teuer ([\\s]+)?(?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (type.getCurrentContext().getBoolean("sale"))
                        processTaxEntries(t, v, type);
                })

                // @formatter:off
                // Solidaritätszuschlag               0,00 EUR
                // @formatter:on
                .section("tax", "currency").optional()
                .match("^Solidarit.tszuschlag ([\\s]+)?(?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!type.getCurrentContext().getBoolean("sale"))
                        processTaxEntries(t, v, type);
                })

                // @formatter:off
                // abgeführter Solidaritätszuschlag 1,16 EUR
                // @formatter:on
                .section("tax", "currency").optional()
                .match("^abgef.hrter Solidarit.tszuschlag ([\\s]+)?(?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (type.getCurrentContext().getBoolean("sale"))
                        processTaxEntries(t, v, type);
                })

                // @formatter:off
                // Kirchensteuer               0,00 EURR
                // @formatter:on
                .section("tax", "currency").optional()
                .match("^Kirchensteuer ([\\s]+)?(?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!type.getCurrentContext().getBoolean("sale"))
                        processTaxEntries(t, v, type);
                })

                // @formatter:off
                // abgeführte Kirchensteuer 0,91 EUR
                // @formatter:on
                .section("tax", "currency").optional()
                .match("^abgef.hrte Kirchensteuer ([\\s]+)?(?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (type.getCurrentContext().getBoolean("sale"))
                        processTaxEntries(t, v, type);
                });
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // @formatter:off
                // Ausgabeaufschlag / Provision (0,00 %) 0,00 EUR
                // @formatter:on
                .section("fee", "currency").optional()
                .match("^Ausgabeaufschlag \\/ Provision .* (?<fee>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // @formatter:off
                // Additional Trading Costs 0,52 EUR
                // @formatter:on
                .section("fee", "currency").optional()
                .match("^Additional Trading Costs (?<fee>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // @formatter:off
                // ETF Transaktionskosten FFB 0,00 EUR
                // @formatter:on
                .section("fee", "currency").optional()
                .match("^ETF Transaktionskosten FFB (?<fee>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type));
    }
}